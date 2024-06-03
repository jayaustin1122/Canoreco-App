package com.example.canorecoapp.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class DateTimeUtils {
    companion object {
        fun getCurrentTime(): String {
            val tz = TimeZone.getTimeZone("GMT+08:00")
            val c = Calendar.getInstance(tz)
            val hours = String.format("%02d", c.get(Calendar.HOUR))
            val minutes = String.format("%02d", c.get(Calendar.MINUTE))
            return "$hours:$minutes"
        }

        @SuppressLint("SimpleDateFormat")
        fun getCurrentDate(): String {
            val currentDateObject = Date()
            val formatter = SimpleDateFormat("dd-MM-yyyy")
            return formatter.format(currentDateObject)
        }
    }
}