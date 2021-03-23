package com.wizt.myservice

import android.util.Base64
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.wizt.common.constants.Constants
import com.wizt.common.http.HttpManager
import com.wizt.models.Global
import com.wizt.utils.PreferenceUtils
import java.nio.charset.StandardCharsets

class MyFirebaseInstanceIdService : FirebaseInstanceIdService() {

    val TAG = "PushNotifService"

    override fun onTokenRefresh() {
        // Mengambil token perangkat
        val token = FirebaseInstanceId.getInstance().token
        Log.d(TAG, "Token Firebase: ${token}")

        //PreferenceUtils.saveString(Constants.PREF_FIREBASE_TOKEN,token.toString())

        Global.firebaseToken = token.toString()

//        var credentialsProvider = CognitoCachingCredentialsProvider(
//            applicationContext,
//            "ap-southeast-1:38f73bbc-bf4a-427b-ba7d-1b34616966ed", // Identity Pool ID
//            Regions.AP_SOUTHEAST_1)
//        val pushClient = AmazonSNSClient(credentialsProvider)
//        pushClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))

        val data_accessKey = Base64.decode(Constants.AWS_ACCESS_KEY, Base64.DEFAULT)
        val text_AccessKey = String(data_accessKey, StandardCharsets.UTF_8)
        val data_secretKey = Base64.decode(Constants.AWS_SECRETKEY, Base64.DEFAULT)
        val text_secretKey = String(data_secretKey, StandardCharsets.UTF_8)
        val awsCredentials = BasicAWSCredentials(text_AccessKey,text_secretKey)
        val pushClient = AmazonSNSClient(awsCredentials)
        pushClient.setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        val customPushData = "my custom data"
        val platformEndpointRequest = CreatePlatformEndpointRequest()

        platformEndpointRequest.setCustomUserData(customPushData)
        //Global.firebaseToken = PreferenceUtils.getString(Constants.PREF_FIREBASE_TOKEN)
        Log.d(HttpManager.TAG, "Global_FirebaseToken -> " + Global.firebaseToken)
        Log.d(HttpManager.TAG, "AWS_ACCESS_KEY -> " + text_AccessKey)
        Log.d(HttpManager.TAG, "AWS_SECRETKEY -> " + text_secretKey)
        platformEndpointRequest.setToken(Global.firebaseToken)
        platformEndpointRequest.setPlatformApplicationArn(Constants.PLATFORMAPPLICATIONARN)

        //pushClient.createPlatformEndpoint(platformEndpointRequest)

        val endPointArn = pushClient.createPlatformEndpoint(platformEndpointRequest).endpointArn
        PreferenceUtils.saveString(Constants.PREF_ENDPOINTARN,endPointArn)

        //PreferenceUtils.saveString(Constants.PREF_ENDPOINTARN,"123")

        // Jika ingin mengirim push notifcation ke satu atau sekelompok perangkat,
        // simpan token ke server di sini.
    }

}