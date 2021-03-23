package com.wizt.activities.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import com.wizt.R
import com.wizt.extensions.setupClearButtonWithAction
import com.wizt.utils.MyToastUtil
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var newPasswordStr : String
    private lateinit var confirmCodeStr : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_reset_password)

        init()
    }

    private fun init() {
        val extras = intent.extras
        if (extras != null) {
            if (extras.containsKey("destination")) {
            }
        }

        resetPWET.setupClearButtonWithAction()
        confirmCodeET.setupClearButtonWithAction()

        btn_reset_password.setOnClickListener {
            getConfirmCode()
        }
    }

    private fun getConfirmCode() {
        if (!checkValidation()) {
            return
        }
        exitActivity(newPasswordStr, confirmCodeStr)
    }

    private fun checkValidation(): Boolean {
        newPasswordStr = resetPWET.text.toString()
        confirmCodeStr = confirmCodeET.text.toString()

        if (newPasswordStr.isEmpty()) {
            MyToastUtil.showNotice(this, "Password required")
            return false
        }

        if (confirmCodeStr.isEmpty()) {
            MyToastUtil.showNotice(this, "Confirm code required")
            return false
        }

        return true
    }

    private fun exitActivity(newPass: String?, code: String?) {
        val intent = Intent()
        intent.putExtra("newPass", newPass ?: "")
        intent.putExtra("code", code ?: "")
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}