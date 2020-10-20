package com.firebase.chatapp

data class MessageDetail constructor(
    var text: String? = null,
    var name: String? = null,
    var photoUrl: String? = null,
    var userId: String? = null

) {
    constructor() : this(null, null, null, null)
}
