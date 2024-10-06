package com.example.canorecoapp.views.user.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import androidx.viewpager2.widget.ViewPager2
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.FullScreenImageAdapter
import com.example.canorecoapp.databinding.FragmentFullScreenImageBinding

class FullScreenImageFragment : Fragment() {

    private lateinit var binding: FragmentFullScreenImageBinding
    private lateinit var imageList: List<String>
    private var initialPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFullScreenImageBinding.inflate(inflater, container, false)

        // Get the image list and initial position from the arguments
        arguments?.let {
            imageList = it.getStringArrayList("imageList") ?: emptyList()
            initialPosition = it.getInt("initialPosition", 0)
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )
        // Set up ViewPager2 with an adapter to display images
        val pagerAdapter = FullScreenImageAdapter(imageList)
        binding.viewPager.adapter = pagerAdapter

        // Set initial position to the image that was clicked
        binding.viewPager.setCurrentItem(initialPosition, false)

        return binding.root
    }
}
