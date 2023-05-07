package com.davidrenyak.chatter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ConversationAdapter(private val currentUserId: String, private val otherUserId: String?) :
    RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<Chat>()

    private val firestore = FirebaseFirestore.getInstance()

    private val query = firestore.collection("chats")
        .whereIn("senderId", listOf(currentUserId, otherUserId))
        .whereIn("receiverId", listOf(currentUserId, otherUserId))
        .orderBy("timestamp", Query.Direction.ASCENDING)
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = if (viewType == VIEW_TYPE_OUTGOING) {
            inflater.inflate(R.layout.message_background_outgoing, parent, false)
        } else {
            inflater.inflate(R.layout.message_background_to, parent, false)
        }
        print(query)
        return MessageViewHolder(itemView)

    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING
    }

    @SuppressLint("NotifyDataSetChanged")
    fun startListening() {
        listenerRegistration = query.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.w(TAG, "Listen failed.", exception)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                messages.clear()
                for (document in snapshot.documents) {
                    val message = document.toObject(Chat::class.java)
                    if (message != null) {
                        messages.add(message)
                    }
                }
                notifyDataSetChanged()
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(message: Chat) {
            messageText.text = message.message
        }
    }

    companion object {
        private const val TAG = "ConversationAdapter"
        private const val VIEW_TYPE_OUTGOING = 0
        private const val VIEW_TYPE_INCOMING = 1
    }
}
