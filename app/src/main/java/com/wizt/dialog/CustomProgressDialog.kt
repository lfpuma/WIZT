package com.wizt.dialog

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.wizt.R

class CustomProgressDialog() {

    // Singleton
    companion object{
        var instance = CustomProgressDialog()
    }

    private var mAlertDialog: AlertDialog? = null

    fun show(context: Context){
        // Inflate the dialog with Custom view
        val mDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)

        //AlertDialogBuilder
        val mBuilder = AlertDialog.Builder(context).setView(mDialogView)

        // Show Dialog
        mAlertDialog = mBuilder.show()
        mAlertDialog?.setCanceledOnTouchOutside(false)
        mAlertDialog?.window?.setBackgroundDrawableResource(R.color.colorTransparency)
    }

    fun dismiss() {
        mAlertDialog?.dismiss()
    }
}