package com.example.canorecoapp.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthProvider

class SignUpViewModel  : ViewModel() {
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var password: String = ""
    var confirmPass: String = ""
    var municipality: String = ""
    var barangay: String = ""
    var phone: String = ""
    var month : String = ""
    var day : String = ""
    var year : String = ""
    var meterNumber : String = ""
    var street : String = ""
    var area : String = ""
    var otp : String = ""
    var  token: PhoneAuthProvider.ForceResendingToken? = null
    var verificationId : String = ""
    var smsIsVerified : Boolean = false
    var resendAttempts = 0
    var skipOtpVerification = false
    var uid : String = ""
    var position : String = ""
    var authEmail : String = ""

    var image: Uri? = null

    // Add a method to set the image URI
    fun setImage2(uri: Uri?) {
        image = uri
    }


}