package com.davidrenyak.chatter

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Chat(
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    @Exclude var lastMessage: String = ""
) {
    constructor() : this("", "", "", Timestamp.now(), "")
}