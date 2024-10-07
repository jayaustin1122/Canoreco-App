package com.example.canorecoapp.views.user.notif

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.canorecoapp.databinding.FragmentNotificationBottomSheetDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NotificationBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentNotificationBottomSheetDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationBottomSheetDialogBinding.inflate(inflater, container, false)

        // Set values to views
        val title = arguments?.getString("title")
        val text = arguments?.getString("text")
        val date = arguments?.getString("date")

        binding.tvTitle.text = title
        binding.tvText.text = text
        binding.tvDate.text = "Date: $date"

        // Close button listener
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    companion object {
        fun newInstance(title: String, text: String, isFromDevice: Boolean, date: String): NotificationBottomSheetDialogFragment {
            return NotificationBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putString("text", text)
                    putBoolean("isFromDevice", isFromDevice)
                    putString("date", date)
                }
            }
        }
    }
}
