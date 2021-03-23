package com.wizt.common.base

import android.support.v7.app.AppCompatActivity
import com.wizt.dialog.CustomProgressDialog
import com.wizt.utils.MyToastUtil

open class BaseActivity : AppCompatActivity() {

    /**
     * API Request Error Handler
     */
    val errorCallback: (String) -> Unit = { message ->
        this.runOnUiThread {
            MyToastUtil.showWarning(this, message)
        }
    }

    private fun showWarningToast(message: String) {
        this.runOnUiThread {
            MyToastUtil.showWarning(this, message)
        }
    }

    fun showWaitDialog() {
        closeWaitDialog()
        CustomProgressDialog.instance.show(this)
    }

    fun closeWaitDialog() {
        CustomProgressDialog.instance.dismiss()
    }
}