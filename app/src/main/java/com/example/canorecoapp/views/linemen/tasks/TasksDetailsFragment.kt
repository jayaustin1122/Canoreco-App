package com.example.canorecoapp.views.linemen.tasks

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentTasksDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore


class TasksDetailsFragment  : BottomSheetDialogFragment() {

    private lateinit var binding : FragmentTasksDetailsBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTasksDetailsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        val locationName = args?.getString("locationName")
        val latitude = args?.getDouble("latitude")
        val longitude = args?.getDouble("longitude")
        val startTime = args?.getString("startTime")
        val endTime = args?.getString("endTime")
        val id = args?.getString("id")
        val assigned = args?.getString("assigned")
        val status = args?.getString("status")?.capitalize()

        // Set values to UI elements
        binding.tvLocationName.text = locationName
        binding.tvLangLat.text = "${latitude} ${longitude}"


        when (status) {
            "Damaged" -> {
                binding.tvStartTime.visibility =View.GONE
                binding.tvEndTime.visibility =View.GONE
                binding.btnGetTask.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        loadUsersInfo(id)


                    }
                }
                binding.tvStatus.text = "Damaged"
            }
            "Under repair" -> {
                loadUsersInfos(assigned,startTime,endTime,id)

            }
            else -> {
                binding.tvStartTime.visibility =View.GONE
                binding.tvEndTime.visibility =View.GONE
                binding.btnGetTask.visibility = View.GONE
                binding.tvStatus.visibility = View.GONE
                binding.tvStatusLabel.visibility = View.GONE
                binding.tvStartTimeLabel.visibility = View.GONE
                binding.tvEndTimeLabel.visibility = View.GONE
                binding.tvEndTimeLabel.visibility = View.GONE
            }
        }
    }
    private fun loadUsersInfos(assigned: String?, startTime: String?, endTime: String?, id: String?) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->

                    // Get user data
                    val userName = document.getString("firstName")
                    val lastName = document.getString("lastName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")
                    val email = document.getString("email")
                    val password = document.getString("password")

                    // Log the values for debugging
                    Log.d("UserInfo", "Assigned: $assigned, UserName: $userName")


                    if (assigned == userName) {


                        binding.btnUpdateStatus.visibility = View.VISIBLE
                        binding.tvAssigned.visibility = View.VISIBLE
                        binding.btnGetTask.visibility = View.GONE
                        binding.tvStatus.text = "Under Repair"
                        binding.tvStartTime.text = startTime
                        binding.tvEndTime.text = endTime
                        binding.tvAssigned.text = assigned


                        binding.btnUpdateStatus.setOnClickListener {
                            askAndUpdateStatus(id!!)

                        }
                    }else {

                        binding.btnUpdateStatus.visibility = View.GONE
                        binding.tvAssigned.visibility = View.VISIBLE
                        binding.btnGetTask.visibility = View.GONE
                        binding.tvStatus.text = "Under Repair"
                        binding.tvStartTime.text = startTime
                        binding.tvEndTime.text = endTime
                        binding.tvAssigned.text = assigned
                    }

                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        requireContext(),
                        "Error Loading User Data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(
                requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun askAndUpdateStatus(id: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Power Restoration")
        builder.setMessage("Is the power restored?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()

            val database: DatabaseReference = FirebaseDatabase.getInstance().reference
            val deviceRef = database.child("devices/$id")
            val updates = mapOf<String, Any?>(
                "status" to "working",
                "assigned" to "",
                "date" to "",
                "endTime" to "",
                "startTime" to "",
                "endTime" to "",
            )
            deviceRef.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Status updated to 'Restored'", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    val bundle = Bundle().apply {

                        val selectedFragmentId = R.id.navigation_notificationl_linemen

                        putInt("selectedFragmentId", selectedFragmentId)
                    }

                    findNavController().navigate(R.id.adminHolderFragment, bundle)

                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Close the dialog
        }
        builder.show()
    }


    private fun loadUsersInfo(id: String?) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->

                    val userName = document.getString("firstName")
                    val lastName = document.getString("lastName")
                    val contact = document.getString("phone")
                    val image = document.getString("image")
                    val email = document.getString("email")
                    val password = document.getString("password")
                    Log.d("UploadData1", "saveUserTaskInDevice called with userName: $userName and id: $id")
                    saveUserTaskInDevice(userName,id)

                }
                .addOnFailureListener { exception ->

                    Toast.makeText(
                        requireContext(),
                        "Error Loading User Data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {

            Toast.makeText(
                requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveUserTaskInDevice(userName: String?, id: String?) {
        val detailsDialog = FragmentSetTimeDateFragment()
        val bundle = Bundle().apply {
            putString("userName", userName)
            putString("id", id )
            Log.d("UploadData1", "pass: $userName and id: $id")

        }
        detailsDialog.arguments = bundle
        detailsDialog.show(childFragmentManager, "FragmentSetTimeDateFragment")

    }


}