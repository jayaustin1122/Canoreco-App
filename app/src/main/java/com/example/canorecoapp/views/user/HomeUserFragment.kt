import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.MaintenanceAdapter
import com.example.canorecoapp.adapter.NewsAdapter
import com.example.canorecoapp.databinding.FragmentHomeUserBinding
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.models.News
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class HomeUserFragment : Fragment() {
    private lateinit var binding: FragmentHomeUserBinding
    private lateinit var adapter: NewsAdapter
    private lateinit var adapter2: MaintenanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeUserBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getNews()
        getMaintenances()


        binding.tvViewAllNews.setOnClickListener {
            findNavController().apply {
                navigate(R.id.newsFragment)
            }
        }

        binding.tvViewAllMaintenance.setOnClickListener {
            findNavController().apply {
                navigate(R.id.maintenanceListFragment)
            }
        }
        loadUsersInfo()

    }
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("fullName")
                    binding.textViewUser.setText("$userName")


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



    private fun getNews() {
        val freeItems = ArrayList<News>()
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("news")

        ref.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("Title") ?: ""
                    val shortDesc = document.getString("Short Description") ?: ""
                    val date = document.getString("Date") ?: ""

                    // Get the array of images from Firestore
                    val imageList = document.get("image") as? List<String> ?: emptyList()
                    val firstImage = imageList.getOrNull(0) ?: ""

                    Log.d("Home", ": $title, $shortDesc, $date, $firstImage")

                    val timestamp = when (val value = document.get("timestamp")) {
                        is Double -> value.toString()
                        is String -> value
                        else -> ""
                    }
                    Log.d("HOme", ": $title, $shortDesc, $date, $firstImage")
                    freeItems.add(News(title, shortDesc, "", firstImage, timestamp, date, "", "", "", ""))
                }
                lifecycleScope.launchWhenResumed {
                    adapter = NewsAdapter(this@HomeUserFragment.requireContext(), findNavController(), freeItems)
                    binding.rvLatestNews.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.rvLatestNews.layoutManager = layoutManager
                    binding.rvLatestNews.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Home", "Error getting documents: ", exception)
            }
    }

    private fun getMaintenances() {
        val freeItems = ArrayList<Maintenance>()
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("news")

        ref.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val title = document.getString("Title") ?: ""
                    val shortDesc = document.getString("Short Description") ?: ""
                    val date = document.getString("Date") ?: ""

                    // Get the array of images from Firestore
                    val imageList = document.get("image") as? List<String> ?: emptyList()
                    val firstImage = imageList.getOrNull(0) ?: "" // Get image at index 0 or default to an empty string

                    Log.d("Home", ": $title, $shortDesc, $date, $firstImage")

                    freeItems.add(Maintenance(title, shortDesc, "", firstImage, "", date, "", "", "", ""))
                }

                lifecycleScope.launchWhenResumed {
                    adapter2 = MaintenanceAdapter(this@HomeUserFragment.requireContext(), findNavController(), freeItems)
                    binding.rvMaintenanceActivities.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.rvMaintenanceActivities.layoutManager = layoutManager
                    binding.rvMaintenanceActivities.adapter = adapter2
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Home", "Error getting documents: ", exception)
            }
    }



}
