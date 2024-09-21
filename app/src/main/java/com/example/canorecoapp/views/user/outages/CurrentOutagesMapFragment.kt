package com.example.canorecoapp.views.user.outages

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentCurrentOutagesMapBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.example.canorecoapp.utils.ProgressDialogUtils.dismissProgressDialog
import com.example.canorecoapp.utils.ProgressDialogUtils.showProgressDialog
import com.example.canorecoapp.views.user.news.DetailsOutageFragment
import com.example.canorecoapp.views.user.news.NewsDetailsFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CurrentOutagesMapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener , GoogleMap.OnPolygonClickListener{

    private lateinit var binding: FragmentCurrentOutagesMapBinding
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val previousLocations = mutableSetOf<String>()
    private lateinit var loadingDialog: SweetAlertDialog

    private fun resetFragmentWithProgress() {
            reloadFragment()
    }

    private fun reloadFragment() {
        findNavController().navigate(R.id.outagesFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCurrentOutagesMapBinding.inflate(inflater, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    private fun loadJsonFromRaw(resourceId: Int): String? {
        return if (isAdded) {
            try {
                val inputStream = requireContext().resources.openRawResource(resourceId)
                inputStream.bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                Log.e("JSON", "Error reading JSON file from raw resources: ${e.message}")
                null
            }
        } else {
            Log.e("JSON", "Fragment not attached to context.")
            null
        }
    }

    private fun showPolygonsBasedOnFirestore() {
        val firestoreReference = FirebaseFirestore.getInstance().collection("outages")

        // Get current date and time
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Assuming UTC, adjust if needed
        timeFormat.timeZone = TimeZone.getTimeZone("UTC")

        val currentDate = dateFormat.format(now)
        val currentTime = timeFormat.format(now)

        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            val selectedLocations = mutableSetOf<String>()

            for (document in querySnapshot.documents) {
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

                Log.d("FirestoreData", "Selected Locationss: $selectedLocations")
            }

            val jsonData = loadJsonFromRaw(R.raw.filtered_barangayss)
            jsonData?.let { parseAndDrawPolygons(it, selectedLocations, "devices") }
                ?: run {
                    Log.e("JSON", "Failed to load JSON data")
                }

        }.addOnFailureListener { exception ->
            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
        }
    }

    override fun onPolygonClick(polygon: Polygon) {
        Log.d("PolygonClick", "$polygon clicked")
        val barangayName = polygon.tag as? String
        if (barangayName != null) {
            val addDataDialog = DetailsOutageFragment()
            val bundle = Bundle()
            bundle.putString("areaCode", barangayName)
            bundle.putString("from", "Current")
            Log.d("PolygonClick", "$barangayName")
            addDataDialog.arguments = bundle
            addDataDialog.show(childFragmentManager, "DetailsOutageFragment")
        } else {
            Log.d("PolygonClick", "Polygon tag is null")
        }
    }

    private fun showDataInRealTime() {
        val db = FirebaseDatabase.getInstance().reference.child("devices")

        // Use a thread-safe collection
        val currentLocations = mutableSetOf<String>()
        val damagedLocations = mutableSetOf<String>()

        db.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleDataChange(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleDataChange(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val documentId = snapshot.key
                if (documentId != null) {
                    currentLocations.remove(documentId)
                    damagedLocations.remove(documentId)
                    updateUI()
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Optional: Handle child moved if needed
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapData", "Error retrieving data from Realtime Database: ${error.message}")
            }

            private fun handleDataChange(snapshot: DataSnapshot) {
                val documentId = snapshot.key
                val status = snapshot.child("status").getValue(String::class.java)

                Log.e("qwer", "${snapshot.key}, Status: $status")

                documentId?.let {
                    currentLocations.add(it)
                    if (status == "damaged") {
                        damagedLocations.add(it)
                    } else {
                        damagedLocations.remove(it)
                    }
                    updateUI()
                }
            }

            private fun updateUI() {
                try {
                    val jsonData = loadJsonFromRaw(R.raw.filtered_barangayss)
                    Log.e("qwer", "$currentLocations")

                    if (jsonData != null) {
                        val locationsToRemove = previousLocations - currentLocations
                        val locationsToAdd = damagedLocations - previousLocations

                        if (locationsToRemove.isNotEmpty()) {
                            resetFragmentWithProgress()
                        } else {
                            parseAndDrawPolygons(jsonData, locationsToAdd, "devices")
                            previousLocations.clear()
                            previousLocations.addAll(currentLocations)
                        }
                    } else {
                        Log.d("JSON", "Failed to load JSON data")
                    }
                } catch (e: Exception) {
                    Log.e("DataProcessing", "Error processing data: ${e.message}")
                }
            }
        })
    }

    private fun parseAndDrawPolygons(jsonData: String, selectedLocations: Set<String>, devices: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val features = jsonObject.getJSONArray("features")
            Log.d("MapData", "Selected Locations: $selectedLocations")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val barangayName = properties.getString("ID_3")
                Log.d("MapData", "Found ID_3: $barangayName")

                if (barangayName in selectedLocations) {
                    Log.d("MapData", "Drawing polygon for ID_3: $barangayName")
                    val geometry = feature.getJSONObject("geometry")

                    if (geometry.getString("type") == "Polygon") {
                        val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)

                        val polygonOptions = PolygonOptions()
                        val latLngList = mutableListOf<LatLng>()

                        for (j in 0 until coordinates.length()) {
                            val coordinate = coordinates.getJSONArray(j)
                            val latLng = LatLng(coordinate.getDouble(1), coordinate.getDouble(0))
                            latLngList.add(latLng)
                            polygonOptions.add(latLng)
                        }

                        // Calculate centroid
                        val centroid = calculateCentroid(latLngList)

                        polygonOptions.strokeColor(Color.RED)
                        polygonOptions.fillColor(Color.argb(100, 255, 0, 0))
                        polygonOptions.strokeWidth(3f)
                        val polygon = gMap?.addPolygon(polygonOptions)
                        polygon?.tag = barangayName
                        Log.d("PolygonTag", "Assigned tag: $barangayName to polygon")

                        // Add marker at the centroid
                        val markerOptions = MarkerOptions()
                            .position(centroid)
                            .title(barangayName)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        val marker = gMap?.addMarker(markerOptions)
                        marker?.tag = "$barangayName, $devices"
                        Log.d("MarkerTag", "Assigned tag: $barangayName to marker")
                    }
                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                loadingDialog.dismiss()
            }, 1000)

           // getCurrentLocation()
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
        }
    }

    private fun calculateCentroid(latLngList: List<LatLng>): LatLng {
        var area = 0.0
        var centroidLat = 0.0
        var centroidLng = 0.0
        val numPoints = latLngList.size

        for (i in 0 until numPoints) {
            val current = latLngList[i]
            val next = latLngList[(i + 1) % numPoints]

            val tempArea = current.longitude * next.latitude - next.longitude * current.latitude
            area += tempArea
            centroidLat += (current.latitude + next.latitude) * tempArea
            centroidLng += (current.longitude + next.longitude) * tempArea
        }

        area /= 2.0
        centroidLat /= (6.0 * area)
        centroidLng /= (6.0 * area)

        return LatLng(centroidLat, centroidLng)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = DialogUtils.showLoading(requireActivity())
        loadingDialog.show()
        checkPermissionLocation()
        binding.fabRefresh.setOnClickListener{
            resetFragmentWithProgress()
        }
        binding.viewListButton.setOnClickListener {
            val detailsFragment = ListOfFutureAndCurrentOutagesFragment()
            val bundle = Bundle().apply {
                putString("from", "current")
            }
            detailsFragment.arguments = bundle
            findNavController().navigate(R.id.listOfFutureAndCurrentOutagesFragment, bundle)
        }

    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val tagData = marker.tag as? String

        if (tagData != null) {
            val dataParts = tagData.split(", ")
            val barangayName = dataParts.getOrNull(0) ?: "Unknown"
            val devices = dataParts.getOrNull(1) ?: "Unknown"

            val addDataDialog = DetailsOutageFragment()
            val bundle = Bundle().apply {
                putString("from", devices)
                putString("areaCode", barangayName)
            }
            Log.d("PolygonClick", "$barangayName")
            addDataDialog.arguments = bundle
            addDataDialog.show(childFragmentManager, "DetailsOutageFragment")

            return true
        } else {
            Log.e("MarkerClick", "Marker tag is null")
            return false
        }
    }

    private fun checkPermissionLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val camarinesNorte = LatLng(14.222795, 122.689153)
        val zoomLevel = 9.4f
        gMap?.setOnMarkerClickListener(this)
        gMap?.setOnPolygonClickListener(this)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(camarinesNorte, zoomLevel))
        showPolygonsBasedOnFirestore()
        showDataInRealTime()

        Handler(Looper.getMainLooper()).postDelayed({
            loadingDialog.dismiss()
        }, 1000)

    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
