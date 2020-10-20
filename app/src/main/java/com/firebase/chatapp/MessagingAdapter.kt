package com.firebase.chatapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_message.view.*

class MessagingAdapter(
    private val messageList: List<MessageDetail>?,
    private val context: Context
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }


    override fun getItemCount(): Int {
        return messageList?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.onBind(messageList?.get(position)!!)
        }
    }

}


class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val photoImageView: ImageView = view.photoImageView
    private val messageTextView: TextView = view.messageTextView
    private val nameTextView: TextView = view.nameTextView
    internal fun onBind(messageDetail: MessageDetail) {

        messageTextView.text = messageDetail.text
        nameTextView.text = messageDetail.name

        if (messageDetail.photoUrl != null) {
            photoImageView.visibility = View.VISIBLE
            Picasso.get().load(messageDetail.photoUrl).into(photoImageView)
        } else {
            photoImageView.visibility = View.GONE
        }

    }

}