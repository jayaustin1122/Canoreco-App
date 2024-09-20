
package com.example.canorecoapp.views.linemen.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentHomeLineMenBinding
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.example.canorecoapp.views.linemen.tasks.TasksDetailsFragment
import com.example.canorecoapp.views.user.news.DetailsOutageFragment
import com.example.canorecoapp.views.user.outages.ListOfFutureAndCurrentOutagesFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
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


class HomeLineMenFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener , GoogleMap.OnPolygonClickListener{
    private lateinit var binding : FragmentHomeLineMenBinding
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeLineMenBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }
    private fun resetFragmentWithProgress() {
        ProgressDialogUtils.showProgressDialog(requireContext(),"Loading...")
        Handler(Looper.getMainLooper()).post {
            reloadFragment()
            ProgressDialogUtils.dismissProgressDialog()
        }
    }
    private fun reloadFragment() {
        findNavController().navigate(R.id.outagesFragment)
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
            ProgressDialogUtils.dismissProgressDialog()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissionLocation()
        showPolygonsBasedOnFirestore()
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
        val dataKey = marker.tag as? String
        if (dataKey != null) {
            showData(dataKey)
            return true
        } else {
            // Handle the case when marker.tag is null
            return false
        }
    }

    private fun showData(dataKey: String) {
        val databaseReference = FirebaseDatabase.getInstance().reference.child("devices")

        databaseReference.child(dataKey).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val locationName = dataSnapshot.child("locationName").getValue(String::class.java)
                    val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)
                    val startTime = dataSnapshot.child("startTime").getValue(String::class.java)
                    val endTime = dataSnapshot.child("endTime").getValue(String::class.java)
                    val status = dataSnapshot.child("status").getValue(String::class.java)
                    val assigned = dataSnapshot.child("assigned").getValue(String::class.java)

                    // Create the dialog fragment and set the data
                    val detailsDialog = TasksDetailsFragment()
                    val bundle = Bundle().apply {
                        putString("locationName", locationName)
                        putDouble("latitude", latitude ?: 0.0)
                        putDouble("longitude", longitude ?: 0.0)
                        putString("startTime", startTime)
                        putString("endTime", endTime)
                        putString("status", status)
                        putString("id", dataKey)
                        putString("assigned", assigned)
                    }
                    detailsDialog.arguments = bundle
                    detailsDialog.show(childFragmentManager, "TasksDetailsFragment")
                } else {
                    Toast.makeText(requireContext(), "No data found for the key: $dataKey", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("RTDB", "Error fetching data: ${exception.message}")
                Toast.makeText(requireContext(), "Error fetching data", Toast.LENGTH_SHORT).show()
            }
    }




    private fun checkPermissionLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        gMap?.addMarker(
                            MarkerOptions().position(currentLatLng).title("Current Location")
                        )

                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.0f)
                        gMap?.animateCamera(cameraUpdate)
                    }
                }
        }
    }
    private fun showAllDevicesLocations() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("devices")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                gMap?.clear()

                for (deviceSnapshot in dataSnapshot.children) {
                    val lat = deviceSnapshot.child("latitude").getValue(Double::class.java)
                    val lng = deviceSnapshot.child("longitude").getValue(Double::class.java)
                    val status = deviceSnapshot.child("status").getValue(String::class.java)

                    if (lat != null && lng != null && status != null) {
                        val color = when (status.lowercase()) {
                            "working" -> Color.BLUE
                            "under repair" -> Color.GREEN
                            "damaged", "not working" -> Color.RED
                            else -> Color.GRAY
                        }
                        lifecycleScope.launchWhenResumed {
                            val markerIcon = bitmapFromVector(this@HomeLineMenFragment.requireContext(), R.drawable.baseline_adjust_24, color)

                            val marker = gMap?.addMarker(
                                MarkerOptions()
                                    .position(LatLng(lat, lng))
                                    .icon(markerIcon)
                            )
                            marker?.tag = deviceSnapshot.child("id").getValue(String::class.java)
                        }

                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MapData", "Database error: ${databaseError.message}")
            }
        })
    }



    private fun bitmapFromVector(context: Context, vectorResId: Int, @ColorInt color: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable?.setBounds(
            0, 0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )


        vectorDrawable?.setTint(color)

        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val sanVicenteCamarinesNorte = LatLng(14.08446, 122.88797)
        val zoomLevel = 5.0f
        gMap?.setOnMarkerClickListener(this)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sanVicenteCamarinesNorte, zoomLevel))
        showAllDevicesLocations()

    }





    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
