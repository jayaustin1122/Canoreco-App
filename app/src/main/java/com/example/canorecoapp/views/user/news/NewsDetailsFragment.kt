package com.example.canorecoapp.views.user.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.NewsImagesAdapter
import com.example.canorecoapp.databinding.FragmentNewsDetailsBinding
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class NewsDetailsFragment : Fragment() {
    private lateinit var binding: FragmentNewsDetailsBinding
    private var db  = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNewsDetailsBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = arguments?.getString("title")
        val from = arguments?.getString("from")
        ProgressDialogUtils.showProgressDialog(requireContext(),"PLease Wait...")
        Log.d("Firestoress", "Querying document with category of: $from")

        getNewsDetailsByTimestamp(title,from)
        binding.backArrow.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    private fun setupRecyclerView(images: List<String>) {
        val recyclerView = binding.imagesRV
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = NewsImagesAdapter(requireContext(),findNavController(),images)
        Log.d("NewsImagesAdapter", "Loading image from URL: $images")
    }
    private fun getNewsDetailsByTimestamp(title: String?, from: String?) {
        if (title.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid title!", Toast.LENGTH_SHORT).show()
            Log.d("Firestore", "Invalid title provided: $title")
            return
        }
        Log.d("maintenancedetails", "Querying document with title: '$title'")
        val collectionRef = db.collection("news")
        val query = collectionRef.whereEqualTo("timestamp", title)
        val selectedLocations = mutableSetOf<String>()
        query.get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val from = arguments?.getString("from")
                        val docTitle = document.getString("title") ?: ""
                        val images = document.get("image") as? List<String> ?: emptyList()
                        val content = document.getString("content") ?: ""
                        val gawain = document.getString("gawain") ?: ""
                        val date = document.getString("date") ?: ""
                        val startTime = document.getString("startTime") ?: ""
                        val category = document.getString("category") ?: ""
                        val endTime = document.getString("endTime") ?: ""
                        val timestamp = document.getString("timestamp") ?: ""
                        val selectedLocationsList = document.get("selectedLocations") as? List<*>
                        selectedLocationsList?.let {
                            selectedLocations.addAll(it.filterIsInstance<String>())
                        }
                        val formattedDate = parseAndFormatDate(timestamp)
                        val formattedTime = formatTimeRange(startTime, endTime)
                        binding.tvOras.text = Html.fromHtml("<b>ORAS:</b> $formattedTime.")
                        binding.newsTitle.text = docTitle
                        binding.newsDate.text = formattedDate
                        binding.tvGawain.text = Html.fromHtml("<b>GAWAIN:</b> $gawain")
                        binding.tvPetsa.text = Html.fromHtml("<b>PETSA:</b> $date")
                        binding.tvContent.text = content

                        if (category == "Patalastas ng Power Interruption") {
                            binding.viewInMapButton.visibility = View.VISIBLE
                                //  binding.tvLugar.text = Html.fromHtml("<b>APEKTADONG LUGAR:</b> ${selectedLocations.joinToString(", ")}")
                            retrieveLocationName(selectedLocations)
                            binding.viewInMapButton.setOnClickListener {
                                if (selectedLocations.isNotEmpty()) {
                                    val detailsFragment = ViewMapsWithAreasFragment()
                                    val bundle = Bundle()
                                    bundle.putString("Areas", selectedLocations.joinToString(","))
                                    bundle.putString("from", from)
                                    Log.e("from", "$from")
                                    detailsFragment.arguments = bundle
                                    findNavController().navigate(R.id.viewMapsWithAreasFragment, bundle)
                                } else {
                                    Toast.makeText(requireContext(), "No areas to show", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            binding.viewInMapButton.visibility = View.GONE
                            binding.tvLugar.visibility = View.GONE
                            binding.tvGawain.visibility = View.GONE
                            binding.tvPetsa.visibility = View.GONE
                            binding.tvOras.visibility = View.GONE
                            binding.ss.visibility = View.GONE
                        }
                        setupRecyclerView(images)
                        ProgressDialogUtils.dismissProgressDialog()
                    }
                } else {
                    Log.d("Firestore", "No document found with the given title")
                    Toast.makeText(requireContext(), "No document found with the given title", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error retrieving documents", exception)
                Toast.makeText(requireContext(), "Failed to retrieve documents: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun loadJsonFromRaw(resourceId: Int): String? {
        return try {
            val inputStream =this@NewsDetailsFragment.requireContext().resources.openRawResource(resourceId)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e("JSON", "Error reading JSON file from raw resources: ${e.message}")
            null
        }
    }
    private fun retrieveLocationName(selectedLocations: Set<String>) {
        try {
            val jsonData = loadJsonFromRaw(R.raw.filtered_barangayss)
            val jsonObject = JSONObject(jsonData)
            val features = jsonObject.getJSONArray("features")
            val names = mutableListOf<String>()

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val barangayId = properties.getString("ID_3")
                val barangayName = properties.getString("NAME_3")

                if (barangayId in selectedLocations) {
                    names.add(barangayName)
                }
            }

            binding.tvLugar.text = Html.fromHtml("<b>APEKTADONG LUGAR:</b> ${names.joinToString(", ")}")
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
        }
    }



    @SuppressLint("SimpleDateFormat")
     private fun parseAndFormatDate(timestampString: String): String {
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
    private fun formatTimeRange(startTime: String, endTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val startDate = inputFormat.parse(startTime)
            val endDate = inputFormat.parse(endTime)
            if (startDate != null && endDate != null) {
                val formattedStart = outputFormat.format(startDate)
                val formattedEnd = outputFormat.format(endDate)
                val differenceInMillis = endDate.time - startDate.time
                val differenceInMinutes = differenceInMillis / (1000 * 60)
                val hours = differenceInMinutes / 60
                val minutes = differenceInMinutes % 60
                "$formattedStart - $formattedEnd ($hours hrs $minutes mins)"
            } else {
                "$startTime - $endTime"
            }
        } catch (e: Exception) {
            "$startTime - $endTime"
        }
    }

}