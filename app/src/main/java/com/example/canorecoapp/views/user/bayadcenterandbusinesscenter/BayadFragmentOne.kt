package com.example.canorecoapp.views.user.bayadcenterandbusinesscenter

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.canorecoapp.databinding.FragmentBayadOneBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.Locale


class BayadFragmentOne : Fragment() , OnMapReadyCallback, GoogleMap.OnMarkerClickListener{
    private lateinit var binding: FragmentBayadOneBinding
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBayadOneBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissionLocation()
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
            //  showMarkerDetailsDialog(dataKey)
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
                    if (addresses.isNotEmpty()) {
                        val locationName = addresses?.get(0)?.getAddressLine(0)
                        locationTextView.text = locationName
                    } else {
                        // Handle the case where no address is found
                        locationTextView.text = "Unknown Location"
                    }
                }
            } catch (e: IOException) {
                // Handle the exception
                Log.e("Geocoding", "Error getting location name: ${e.message}")
                locationTextView.text = "Error getting location name"
            }
        } else {
            // Handle the case where latitude or longitude is null or not a valid number
            locationTextView.text = "Invalid Coordinates"
        }
    }

    private fun checkPermissionLocation() {
        // Request location permission
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Get current location and add marker
            getCurrentLocation()
        } else {
            // Request location permission
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
                    // Add marker for the current location
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        gMap?.addMarker(
                            MarkerOptions().position(currentLatLng).title("Current Location")
                        )

                        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 15.0f)
                        gMap?.animateCamera(cameraUpdate)

                        // binding.tvCurrentLocation.text = it.latitude.toString()
                        //     binding.tvCurrentLocation2.text = it.longitude.toString()
                    }

                }
        }
    }
    override fun onResume() {
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val sanVicenteCamarinesNorte = LatLng(14.08446, 122.88797)
        val zoomLevel = 5.0f // Adjust the zoom level as needed
        gMap?.setOnMarkerClickListener(this)
        // Move the camera to the initial position
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sanVicenteCamarinesNorte, zoomLevel))

        //getCurrentLocation()
    }
}