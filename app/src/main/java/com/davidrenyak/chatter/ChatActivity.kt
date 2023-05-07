package com.davidrenyak.chatter
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ChatActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var chatList: MutableList<Chat>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        chatList = mutableListOf()
        fetchConversations()

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.home_item
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home_item -> {
                    fetchConversations()
                    true
                }

                R.id.profile_item -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    true
                }
                R.id.logout_item -> {
                    // handle logout action
                    FirebaseAuth.getInstance().signOut() // Firebase authentication logout
                    //FirebaseFirestore.getInstance().firestoreSettings = FirestoreSettings.Builder().setPersistenceEnabled(false).build() // Disable Firestore persistence
                    // Redirect the user to the login screen
                    val intent = Intent(this@ChatActivity, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        val newChatButton = findViewById<FloatingActionButton>(R.id.newChatButton)
        newChatButton.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            val editText = EditText(this)

            dialogBuilder.setTitle("Start a new chat")
            dialogBuilder.setMessage("What is the username of the person you want to chat with?")
            dialogBuilder.setView(editText)

            dialogBuilder.setPositiveButton("OK") { _, _ ->
                val otherUsername = editText.text.toString()
                val db = FirebaseFirestore.getInstance()
                val usersRef = db.collection("users")
                val query = usersRef.whereEqualTo("username", otherUsername)
                query.get().addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.size() == 0) {
                        Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                    } else {
                        val otherUserId = querySnapshot.documents[0].id
                        var conversationExists = false
                        for (conversation in chatList) {
                            if (conversation.receiverId == otherUserId || conversation.senderId == otherUserId) {
                                conversationExists = true
                                break
                            }
                        }
                        if (conversationExists) {
                            Toast.makeText(this, "Conversation already exists", Toast.LENGTH_SHORT).show()
                        } else {

                            val intent = Intent(this, ConversationActivity::class.java)
                            intent.putExtra("otherUsername", otherUsername)
                            intent.putExtra("otherUserId", otherUserId)
                            startActivity(intent)
                        }
                    }
                }
            }

            dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            val dialog = dialogBuilder.create()
            dialog.show()
        }

    }
    override fun onResume() {
        super.onResume()
        fetchConversations()
    }
    private fun fetchConversations() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val currentUserUid = auth.currentUser?.uid
        val conversations = mutableMapOf<String, Chat>()

        db.collection("chats")
            .whereEqualTo("senderId", currentUserUid)
            .get()
            .addOnSuccessListener { senderQuerySnapshot ->
                for (senderDocument in senderQuerySnapshot.documents) {
                    val chat = senderDocument.toObject(Chat::class.java)
                    if (chat != null) {
                        if (conversations.containsKey(chat.receiverId)) {

                            val existingChat = conversations[chat.receiverId]!!
                            if (existingChat.timestamp < chat.timestamp) {
                                existingChat.message = chat.message
                                existingChat.timestamp = chat.timestamp
                                existingChat.lastMessage = chat.message
                            }
                        } else {

                            conversations[chat.receiverId] = chat
                            chat.lastMessage = chat.message // set the lastMessage field
                        }
                    }
                }

                db.collection("chats")
                    .whereEqualTo("receiverId", currentUserUid)
                    .get()
                    .addOnSuccessListener { receiverQuerySnapshot ->
                        for (receiverDocument in receiverQuerySnapshot.documents) {
                            val chat = receiverDocument.toObject(Chat::class.java)
                            if (chat != null) {
                                if (conversations.containsKey(chat.senderId)) {

                                    val existingChat = conversations[chat.senderId]!!
                                    if (existingChat.timestamp < chat.timestamp) {
                                        existingChat.message = chat.message
                                        existingChat.timestamp = chat.timestamp
                                        existingChat.lastMessage = chat.message
                                    }
                                } else {

                                    conversations[chat.senderId] = chat
                                    chat.lastMessage = chat.message
                                }
                            }
                        }


                        val recyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
                        recyclerView.layoutManager = LinearLayoutManager(this)
                        val conversationList = ArrayList(conversations.values).sortedByDescending { it.timestamp }
                        val chatAdapter = ChatAdapter(conversationList, currentUserUid!!, db)
                        recyclerView.adapter = chatAdapter
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error querying conversations: ", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error querying conversations: ", exception)
            }
    }


}
