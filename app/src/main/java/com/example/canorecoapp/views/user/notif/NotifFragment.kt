package com.example.canorecoapp.views.user.notif

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.NotifDetailsAdapter
import com.example.canorecoapp.databinding.FragmentNotifBinding
import com.example.canorecoapp.models.Notif
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class NotifFragment : Fragment() {
    private lateinit var loadingDialog: SweetAlertDialog
   private lateinit var binding : FragmentNotifBinding
    private lateinit var notifList: ArrayList<Notif>
    private lateinit var newsAdapter: NotifDetailsAdapter
    private lateinit var deletedNotifications: List<Notif>
    private var db  = Firebase.firestore
    private var from: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotifBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun handleBackNavigation() {

        when (from) {
            "member" -> findNavController().navigate(R.id.userHolderFragment)
            "linemen" -> {
//                bundle.putInt("selectedFragmentId", null ?: R.id.navigation_services)
                findNavController().navigate(R.id.adminHolderFragment)
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notifList = ArrayList()
        newsAdapter = NotifDetailsAdapter(requireContext(), findNavController(), notifList)
        binding.recyclerNews.adapter = newsAdapter
        getAllNews()
        setupSwipeToDelete()
        arguments?.let {
            from = it.getString("from")
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackNavigation()
                }
            })
        binding.backButton.setOnClickListener {
            handleBackNavigation()
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

    private fun clearAllNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificationsRef = db.collection("users")
            .document(userId)
            .collection("notifications")

        notificationsRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    deletedNotifications = querySnapshot.documents.map { document ->
                        Notif(
                            title = document.getString("title") ?: "",
                            text = document.getString("text") ?: "",
                            timestamp = document.get("timestamp").toString(),
                            status = document.getBoolean("status") ?: false
                        )
                    }
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                Log.d("NotificationService", "Notification ${document.id} deleted successfully")
                                getAllNews()
                                loadingDialog.dismiss()
                                
                            }
                            .addOnFailureListener { e ->
                                Log.e("NotificationService", "Failed to delete notification ${document.id}", e)
                            }
                    }
                    showUndoSnackbar()
                } else {
                    Log.d("NotificationService", "No notifications found to delete.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("NotificationService", "Error fetching notifications", e)
            }
    }
    private fun showUndoSnackbar() {
        val snackbar = Snackbar.make(binding.recyclerNews, "All notifications deleted", Snackbar.LENGTH_LONG)
        snackbar.setAction("UNDO") {
            restoreDeletedNotifications()
        }
        snackbar.show()
    }
    private fun restoreDeletedNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notificationsRef = db.collection("users")
            .document(userId)
            .collection("notifications")

        // Add back the deleted notifications
        deletedNotifications.forEach { notif ->
            notificationsRef.add(
                mapOf(
                    "title" to notif.title,
                    "text" to notif.text,
                    "timestamp" to notif.timestamp.toLong(),
                    "status" to notif.status
                )
            ).addOnSuccessListener {
                Log.d("NotificationService", "Notification restored successfully")
            }.addOnFailureListener { e ->
                Log.e("NotificationService", "Failed to restore notification", e)
            }
        }

        // Refresh the notification list
        getAllNews()
    }
    private fun setupSwipeToDelete() {
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
                val snackbar = Snackbar.make(binding.recyclerNews, "Notification deleted", Snackbar.LENGTH_LONG)
                snackbar.setAction("UNDO") {
                    notifList.add(position, notifToDelete)
                    newsAdapter.notifyItemInserted(position)
                }
                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(snackbar: Snackbar?, event: Int) {
                        super.onDismissed(snackbar, event)
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            deleteNotificationFromFirestore(notifToDelete.timestamp)
                        }
                    }
                })
                snackbar.show()
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
                    getAllNews()
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
                                val news = Notif(title, text, timestamp.toString(), status)
                                notifList.add(news)
                            } else if (timestamp is Double) {
                                val news = Notif(title, text, timestamp.toString(), status)
                                notifList.add(news)
                            } else {
                                val news = Notif(title, text, timestamp.toString(), status)
                                notifList.add(news)
                            }
                        }
                        notifList.reverse()
                        Log.d("NewsFragment", "Fetched ${notifList.size} news items")
                        binding.tvClearAll.setOnClickListener {
                            loadingDialog = DialogUtils.showLoading(requireActivity())
                            loadingDialog.show()
                            clearAllNotifications()

                        }
                        binding.recyclerNews.visibility = View.VISIBLE

                        lifecycleScope.launchWhenResumed {
                            newsAdapter = NotifDetailsAdapter(requireContext(), findNavController(), notifList)
                            binding.recyclerNews.setHasFixedSize(true)
                            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                            binding.recyclerNews.layoutManager = layoutManager
                            binding.recyclerNews.adapter = newsAdapter
                            newsAdapter.notifyDataSetChanged()
                        }
                    } else {
                        Log.d("NewsFragment", "No documents found")
                        // Show the "No Notifications" text and hide the RecyclerView
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.imgEmpty.visibility = View.VISIBLE
                        binding.recyclerNews.visibility = View.GONE
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