package com.davidrenyak.chatter

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class ConversationActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUserUid: String
    private lateinit var toolbar: Toolbar
    private lateinit var usernameTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var inputText: EditText
    private lateinit var sendButton: Button
    private lateinit var conversationAdapter: ConversationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)


        auth = FirebaseAuth.getInstance()

        currentUserUid = auth.currentUser?.uid ?: throw RuntimeException("No user authenticated")

        toolbar = findViewById(R.id.toolbar)
        usernameTextView = findViewById(R.id.username)
        recyclerView = findViewById(R.id.recycler_view)
        inputText = findViewById(R.id.input_text)
        sendButton = findViewById(R.id.send_button)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val username = intent.getStringExtra("otherUsername")
        val otherUserId = intent.getStringExtra("otherUserId")
        usernameTextView.text = username

        conversationAdapter = ConversationAdapter(currentUserUid, otherUserId)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = conversationAdapter

        sendButton.setOnClickListener {
            val message = inputText.text.toString().trim()
            if (message.isNotEmpty()) {
                val db = FirebaseFirestore.getInstance()
                val chatRef = db.collection("chats").document()
                val chat = hashMapOf(
                    "message" to message,
                    "receiverId" to otherUserId,
                    "senderId" to currentUserUid,
                    "timestamp" to Timestamp.now()
                )
                chatRef.set(chat)
                inputText.setText("")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        conversationAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        conversationAdapter.stopListening()
    }
}

