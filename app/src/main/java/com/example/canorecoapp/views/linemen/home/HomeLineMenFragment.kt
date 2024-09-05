package com.example.canorecoapp.views.linemen.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentHomeLineMenBinding
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
import com.google.android.gms.maps.model.PolygonOptions
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale


class HomeLineMenFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
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

    private fun loadJsonFromRaw(resourceId: Int): String? {
        return try {
            val inputStream = requireContext().resources.openRawResource(resourceId)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e("JSON", "Error reading JSON file from raw resources: ${e.message}")
            null
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissionLocation()
        binding.btnZoomIn.setOnClickListener {
            getCurrentLocation()
        }

    }

    private fun zoomIn(location: LatLng? = null) {
        gMap?.let {
            val cameraPosition = it.cameraPosition
            val targetLocation = location ?: cameraPosition.target
            val newZoom = cameraPosition.zoom + 5.0f
            val newCameraPosition = CameraUpdateFactory.newLatLngZoom(targetLocation, newZoom)
            it.animateCamera(newCameraPosition)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val dataKey = marker.tag as? String
        if (dataKey != null) {
            // showMarkerDetailsDialog(dataKey)
            return true
        } else {
            // Handle the case when marker.tag is null
            return false
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
        val firestoreReference = FirebaseFirestore.getInstance().collection("devicelocation")
        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                val lat = (document.getDouble("lat") ?: document.getLong("lat")?.toDouble()) ?: document.getString("lat")?.toDoubleOrNull()
                val lng = (document.getDouble("lng") ?: document.getLong("lng")?.toDouble()) ?: document.getString("lng")?.toDoubleOrNull()
                val status = document.getString("status")

                if (lat != null && lng != null && status != null) {
                    // Determine the color based on the status
                    val color = when (status.lowercase()) {
                        "working" -> Color.BLUE
                        "under repair" -> Color.GREEN
                        "not working" -> Color.RED
                        else -> Color.GRAY
                    }


                    val markerIcon = bitmapFromVector(this@HomeLineMenFragment.requireContext(), R.drawable.baseline_adjust_24, color)


                    val marker = gMap?.addMarker(
                        MarkerOptions()
                            .position(LatLng(lat, lng))
                            .icon(markerIcon)
                    )
                    marker?.tag = document.id
                }
            }
        }.addOnFailureListener { exception ->

            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
        }
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
        showPolygonsBasedOnFirestore()
    }
    private fun showPolygonsBasedOnFirestore() {
        val firestoreReference = FirebaseFirestore.getInstance().collection("areas_affected")
        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            val validBarangays = mutableSetOf<String>()

            Log.d("FirestoreData", "Retrieving data from Firestore")
            for (document in querySnapshot.documents) {
                Log.d("FirestoreData", "Document ID: ${document.id}")


                val barangayDataMap = document.data
                barangayDataMap?.forEach { (barangayName, isAffected) ->
                    if (barangayName is String && isAffected is Boolean && isAffected) {
                        validBarangays.add(barangayName)
                        Log.d("FirestoreData", "Barangay: $barangayName, Affected: $isAffected")
                    } else {
                        Log.w("FirestoreData", "Unexpected data format for barangay: $barangayName")
                    }
                }
            }

            Log.d("FirestoreData", "Valid Barangays: $validBarangays")


            val jsonData = loadJsonFromRaw(R.raw.filtered_barangayss)
            Log.d("JSON", "Loaded JSON data: $jsonData")

            jsonData?.let { parseAndDrawPolygons(it, validBarangays) } ?: run {
                Log.e("JSON", "Failed to load JSON data")
            }

        }.addOnFailureListener { exception ->

            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
        }
    }


    private fun parseAndDrawPolygons(jsonData: String, validBarangays: Set<String>) {
        Log.d("JSON", "Parsing JSON data")
        try {
            val jsonObject = JSONObject(jsonData)
            val features = jsonObject.getJSONArray("features")
            Log.d("JSON", "Number of features: ${features.length()}")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val barangayName = properties.getString("ID_3")

                if (barangayName in validBarangays) {
                    val geometry = feature.getJSONObject("geometry")

                    if (geometry.getString("type") == "Polygon") {
                        val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)

                        val polygonOptions = PolygonOptions()

                        for (j in 0 until coordinates.length()) {
                            val coordinate = coordinates.getJSONArray(j)
                            val latLng = LatLng(coordinate.getDouble(1), coordinate.getDouble(0))
                            polygonOptions.add(latLng)
                        }

                        polygonOptions.strokeColor(Color.RED)
                        polygonOptions.fillColor(Color.argb(100, 255, 0, 0)) // Customize as needed
                        polygonOptions.strokeWidth(3f)

                        gMap?.addPolygon(polygonOptions)
                    }
                }
            }
            Log.d("JSON", "Polygons successfully drawn on map")
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
        }
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}