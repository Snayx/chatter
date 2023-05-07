package com.davidrenyak.chatter
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val chatList: List<Chat>, private val currentUserId: String, private val db: FirebaseFirestore) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.user_name)
        val messageTextView: TextView = itemView.findViewById(R.id.message_text)
        val messageTimeTextView: TextView = itemView.findViewById(R.id.message_time)

        init {
            itemView.setOnClickListener {
                val currentChat = chatList[bindingAdapterPosition]
                val isCurrentUserSender = currentChat.senderId == currentUserId
                val otherUserId = if (isCurrentUserSender) {
                    currentChat.receiverId
                } else {
                    currentChat.senderId
                }

                db.collection("users")
                    .whereEqualTo("userid", otherUserId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            val username = document.getString("username")
                            val intent = Intent(itemView.context, ConversationActivity::class.java)
                            intent.putExtra("otherUserId", otherUserId)
                            intent.putExtra("otherUsername", username)
                            itemView.context.startActivity(intent)
                            break // assuming there is only one matching document
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting user document: ", exception)
                    }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item_view, parent, false)
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentChat = chatList[position]
        val isCurrentUserSender = currentChat.senderId == currentUserId
        val otherUserId = if (isCurrentUserSender) {
            currentChat.receiverId
        } else {
            currentChat.senderId
        }
        holder.userNameTextView.text = ""
        db.collection("users")
            .whereEqualTo("userid", otherUserId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val username = document.getString("username")
                    holder.userNameTextView.text = username
                    break
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user document: ", exception)
            }


        holder.messageTextView.text = currentChat.message

        val timestamp = currentChat.timestamp.toDate()
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        holder.messageTimeTextView.text = formatter.format(timestamp)
    }

    override fun getItemCount() = chatList.size
}




