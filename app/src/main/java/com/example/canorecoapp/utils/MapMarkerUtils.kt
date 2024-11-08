package com.example.canorecoapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.example.canorecoapp.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.type.LatLng

class MapMarkerUtils(private val googleMap: GoogleMap, private val context: Context) {

    fun addMarkers(locations: List<com.google.android.gms.maps.model.LatLng>) {
        // Add markers
        locations.forEach { location ->
            val markerIcon = bitmapFromVector(context, R.drawable.post)
            val markerOptions = MarkerOptions()
                .position(location)
                .icon(markerIcon)
            googleMap.addMarker(markerOptions)
        }

        if (locations.size > 1) {
            // Add line from marker 1 to marker 2
            val polylineOptions1 = PolylineOptions().add(
                locations[0],
                locations[1],
                locations[2],
                locations[3],
                locations[4],
                locations[5],
                locations[6],
                locations[7],
                locations[8],
            ).color(Color.RED).width(5f)
            googleMap.addPolyline(polylineOptions1)
        }

        if (locations.size > 2) {
            // Add line from marker 1 to marker 3
            val polylineOptions2 = PolylineOptions().add(
                locations[0],
                locations[9],
                locations[10],
                locations[11],
                locations[12],
                locations[13],
                ).color(Color.BLUE).width(5f)
            googleMap.addPolyline(polylineOptions2)
        }
    }


    fun addMarkerAtLocation(location: com.google.android.gms.maps.model.LatLng) {
        val markerIcon = bitmapFromVector(context, R.drawable.post)
        val markerOptions = MarkerOptions()
            .position(location)
            .icon(markerIcon)
        googleMap.addMarker(markerOptions)
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
}
