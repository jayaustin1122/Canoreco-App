import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    val image = document.getString("image")
                    binding.textViewUser.setText("$userName")
                    val context = context ?: return@addOnSuccessListener
                    binding.imageViewProfile?.let {
                        Glide.with(context)
                            .load(image)
                            .into(it)
                    }
                    Toast.makeText(
                        requireContext(),
                        "Welcome ${userName ?: "User"}!",
                        Toast.LENGTH_SHORT
                    ).show()

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
                var itemCount = 0
                for (document in documents) {
                    if (itemCount >= 4) break
                    val title = document.getString("title") ?: ""
                    val shortDesc = document.getString("content") ?: ""
                    val timestampString = document.getString("timestamp") ?: ""
                    val formattedDate = parseAndFormatDate(timestampString)
                    val imageList = document.get("image") as? List<String> ?: emptyList()
                    val firstImage = imageList.getOrNull(0) ?: ""

                    freeItems.add(News(
                        title,
                        shortDesc,
                        "",
                        firstImage,
                        formattedDate,
                        formattedDate,
                        "",
                        "",
                        "",
                        ""))
                    itemCount++
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




    @SuppressLint("SimpleDateFormat")
    fun parseAndFormatDate(timestampString: String): String {
        return try {
            val timestampSeconds = timestampString.toLongOrNull() ?: return ""
            val date = Date(timestampSeconds * 1000)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy        h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: NumberFormatException) {
            Log.e("Home", "Number format error: ", e)
            ""
        }
    }




    private fun getMaintenances() {
        val freeItems = ArrayList<Maintenance>()
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("news")

        ref.whereEqualTo("category", "Patalastas ng Power Interruption").get()
            .addOnSuccessListener { documents ->
                var itemCount = 0
                for (document in documents) {
                    if (itemCount >= 4) break
                    val title = document.getString("title") ?: ""
                    val shortDesc = document.getString("content") ?: ""
                    val timestampString = document.getString("timestamp") ?: ""
                    val formattedDate = parseAndFormatDate(timestampString)

                    val imageList = document.get("image") as? List<String> ?: emptyList()
                    val firstImage = imageList.getOrNull(0) ?: ""

                    Log.d("Home", ": $title, $shortDesc, $firstImage")

                    freeItems.add(Maintenance(
                        title,
                        shortDesc,
                        "",
                        firstImage,
                        "",
                        formattedDate,
                        "",
                        "",
                        "",
                        ""))
                    itemCount++
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
