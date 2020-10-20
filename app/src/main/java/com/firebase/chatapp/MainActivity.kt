package com.firebase.chatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*


const val TAG = "MainActivity"
const val ANONYMOUS = "anonymous"
const val DEFAULT_MSG_LENGTH_LIMIT = 1000
const val RC_SIGN_IN = 1
const val IMAGE_PICKER = 2


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var messageList = ArrayList<MessageDetail>()
    private val messageAdapter: MessagingAdapter by lazy {
        MessagingAdapter(messageList, this)

    }
    private lateinit var mUsername: String
    private lateinit var mUid: String
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mPhotoPickerButton: ImageButton
    private lateinit var mMessageEditText: EditText
    private lateinit var mSendButton: Button
    private lateinit var messageListView: RecyclerView

    private lateinit var mFirebaseDatabase: FirebaseDatabase
    private lateinit var mFirebaseDatabaseReference: DatabaseReference
    private lateinit var mUserDatabaseReference: DatabaseReference
    private var childEventListener: ChildEventListener? = null

    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mAuthStateListener: FirebaseAuth.AuthStateListener
    private lateinit var mFirebaseStorage: FirebaseStorage
    private lateinit var mFirebaseStorageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bundle = intent.extras
        bundle?.containsKey("type")?.let {
            when (it) {
                true -> {
                    Log.d("Tag", bundle.getString("type")!!)
                }
                else -> {
                }
            }
        };

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Log and toast

                Log.d(TAG, token + "----------------")
                Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
            })

        mUsername = ANONYMOUS
        mFirebaseDatabase = FirebaseDatabase.getInstance()
        mFirebaseAuth = FirebaseAuth.getInstance()
        mFirebaseStorage = FirebaseStorage.getInstance()
        mFirebaseDatabaseReference = mFirebaseDatabase.reference.child("messages")
        mUserDatabaseReference = mFirebaseDatabase.reference.child("users")
        mFirebaseStorageReference = mFirebaseStorage.reference.child("chat_photos")
        mProgressBar = findViewById(R.id.progressBar)
        messageListView = findViewById(R.id.messageListView)
        mPhotoPickerButton = findViewById(R.id.photoPickerButton)
        mMessageEditText = findViewById(R.id.messageEditText)
        mSendButton = findViewById(R.id.sendButton)
        messageListView.layoutManager = LinearLayoutManager(this)
        messageListView.adapter = messageAdapter
        messageAdapter.notifyDataSetChanged()
        mProgressBar.visibility = ProgressBar.INVISIBLE
        mPhotoPickerButton.setOnClickListener(this)
        mSendButton.setOnClickListener(this)

        mMessageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {


                mSendButton.setOnClickListener {
                    val userChatMessage = MessageDetail(
                        text = messageEditText.text.toString(),
                        photoUrl = null,
                        name = mUsername
                    )
                    mFirebaseDatabaseReference.push().setValue(userChatMessage)
                        .addOnFailureListener {
                            Toast.makeText(this@MainActivity, "no write access", Toast.LENGTH_SHORT)
                                .show()

                        }
                    val user = MessageDetail(userId = mUid)
                    mUserDatabaseReference.push().setValue(user)
                    mMessageEditText.setText("")

                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                mSendButton.isEnabled = char.toString().isNotBlank()
            }


        }

        )


        mAuthStateListener = FirebaseAuth.AuthStateListener { it ->
            val user = it.currentUser
            user?.let {
                //user signed in
                onSignIn(it.displayName!!)
                mUid = it.uid
            } ?: run {
                //user signed out
                onSignOut()
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(
                            listOf(
                                GoogleBuilder().build(),
                                EmailBuilder().build()
                            )
                        )
                        .build(),
                    RC_SIGN_IN
                )
            }


        }


    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.photoPickerButton -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                startActivityForResult(
                    Intent.createChooser(intent, "Complete Action"),
                    IMAGE_PICKER
                )

            }

            else -> {

            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        mFirebaseAuth.addAuthStateListener(mAuthStateListener)
    }


    override fun onPause() {
        super.onPause()
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener)
        messageList.clear()
        messageAdapter.notifyDataSetChanged()
        detachChildEventListener()
    }


    private fun onSignIn(userName: String) {
        this.mUsername = userName
        attachChildEventListener()
    }

    private fun onSignOut() {
        this.mUsername = ANONYMOUS
        messageList.clear()
        messageAdapter.notifyDataSetChanged()
        detachChildEventListener()
    }


    private fun attachChildEventListener() {
        childEventListener?.let {
            mFirebaseDatabaseReference.addChildEventListener(it)
        } ?: run {
            childEventListener = object : ChildEventListener {
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onChildMoved(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onChildChanged(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onChildAdded(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                    val updatedList = snapshot.getValue(MessageDetail::class.java)
                    messageList.add(updatedList!!)
                    messageAdapter.notifyDataSetChanged()
                    messageListView.scrollToPosition(messageAdapter.itemCount - 1)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    Toast.makeText(this@MainActivity, "no write access", Toast.LENGTH_SHORT).show()
                    messageList.removeAt(messageList.size - 1)
                    messageAdapter.notifyDataSetChanged()
                    messageListView.scrollToPosition(messageAdapter.itemCount - 1)
                }

            }
            childEventListener?.let {
                mFirebaseDatabaseReference.addChildEventListener(it)
            }
        }
    }

    private fun detachChildEventListener() {
        childEventListener?.let {
            mFirebaseDatabaseReference.removeEventListener(it)
            childEventListener = null
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this@MainActivity, "login successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "login failed", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            val photoRef = mFirebaseStorageReference.child(uri?.lastPathSegment!!)
            photoRef.putFile(uri).addOnSuccessListener { it ->
                if (it.task.isSuccessful) {
                    val taskUrl = photoRef.downloadUrl
                    if (taskUrl.isSuccessful) {
                        val imageUrl = taskUrl.result
                        imageUrl?.let {
                            val message =
                                MessageDetail(
                                    name = mUsername,
                                    photoUrl = it.toString(),
                                    text = null
                                )
                            mFirebaseDatabaseReference.push().setValue(message)
                                .addOnFailureListener {
                                    Toast.makeText(this, "no write access", Toast.LENGTH_SHORT)
                                        .show()

                                }
                        }
                    }
                }
            }
        }
    }

}