package com.example.canorecoapp.views.user.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.FullScreenImageAdapter
import com.example.canorecoapp.databinding.FragmentFullScreenImageBinding

class FullScreenImageFragment : Fragment() {

    private lateinit var binding: FragmentFullScreenImageBinding
    private lateinit var imageList: List<String>
    private var initialPosition: Int = 0
    private var defaultImageRes: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFullScreenImageBinding.inflate(inflater, container, false)

        // Retrieve arguments from the bundle
        arguments?.let {
            imageList = it.getStringArrayList("imageList") ?: emptyList()
            initialPosition = it.getInt("initialPosition", 0)
            defaultImageRes = it.getInt("defaultImageRes", R.drawable.img_user_placeholder)
        }

        // Handle the back press to navigate up
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            }
        )

        // If image list is empty, display the default image
        if (imageList.isEmpty() && defaultImageRes != null) {
            imageList = listOf(defaultImageRes.toString()) // Use the default image resource as a fallback
        }

        // Set up ViewPager2 with the adapter to display images
        val pagerAdapter = FullScreenImageAdapter(imageList)
        binding.viewPager.adapter = pagerAdapter

        // Set the initial position based on the image that was clicked
        binding.viewPager.setCurrentItem(initialPosition, false)

        return binding.root
    }
}
