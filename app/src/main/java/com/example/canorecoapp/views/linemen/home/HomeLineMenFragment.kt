package com.example.canorecoapp.views.linemen.home

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
import com.example.canorecoapp.databinding.FragmentHomeLineMenBinding
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

    private fun parseAndDrawPolygons(jsonData: String) {
        Log.d("JSON", "Parsing JSON data")
        try {
            val jsonObject = JSONObject(jsonData)
            val features = jsonObject.getJSONArray("features")
            Log.d("JSON", "Number of features: ${features.length()}")

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.getJSONObject("geometry")

                if (geometry.getString("type") == "Polygon") {
                    val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)

                    val polygonOptions = PolygonOptions()

                    for (j in 0 until coordinates.length()) {
                        val coordinate = coordinates.getJSONArray(j)
                        val latLng = LatLng(coordinate.getDouble(1), coordinate.getDouble(0))
                        polygonOptions.add(latLng)
                    }

                    polygonOptions.strokeColor(Color.RED) // Customize as needed
                    polygonOptions.fillColor(Color.BLUE) // Customize as needed
                    polygonOptions.strokeWidth(2f) // Customize as needed

                    gMap?.addPolygon(polygonOptions)
                }
            }
            Log.d("JSON", "Polygons successfully drawn on map")
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
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

    private fun getLocationName(latitude: String?, longitude: String?, locationTextView: TextView) {
        val latitudeValue = latitude?.toDoubleOrNull()
        val longitudeValue = longitude?.toDoubleOrNull()

        if (latitudeValue != null && longitudeValue != null) {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            try {
                val addresses = geocoder.getFromLocation(latitudeValue, longitudeValue, 1)
                if (addresses != null) {
                    locationTextView.text = addresses.firstOrNull()?.getAddressLine(0) ?: "Unknown Location"
                }
            } catch (e: IOException) {
                Log.e("Geocoding", "Error getting location name: ${e.message}")
                locationTextView.text = "Error getting location name"
            }
        } else {
            locationTextView.text = "Invalid Coordinates"
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

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val sanVicenteCamarinesNorte = LatLng(14.08446, 122.88797)
        val zoomLevel = 5.0f
        gMap?.setOnMarkerClickListener(this)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sanVicenteCamarinesNorte, zoomLevel))

        // Directly load and parse the JSON file
        val jsonData = loadJsonFromRaw(R.raw.barangaycamnorte)
        Log.d("JSON", "Loaded JSON data: $jsonData")

        jsonData?.let { parseAndDrawPolygons(it) } ?: run {
            Log.e("JSON", "Failed to load JSON data")
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}