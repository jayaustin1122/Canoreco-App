package com.example.canorecoapp.views.user.bayadcenterandbusinesscenter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.canorecoapp.adapter.CentersAdapter
import com.example.canorecoapp.databinding.FragmentListOfCentersBinding
import com.example.canorecoapp.models.Centers


class ListOfCentersFragment : Fragment() {
    private var _binding : FragmentListOfCentersBinding? = null
    private val binding get() = _binding!!
    private var centersList: ArrayList<Centers>? = null
    private var from: String? = null
    private lateinit var adapter: CentersAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListOfCentersBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { bundle ->
            centersList = bundle.getParcelableArrayList<Centers>("data")
            from = bundle.getString("from")
        }
        binding.textView.text = from

        lifecycleScope.launchWhenResumed {
            adapter = CentersAdapter(this@ListOfCentersFragment.requireContext(), findNavController(), centersList!!.toList(),from)
            binding.rvCenters.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            binding.rvCenters.layoutManager = layoutManager
            binding.rvCenters.adapter = adapter
        }
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}