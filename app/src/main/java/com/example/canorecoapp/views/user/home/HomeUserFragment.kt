package com.example.canorecoapp.views.user.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

 class HomeUserFragment : Fragment() {
    private lateinit var binding: FragmentHomeUserBinding
    private lateinit var adapter: NewsAdapter
    private lateinit var adapter2: MaintenanceAdapter
     private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeUserBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ProgressDialogUtils.showProgressDialog(requireContext(),"PLease Wait...")
        auth = FirebaseAuth.getInstance()
        verifyEmail()
        loadUsersInfo()
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


    }
    private fun loadUsersInfo() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("fullName")
                    val image = document.getString("image")
                    val verification = document.getBoolean("verification")


                    binding.textViewUser.setText("$userName")
                    val context = context ?: return@addOnSuccessListener
                    binding.imageViewProfile?.let {
                        Glide.with(context)
                            .load(image)
                            .into(it)
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
                         "",
                         category))
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
     private fun verifyEmail() {
         val user = auth.currentUser
         if (user == null) {
             Log.e("EmailVerification", "User is not logged in. Cannot send verification email.")
             Toast.makeText(requireContext(), "User is not logged in. Cannot send verification email.", Toast.LENGTH_SHORT).show()
             return
         }
         user.sendEmailVerification()
             .addOnCompleteListener { task ->
                 if (task.isSuccessful) {
                     Log.d("EmailVerification", "Verification email sent to ${user.email}")
                     Toast.makeText(requireContext(), "Verification email sent. Please check your inbox.", Toast.LENGTH_SHORT).show()
                     showVerificationDialog()
                 } else {
                     Log.e("EmailVerification", "Failed to send verification email to ${user.email}. Task failed.")
                     Toast.makeText(requireContext(), "Error sending verification email", Toast.LENGTH_SHORT).show()
                 }
             }
             .addOnFailureListener { exception ->
                 Log.e("EmailVerification", "Error sending verification email: ${exception.message}")
                 Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
             }
     }


     @SuppressLint("MissingInflatedId")
     private fun showVerificationDialog() {
         val dialogBuilder = AlertDialog.Builder(requireContext())
         val inflater = layoutInflater
         val dialogView = inflater.inflate(R.layout.dialog_verification, null)
         dialogBuilder.setView(dialogView)
         val btnContinue = dialogView.findViewById<Button>(R.id.btnContinue)
         val btnResend = dialogView.findViewById<Button>(R.id.btnResend)
         val logout = dialogView.findViewById<TextView>(R.id.tvLogOut)
         val dialog = dialogBuilder.create()
         dialog.setCancelable(false)
         dialog.show()

         btnContinue.isEnabled = false
         btnResend.setOnClickListener {
             verifyEmail()
         }
         logout.setOnClickListener {
             val progressDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
             progressDialog.titleText = "Logging out..."
             progressDialog.show()

             auth.signOut()
             Handler(Looper.getMainLooper()).postDelayed({
                 progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                 progressDialog.titleText = "Logged Out!"
                 progressDialog.confirmText = "OK"
                 progressDialog.setConfirmClickListener {
                     findNavController().navigate(R.id.signInFragment)
                     progressDialog.dismiss()
                 }
             }, 1000)
         }

         lifecycleScope.launch {
             while (auth.currentUser?.isEmailVerified == false) {
                 auth.currentUser?.reload()?.addOnCompleteListener { task ->
                     if (task.isSuccessful && auth.currentUser?.isEmailVerified == true) {
                         btnContinue.isEnabled = true
                     }
                 }
                 delay(5000)
             }
             dialog.dismiss()
         }

         btnContinue.setOnClickListener {
             if (auth.currentUser?.isEmailVerified == true) {
                 dialog.dismiss()
             } else {
                 Toast.makeText(requireContext(), "Please verify your email before proceeding.", Toast.LENGTH_SHORT).show()
             }
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
                        "",
                        category))
                    itemCount++
                }
                ProgressDialogUtils.dismissProgressDialog()
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
