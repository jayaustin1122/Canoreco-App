package com.example.canorecoapp.views.user.news

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.MaintenanceAdapter
import com.example.canorecoapp.databinding.FragmentViewMapsWithAreasBinding
import com.example.canorecoapp.models.Maintenance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ViewMapsWithAreasFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnPolygonClickListener {
    private lateinit var binding: FragmentViewMapsWithAreasBinding
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewMapsWithAreasBinding.inflate(layoutInflater)
        val mapFragment = childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getCurrentLocation()
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val sanVicenteCamarinesNorte = LatLng(14.08446, 122.88797)
        val zoomLevel = 5.0f
        gMap?.setOnMarkerClickListener(this)
        gMap?.setOnPolygonClickListener(this) // Add polygon click listener
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sanVicenteCamarinesNorte, zoomLevel))

        arguments?.let {
            val areas = it.getString("Areas")
            if (areas != null) {
                Log.d("ViewMapsWithAreasFragment", "Areas: $areas")
                getCurrentLocation()
                val selectedLocations = areas.split(",").map { it.trim() }.toSet()
                val jsonData = loadJsonFromRaw(R.raw.filtered_barangayss)
                jsonData?.let {
                    parseAndDrawPolygons(it, selectedLocations)
                } ?: run {
                    Log.e("JSON", "Failed to load JSON data")
                }
            }
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val dataKey = marker.tag as? String
        if (dataKey != null) {
            // Handle marker click (optional)
            return true
        }
        return false
    }

    override fun onPolygonClick(polygon: Polygon) {
        Log.d("PolygonClick", "Polygon clicked")
        val barangayName = polygon.tag as? String
        if (barangayName != null) {
            val addDataDialog = DetailsOutageFragment()
            val bundle = Bundle()
            bundle.putString("areaCode", barangayName)
            addDataDialog.arguments = bundle
            addDataDialog.show(childFragmentManager, "DetailsOutageFragment")
        } else {
            Log.d("PolygonClick", "Polygon tag is null")
        }
    }
    private fun parseAndDrawPolygons(jsonData: String, selectedLocations: Set<String>) {
        try {
            val jsonObject = JSONObject(jsonData)
            val features = jsonObject.getJSONArray("features")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val barangayName = properties.getString("ID_3")

                if (barangayName in selectedLocations) {
                    val geometry = feature.getJSONObject("geometry")

                    if (geometry.getString("type") == "Polygon") {
                        val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)

                        val polygonOptions = PolygonOptions().clickable(true) // Ensure polygons are clickable
                        for (j in 0 until coordinates.length()) {
                            val coordinate = coordinates.getJSONArray(j)
                            val latLng = LatLng(coordinate.getDouble(1), coordinate.getDouble(0))
                            polygonOptions.add(latLng)
                        }

                        polygonOptions.strokeColor(Color.RED)
                        polygonOptions.fillColor(Color.argb(100, 255, 0, 0))
                        polygonOptions.strokeWidth(3f)

                        val polygon = gMap?.addPolygon(polygonOptions)
                        polygon?.tag = barangayName
                        Log.d("PolygonTag", "Assigned tag: $barangayName to polygon")
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
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

    private fun loadJsonFromRaw(resourceId: Int): String? {
        return try {
            val inputStream = requireContext().resources.openRawResource(resourceId)
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e("JSON", "Error reading JSON file from raw resources: ${e.message}")
            null
        }
    }
}
