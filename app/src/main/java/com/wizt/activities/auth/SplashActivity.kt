package com.wizt.activities.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.wizt.R
import android.content.pm.PackageManager
import android.content.Context
import android.util.Base64
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.wizt.activities.MainActivity
import com.wizt.activities.TutorialActivity
import com.wizt.activities.chatbot.ChatBotActivity
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.models.Global
import com.wizt.models.Profile
import com.wizt.utils.PreferenceUtils
import io.fabric.sdk.android.Fabric
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class SplashActivity : AppCompatActivity() {
    companion object {
        const val TAG = "WIZT:SplashActivity"
    }

    private var mDelayHandler: Handler? = null
    private val mDelay: Long = 1000

    private val mRunnable: Runnable = Runnable {
        if (!isFinishing) {

            val isViewTutorial: Boolean = PreferenceUtils.getBoolean(Constants.PREF_TUTORIAL)
            if (!isViewTutorial) {
                PreferenceUtils.saveBoolean(Constants.PREF_TUTORIAL, true)
                val intent = Intent(applicationContext, TutorialActivity::class.java)
                startActivity(intent)
                finish()
                return@Runnable
            }

            if (PreferenceUtils.getBoolean(Constants.PREF_IS_LOGIN)) {
                myProfile()
            }
            else {
                gotoLoginActivity()

//                gotoChatBotActivity()
            }

        }
    }

    fun gotoChatBotActivity() {
        val intent = Intent(applicationContext, ChatBotActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun gotoLoginActivity() {
        val intent = Intent(applicationContext, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_splash)

        //Initialize the Handler
        mDelayHandler = Handler()

        //Navigate with delay
        mDelayHandler!!.postDelayed(mRunnable, mDelay)

        printHashKey(applicationContext)
    }

    override fun onDestroy() {

        if (mDelayHandler != null) {
            mDelayHandler!!.removeCallbacks(mRunnable)
        }

        super.onDestroy()
    }

    private fun printHashKey(pContext: Context) {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i(TAG, "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "printHashKey()", e)
        } catch (e: Exception) {
            Log.e(TAG, "printHashKey()", e)
        }

    }

    private fun myProfile() {
        // Get Profile
        val successCallback: (Profile) -> Unit = { profile: Profile ->
            Global.profile = profile
            PreferenceUtils.saveString(Constants.PROFILE_USER_NAME, profile.name)
            PreferenceUtils.saveString(Constants.PROFILE_USER_EMAIL, profile.email)
            if (profile.picture != null) {
                PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, profile.picture!!)
            }

            checkSubscription()
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                //MyToastUtil.showWarning(this, message)

                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
                finish()

            }
        }

        APIManager.share.getMyProfile(successCallback, errorCallback)
    }

    private fun checkSubscription() {
        Log.d(TAG,"Check Subscription")
        val successCallback: (String) -> Unit = { message ->
            Log.d(TAG,"Subscription Status -> " + message)
            val msg = message.replace("\"","")
            if (!"active".equals(msg)) {
                Global.isSubscription = false
            } else {
                Global.isSubscription = true
            }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val errorCallback: (String) -> Unit = { message ->
            Global.isSubscription = false
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        APIManager.share.checkSubscribe(successCallback, errorCallback)
    }
}
