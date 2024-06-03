package com.example.canorecoapp.utils

import android.app.ProgressDialog
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class FirebaseUtils {
    lateinit var auth: FirebaseAuth
    lateinit var storage: FirebaseStorage
    lateinit var database: FirebaseDatabase
    lateinit var progressDialog: ProgressDialog

    fun initialize(context: Context) {
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
    }
}