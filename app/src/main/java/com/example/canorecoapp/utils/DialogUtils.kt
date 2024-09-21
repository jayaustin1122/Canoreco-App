package com.example.canorecoapp.utils

import android.app.Activity
import android.graphics.Color
import cn.pedant.SweetAlert.SweetAlertDialog
import cn.pedant.SweetAlert.SweetAlertDialog.OnSweetClickListener

object DialogUtils {
    fun showLoading(activity: Activity?): SweetAlertDialog {
        val pDialog = SweetAlertDialog(activity, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = Color.parseColor("#A5DC86")
        pDialog.setTitleText("Loading")
        pDialog.setCancelable(false)
        pDialog.show()
        return pDialog
    }

    fun showBasicMessage(activity: Activity?, title: String?): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity)
            .setTitleText(title)
        dialog.show()
        return dialog // Return the dialog instance
    }

    fun showMessageWithContent(activity: Activity?, title: String?, content: String?): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity)
            .setTitleText(title)
            .setContentText(content)
        dialog.show()
        return dialog // Return the dialog instance
    }

    fun showErrorMessage(activity: Activity?, title: String?, content: String?): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(content)
        dialog.show()
        return dialog // Return the dialog instance
    }

    fun showWarningMessage(
        activity: Activity?, title: String?, content: String?,
        confirmListener: OnSweetClickListener?
    ): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("Yes")
            .setConfirmClickListener(confirmListener)
            .setCancelText("No")
            .setCancelClickListener { it.dismissWithAnimation() }
        dialog.show()
        return dialog
    }


    fun showSuccessMessage(activity: Activity?, title: String?, content: String?): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(title)
            .setContentText(content)
        dialog.show()
        return dialog // Return the dialog instance
    }

    fun showCustomIconMessage(
        activity: Activity?,
        title: String?,
        content: String?,
        imageResId: Int
    ): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity, SweetAlertDialog.CUSTOM_IMAGE_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setCustomImage(imageResId)
        dialog.show()
        return dialog // Return the dialog instance
    }

    fun showCancelableWarning(
        activity: Activity?, title: String?, content: String?,
        confirmListener: OnSweetClickListener?,
        cancelListener: OnSweetClickListener?
    ): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("Yes, delete it!")
            .setConfirmClickListener(confirmListener)
            .setCancelText("No, cancel plx!")
            .showCancelButton(true)
            .setCancelClickListener(cancelListener)
        dialog.show()
        return dialog // Return the dialog instance
    }

    fun showChangeStyleOnConfirm(activity: Activity?, title: String?, content: String?): SweetAlertDialog {
        val dialog = SweetAlertDialog(activity, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmText("Yes, delete it!")
            .setConfirmClickListener { sDialog ->
                sDialog
                    .setTitleText("Deleted!")
                    .setContentText("Your imaginary file has been deleted!")
                    .setConfirmText("OK")
                    .setConfirmClickListener(null)
                    .changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
            }
        dialog.show()
        return dialog // Return the dialog instance
    }
}
