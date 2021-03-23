package com.wizt.activities.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler
import com.crashlytics.android.Crashlytics
import com.wizt.R
import com.wizt.common.aws.AppHelper
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.dialog.CustomProgressDialog
import com.wizt.extensions.setupClearButtonWithAction
import com.wizt.models.User
import com.wizt.utils.MyToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_forgot_password.*
import matteocrippa.it.karamba.isValidEmail

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var userEmailStr: String

    private lateinit var continuation: ForgotPasswordContinuation
    private var myNewPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_forgot_password)

        init()
    }

    private fun init() {
        emailETF.setupClearButtonWithAction()

        btn_forgot_password.setOnClickListener {
            forgotPasswordUser()
        }
    }

    private fun forgotPasswordUser() {
        userEmailStr = emailETF.text.toString()

        if (!userEmailStr.isValidEmail()) {
            MyToastUtil.showNotice(this, "Invalid email address.")
            return
        }

        CustomProgressDialog.instance.show(this)
        AppHelper.pool?.getUser(userEmailStr)?.forgotPasswordInBackground(forgotPasswordHandler)
    }

    private fun getForgotPasswordCode(forgotPasswordContinuation: ForgotPasswordContinuation) {
        CustomProgressDialog.instance.dismiss()
        this.continuation = forgotPasswordContinuation
        val intent = Intent(this, ResetPasswordActivity::class.java)
        intent.putExtra("destination", forgotPasswordContinuation.parameters.destination)
        intent.putExtra("deliveryMed", forgotPasswordContinuation.parameters.deliveryMedium)
        startActivityForResult(intent, Constants.ACTIVITY_RESULT_RESET_PW_OK)
    }

    // Callback
    private val forgotPasswordHandler: ForgotPasswordHandler = object : ForgotPasswordHandler {
        override fun onSuccess() {
            //showWaitDialog()
            resetPassword()
        }

        override fun onFailure(exception: Exception?) {
            CustomProgressDialog.instance.dismiss()
            MyToastUtil.showMessage(this@ForgotPasswordActivity, AppHelper.formatException(exception!!))
            //exitActivity()
        }

        override fun getResetCode(continuation: ForgotPasswordContinuation?) {
            getForgotPasswordCode(continuation!!)
        }
    }

    private fun exitActivity() {
        val intent = Intent()
        userEmailStr = emailETF.text.toString()
        intent.putExtra("userEmail", userEmailStr)
        setResult(Activity.RESULT_OK, intent)
        this@ForgotPasswordActivity.finish()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_RESULT_RESET_PW_OK && resultCode == Activity.RESULT_OK) {
            val newPass = data?.getStringExtra("newPass")
            val confirmCode = data?.getStringExtra("code")

            if (newPass != null && confirmCode != null) {
                if (newPass.isNotEmpty() && confirmCode.isNotEmpty()) {
                    //showWaitDialog()
                    myNewPassword = newPass
                    continuation.setPassword(newPass)
                    continuation.setVerificationCode(confirmCode)
                    continuation.continueTask()
                }
            }
        }
    }

    private fun showWaitDialog() {
        closeWaitDialog()
        CustomProgressDialog.instance.show(this)
    }

    private fun closeWaitDialog() {
        CustomProgressDialog.instance.dismiss()
    }

    private fun resetPassword(){

        val user = User.emptyUser()

        user.email = userEmailStr
        user.password = myNewPassword


        val successCallback: () -> Unit = {
            this.runOnUiThread {
                MyToastUtil.showMessage(this@ForgotPasswordActivity, "Password successfully changed!")
                //closeWaitDialog()
                exitActivity()
            }
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                //closeWaitDialog()
                MyToastUtil.showWarning(this, "Failed")
            }
        }

        //showWaitDialog()
        APIManager.share.resetPassword(user, successCallback, errorCallback)
    }
}
