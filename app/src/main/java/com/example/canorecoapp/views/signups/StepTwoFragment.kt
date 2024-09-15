package com.example.canorecoapp.views.signups

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentStepTwoBinding
import com.example.canorecoapp.utils.FirebaseUtils
import com.example.canorecoapp.viewmodels.SignUpViewModel


class StepTwoFragment : Fragment() {
    private lateinit var binding: FragmentStepTwoBinding

    private lateinit var viewModel: SignUpViewModel
    private val municipalitiesWithBarangays = mapOf(
        "Basud" to listOf("Angas", "Bactas", "Binatagan", "Caayunan", "Guinatungan", "Hinampacan", "Langa", "Laniton", "Lidong", "Mampili", "Mandazo", "Mangcamagong", "Manmuntay", "Mantugawe", "Matnog", "Mocong", "Oliva", "Pagsangahan", "Pinagwarasan", "Plaridel", "Poblacion 1", "Poblacion 2", "San Felipe", "San Jose", "San Pascual", "Taba-taba", "Tacad", "Taisan", "Tuaca"),
        "Capalonga" to listOf("Alayao", "Binawangan", "Calabaca", "Camagsaan", "Catabaguangan", "Catioan", "Del Pilar", "Itok", "Lucbanan", "Mabini", "Mactang", "Magsaysay", "Mataque", "Old Camp", "Poblacion", "San Antonio", "San Isidro", "San Roque", "Tanawan", "Ubang", "Villa Aurora", "Villa Belen"),
        "Daet" to listOf("Alawihao", "Awitan", "Bagasbas", "Barangay I", "Barangay II", "Barangay III", "Barangay IV", "Barangay V", "Barangay VI", "Barangay VII", "Barangay VIII", "Bibirao", "Borabod", "Calasgasan", "Camambugan", "Cobangbang", "Dogongan", "Gahonon", "Gubat", "Lag-on", "Magang","Mambalite","Mancruz","Pamorangon","San Isidro"),
        "Jose Panganiban" to listOf(
            "Bagong Bayan", "Calero", "Dahican", "Dayhagan", "Larap", "Luklukan Norte", "Luklukan Sur",
            "Motherlode", "Nakalaya", "North Poblacion", "Osmeña", "Pag-asa", "Parang", "Plaridel",
            "Salvacion", "San Isidro", "San Jose", "San Martin", "San Pedro", "San Rafael",
            "Santa Cruz", "Santa Elena", "Santa Milagrosa", "Santa Rosa Norte", "Santa Rosa Sur",
            "South Poblacion", "Tamisan"
        ),
        "Labo" to listOf(
            "Anahaw", "Anameam", "Awitan", "Baay", "Bagacay", "Bagong Silang I", "Bagong Silang II",
            "Bagong Silang III", "Bakiad", "Bautista", "Bayabas", "Bayan-bayan", "Benit", "Bulhao",
            "Cabatuhan", "Cabusay", "Calabasa", "Canapawan", "Daguit", "Dalas", "Dumagmang", "Exciban",
            "Fundado", "Guinacutan", "Guisican", "Gumamela", "Iberica", "Kalamunding", "Lugui",
            "Mabilo I", "Mabilo II", "Macogon", "Mahawan-hawan", "Malangcao-Basud", "Malasugui",
            "Malatap", "Malaya", "Malibago", "Maot", "Masalong", "Matanlang", "Napaod", "Pag-asa",
            "Pangpang", "Pinya", "San Antonio", "San Francisco", "Santa Cruz", "Submakin",
            "Talobatib", "Tigbinan", "Tulay na Lupa"
        ),
        "Mercedes" to listOf(
            "Apuao", "Barangay I", "Barangay II", "Barangay III", "Barangay IV", "Barangay V",
            "Barangay VI", "Barangay VII", "Caringo", "Catandunganon", "Cayucyucan", "Colasi",
            "Del Rosario", "Gaboc", "Hamoraon", "Hinipaan", "Lalawigan", "Lanot", "Mambungalon",
            "Manguisoc", "Masalongsalong", "Matoogtoog", "Pambuhan", "Quinapaguian", "San Roque",
            "Tarum"
        ),
        "Paracale" to listOf(
            "Awitan", "Bagumbayan", "Bakal", "Batobalani", "Calaburnay", "Capacuan", "Casalugan",
            "Dagang", "Dalnac", "Dancalan", "Gumaus", "Labnig", "Macolabo Island", "Malacbang",
            "Malaguit", "Mampungo", "Mangkasay", "Maybato", "Palanas", "Pinagbirayan Malaki",
            "Pinagbirayan Munti", "Poblacion Norte", "Poblacion Sur", "Tabas", "Talusan", "Tawig",
            "Tugos"
        ),
        "San Lorenzo Ruiz" to listOf(
            "Daculang Bolo",
            "Dagotdotan",
            "Langga",
            "Laniton",
            "Maisog",
            "Mampurog",
            "Manlimonsito",
            "Matacong",
            "Salvacion",
            "San Antonio",
            "San Isidro",
            "San Ramon"
        ),
        "San Vicente" to listOf(
            "Asdum",
            "Cabanbanan",
            "Calabagas",
            "Fabrica",
            "Iraya Sur",
            "Man-ogob",
            "Poblacion District I",
            "Poblacion District II",
            "San Jose"
        ),
        "Santa Elena" to listOf(
            "Basiad",
            "Bulala",
            "Don Tomas",
            "Guitol",
            "Kabuluan",
            "Kagtalaba",
            "Maulawin",
            "Patag Ibaba",
            "Patag Iraya",
            "Plaridel",
            "Polungguitguit",
            "Rizal",
            "Salvacion",
            "San Lorenzo",
            "San Pedro",
            "San Vicente",
            "Santa Elena",
            "Tabugon",
            "Villa San Isidro"
        ),
        "Talisay" to listOf(
            "Binanuaan",
            "Caawigan",
            "Cahabaan",
            "Calintaan",
            "Del Carmen",
            "Gabon",
            "Itomang",
            "Poblacion",
            "San Francisco",
            "San Isidro",
            "San Jose",
            "San Nicolas",
            "Santa Cruz",
            "Santa Elena",
            "Santo Niño"
        ),
        "Vinzons" to listOf(
            "Aguit-it",
            "Banocboc",
            "Barangay I",
            "Barangay II",
            "Barangay III",
            "Cagbalogo",
            "Calangcawan Norte",
            "Calangcawan Sur",
            "Guinacutan",
            "Mangcawayan",
            "Mangcayo",
            "Manlucugan",
            "Matango",
            "Napilihan",
            "Pinagtigasan",
            "Sabang",
            "Santo Domingo",
            "Singi",
            "Sula"
        )

    )
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStepTwoBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etContactNumber.addTextChangedListener {
            viewModel.phone = it.toString()
        }
        val municipalities = municipalitiesWithBarangays.keys.toList()
        val municipalityAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, municipalities)
        binding.tvMunicipality.setAdapter(municipalityAdapter)


        binding.tvMunicipality.setOnItemClickListener { parent, view, position, id ->
            val selectedMunicipality = parent.getItemAtPosition(position).toString()
            viewModel.address = selectedMunicipality

            val barangays = municipalitiesWithBarangays[selectedMunicipality] ?: emptyList()
            val barangayAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, barangays)
            binding.tvBrgy.setAdapter(barangayAdapter)
        }

        binding.tvBrgy.setOnItemClickListener { parent, view, position, id ->
            val selectedBarangay = parent.getItemAtPosition(position).toString()
            viewModel.barangay = selectedBarangay
        }
        binding.etStreet.addTextChangedListener{
            viewModel.street = it.toString()
        }
    }
    override fun onResume() {
        super.onResume()
        val municipalities = municipalitiesWithBarangays.keys.toList()
        val municipalityAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, municipalities)
        binding.tvMunicipality.setAdapter(municipalityAdapter)
    }

}