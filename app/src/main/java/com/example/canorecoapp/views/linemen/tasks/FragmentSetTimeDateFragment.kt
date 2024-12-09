package com.example.canorecoapp.views.linemen.tasks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentSetTimeDateragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar


class FragmentSetTimeDateFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentSetTimeDateragmentBinding? = null
    val binding get() = _binding!!
    private var startTime: String? = null
    private var endTime: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSetTimeDateragmentBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userName = arguments?.getString("userName")
        val lastName = arguments?.getString("lastName")
        val id = arguments?.getString("id")
        Log.d("UploadData1", "recieve: $userName and id: $id")
        binding.etBirthDate.setOnClickListener {
            showDatePickerDialog()
        }
        binding.etStartTime.setOnClickListener {
            showTimePicker(binding.etStartTime, "start")
        }
        binding.etEndTime.setOnClickListener {
            showTimePicker(binding.etEndTime, "end")
        }
        binding.btnSet.setOnClickListener {
            uploadData(userName, id,lastName)
        }

    }


    private fun uploadData(userName: String?, id: String?, lastName: String?) {
        val database: DatabaseReference = FirebaseDatabase.getInstance().reference
        val devicesRef = database.child("devices/$id")
        val timestamp = System.currentTimeMillis() / 1000

        val updates = mapOf<String, Any?>(
            "assigned" to "$userName $lastName",
            "date" to binding.etBirthDate.text.toString(),
            "endTime" to endTime,
            "startTime" to startTime,
            "status" to "under repair",
            "timestamp" to timestamp
        )

        devicesRef.updateChildren(updates)
            .addOnSuccessListener {
                dismiss()
                Toast.makeText(context, "Data updated successfully for ID: $id", Toast.LENGTH_SHORT)
                    .show()
                val bundle = Bundle().apply {

                    val selectedFragmentId = R.id.navigation_notificationl_linemen

                    putInt("selectedFragmentId", selectedFragmentId)
                }

                findNavController().navigate(R.id.adminHolderFragment, bundle)

                println("Data updated successfully")

                dismiss()

            }
    }


    private fun showDatePickerDialog() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = selection
            }
            val year = calendar.get(Calendar.YEAR).toString()
            val month = (calendar.get(Calendar.MONTH) + 1).toString()
            val day = calendar.get(Calendar.DAY_OF_MONTH).toString()

            binding.etBirthDate.setText("${month}/${day}/${year}")
        }
        datePicker.show(parentFragmentManager, "MaterialDatePicker")
    }

    private fun showTimePicker(editText: EditText, timeType: String) {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Time")
            .build()

        timePicker.show(parentFragmentManager, "TIME_PICKER")
        timePicker.addOnPositiveButtonClickListener {
            val time = formatTime(timePicker.hour, timePicker.minute)
            if (timeType == "start") {
                startTime = time
                editText.setText(time)
            } else if (timeType == "end") {
                endTime = time
                editText.setText(time)
            }
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val formattedHour = if (hour < 10) "0$hour" else hour.toString()
        val formattedMinute = if (minute < 10) "0$minute" else minute.toString()
        return "$formattedHour:$formattedMinute"
    }
}