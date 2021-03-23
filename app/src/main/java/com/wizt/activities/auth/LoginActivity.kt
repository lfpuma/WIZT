package com.wizt.activities.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult
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
import com.wizt.common.aws.AppHelper
import com.wizt.common.base.BaseActivity
import com.wizt.common.constants.Constants
import com.wizt.common.http.APIManager
import com.wizt.common.http.SessionManager
import com.wizt.dialog.CustomProgressDialog
import com.wizt.extensions.setupClearButtonWithAction
import com.wizt.models.Global
import com.wizt.models.Profile
import com.wizt.models.User
import com.wizt.utils.MyToastUtil
import com.wizt.utils.PreferenceUtils
import com.wizt.utils.ToastUtil
import com.stripe.android.model.Token
import com.wizt.activities.WebViewActivity
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login.btn_signin
import kotlinx.android.synthetic.main.activity_register.*
import matteocrippa.it.karamba.isValidEmail
import kotlin.concurrent.thread

class LoginActivity : BaseActivity() {
    companion object {
        const val TAG = "WIZT:LoginActivity"
        const val GOOGLE_SIGN_IN = 0xffff
    }

    // UI
    private lateinit var userDialog: AlertDialog

    // User Details
    private lateinit var userEmailStr: String
    private lateinit var passwordStr: String

    private var callbackManager: CallbackManager? = null

    // AWS Cognito Continuation
    private var multiFactorAuthenticationContinuation: MultiFactorAuthenticationContinuation? = null
    private var forgotPasswordContinuation: ForgotPasswordContinuation? = null
    private var newPasswordContinuation: NewPasswordContinuation? = null
    private var mfaOptionsContinuation: ChooseMfaContinuation? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_login)

        //Google

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.google_client))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        google.setOnClickListener {

            if(!checkbox_termsandconditions.isChecked) {
                MyToastUtil.showWarning(this,getString(R.string.termsandconditionagreenotice))
                return@setOnClickListener
            }

            showWaitDialog()
            startActivityForResult(googleSignInClient.signInIntent, GOOGLE_SIGN_IN)
        }

        callbackManager = CallbackManager.Factory.create()

        btn_termsandconditions.setOnClickListener {
            //val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.termsandconditionurl)))
            //startActivity(browserIntent)
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        // Facebook
        // Callback registration

        facebookOrigin.setPermissions("public_profile", "email")
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

        btn_signin.setOnClickListener {
            //CustomProgressDialog.instance.show(this)

            if(!checkbox_termsandconditions.isChecked) {
                MyToastUtil.showWarning(this,getString(R.string.termsandconditionagreenotice))
                return@setOnClickListener
            }

            if (!checkValidation()) {
                return@setOnClickListener
            }
            signInUser()
        }

        btn_register.setOnClickListener {
            signUpNewUser()
        }

        btn_forgot_password.setOnClickListener {
            val intent = Intent(applicationContext, ForgotPasswordActivity::class.java)
            startActivityForResult(intent, Constants.ACTIVITY_RESULT_CHANGE_PW_OK)
        }

        // Initialize application
        AppHelper.init(this)
        initApp()

        // login automatically
        //if (PreferenceUtils.getBoolean(Constants.PREF_IS_LOGIN)) {
        //    loginAutomatically()
        //}
    }

    private fun initApp() {
        emailET.setupClearButtonWithAction()
        passwordET.setupClearButtonWithAction()

        googleAuthSetup()
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

    fun onClick(view: View) {
        if (view.id == facebook.id) {
            if (checkbox_termsandconditions.isChecked )
                facebookOrigin.performClick()
            else {
                MyToastUtil.showWarning(this,getString(R.string.termsandconditionagreenotice))
            }
        }
    }

    /**
     * Federate with Google authentication
     */
    fun federateWithGoogle(account: GoogleSignInAccount) {
        if (account.idToken != null) {
            Log.d(TAG, "Federating with Google")
            thread(start = true) {
//                with(service.identityManager.underlyingProvider) {
//                    clear()
//                    withLogins(mapOf("accounts.google.com" to account.idToken))
//                    refresh()
//                }

//                val user = User().apply {
//                    username = account.id!!
//                    tokens[Token.TokenType.ACCESS_TOKEN] = account.idToken!!
//                    userAttributes["provider"] = "accounts.google.com"
//                    userAttributes["email"] = account.email!!
//                }
                //Log.d(TAG, "Federated result: ${service.identityManager.isUserSignedIn}")
                //runOnUiThread { mCurrentUser.postValue(user) }
            }
        } else {
            Log.d(TAG, "Federating with Google (ID token is null, so nothing happening)")
        }
    }

//    private fun loginAutomatically() {
//        try {
//            val user = AppHelper.pool?.currentUser
//
//            Log.d(TAG,"currentUser : " + user)
//
//            if (user != null) {
//                userEmailStr = user.userId
//            }
//
//            if (!userEmailStr.isValidEmail()) {
//                MyToastUtil.showNotice(this, "Invalid email address.")
//                return
//            }
//
//            AppHelper.setUser(userEmailStr)
//            user?.getSessionInBackground(authenticationHandler)
//        } catch (exception: Exception) {
//            Log.e(TAG, exception.message)
//        }
//    }

    private fun loginAutomatically() {
        getMyProfile()
    }

    private fun signInUser() {
        //AppHelper.setUser(userEmailStr)
        //AppHelper.pool?.getUser(userEmailStr)!!.getSessionInBackground(authenticationHandler)

        val user = User.emptyUser()

        user.email = userEmailStr
        user.password = passwordStr

        login(user)

    }

    private fun signUpNewUser() {
        val registerActivity = Intent(this, RegisterActivity::class.java)
        startActivity(registerActivity)
        finish()
    }

    fun getUserAuthentication(continuation: AuthenticationContinuation, username: String?) {
        if (username != null) {
            this.userEmailStr = username
            AppHelper.setUser(username)
        }

        passwordStr = passwordET.text.toString()
        if (passwordStr.isEmpty()) {
            return
        }

        val authenticationDetails = AuthenticationDetails(this.userEmailStr, passwordStr, null)
        continuation.setAuthenticationDetails(authenticationDetails)
        continuation.continueTask()
    }

    private val authenticationHandler: AuthenticationHandler = object : AuthenticationHandler {
        override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
            AppHelper.currSession

            SessionManager.share.idTokenAWSUserPool = userSession?.idToken?.jwtToken!!
            SessionManager.share.accessTokenAWSUserPool = userSession.accessToken?.jwtToken!!

            if (newDevice != null) {
                AppHelper.newDevice(newDevice)
            }

            showWaitDialog()
            //login(SessionManager.share.accessTokenAWSUserPool)
        }

        override fun onFailure(exception: Exception?) {
            closeWaitDialog()
            MyToastUtil.showWarning(this@LoginActivity, AppHelper.formatException(exception!!))
        }

        override fun getAuthenticationDetails(
            authenticationContinuation: AuthenticationContinuation?,
            userId: String?
        ) {
            showWaitDialog()
            if (authenticationContinuation != null) {
                getUserAuthentication(authenticationContinuation, userId)
            }
        }

        override fun authenticationChallenge(continuation: ChallengeContinuation?) {
            /**
             * For Custom authentication challenge, implement your logic to present challenge to the
             * user and pass the user's responses to the continuation.
             */
            closeWaitDialog()
            ToastUtil.show(this@LoginActivity, "Need to set new password")
            if ("NEW_PASSWORD_REQUIRED" == continuation!!.challengeName) {
                // This is the first sign-in attempt for an admin created user
                newPasswordContinuation = continuation as NewPasswordContinuation
            } else if ("SELECT_MFA_TYPE" == continuation.challengeName) {
            }
        }

        override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {
            closeWaitDialog()
            ToastUtil.show(this@LoginActivity, "Need MFA")

            //continuation!!.setMfaCode("")
            //continuation!!.continueTask()

            //mfaAuth(multiFactorAuthenticationContinuation!!)

            confirmSignIn(continuation!!.parameters)


        }
    }

    private fun confirmSignIn(details: CognitoUserCodeDeliveryDetails) {
        val intent = Intent(this, AuthActivity::class.java)
        intent.putExtra("source", "signin")
        intent.putExtra("name", emailET.text.toString())
        intent.putExtra("email", emailET.text.toString())
        intent.putExtra("destination", details.destination)
        intent.putExtra("deliveryMed", details.deliveryMedium)
        intent.putExtra("attribute", details.attributeName)
        startActivityForResult(intent, Constants.ACTIVITY_RESULT_CONFIRM_PHONENUMBER)
    }

    private fun mfaAuth(continuation: MultiFactorAuthenticationContinuation) {
        multiFactorAuthenticationContinuation = continuation
        val mfaActivity = Intent(this, AuthActivity::class.java)
        mfaActivity.putExtra("mode", multiFactorAuthenticationContinuation!!.getParameters().deliveryMedium)
        startActivityForResult(mfaActivity, Constants.ACTIVITY_RESULT_CONFIRM_PHONENUMBER)
    }

    private fun getUserDetails(loginResult: LoginResult) {
        val fbAccessToken = loginResult.accessToken
        val request = GraphRequest.newMeRequest(loginResult.accessToken) { json_object, _ ->
            Log.d(TAG, "data =$json_object")

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

        Log.d(TAG, "fb token =${fbAccessToken.token}")
        Log.d(TAG, "data =$request")
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            Constants.ACTIVITY_RESULT_CONFIRM_PHONENUMBER -> {

                closeWaitDialog()
                if (resultCode == Activity.RESULT_OK) {

                    showWaitDialog()
                    //login(SessionManager.share.accessTokenAWSUserPool)

                }

            }

            GOOGLE_SIGN_IN -> {

                closeWaitDialog()

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    Log.d(TAG, "Authenticated with Google: token = ${account!!.idToken}")

                    var user = User.emptyUser()

                    user.id = account.id!!
                    user.name = account.displayName!!
                    user.email = account.email!!
                    user.phone_number = "123456"
                    user.picture = account.photoUrl!!.toString()

                    googleLogin(user)
                } catch (error: ApiException) {
                    //toast("Google Authentication failed: ${error.message}")
                    Log.d(TAG, "Google Authentication failed: ${error}")
                }
            }

            Constants.ACTIVITY_RESULT_REGISTER_OK ->
                // Register user
                if (resultCode == Activity.RESULT_OK) {
                    val email = data?.getStringExtra("email")
                    if (!(email?.isEmpty())!!) {
                        emailET.setText(email)
                        passwordET.setText("")
                        passwordET.requestFocus()
                    }
                    val userPassWd = data.getStringExtra("password")
                    if (userPassWd.isNotEmpty()) {
                        passwordET.setText(userPassWd)
                    }
                    if (email.isNotEmpty() && userPassWd.isNotEmpty()) {
                        // We have the user details, so sign in!
                        userEmailStr = email
                        Global.email = email
                        passwordStr = userPassWd
                        //AppHelper.pool?.getUser(userEmail)?.getSessionInBackground(authenticationHandler)
                    }
                }

            Constants.ACTIVITY_RESULT_CHANGE_PW_OK ->
                // Forgot password
                if (resultCode == Activity.RESULT_OK) {
                    val email = data?.getStringExtra("userEmail")
                    if (email != null) {
                        if (email.isNotEmpty()) {
                            emailET.setText(email)
                            passwordET.setText("")
                            passwordET.requestFocus()
                        }
                    }
                }
        }
    }

    /**
     * Request Methods
     */
//    private fun login(jwt: String) {
//        val successCallback: () -> Unit = {
//            getMyProfile()
//        }
//
//        val errorCallback: (String) -> Unit = { message ->
//            this.runOnUiThread {
//                closeWaitDialog()
//                MyToastUtil.showWarning(this, message)
//            }
//        }
//
//        APIManager.share.login(jwt, successCallback, errorCallback)
//    }

    private fun login(user: User) {

        val successCallback: () -> Unit = {

            closeWaitDialog()
            getMyProfile()

        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                closeWaitDialog()
                MyToastUtil.showWarning(this, "Email or password is invalid")
            }
        }
        showWaitDialog()
        APIManager.share.login(user, successCallback, errorCallback)

    }

    private fun facebookLogin(user: User) {
        val successCallback: () -> Unit = {

            Log.d(TAG,"here is facebook Success state")

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
            userEmailStr = profile.email
            gotoMainPage()
        }

        val errorCallback: (String) -> Unit = { message ->
            this.runOnUiThread {
                closeWaitDialog()
                MyToastUtil.showWarning(this, message)
            }
        }

        APIManager.share.getMyProfile(successCallback, errorCallback)
    }

    private fun checkValidation(): Boolean {
        userEmailStr = emailET.text.toString()
        passwordStr = passwordET.text.toString()

        if (userEmailStr.isEmpty()) {
            MyToastUtil.showNotice(this, "Email required")
            return false
        }

        if (!userEmailStr.isValidEmail()) {
            MyToastUtil.showNotice(this, "Invalid email")
            return false
        }

        if (passwordStr.isEmpty()) {
            MyToastUtil.showNotice(this, "Password required")
            return false
        }

        return true
    }

    private fun gotoMainPage() {
        closeWaitDialog()
        PreferenceUtils.saveBoolean(Constants.PREF_IS_LOGIN, true)
        PreferenceUtils.saveString(Constants.PREF_USER_EMAIL, userEmailStr)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

//    private fun showWaitDialog() {
//        closeWaitDialog()
//        CustomProgressDialog.instance.show(this)
//    }

    private fun showDialogMessage(title: String, body: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(body).setNeutralButton(
            "OK"
        ) { _, _ ->
            try {
                userDialog.dismiss()
            } catch (e: Exception) {
                //
            }
        }
        userDialog = builder.create()
        userDialog.show()
    }

//    private fun closeWaitDialog() {
//        CustomProgressDialog.instance.dismiss()
//    }
}
