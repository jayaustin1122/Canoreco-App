package com.example.canorecoapp.views.user.outages

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentFutureOutagesMapBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.utils.ProgressDialogUtils
import com.example.canorecoapp.utils.ProgressDialogUtils.dismissProgressDialog
import com.example.canorecoapp.views.user.news.DetailsOutageFragment
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
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FutureOutagesMapFragment : Fragment() , OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnPolygonClickListener {
    private lateinit var binding : FragmentFutureOutagesMapBinding
    private var gMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var from: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFutureOutagesMapBinding.inflate(layoutInflater)
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

        firestoreReference.get().addOnSuccessListener { querySnapshot ->
            val selectedLocations = mutableSetOf<String>()

            val currentDate = Date() // Current date
            for (document in querySnapshot.documents) {
                val selectedLocationsList = document.get("selectedLocations") as? List<*>
                val dateString = document.getString("date")
                val documentDate = dateString?.let { parseDate(it) }

                if (documentDate != null && documentDate > currentDate) {
                    selectedLocationsList?.let {
                        selectedLocations.addAll(it.filterIsInstance<String>())
                    }
                }
                Log.d("FirestoreData", "Selected Locations: $selectedLocations")
            }

            val jsonData = loadJsonFromRaw(R.raw.filtered_barangayss)
            jsonData?.let { parseAndDrawPolygons(it, selectedLocations) }
                ?: run {
                    Log.e("JSON", "Failed to load JSON data")
                }

        }.addOnFailureListener { exception ->
            Log.e("MapData", "Error retrieving data from Firestore: ${exception.message}")
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        } catch (e: ParseException) {
            null
        }
    }


    private fun parseAndDrawPolygons(jsonData: String, selectedLocations: Set<String>) {
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
                    val latLngList = mutableListOf<LatLng>()
                    if (geometry.getString("type") == "Polygon") {
                        val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)

                        val polygonOptions = PolygonOptions()

                        for (j in 0 until coordinates.length()) {
                            val coordinate = coordinates.getJSONArray(j)
                            val latLng = LatLng(coordinate.getDouble(1), coordinate.getDouble(0))
                            latLngList.add(latLng)
                            polygonOptions.add(latLng)
                        }
                        val centroid = calculateCentroid(latLngList)

                        polygonOptions.strokeColor(Color.parseColor("#ff7b07"))
                        polygonOptions.fillColor(Color.parseColor("#80FF7B07"))
                        polygonOptions.strokeWidth(3f)

                        val polygon = gMap?.addPolygon(polygonOptions)
                        polygon?.tag = barangayName
                        Log.d("PolygonTag", "Assigned tag: $barangayName to polygon")

                        val markerOptions = MarkerOptions()
                            .position(centroid)
                            .title(barangayName)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_outage_pin_orange))
                        val marker = gMap?.addMarker(markerOptions)
                        marker?.tag = barangayName
                        Log.d("MarkerTag", "Assigned tag: $barangayName to marker")
                    }
                }
            }

            getCurrentLocation()
        } catch (e: JSONException) {
            Log.e("JSON", "Error parsing JSON data: ${e.message}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissionLocation()
        arguments?.let {
            from = it.getString("from")
        }

        binding.viewListButton.setOnClickListener {
            val detailsFragment = ListOfFutureAndCurrentOutagesFragment()
            val bundle = Bundle().apply {
                putString("from", "future")
                putString("from2", from)
            }
            detailsFragment.arguments = bundle
            findNavController().navigate(R.id.listOfFutureAndCurrentOutagesFragment, bundle)
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



    override fun onMarkerClick(marker: Marker): Boolean {
        val dataKey = marker.tag as? String
        if (dataKey != null) {
            val addDataDialog = DetailsOutageFragment()
            val bundle = Bundle()
            bundle.putString("areaCode", marker.tag.toString())
            bundle.putString("from", "future")
            addDataDialog.arguments = bundle
            addDataDialog.show(childFragmentManager, "DetailsOutageFragment")
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
        val camarinesNorte = LatLng(14.222795, 122.689153)
        val zoomLevel = 9.4f
        gMap?.setOnMarkerClickListener(this)
        gMap?.setOnPolygonClickListener(this)
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(camarinesNorte, zoomLevel))
        showPolygonsBasedOnFirestore()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onPolygonClick(p0: Polygon) {
        Log.d("PolygonClick", "Polygon clicked")
        val barangayName = p0.tag as? String
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
}