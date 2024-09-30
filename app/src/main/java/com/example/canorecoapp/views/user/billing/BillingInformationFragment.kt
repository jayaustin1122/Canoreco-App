import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.canorecoapp.R
import com.example.canorecoapp.adapter.PaymentAdapter
import com.example.canorecoapp.databinding.FragmentBillingInformationBinding
import com.example.canorecoapp.models.PaymentInfo
import com.example.canorecoapp.utils.ApiResponse
import com.example.canorecoapp.utils.DialogUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class BillingInformationFragment : Fragment() {
    private lateinit var binding: FragmentBillingInformationBinding
    private var loadingDialog: SweetAlertDialog? = null
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    private lateinit var adapter: PaymentAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBillingInformationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("selectedFragmentId", null ?: R.id.navigation_services)
            }
            findNavController().navigate(R.id.userHolderFragment, bundle)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val report = binding.etPayment.text.toString().trim()
                    if (report.isNotEmpty()) {
                        DialogUtils.showWarningMessage(
                            requireActivity(),
                            "Warning",
                            "Are you sure you want to exit? Changes will not be saved."
                        ) { sweetAlertDialog ->
                            sweetAlertDialog.dismissWithAnimation()
                            val bundle = Bundle().apply {
                                putInt("selectedFragmentId", R.id.navigation_services)
                            }
                            findNavController().navigate(R.id.userHolderFragment, bundle)
                        }
                    } else {
                        findNavController().navigateUp()
                    }
                }
            })

        binding.buttonPayBill.setOnClickListener {

            val payment = binding.etPayment.text.toString().trim()
            val accountNumber = binding.etAccountNumber.text.toString().trim()
            if (payment.isNotEmpty() || accountNumber.isNotEmpty()) {
                try {
                    val amount = payment.toDouble()
                    val amountInCents = when {
                        amount < 1.00 -> (amount * 10000).toInt()
                        amount < 100 -> (amount * 100).toInt()
                        else -> (amount * 100).toInt()
                    }
                    Log.d(
                        "BillingInformation",
                        "Converted amount: $amountInCents cents for input: $payment"
                    )
                    DialogUtils.showWarningMessage(
                        requireActivity(),
                        "Warning",
                        "Are you sure you want to pay this bill?"
                    ) { sweetAlertDialog ->
                        sweetAlertDialog.dismissWithAnimation()
                        showLoadingDialog() // Show loading
                        makeRequest(amountInCents, accountNumber)
                    }

                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(
                    context,
                    "Please enter a valid amount or account number",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
        retrieveLinkPayment()
        showLoadingDialog()
        Handler(Looper.getMainLooper()).postDelayed({

           dismissLoadingDialog()
        }, 2000)


    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = DialogUtils.showLoading(requireActivity())
        }
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
    private fun retrieveLinkPayment() {

        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val querySnapshot = db.collection("users")
                        .document(currentUser.uid)
                        .collection("myPayments")
                        .get()
                        .await()
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents.firstOrNull()

                        document?.let {
                            val paymentIds = it.getString("paymentIds") ?: ""
                            Log.d("BillingInformation", "Retrieved paymentIds: $paymentIds")
                            val retrievedData = retrieveData(paymentIds)
                            if (retrievedData != null) {
                                Log.d("BillingInformation", "Retrieved data: $retrievedData")
                                withContext(Dispatchers.Main) {

                                    lifecycleScope.launch {
                                        val apiResponse = retrieveData(paymentIds)
                                        apiResponse?.data?.attributes?.payments?.let { payments ->
                                            val paymentList = payments.map { it.data.attributes }
                                            adapter = PaymentAdapter(paymentList)
                                            binding.recyclerPayments.adapter = adapter
                                            binding.recyclerPayments.layoutManager = LinearLayoutManager(requireContext())
                                        }
                                    } }
                            }
                        } ?: run {
                            Log.d("BillingInformation", "No documents found in myPayments")
                        }
                    } else {
                        Log.d("BillingInformation", "No payments found for user")
                    }
                } catch (e: Exception) {
                    Log.e("BillingInformation", "Error retrieving data: ${e.message}")
                }
            }
        } else {
            Log.e("BillingInformation", "User is not logged in")
        }
    }
    private suspend fun retrieveData(paymentIds: String): ApiResponse? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.paymongo.com/v1/links/$paymentIds")
            .get()
            .addHeader("accept", "application/json")
            .addHeader("authorization", "Basic c2tfdGVzdF9vVEJzbUJjdjhxZ1A5UXpBbmJld2prZUs6")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.let { body ->
                            Log.d("BillingInformation", "retrieveData successful: $body")
                            // Parse the JSON using Gson

                            val gson = Gson()
                            return@withContext gson.fromJson(body, ApiResponse::class.java)
                        }
                    } else {
                        Log.e("BillingInformation", "retrieveData failed: ${response.code}, Message: ${response.message}")
                        null
                    }
                }
            } catch (e: IOException) {
                Log.e("BillingInformation", "retrieveData IOException: ${e.message}")
                null
            }
        }
    }

    private fun makeRequest(amount: Int, accountNumber: String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(
            mediaType,
            "{\"data\":{\"attributes\":{\"amount\":$amount,\"description\":\"Payment Bill - $accountNumber\",\"remarks\":\"$accountNumber\"}}}"
        )

        val request = Request.Builder()
            .url("https://api.paymongo.com/v1/links")
            .post(body)
            .addHeader("accept", "application/json")
            .addHeader("content-type", "application/json")
            .addHeader("authorization", "Basic c2tfdGVzdF9vVEJzbUJjdjhxZ1A5UXpBbmJld2prZUs6")
            .build()

        lifecycleScope.launch {
            try {
                val response: Response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val responseData = responseBody.string()
                        Log.d("BillingInformation", "Response: $responseData")
                        val checkoutUrl = extractCheckoutUrl(responseData)
                        Log.d("BillingInformation", "Checkout URL: $checkoutUrl")
                        dismissLoadingDialog() // Dismiss loading once successful
                        displayCheckoutLink(checkoutUrl)
                        uploadPaymentDataToFirestore(responseData, accountNumber)

                    } ?: Log.e("BillingInformation", "Response body is null")
                } else {
                    Log.e(
                        "BillingInformation",
                        "Unexpected code: ${response.code}, Message: ${response.message}"
                    )
                    dismissLoadingDialog() // Dismiss on failure
                }
            } catch (e: IOException) {
                Log.e("BillingInformation", "Request Failed: ${e.message}")
                dismissLoadingDialog() // Dismiss on exception
            }
        }
    }

    private fun extractCheckoutUrl(responseData: String): String? {
        val jsonObject = JSONObject(responseData)
        return jsonObject.getJSONObject("data")
            .getJSONObject("attributes")
            .getString("checkout_url")
    }

    private fun displayCheckoutLink(checkoutUrl: String?) {
        if (checkoutUrl != null) {
            val webViewBottomSheet = WebViewBottomSheetDialogFragment(checkoutUrl)
            webViewBottomSheet.show(parentFragmentManager, "WebViewBottomSheetDialog")
        } else {
            Log.e("BillingInformation", "Checkout URL is null")
        }
    }
    private fun uploadPaymentDataToFirestore(responseData: String, accountNumber: String) {
        currentUser?.let { user ->
            try {
                val jsonObject = JSONObject(responseData)
                val dataObject = jsonObject.getJSONObject("data")
                val attributesObject = dataObject.getJSONObject("attributes")

                // Extract the `id` separately
                val paymentId = dataObject.getString("id")

                // Extracting other values from the JSON
                val paymentInfo = PaymentInfo(
                    id = paymentId,
                    type = dataObject.getString("type"),
                    amount = attributesObject.getInt("amount"),
                    currency = attributesObject.getString("currency"),
                    description = attributesObject.getString("description"),
                    status = attributesObject.getString("status"),
                    checkoutUrl = attributesObject.getString("checkout_url"),
                    referenceNumber = attributesObject.getString("reference_number"),
                    createdAt = attributesObject.getLong("created_at"),
                    updatedAt = attributesObject.getLong("updated_at")
                )

                // Create a map to store in Firestore for the payment data
                val paymentData = hashMapOf(
                    "amount" to paymentInfo.amount,
                    "accountNumber" to accountNumber,
                    "description" to paymentInfo.description,
                    "status" to paymentInfo.status,
                    "checkoutUrl" to paymentInfo.checkoutUrl,
                    "referenceNumber" to paymentInfo.referenceNumber,
                    "createdAt" to paymentInfo.createdAt,
                    "updatedAt" to paymentInfo.updatedAt,
                    "response" to responseData,
                    "timestamp" to System.currentTimeMillis(),
                    "paymentIds" to paymentId
                )

                // Upload to user payments path
                db.collection("users")
                    .document(user.uid)
                    .collection("myPayments")
                    .add(paymentData)
                    .addOnSuccessListener {
                        Log.d("BillingInformation", "Payment data uploaded successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BillingInformation", "Failed to upload payment data: ${e.message}")
                    }

            } catch (e: Exception) {
                Log.e("BillingInformation", "Error parsing response data: ${e.message}")
            }
        } ?: Log.e("BillingInformation", "User is not logged in")
    }



}
