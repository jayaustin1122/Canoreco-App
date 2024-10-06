package com.example.canorecoapp.utils

import android.app.ProgressDialog
import android.content.Context

object ProgressDialogUtils {
    private var progressDialog: ProgressDialog? = null

    fun showProgressDialog(context: Context, message: String) {
        dismissProgressDialog()
        progressDialog = ProgressDialog(context).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    fun dismissProgressDialog() {
        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        progressDialog = null
    }
}