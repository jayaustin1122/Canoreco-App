package com.example.canorecoapp.views.user.notif

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.canorecoapp.adapter.NotifDetailsAdapter
import com.example.canorecoapp.databinding.FragmentNotifBinding
import com.example.canorecoapp.models.Notif
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class NotifFragment : Fragment() {
   private lateinit var binding : FragmentNotifBinding
    private lateinit var notifList: ArrayList<Notif>
    private lateinit var newsAdapter: NotifDetailsAdapter
    private var db  = Firebase.firestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotifBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notifList = ArrayList()
        newsAdapter = NotifDetailsAdapter(requireContext(), findNavController(), notifList)
        binding.recyclerNews.adapter = newsAdapter
        getAllNews()
        binding.backButton.setOnClickListener {
            findNavController().apply {
                navigateUp()
            }
        }
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    newsAdapter.filter.filter(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    newsAdapter.filter.filter(newText)
                }
                return false
            }
        })
    }
    private fun setupSwipeToDelete(timestamp: String) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notifToDelete = notifList[position]
                notifList.removeAt(position)
                newsAdapter.notifyItemRemoved(position)
                Toast.makeText(requireContext(), "Notification deleted", Toast.LENGTH_SHORT).show()
                deleteNotificationFromFirestore(timestamp)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerNews)
    }

    private fun deleteNotificationFromFirestore(timestamp: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val notificationsRef = db.collection("users")
                .document(user.uid)
                .collection("notifications")
                .document(timestamp)

            notificationsRef.delete()
                .addOnSuccessListener {
                    Log.d("NotifFragment", "Notification successfully deleted!")
                }
                .addOnFailureListener { e ->
                    Log.e("NotifFragment", "Error deleting notification: ${e.message}")
                }
        }
    }

    private fun getAllNews() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            val notificationsRef = db.collection("users")
                .document(user.uid)
                .collection("notifications")

            notificationsRef.get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        notifList.clear()
                        for (document in querySnapshot.documents) {
                            val title = document.getString("title") ?: ""
                            val text = document.getString("text") ?: ""
                            val status = document.getBoolean("status") ?: false
                            val timestamp = document.get("timestamp")
                            if (timestamp is Long) {
                                val news = Notif(title,text, timestamp.toString() , status)
                                notifList.add(news)
                                setupSwipeToDelete(timestamp.toString())
                            } else if (timestamp is Double) {
                                val news = Notif(title,text, timestamp.toString() , status)
                                notifList.add(news)
                                setupSwipeToDelete(timestamp.toString())
                            } else {
                                val news = Notif(title,text, timestamp.toString() , status)
                                notifList.add(news)
                                setupSwipeToDelete(timestamp.toString())
                            }


                        }
                        notifList.reverse()
                        Log.d("NewsFragment", "Fetched ${notifList.size} news items")

                        lifecycleScope.launchWhenResumed {
                            newsAdapter = NotifDetailsAdapter(requireContext(), findNavController(), notifList)
                            binding.recyclerNews.setHasFixedSize(true)
                            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                            binding.recyclerNews.layoutManager = layoutManager
                            binding.recyclerNews.adapter = newsAdapter
                        }
                    } else {
                        Log.d("NewsFragment", "No documents found")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("NewsFragment", "Error fetching news: ${exception.message}")
                    Toast.makeText(requireContext(), "Error fetching news", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }



}