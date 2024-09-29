import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.canorecoapp.databinding.FragmentBillingInformationBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class BillingInformationFragment : Fragment() {
    private lateinit var binding: FragmentBillingInformationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBillingInformationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonPayBill.setOnClickListener {
            val payment = binding.etPayment.text.toString().trim()
            if (payment.isNotEmpty()) {
                try {
                    val amount = payment.toDouble() // Parse the input as a double
                    val amountInCents = when {
                        amount < 1.00 -> (amount * 10000).toInt() // For values less than 1.00
                        amount < 100 -> (amount * 100).toInt() // For whole number amounts below 100 (like 1, 2)
                        else -> (amount * 100).toInt() // For amounts 100 and above
                    }
                    Log.d("BillingInformation", "Converted amount: $amountInCents cents for input: $payment") // Log the conversion
                    makeRequest(amountInCents) // Pass the calculated amount
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }



    }


        private fun makeRequest(amount: Int) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, "{\"data\":{\"attributes\":{\"amount\":$amount,\"description\":\"sds\",\"remarks\":\"sd\"}}}")

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
                        displayCheckoutLink(checkoutUrl)
                    } ?: Log.e("BillingInformation", "Response body is null")
                } else {
                    Log.e("BillingInformation", "Unexpected code: ${response.code}, Message: ${response.message}")

                }
            } catch (e: IOException) {
                Log.e("BillingInformation", "Request Failed: ${e.message}")
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
            val chooser = Intent.createChooser(intent, "Open with")
            startActivity(chooser)
        } else {
            Log.e("BillingInformation", "Checkout URL is null")
        }
    }

}