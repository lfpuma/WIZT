package com.wizt.activities.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.VerificationHandler
import com.crashlytics.android.Crashlytics
import com.wizt.R
import com.wizt.common.aws.AppHelper
import com.wizt.utils.MyToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_auth.*

class AuthActivity : AppCompatActivity() {
    companion object {
        const val TAG = "WIZT::AuthActivity"
    }
    private lateinit var userEmailStr: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_auth)

        init()
    }

    private fun init() {
        val extras = intent.extras

        if (extras != null) {
            userEmailStr = if (extras.containsKey("email"))
                extras.getString("email", "")
            else
                ""
        }

        verifyBtn.setOnClickListener {
            if (!checkValidation())
                return@setOnClickListener
            sendConfirmCode()
        }

        resendVerifyCode.setOnClickListener {
            reqConfCode()
        }

    }

    private fun reqConfCode() {
        AppHelper.pool?.getUser(userEmailStr)?.resendConfirmationCodeInBackground(resendConfirmCodeHandler)
    }

    private val resendConfirmCodeHandler: VerificationHandler = object : VerificationHandler {
        override fun onSuccess(details: CognitoUserCodeDeliveryDetails?) {
            blockEditText_tac.requestFocus()

            MyToastUtil.showMessage(
                this@AuthActivity,
                "Verification code sent to ${details?.destination} via ${details?.deliveryMedium}."
            )
        }

        override fun onFailure(exception: Exception?) {
            MyToastUtil.showWarning(this@AuthActivity, "Resend verification code failed.")
        }
    }

    private fun sendConfirmCode() {
        val confirmCode = blockEditText_tac.text.toString()
        AppHelper.pool?.getUser(userEmailStr)?.confirmSignUpInBackground(confirmCode, true, confirmHandler)
    }

    private val confirmHandler: GenericHandler = object : GenericHandler {
        override fun onSuccess() {
            MyToastUtil.showMessage(this@AuthActivity, "Email has been confirmed.")
            var intent = Intent()
            setResult(Activity.RESULT_OK,intent)
            finish()
        }

        override fun onFailure(exception: Exception?) {
            MyToastUtil.showWarning(this@AuthActivity, AppHelper.formatException(exception!!))
            Log.d(TAG, exception.localizedMessage)
        }
    }

    private fun checkValidation(): Boolean {
        val confirmStr = blockEditText_tac.text.toString()

        if (confirmStr.isEmpty()) {
            MyToastUtil.showNotice(this, "Please confirm code.")
            return false
        }
        return true
    }

}
