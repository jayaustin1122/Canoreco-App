package com.example.canorecoapp.views.user.outages

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.ListOfOutagesAdapter
import com.example.canorecoapp.databinding.FragmentListOfFutureAndCurrentOutagesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ListOfFutureAndCurrentOutagesFragment : Fragment() {
    private lateinit var binding: FragmentListOfFutureAndCurrentOutagesBinding
    private lateinit var adapter: ListOfOutagesAdapter
    private lateinit var devicesRef: DatabaseReference

    private val barangaysWithDamagedDevices = mutableListOf<String>()
    private val firestoreOutages = mutableSetOf<String>()
    val from = arguments?.getString("from")
    private var from2: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListOfFutureAndCurrentOutagesBinding.inflate(layoutInflater)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val from = arguments?.getString("from")
        arguments?.let {
            from2 = it.getString("from2")
        }
        if (from == "current"){
            retrieveAllCurrentOutages()
            retrieveDeviceWithDamaged()
        }
        if (from =="future")
        {
            retrieveFutureOutages()
        }

        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    adapter.filter.filter(query)  // Filter the adapter data
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    adapter.filter.filter(newText)  // Filter while typing
                }
                return false
            }
        })
        loadUsersInfo()

    }
    private fun handleBackNavigation() {
        val bundle = Bundle().apply {
            putString("from", from2)
            putInt("selectedFragmentId", null ?: R.id.navigation_Home)
        }
        findNavController().previousBackStackEntry?.savedStateHandle?.set("bundleKey", bundle)
        findNavController().navigateUp()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userType = document.getString("userType")

                    when (userType) {
                        "member" -> {
                            binding.backButton.setOnClickListener {
                             handleBackNavigation()
                            }
                            requireActivity().onBackPressedDispatcher.addCallback(
                                viewLifecycleOwner,
                                object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                    handleBackNavigation()
                                    }
                                })
                        }

                        "linemen" -> {
                            binding.backButton.setOnClickListener {
                                val bundle = Bundle().apply {
                                    findNavController().navigate(R.id.adminHolderFragment)
                                }
                                findNavController().navigate(R.id.adminHolderFragment, bundle)
                            }
                            requireActivity().onBackPressedDispatcher.addCallback(
                                viewLifecycleOwner,
                                object : OnBackPressedCallback(true) {
                                    override fun handleOnBackPressed() {
                                        findNavController().navigate(R.id.adminHolderFragment)
                                    }
                                })
                        }

                        else -> {
                            Toast.makeText(
                                requireContext(),
                                "Unknown user type",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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

    private fun retrieveFutureOutages() {
            val firestoreReference = FirebaseFirestore.getInstance().collection("outages")

            firestoreReference.get().addOnSuccessListener { querySnapshot ->
                val selectedLocations = mutableSetOf<String>()

                val currentDate = Date() // Current date
                for (document in querySnapshot.documents) {
                    val selectedLocationsList = document.get("selectedLocations") as? List<*>
                    val dateString = document.getString("date")
                    val documentDate = dateString?.let { parseDate(it) }

                    if (documentDate != null && documentDate > currentDate) {
                        selectedLocationsList?.let {
                            selectedLocations.addAll(it.filterIsInstance<String>())
                        }
                    }
                    Log.d("FirestoreData", "Selected Locations: $selectedLocations")

                }
                val from = arguments?.getString("from")
                lifecycleScope.launchWhenResumed {
                    adapter = ListOfOutagesAdapter(
                        this@ListOfFutureAndCurrentOutagesFragment.requireContext(),
                        findNavController(),
                        selectedLocations.toList(),
                        from
                    )
                    binding.rvListOutages.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    binding.rvListOutages.layoutManager = layoutManager
                    binding.rvListOutages.adapter = adapter
                }



            }.addOnFailureListener { exception ->
                Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
            }

    }

    private fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        } catch (e: ParseException) {
            null
        }
    }
    private fun retrieveDeviceWithDamaged() {
        devicesRef = FirebaseDatabase.getInstance().getReference("devices")
        devicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                barangaysWithDamagedDevices.clear()
                snapshot.children.forEach { deviceSnapshot ->
                    val status = deviceSnapshot.child("status").getValue(String::class.java) ?: "unknown"
                    if (status == "damaged") {
                        val barangay = deviceSnapshot.child("id").getValue(String::class.java) ?: ""
                        barangaysWithDamagedDevices.add(barangay)
                    }
                }
                Log.d("DeviceNotifFragment", "Updated barangaysWithDamagedDevices: $barangaysWithDamagedDevices")
                updateRecyclerView()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DeviceNotifFragment", "Failed to read device statuses", error.toException())
            }
        })
    }

    private fun retrieveAllCurrentOutages() {
        val firestoreReference = FirebaseFirestore.getInstance().collection("outages")
        val from = arguments?.getString("from")
        // Get current date and time
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Assuming UTC, adjust if needed
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")

        val currentDate = dateFormat.format(now)
        val currentTime = timeFormat.format(now)

        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            val selectedLocations = mutableListOf<String>()

            for (document in querySnapshot.documents) {
                if (document == null) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.imgEmpty.visibility = View.VISIBLE
                    continue
                }
                val date = document.getString("date") ?: ""
                val startTime = document.getString("startTime") ?: ""
                val endTime = document.getString("endTime") ?: ""
                val selectedLocationsList = document.get("selectedLocations") as? List<*>
                val isDateMatch = date == currentDate
                val isTimeInRange = startTime >= currentTime && currentTime <= endTime

                if (isDateMatch && isTimeInRange) {
                    selectedLocationsList?.let {
                        selectedLocations.addAll(it.filterIsInstance<String>())
                    }
                }

                Log.d("FirestoreDataCurrent", "Selected Locations: $selectedLocations")

            }
            lifecycleScope.launchWhenResumed {

                adapter = ListOfOutagesAdapter(
                    this@ListOfFutureAndCurrentOutagesFragment.requireContext(),
                    findNavController(),
                    selectedLocations,
                    from
                )
                binding.rvListOutages.setHasFixedSize(true)
                val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                binding.rvListOutages.layoutManager = layoutManager
                binding.rvListOutages.adapter = adapter
            }
            firestoreOutages.clear()
            firestoreOutages.addAll(selectedLocations)
            updateRecyclerView()
        }.addOnFailureListener { exception ->
            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
        }
    }

    private fun updateRecyclerView() {
        lifecycleScope.launchWhenResumed {
            val combinedList = (barangaysWithDamagedDevices + firestoreOutages).toList().sorted()

            if (combinedList.isNullOrEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.imgEmpty.visibility = View.VISIBLE
            } else {

                adapter = ListOfOutagesAdapter(
                    this@ListOfFutureAndCurrentOutagesFragment.requireContext(),
                    findNavController(),
                    combinedList,
                    from
                )
                binding.rvListOutages.setHasFixedSize(true)
                val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                binding.rvListOutages.layoutManager = layoutManager
                binding.rvListOutages.adapter = adapter
            }
        }
    }

}
