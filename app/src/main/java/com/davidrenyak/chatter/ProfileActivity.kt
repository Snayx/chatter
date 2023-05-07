package com.davidrenyak.chatter

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUser: FirebaseUser

    private lateinit var usernameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUser = auth.currentUser!!

        usernameTextView = findViewById(R.id.usernameTextView)

        loadUsername()


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home_item -> {
                    val intent = Intent(this, ChatActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.logout_item -> {
                    // handle logout action
                    FirebaseAuth.getInstance().signOut() // Firebase authentication logout
                    //FirebaseFirestore.getInstance().firestoreSettings = FirestoreSettings.Builder().setPersistenceEnabled(false).build() // Disable Firestore persistence
                    // Redirect the user to the login screen
                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        usernameTextView = findViewById(R.id.usernameTextView)
        usernameTextView.setOnClickListener { showChangeUsernameDialog() }

        // Set the initial username in the TextView
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid != null) {
            db.collection("users").document(currentUserUid)
                .get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username")
                    if (username != null) {
                        usernameTextView.text = username
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting user document: ", exception)
                }
        }

    }

    private fun loadUsername() {
        db.collection("users")
            .whereEqualTo("userid", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val username = document.getString("username")
                    usernameTextView.text = username
                    break // assuming there is only one matching document
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting user document: ", exception)
            }
    }
    private fun showChangeUsernameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_username, null)
        val editText = dialogView.findViewById<EditText>(R.id.newUsernameEditText)

        AlertDialog.Builder(this)
            .setTitle("Change Username")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, which ->
                val newUsername = editText.text.toString()
                saveNewUsername(newUsername)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun saveNewUsername(newUsername: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .whereEqualTo("username", newUsername)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        val userQuery = db.collection("users").whereEqualTo("userid", currentUser.uid)
                        userQuery.get().addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val userDoc = querySnapshot.documents[0]
                                userDoc.reference.update("username", newUsername)
                                    .addOnSuccessListener {
                                        // Update the usernameTextView with the new username
                                        usernameTextView.text = newUsername
                                        Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e(TAG, "Error updating username: ", exception)
                                        Toast.makeText(this, "Error updating username", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Log.e(TAG, "User document not found")
                                Toast.makeText(this, "Error updating username", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error querying username: ", exception)
                    Toast.makeText(this, "Error querying username", Toast.LENGTH_SHORT).show()
                }
        }
    }

    companion object {
        private const val TAG = "ProfileActivity"
    }
}
