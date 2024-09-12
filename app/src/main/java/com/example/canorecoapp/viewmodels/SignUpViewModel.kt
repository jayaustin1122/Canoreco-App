package com.example.canorecoapp.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel

class SignUpViewModel  : ViewModel() {
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var password: String = ""
    var address: String = ""
    var phone: String = ""
    var month : String = ""
    var day : String = ""
    var year : String = ""
    var meterNumber : String = ""

    var image: Uri? = null

    // Add a method to set the image URI
    fun setImage2(uri: Uri?) {
        image = uri
    }


}