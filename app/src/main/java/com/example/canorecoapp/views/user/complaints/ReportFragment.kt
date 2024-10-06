package com.example.canorecoapp.views.user.complaints

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.canorecoapp.R
import com.example.canorecoapp.databinding.FragmentReportBinding
import com.example.canorecoapp.utils.DialogUtils
import com.example.canorecoapp.viewmodels.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class ReportFragment : Fragment() {

    private lateinit var binding: FragmentReportBinding
    private lateinit var selectedImage: Uri
    private val IMAGE_PICK_GALLERY_CODE = 102
    private val IMAGE_PICK_CAMERA_CODE = 103
    private lateinit var auth: FirebaseAuth
    private var from: String? = null
    private val CAMERA_PERMISSION_CODE = 101
    private lateinit var storage: FirebaseStorage
    private lateinit var progressDialog: ProgressDialog
    private var selectedFragmentId: Int? = null
    private val viewModel: UserViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportBinding.inflate(layoutInflater)
        return binding.root
    }

    private val complaints = mapOf(
        "Power Outages" to listOf(
            "Unscheduled Outages",
            "Scheduled Maintenance",
            "Duration of Outage",
            "Frequency of Outages"
        ),
        "Billing Issues" to listOf(
            "Incorrect Billing Amount",
            "Late Fees",
            "Payment Processing Issues",
            "Request for Billing Adjustment"
        ),
        "Service Quality" to listOf(
            "Voltage Fluctuations",
            "Flickering Lights",
            "Appliance Damage",
            "Poor Customer Service Experience",
            "Connection Issues",
            "New Connection Request",
            "Disconnection Issues",
            "Service Interruption",
            "Request for Reconnection"
        ),
        "Safety Concerns" to listOf(
            "Downed Power Lines",
            "Exposed Wiring",
            "Hazardous Conditions",
            "Electrical Fires"
        ),
        "Feedback and Suggestions" to listOf(
            "General Feedback on Services",
            "Suggestions for Improvement",
            "Complaints about Communication"
        ),
    )
    private val municipalitiesWithBarangays = mapOf(
        "Basud" to listOf(
            "Angas",
            "Bactas",
            "Binatagan",
            "Caayunan",
            "Guinatungan",
            "Hinampacan",
            "Langa",
            "Laniton",
            "Lidong",
            "Mampili",
            "Mandazo",
            "Mangcamagong",
            "Manmuntay",
            "Mantugawe",
            "Matnog",
            "Mocong",
            "Oliva",
            "Pagsangahan",
            "Pinagwarasan",
            "Plaridel",
            "Poblacion 1",
            "Poblacion 2",
            "San Felipe",
            "San Jose",
            "San Pascual",
            "Taba-taba",
            "Tacad",
            "Taisan",
            "Tuaca"
        ),
        "Capalonga" to listOf(
            "Alayao",
            "Binawangan",
            "Calabaca",
            "Camagsaan",
            "Catabaguangan",
            "Catioan",
            "Del Pilar",
            "Itok",
            "Lucbanan",
            "Mabini",
            "Mactang",
            "Magsaysay",
            "Mataque",
            "Old Camp",
            "Poblacion",
            "San Antonio",
            "San Isidro",
            "San Roque",
            "Tanawan",
            "Ubang",
            "Villa Aurora",
            "Villa Belen"
        ),
        "Daet" to listOf(
            "Alawihao",
            "Awitan",
            "Bagasbas",
            "Barangay I",
            "Barangay II",
            "Barangay III",
            "Barangay IV",
            "Barangay V",
            "Barangay VI",
            "Barangay VII",
            "Barangay VIII",
            "Bibirao",
            "Borabod",
            "Calasgasan",
            "Camambugan",
            "Cobangbang",
            "Dogongan",
            "Gahonon",
            "Gubat",
            "Lag-on",
            "Magang",
            "Mambalite",
            "Mancruz",
            "Pamorangon",
            "San Isidro"
        ),
        "Jose Panganiban" to listOf(
            "Bagong Bayan",
            "Calero",
            "Dahican",
            "Dayhagan",
            "Larap",
            "Luklukan Norte",
            "Luklukan Sur",
            "Motherlode",
            "Nakalaya",
            "North Poblacion",
            "Osmeña",
            "Pag-asa",
            "Parang",
            "Plaridel",
            "Salvacion",
            "San Isidro",
            "San Jose",
            "San Martin",
            "San Pedro",
            "San Rafael",
            "Santa Cruz",
            "Santa Elena",
            "Santa Milagrosa",
            "Santa Rosa Norte",
            "Santa Rosa Sur",
            "South Poblacion",
            "Tamisan"
        ),
        "Labo" to listOf(
            "Anahaw",
            "Anameam",
            "Awitan",
            "Baay",
            "Bagacay",
            "Bagong Silang I",
            "Bagong Silang II",
            "Bagong Silang III",
            "Bakiad",
            "Bautista",
            "Bayabas",
            "Bayan-bayan",
            "Benit",
            "Bulhao",
            "Cabatuhan",
            "Cabusay",
            "Calabasa",
            "Canapawan",
            "Daguit",
            "Dalas",
            "Dumagmang",
            "Exciban",
            "Fundado",
            "Guinacutan",
            "Guisican",
            "Gumamela",
            "Iberica",
            "Kalamunding",
            "Lugui",
            "Mabilo I",
            "Mabilo II",
            "Macogon",
            "Mahawan-hawan",
            "Malangcao-Basud",
            "Malasugui",
            "Malatap",
            "Malaya",
            "Malibago",
            "Maot",
            "Masalong",
            "Matanlang",
            "Napaod",
            "Pag-asa",
            "Pangpang",
            "Pinya",
            "San Antonio",
            "San Francisco",
            "Santa Cruz",
            "Submakin",
            "Talobatib",
            "Tigbinan",
            "Tulay na Lupa"
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
    private fun handleBackNavigation() {
        val bundle = Bundle().apply {
            putInt("selectedFragmentId", null ?: R.id.navigation_Home)
        }
        when (from) {
            "home" -> {
                bundle.putInt("selectedFragmentId", null ?: R.id.navigation_Home)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
            "service" -> {
                bundle.putInt("selectedFragmentId", null ?: R.id.navigation_services)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
            else -> {
                bundle.putInt("selectedFragmentId", null ?: R.id.navigation_Home)
                findNavController().navigate(R.id.userHolderFragment, bundle)
            }
        }

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            selectedFragmentId = it.getInt("selectedFragmentId", R.id.navigation_services)
            from = it.getString("from")
        }
        binding.backButton.setOnClickListener {
            val report = binding.reportTypeSpinner.text.toString().trim()
            val concern = binding.concernSpinner.text.toString().trim()
            val concernDescription = binding.concernDescription.text.toString().trim()

            if (report.isNotEmpty() || concern.isNotEmpty() || concernDescription.isNotEmpty()) {
                DialogUtils.showWarningMessage(
                    requireActivity(),
                    "Warning",
                    "Are you sure you want to exit? Changes will not be saved."
                ) { sweetAlertDialog ->
                    sweetAlertDialog.dismissWithAnimation()
                    handleBackNavigation()
                }
            } else {
                findNavController().navigateUp()
            }

        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val report = binding.reportTypeSpinner.text.toString().trim()
                    val concern = binding.concernSpinner.text.toString().trim()
                    val concernDescription = binding.concernDescription.text.toString().trim()

                    if (report.isNotEmpty() || concern.isNotEmpty() || concernDescription.isNotEmpty()) {
                        DialogUtils.showWarningMessage(
                            requireActivity(),
                            "Warning",
                            "Are you sure you want to exit? Changes will not be saved."
                        ) { sweetAlertDialog ->
                            sweetAlertDialog.dismissWithAnimation()
                            handleBackNavigation()
                        }
                    } else {
                        findNavController().navigateUp()
                    }
                }
            })


        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        progressDialog = ProgressDialog(this.requireContext())
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        selectedImage = Uri.EMPTY

        val concerns = complaints.keys.toList()
        val concernsAdapter = ArrayAdapter(requireContext(), R.layout.address_item_views, concerns)

        binding.reportTypeSpinner.setAdapter(concernsAdapter)
        binding.reportTypeSpinner.setOnItemClickListener { parent, view, position, id ->
            val selectedConcern = parent.getItemAtPosition(position).toString()
            val barangays = complaints[selectedConcern] ?: emptyList()
            val barangayAdapter =
                ArrayAdapter(requireContext(), R.layout.address_item_views, barangays)
            binding.concernSpinner.setAdapter(barangayAdapter)
        }
        binding.concernSpinner.setOnItemClickListener { parent, view, position, id ->
        }
        val municipalities = municipalitiesWithBarangays.keys.toList()
        val municipalityAdapter =
            ArrayAdapter(requireContext(), R.layout.address_item_views, municipalities)
        binding.tvMucipality.setAdapter(municipalityAdapter)
        binding.tvMucipality.setOnItemClickListener { parent, view, position, id ->
            val selectedMunicipality = parent.getItemAtPosition(position).toString()
            val barangays = municipalitiesWithBarangays[selectedMunicipality] ?: emptyList()
            val barangayAdapter =
                ArrayAdapter(requireContext(), R.layout.address_item_views, barangays)
            binding.tvBrgy.setAdapter(barangayAdapter)
        }
        binding.tvBrgy.setOnItemClickListener { parent, view, position, id ->
        }

        binding.fileUploadContainer.setOnClickListener {
            showImagePickerDialog()
        }
        binding.submitButton.setOnClickListener {
            validateData()
        }

    }

    private fun validateData() {
        val report = binding.reportTypeSpinner.text.toString().trim()
        val concern = binding.concernSpinner.text.toString().trim()
        val concernDiscription = binding.concernDescription.text.toString().trim()
        val municipality = binding.tvMucipality.text.toString().trim()
        val barangay = binding.tvBrgy.text.toString().trim()
        when {
            report.isEmpty() -> Toast.makeText(
                this.requireContext(),
                "Complete Report Type",
                Toast.LENGTH_SHORT
            ).show()

            concern.isEmpty() -> Toast.makeText(
                this.requireContext(),
                "Complete Concern",
                Toast.LENGTH_SHORT
            ).show()

            concernDiscription.isEmpty() -> Toast.makeText(
                this.requireContext(),
                "Concern Discription Cannot Be Empty",
                Toast.LENGTH_SHORT
            ).show()

            municipality.isEmpty() -> Toast.makeText(
                this.requireContext(),
                "Municipality Cannot Be Empty",
                Toast.LENGTH_SHORT
            ).show()

            barangay.isEmpty() -> Toast.makeText(
                this.requireContext(),
                "Barangay Cannot Be Empty",
                Toast.LENGTH_SHORT
            ).show()

            !::selectedImage.isInitialized -> Toast.makeText(
                this.requireContext(), "Please Upload a Picture",
                Toast.LENGTH_SHORT
            ).show()

            else -> uploadData()
        }

    }

    private fun uploadData() {
        progressDialog.setMessage("Uploading Image...")
        progressDialog.show()
        val uid = auth.uid

        val reference = storage.reference.child("Complaint Images")
            .child(uid!!)
        reference.putFile(selectedImage).addOnCompleteListener {
            if (it.isSuccessful) {
                reference.downloadUrl.addOnSuccessListener { task ->
                    // Pass the RFID data to uploadInfo
                    uploadDB(task.toString())

                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(
                    this@ReportFragment.requireContext(),
                    "Error uploading image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun uploadDB(imageUrl: String) {
        progressDialog.setMessage("Uploading Your Complaint...")
        progressDialog.show()
        val reportText = binding.reportTypeSpinner.text.toString().trim()
        val concernText = binding.concernSpinner.text.toString().trim()
        val concernDescriptionText = binding.concernDescription.text.toString().trim()
        val municipality = binding.tvMucipality.text.toString().trim()
        val barangay = binding.tvBrgy.text.toString().trim()
        val street = binding.tvStreet.text.toString().trim()
        val uid = auth.uid
        val timestamp = System.currentTimeMillis() / 1000

        val report: HashMap<String, Any?> = hashMapOf(
            "uid" to uid,
            "reportTitle" to reportText,
            "concern" to concernText,
            "concernDescription" to concernDescriptionText,
            "image" to imageUrl,
            "timestamp" to timestamp,
            "status" to "Sent",
            "address" to "$barangay, $municipality $street"
        )

        val firestore = FirebaseFirestore.getInstance()
        try {
            firestore.collection("users/$uid/my_complaints")
                .document(timestamp.toString())
                .set(report)
                .addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        checkSuperAdminAndUpload(
                            reportText, concernText, municipality,
                            timestamp.toString()
                        )
                    } else {
                        Toast.makeText(
                            this@ReportFragment.requireContext(),
                            task.exception?.message ?: "Error uploading complaint",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(
                this.requireContext(),
                "Error uploading data: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun checkSuperAdminAndUpload(
        reportText: String,
        concernText: String,
        municipality: String,
        timestamp: String
    ) {
        val areaMap = mapOf(
            1 to listOf(
                "Basud",
                "Mercedes",
                "Daet",
                "San Lorenzo Ruiz",
                "San Vicente",
                "Talisay",
                "Vinzons"
            ),
            2 to listOf("Labo", "Santa Elena", "Capalonga"),
            3 to listOf("Paracale", "Jose Panganiban")
        )

        val areaKey = areaMap.entries.find { it.value.contains(municipality) }?.key
        Log.d("ReportFragment", "Municipality: $municipality, Area Key: $areaKey")
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .whereEqualTo("userType", "admin")
            .get()
            .addOnSuccessListener { adminQuerySnapshot ->
                if (!adminQuerySnapshot.isEmpty) {
                    var adminFound = false

                    adminQuerySnapshot.documents.forEach { document ->
                        val userArea = document.getString("area")
                        Log.d("AdminUser", "User ID: ${document.id}, Data: ${document.data}")
                        if (userArea == areaKey.toString()) {
                            adminFound = true
                            uploadComplaintAgain(reportText, concernText, timestamp)
                        }
                    }
                    if (!adminFound) {
                        Toast.makeText(
                            this@ReportFragment.requireContext(),
                            "No matching admin users found for the specified area.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@ReportFragment.requireContext(),
                        "No admin users found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@ReportFragment.requireContext(),
                    "Error fetching admin users: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun uploadComplaintAgain(
        reportText: String,
        concernText: String,
        timestamp: String
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val report = hashMapOf(
            "isRead" to false,
            "status" to false,
            "text" to concernText,
            "timestamp" to timestamp,
            "title" to reportText
        )

        firestore.collection("users")
            .whereEqualTo("userType", "admin")
            .get()
            .addOnSuccessListener { adminQuerySnapshot ->
                if (!adminQuerySnapshot.isEmpty) {
                    for (adminDocument in adminQuerySnapshot.documents) {
                        firestore.collection("users/${adminDocument.id}/notifications")
                            .document(timestamp)
                            .set(report)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    findNavController().navigateUp()
                                } else {
                                    Toast.makeText(
                                        this@ReportFragment.requireContext(),
                                        "Error uploading complaint to admin ${adminDocument.id}: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                    Toast.makeText(
                        this@ReportFragment.requireContext(),
                        "Complaints submitted to all admins successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ReportFragment.requireContext(),
                        "No admins found to notify.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@ReportFragment.requireContext(),
                    "Error fetching admin users: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }




    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose Image From")
            .setItems(options) { dialog: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            pickImageFromCamera()
                        } else {
                            requestCameraPermission()
                        }
                    }

                    1 -> pickImageFromGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickImageFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description")
        selectedImage =
            requireActivity().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )!!
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage)
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_GALLERY_CODE -> {
                    selectedImage = data?.data!!
                    binding.fileUploadContainer.setImageURI(selectedImage)
                    Log.d("ReportFragment", "Image selected from gallery: $selectedImage")
                }

                IMAGE_PICK_CAMERA_CODE -> {
                    binding.fileUploadContainer.setImageURI(selectedImage)
                    Log.d("ReportFragment", "Image captured from camera: $selectedImage")
                }
            }
        }
    }

}