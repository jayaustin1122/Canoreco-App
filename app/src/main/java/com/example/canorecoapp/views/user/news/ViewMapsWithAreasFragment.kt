package com.example.canorecoapp.views.user.news

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentViewMapsWithAreasBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale


class ViewMapsWithAreasFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener  {
    private lateinit var binding : FragmentViewMapsWithAreasBinding
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





    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val sanVicenteCamarinesNorte = LatLng(14.08446, 122.88797)
        val zoomLevel = 5.0f
        gMap?.setOnMarkerClickListener(this)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sanVicenteCamarinesNorte, zoomLevel))
        arguments?.let {
            val areas = it.getString("Areas")
            if (areas != null) {
                Log.d("ViewMapsWithAreasFragment", "Areas: $areas")
                getCurrentLocation()
                // Split areas by commas, trim whitespace, and convert to a set
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
    private fun parseAndDrawPolygons(jsonData: String, selectedLocations: Set<String>) {
        try {
            val jsonObject = JSONObject(jsonData)
            val features = jsonObject.getJSONArray("features")

            // Log the selected locations to confirm which ones we are looking for
            Log.d("MapData", "Selected Locations: $selectedLocations")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val properties = feature.getJSONObject("properties")
                val barangayName = properties.getString("ID_3")

                // Log each ID_3 from the JSON file
                Log.d("MapData", "Found ID_3: $barangayName")

                // Check if the current ID_3 is in the selectedLocations
                if (barangayName in selectedLocations) {
                    Log.d("MapData", "Drawing polygon for ID_3: $barangayName")
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
                        polygonOptions.fillColor(Color.argb(100, 255, 0, 0))
                        polygonOptions.strokeWidth(3f)

                        gMap?.addPolygon(polygonOptions)
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
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