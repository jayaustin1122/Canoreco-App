package com.example.canorecoapp.views.user.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.MaintenanceAdapter
import com.example.canorecoapp.adapter.NewsAdapter
import com.example.canorecoapp.databinding.FragmentHomeUserBinding
import com.example.canorecoapp.models.Maintenance
import com.example.canorecoapp.models.News
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity
import smartdevelop.ir.eram.showcaseviewlib.config.PointerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

 class HomeUserFragment : Fragment() {
    private lateinit var binding: FragmentHomeUserBinding
    private lateinit var adapter: NewsAdapter
    private lateinit var adapter2: MaintenanceAdapter
     private lateinit var auth: FirebaseAuth
     private lateinit var loadingDialog: SweetAlertDialog
     private val viewModel: UserViewModel by viewModels()
     override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeUserBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = DialogUtils.showLoading(requireActivity())
        auth = FirebaseAuth.getInstance()
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        loadingDialog.show()
        getNews()
        getMaintenances()
        val areFinish = sharedPreferences.getBoolean("areFinish", false)
        val areGuidesShown = sharedPreferences.getBoolean("areGuidesShown", false)
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModel.loadUserInfo()
        }
        if (!areGuidesShown) {
            binding.scrollView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_MOVE) {
                    showGuide(binding.rvMaintenanceActivities)
                    binding.scrollView.setOnTouchListener(null)
                }
                true
            }
        }

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
        viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
            userInfo?.let {
                binding.apply {
                    // Set the user's profile image
                    binding.textViewUser.text = userInfo.firstName
                    binding.imageViewProfile?.let {
                        Glide.with(requireContext())
                            .load(userInfo.image)
                            .into(it)
                    }
                }
            }
        })


    }

     private fun showGuide(targetView: View) {
         val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

         val builder = GuideView.Builder(this@HomeUserFragment.requireContext())
             .setTitle("Maintenance")
             .setContentText("This is where you can Read and View Maintenance Activities")
             .setGravity(Gravity.center)
             .setDismissType(DismissType.anywhere)
             .setPointerType(PointerType.circle)
             .setTargetView(binding.rvMaintenanceActivities)
             .setGuideListener { view: View ->
                 showAppBarGuide()
             }

         val guideView = builder.build()
         guideView.show()
         sharedPreferences.edit().putBoolean("areFinish", true).apply()
         sharedPreferences.edit().putBoolean("areGuidesShown", true).apply()

     }
     private fun showAppBarGuide() {
         val toolbarGuide = GuideView.Builder(this@HomeUserFragment.requireContext())
             .setTitle("News")
             .setContentText("This is where you can Read and View News Activities")
             .setGravity(Gravity.center)
             .setDismissType(DismissType.anywhere)
             .setPointerType(PointerType.circle)
             .setTargetView(binding.rvLatestNews)
             .setGuideListener { view: View ->

             }
             .build()

         toolbarGuide.show()
     }



     private fun hideShimmerEffect() {
         binding.shimmerViewContainer.stopShimmerAnimation()
     }

     private fun getNews() {
         val freeItems = ArrayList<News>()
         val db = FirebaseFirestore.getInstance()
         val ref = db.collection("news")
             .orderBy("timestamp", Query.Direction.DESCENDING)
             .limit(4)

         ref.get()
             .addOnSuccessListener { documents ->
                 for (document in documents) {
                     val title = document.getString("title") ?: ""
                     val shortDesc = document.getString("content") ?: ""
                     val timestampString = document.getString("timestamp") ?: ""
                     val category = document.getString("category") ?: ""
                     val formattedDate = parseAndFormatDate(timestampString)
                     val imageList = document.get("image") as? List<String> ?: emptyList()
                     val firstImage = imageList.getOrNull(0) ?: ""

                     // Use a local drawable image if the image list is empty
                     val imageToUse = if (firstImage.isEmpty()) {
                         // Get the resource ID of the drawable
                         R.drawable.icon_home
                     } else {
                         firstImage
                     }

                     freeItems.add(News(
                         title,
                         shortDesc,
                         "",
                         imageToUse.toString(), // Ensure the image is passed as a string for the adapter
                         timestampString,
                         formattedDate,
                         "",
                         "",
                         "",
                         "",
                         category
                     ))
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
    public fun parseAndFormatDate(timestampString: String): String {
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

        ref.whereEqualTo("category", "Patalastas ng Power Interruption")
            .get()
            .addOnSuccessListener { documents ->
                var itemCount = 0
                for (document in documents) {
                    if (itemCount >= 4) break
                    val title = document.getString("title") ?: ""
                    val shortDesc = document.getString("content") ?: ""
                    val category = document.getString("category") ?: ""
                    val timestampString = document.getString("timestamp") ?: ""
                    val formattedDate = parseAndFormatDate(timestampString)

                    val imageList = document.get("image") as? List<String> ?: emptyList()
                    val firstImage = imageList.getOrNull(0) ?: ""


                    Log.d("Home", "$firstImage")

                    freeItems.add(Maintenance(
                        title,
                        shortDesc,
                        "",
                        firstImage,
                        timestampString,
                        formattedDate,
                        "",
                        "",
                        "",
                        "",
                        category))
                    itemCount++
                }
                if (loadingDialog.isShowing) {
                    loadingDialog.dismissWithAnimation()
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
