package com.example.canorecoapp.views.user.bayadcenterandbusinesscenter

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentBusinessCenterTwoBinding
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
import com.google.firebase.firestore.FirebaseFirestore


class BusinessCenterFragmentTwo : Fragment() , OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private lateinit var binding : FragmentBusinessCenterTwoBinding
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBusinessCenterTwoBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fragmentMapBusinessCenter) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissionLocation()
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fragmentMapBusinessCenter) as SupportMapFragment
        mapFragment.getMapAsync(this)
        showAllDataOnMaps()
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
            val addDataDialog = DetailsCenterFragment()
            val bundle = Bundle()
            bundle.putString("marker", marker.tag.toString())
            bundle.putString("id", "businessCenters")
            addDataDialog.arguments = bundle
            addDataDialog.show(childFragmentManager, "DetailsCenterFragment")
            zoomIn(marker.position)
            return true
        } else {
            // Handle the case when marker.tag is null
            return false
        }
    }


    fun showAllDataOnMaps() {
        // Get a reference to your Firestore collection
        val firestoreReference = FirebaseFirestore.getInstance().collection("business_centers")

        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            Log.d("MapData", "Total documents fetched: ${querySnapshot.documents.size}")
            for (document in querySnapshot.documents) {
                val locationName = document.getString("locationName")
                // Retrieve latitude and longitude as Doubles, whether they are integers or decimals
                val latitude = document.getDouble("latitude")
                val longitude = document.getDouble("longitude")
                Log.d("MapData", "Processing document: ${document.id}, locationName: $locationName, latitude: $latitude, longitude: $longitude")

                if (latitude != null && longitude != null) {
                    lifecycleScope.launchWhenResumed {
                        val smallMarker = Bitmap.createScaledBitmap(
                            BitmapFactory.decodeResource(resources, R.drawable.icon_business_center),
                            134, 92, false
                        )

                        // Add a marker for each item
                        val marker = gMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(locationName)
                                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)) // Custom marker icon
                        )
                        marker?.tag = locationName// Save the Firestore document ID as a tag for reference
                    }

                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
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
        //to fix the not attach to a context fragment put lifecycle scope.
        lifecycleScope.launchWhenResumed {
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

    }
    override fun onResume() {
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMapBusinessCenter) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMapBusinessCenter) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMapBusinessCenter) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Initialize the map if it hasn't been initialized already
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMapBusinessCenter) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        val camarinesNorte = LatLng(14.222795, 122.689153)
        val zoomLevel = 9.4f
        gMap?.setOnMarkerClickListener(this)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(camarinesNorte, zoomLevel))

        getCurrentLocation()
    }
}