package com.example.canorecoapp.views.user.bayadcenterandbusinesscenter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentViewMapMarkerClickBinding
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
import com.google.firebase.firestore.FirebaseFirestore


class ViewMapMarkerClickFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private var _binding : FragmentViewMapMarkerClickBinding? = null
    private val binding get() = _binding!!
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentViewMapMarkerClickBinding.inflate(layoutInflater)
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


        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun zoomIn(location: LatLng? = null) {
        gMap?.let {
            val cameraPosition = it.cameraPosition
            val targetLocation = location ?: cameraPosition.target
            val newZoom = cameraPosition.zoom + 8.0f
            val newCameraPosition = CameraUpdateFactory.newLatLngZoom(targetLocation, newZoom)
            it.animateCamera(newCameraPosition)
        }
    }
    override fun onMarkerClick(marker: Marker): Boolean {
        val tag = marker.tag as? String
        if (tag != null) {
            val parts = tag.split(":", limit = 2)
            if (parts.size == 2) {
                val idType = parts[0]
                val locationName = parts[1]

                val addDataDialog = DetailsCenterFragment()
                val bundle = Bundle()
                bundle.putString("marker", locationName)
                bundle.putString("id", idType)
                addDataDialog.arguments = bundle
                addDataDialog.show(childFragmentManager, "DetailsCenterFragment")
                return true
            }
        }
        return false
    }

    private fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        if (vectorDrawable == null) {
            throw IllegalArgumentException("Resource not found: $vectorResId")
        }

        val width = dpToPx(context, 44)
        val height = dpToPx(context, 57)
        vectorDrawable.setBounds(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }


    fun showAllDataOnMaps() {
        val firestoreReference = FirebaseFirestore.getInstance().collection("bayad_centers")

        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                val locationName = document.getString("locationName")
                val latitude = document.getDouble("latitude")
                val longitude = document.getDouble("longitude")

                if (latitude != null && longitude != null) {
                    lifecycleScope.launchWhenResumed {
                        val markerIcon = bitmapFromVector(this@ViewMapMarkerClickFragment.requireContext(), R.drawable.icon_payment_center)

                        val marker = gMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(locationName)
                                .icon(markerIcon)
                        )
                        // Include type ID and locationName in the tag
                        marker?.tag = "bayadCenters:$locationName"
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

        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
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
        if (gMap == null) {
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
            mapFragment.getMapAsync(this)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
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
        val camarinesNorte = LatLng(14.222795, 122.689153)
        val zoomLevel = 9.4f
        gMap?.setOnMarkerClickListener(this)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(camarinesNorte, zoomLevel))

        val from = arguments?.getString("from")

        if ( from == "List Of Bayad Centers"){
            showAllDataOnMaps()
            val lat = arguments?.getString("lat")?.toDoubleOrNull()
            val lng = arguments?.getString("lng")?.toDoubleOrNull()
            if (lat != null && lng != null) {
                zoomIn(LatLng(lat, lng))
            }
        }
        else {
            showAllDataOnMapsBusiness()
            val lat = arguments?.getString("lat")?.toDoubleOrNull()
            val lng = arguments?.getString("lng")?.toDoubleOrNull()
            if (lat != null && lng != null) {
                zoomIn(LatLng(lat, lng))
            }
        }
    }

    fun showAllDataOnMapsBusiness() {
        val firestoreReference = FirebaseFirestore.getInstance().collection("business_centers")

        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            Log.d("MapData", "Total documents fetched: ${querySnapshot.documents.size}")
            for (document in querySnapshot.documents) {
                val locationName = document.getString("locationName")
                val latitude = document.getDouble("latitude")
                val longitude = document.getDouble("longitude")
                Log.d("MapData", "Processing document: ${document.id}, locationName: $locationName, latitude: $latitude, longitude: $longitude")

                if (latitude != null && longitude != null) {
                    lifecycleScope.launchWhenResumed {
                        val smallMarker = Bitmap.createScaledBitmap(
                            BitmapFactory.decodeResource(resources, R.drawable.icon_business_center),
                            134, 92, false
                        )

                        val marker = gMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(latitude, longitude))
                                .title(locationName)
                                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)) // Custom marker icon
                        )
                        // Include type ID and locationName in the tag
                        marker?.tag = "businessCenters:$locationName"
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
        }
    }

}