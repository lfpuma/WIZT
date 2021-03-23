package com.wizt.activities.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler
import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException
import com.crashlytics.android.Crashlytics
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.wizt.R
import com.wizt.activities.MainActivity
import com.wizt.activities.WebViewActivity
import com.wizt.activities.auth.LoginActivity.Companion.GOOGLE_SIGN_IN
import com.wizt.common.aws.AppHelper
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.dialog.CustomProgressDialog
import com.wizt.extensions.setupClearButtonWithAction
import com.wizt.models.Global
import com.wizt.models.Profile
import com.wizt.models.User
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_register.*
import matteocrippa.it.karamba.isValidEmail
import kotlin.concurrent.thread

class RegisterActivity : AppCompatActivity() {
    private lateinit var userName: String
    private lateinit var password: String
    private lateinit var email: String
    private lateinit var countryCode: String
    private lateinit var phoneNumber: String

    private var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_register)

        val isFromChatBot = intent.getBooleanExtra(Constants.PREF_FROM_CHATBOT, false)

        if(isFromChatBot) {
            this.googleCL.visibility = View.VISIBLE
        }
        else this.googleCL.visibility = View.GONE

        btn_signin.setOnClickListener {
            gotoLoginActivity()
        }

        phoneETR.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            phoneLL.setBackgroundResource(
                if (hasFocus)
                    R.drawable.input_box_selected
                else
                    R.drawable.input_box
            )
        }

        init()

        googleAndFacebookSignIn()
    }

    fun onClick_reg(view: View) {
        if (view.id == facebook_reg.id) {
            if (checkbox_termsandconditions_register.isChecked )
                facebookOrigin_reg.performClick()
            else {
                MyToastUtil.showWarning(this,getString(R.string.termsandconditionagreenotice))
            }
        }
    }

    private fun googleAndFacebookSignIn() {

        //Google

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.google_client))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        google_reg.setOnClickListener {

            if(!checkbox_termsandconditions_register.isChecked) {
                MyToastUtil.showWarning(this,getString(R.string.termsandconditionagreenotice))
                return@setOnClickListener
            }

            showWaitDialog()
            startActivityForResult(googleSignInClient.signInIntent, GOOGLE_SIGN_IN)
        }

        callbackManager = CallbackManager.Factory.create()

        googleAuthSetup()

        // Facebook
        // Callback registration

        facebookOrigin_reg.setPermissions("public_profile", "email")
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                // App code
                getUserDetails(loginResult)
                //gotoMainPage()
                //ToastUtil.show(applicationContext, "Login Success")
            }

            override fun onCancel() {
                // App code
                //ToastUtil.show(applicationContext, "Login Cancel")
            }

            override fun onError(exception: FacebookException) {
                // App code
                //ToastUtil.show(applicationContext, "Login Error")
            }
        })

    }

    private fun googleAuthSetup() {

        val context = applicationContext

        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount != null) {
            //Log.d(TAG, "Google Signed In Account found - account=${googleAccount.toJson()} ")
            thread(start = true) {
                try {
                    val googleOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.google_client))
                        .requestEmail()
                        .build()
                    GoogleSignIn.getClient(context, googleOptions).silentSignIn().continueWith {
                        val account = it.getResult(ApiException::class.java)
                        federateWithGoogle(account!!)
                    }
                } catch (error: ApiException) {
                    // Silent ignore
                }
            }
        }

    }

    fun federateWithGoogle(account: GoogleSignInAccount) {
        if (account.idToken != null) {
            thread(start = true) {
            }
        } else {
        }
    }

    private fun getUserDetails(loginResult: LoginResult) {
        val fbAccessToken = loginResult.accessToken
        val request = GraphRequest.newMeRequest(loginResult.accessToken) { json_object, _ ->

            var user = User.emptyUser()

            val jsonString = json_object.toString()
            if (!jsonString.contains("id") || !jsonString.contains("name") || !jsonString.contains("email")) {
                MyToastUtil.showWarning(this,"Can't load your info from facebook.")
            } else {
                user.id = json_object["id"] as String
                user.name = json_object["name"] as String
                user.email = json_object["email"] as String
                user.phone_number = ""
                user.picture = "https://graph.facebook.com/" + user.id + "/picture?type=small"

                facebookLogin(user)
            }

        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    private fun facebookLogin(user: User) {
        val successCallback: () -> Unit = {
            getMyProfile()
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                closeWaitDialog()
                MyToastUtil.showWarning(this, message)
            }
        }

        APIManager.share.loginFaceBook(user, successCallback, errorCallback)
    }

    private fun getMyProfile() {
        // Get Profile
        val successCallback: (Profile) -> Unit = { profile: Profile ->
            Global.profile = profile
            PreferenceUtils.saveString(Constants.PROFILE_USER_NAME, profile.name)
            PreferenceUtils.saveString(Constants.PROFILE_USER_EMAIL, profile.email)
            if (profile.picture != null) {
                PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, profile.picture!!)
            }
            else {
                PreferenceUtils.saveString(Constants.PROFILE_USER_PICTURE_AVATAR, "")
            }

            gotoMainPage(profile.email)
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                closeWaitDialog()
                MyToastUtil.showWarning(this, message)
            }
        }

        APIManager.share.getMyProfile(successCallback, errorCallback)
    }

    private fun gotoMainPage(userEmailStr: String) {
        closeWaitDialog()
        PreferenceUtils.saveBoolean(Constants.PREF_IS_LOGIN, true)
        PreferenceUtils.saveString(Constants.PREF_USER_EMAIL, userEmailStr)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun init() {
        usernameETR.setupClearButtonWithAction()
        passwordETR.setupClearButtonWithAction()
        emailETR.setupClearButtonWithAction()
        confirmPasswordETR.setupClearButtonWithAction()
        phoneETR.setupClearButtonWithAction()

        btn_termsandconditions_register.setOnClickListener {
            //val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.termsandconditionurl)))
            //startActivity(browserIntent)
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        btn_signup.setOnClickListener {
            // Read user data and register

            if(!checkbox_termsandconditions_register.isChecked) {
                MyToastUtil.showWarning(this,getString(R.string.termsandconditionagreenotice))
                return@setOnClickListener
            }

            if (isValid()) {
                val userAttributes = CognitoUserAttributes()

                userName = usernameETR.text.toString()
                password = passwordETR.text.toString()
                email = emailETR.text.toString()

                if (!email.isValidEmail()) {
                    MyToastUtil.showNotice(this, "Invalid email")
                    return@setOnClickListener
                }

                // Config userAttributes
                var userInput = emailETR.text.toString()

                if (userInput.isNotEmpty()) {
                    userAttributes.addAttribute("email", userInput)
                }

                userInput = usernameETR.text.toString()
                if (userInput.isNotEmpty()) {
                    userAttributes.addAttribute("name", userInput)
                }

                userInput = phoneETR.text.toString()
                countryCode = ccp.selectedCountryCodeWithPlus
                phoneNumber = countryCode + userInput
                userAttributes.addAttribute("phone_number", phoneNumber)

                userInput = confirmPasswordETR.text.toString()
                if (userInput != password) {
                    MyToastUtil.showWarning(applicationContext, "The password does not match")
                    return@setOnClickListener
                }

                showWaitDialog()
                AppHelper.pool?.signUpInBackground(
                    email,
                    password,
                    userAttributes,
                    null,
                    signUpCallback
                )

                //registerUser()

            }
        }
    }

    private fun registerUser() {

        var user = User.emptyUser()
        user.name = usernameETR.text.toString()
        user.email = emailETR.text.toString()
        user.username = emailETR.text.toString()
        user.phone_number = phoneETR.text.toString()
        user.password = passwordETR.text.toString()

        val successCallback: () -> Unit = {
            this.runOnUiThread {
                MyToastUtil.showMessage(this,"Sign Up Successfully!")
                gotoLoginActivity()
            }
        }

        val existUserCallback: () -> Unit = {

            this.runOnUiThread {
                MyToastUtil.showWarning(this, "User with this email already exists.")
            }

        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                //MyToastUtil.showWarning(this, message)
            }
        }

        //showWaitDialog()
        APIManager.share.register(user, successCallback, existUserCallback,errorCallback)
    }

    private fun isValid(): Boolean {
        if (usernameETR.text.toString().isEmpty()) {
            MyToastUtil.showNotice(this, "user name is required")
            return false
        }

        if (emailETR.text.toString().isEmpty()) {
            MyToastUtil.showNotice(this, "user email is required")
            return false
        }

        if (!emailETR.text.toString().isValidEmail()) {
            MyToastUtil.showNotice(this, "Invalid email")
            return false
        }

        if (phoneETR.text.toString().isEmpty()) {
            MyToastUtil.showNotice(this, "phone number is required")
            return false
        }

        if (passwordETR.text.toString().isEmpty()) {
            MyToastUtil.showNotice(this, "password is required")
            return false
        }

        if (confirmPasswordETR.text.toString().isEmpty()) {
            MyToastUtil.showNotice(this, "confirm password is required")
            return false
        }
        return true
    }

    private fun confirmSignUp(details: CognitoUserCodeDeliveryDetails) {
        val intent = Intent(this, AuthActivity::class.java)
        intent.putExtra("source", "signup")
        intent.putExtra("name", userName)
        intent.putExtra("email", email)
        intent.putExtra("destination", details.destination)
        intent.putExtra("deliveryMed", details.deliveryMedium)
        intent.putExtra("attribute", details.attributeName)
        startActivityForResult(intent, Constants.ACTIVITY_RESULT_AUTH_OK)
    }

    private var signUpCallback: SignUpHandler = object : SignUpHandler {

        override fun onSuccess(
            cognitoUser: CognitoUser,
            userConfirmed: Boolean,
            details: CognitoUserCodeDeliveryDetails
        ) {
            // Check signUpConfirmationState to see if the user is already confirmed
            closeWaitDialog()

            if (userConfirmed) {
                // User is already confirmed
                //MyToastUtil.showMessage(this@RegisterActivity, "$userName has been Confirmed already.")
                registerUser()
            } else {
                // User is not confirmed

                confirmSignUp(details)
            }
        }

        override fun onFailure(exception: Exception) {
            // Sign-up failed, check exception for the cause
            closeWaitDialog()

            val errorMsg = when (exception) {
                is UsernameExistsException -> exception.errorMessage
                is InvalidParameterException -> "Password length must be equal or greater than 6."
                else -> "Error occurred while registering new user."
            }

            MyToastUtil.showWarning(this@RegisterActivity, errorMsg!!)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.ACTIVITY_RESULT_AUTH_OK && resultCode == Activity.RESULT_OK) {
            var email: String? = null
            if (data != null) {
                if (data.hasExtra("email")) {
                    email = data.getStringExtra("email")
                }
            }
            exit("", "")
        }


        when (requestCode) {

            GOOGLE_SIGN_IN -> {

                closeWaitDialog()

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)

                    var user = User.emptyUser()

                    user.id = account.id!!
                    user.name = account.displayName!!
                    user.email = account.email!!
                    user.phone_number = "123456"
                    user.picture = account.photoUrl!!.toString()

                    googleLogin(user)
                } catch (error: ApiException) {
                    //toast("Google Authentication failed: ${error.message}")
                }
            }
        }
    }

    private fun googleLogin(user: User) {
        val successCallback: () -> Unit = {
            getMyProfile()
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                closeWaitDialog()
                MyToastUtil.showWarning(this, message)
            }
        }

        APIManager.share.loginGoogle(user, successCallback, errorCallback)
    }

    private fun showWaitDialog() {
        closeWaitDialog()
        CustomProgressDialog.instance.show(this)
    }

    private fun closeWaitDialog() {
        CustomProgressDialog.instance.dismiss()
    }

    private fun exit(email: String?, password: String?) {

        registerUser()
        //gotoLoginActivity()
    }

    fun gotoLoginActivity() {
        val intent = Intent(applicationContext, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}