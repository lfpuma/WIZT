package com.wizt.common.http

import android.content.Context
import android.support.annotation.Nullable
import okhttp3.*
import java.util.concurrent.TimeUnit
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import com.google.firebase.iid.FirebaseInstanceId
import com.wizt.R
import com.wizt.common.constants.Constants
import com.wizt.models.Global
import com.wizt.models.PredictionModel
import com.wizt.models.User
import com.wizt.utils.PreferenceUtils
import java.io.File


class HttpManager {

    companion object {
        const val TAG = "WIZT:HttpManager"
        var share = HttpManager()
    }

    /**
     * Constants
     */
    private val timeout: Long = 60
    private val notworkConnectionErrStr = "No Network Connection"
    private val JSON = MediaType.parse("application/json; charset=utf-8")

    private var httpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(timeout, TimeUnit.SECONDS)
        .build()

    /**
     * Public Methods
     */
    fun get(url: String, @Nullable token: String?, callback: JSONCallback) {
        var requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", token)
        }
        val request = requestBuilder.build()

        var call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun postWithJWT(url: String, jwt: String, callback: JSONCallback) {
        val body: RequestBody = RequestBody.create(JSON, "")
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)

        requestBuilder.addHeader("Authorization", jwt)
        val request = requestBuilder.build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun postWithUserInfo(url: String, user: User, callback: JSONCallback) {

        val parameters =  HashMap<String,Any>()
        parameters.put("name" , "${user.name}")
        parameters.put("email" , "${user.email}")
        parameters.put("username" , "${user.email}")
        parameters.put("email_verified" , true)
        parameters.put("password","${user.password}")
        parameters.put("phone_number","${user.phone_number}")

        Log.d(TAG,"loginUser Info \n" + parameters)

        val builder = FormBody.Builder()
        val it = parameters.entries.iterator()
        while (it.hasNext()) {
            val pair = it.next() as Map.Entry<*, *>
            builder.add(pair.key.toString(), pair.value.toString())
        }

        val formBody = builder.build()
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(url)
            .post(formBody)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun postWithImageFile(url: String, file: File , callback: JSONCallback) {

        val builder = MultipartBody.Builder()

        val formBody = builder
            .setType(MultipartBody.FORM)
            .addFormDataPart("image",file.name,RequestBody.create(
            MediaType.parse("image/jpg"), file)).build()

        val request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(url)
            .post(formBody)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun postWithUserInfo(withName: String, url: String, user: User, callback: JSONCallback) {

        val parameters =  HashMap<String,Any>()
        parameters.put(withName + "_id" , "${user.id}")
        parameters.put("name" , "${user.name}")
        parameters.put("email" , "${user.email}")
        parameters.put("email_verified" , true)
//        parameters.put("phone_number_verified" , true)
        //parameters.put("phone_number" , "${user.phone_number}")
        parameters.put("picture","${user.picture}")

        val target_arn = PreferenceUtils.getString(Constants.PREF_ENDPOINTARN)
        parameters.put("target_arn",target_arn)

        //parameters.put("device_token",Global.firebaseToken)
        parameters.put("device_type","android")

        Log.d(TAG,"loginUser Info \n" + parameters)

        val builder = FormBody.Builder()
        val it = parameters.entries.iterator()
        while (it.hasNext()) {
            val pair = it.next() as Map.Entry<*, *>
            builder.add(pair.key.toString(), pair.value.toString())
        }

        val formBody = builder.build()
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(url)
            .post(formBody)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun post(url: String, user: User, callback: JSONCallback) {

        val parameters =  HashMap<String,Any>()
        parameters.put("email" , "${user.email}")
        parameters.put("password","${user.password}")
        val target_arn = PreferenceUtils.getString(Constants.PREF_ENDPOINTARN)
        parameters.put("target_arn",target_arn)
        parameters.put("device_type","android")

        Log.d(TAG,"loginUser Info \n" + parameters)

        val builder = FormBody.Builder()
        val it = parameters.entries.iterator()
        while (it.hasNext()) {
            val pair = it.next() as Map.Entry<*, *>
            builder.add(pair.key.toString(), pair.value.toString())
        }

        val formBody = builder.build()
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(url)
            .post(formBody)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun resetpass(url: String, user: User, callback: JSONCallback) {

        val parameters =  HashMap<String,Any>()
        parameters.put("email" , "${user.email}")
        parameters.put("password","${user.password}")

        val builder = FormBody.Builder()
        val it = parameters.entries.iterator()
        while (it.hasNext()) {
            val pair = it.next() as Map.Entry<*, *>
            builder.add(pair.key.toString(), pair.value.toString())
        }

        val formBody = builder.build()
        val request = Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(url)
            .post(formBody)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun post(url: String, @Nullable token: String?, callback: JSONCallback) {
        val body: RequestBody = RequestBody.create(JSON, "")
        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", token)
        }
        requestBuilder.addHeader("Content-Type", "application/json")
        val request = requestBuilder.build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun post(url: String, @Nullable token: String?, parameters: String, callback: JSONCallback) {
        val body: RequestBody = RequestBody.create(JSON, parameters)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", token)
        }
        requestBuilder.addHeader("Content-Type", "application/json")
        val request = requestBuilder.build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun put(url: String, @Nullable token: String?, parameters: String, callback: JSONCallback) {
        val body: RequestBody = RequestBody.create(JSON, parameters)

        val requestBuilder = Request.Builder()
            .url(url)
            .put(body)

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", token)
        }
        requestBuilder.addHeader("Content-Type", "application/json")
        val request = requestBuilder.build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun patch(url: String, @Nullable token: String?, parameters: String, callback: JSONCallback) {
        val body: RequestBody = RequestBody.create(JSON, parameters)
        val requestBuilder = Request.Builder()
            .url(url)
            .patch(body)

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", token)
        }
        requestBuilder.addHeader("Content-Type", "application/json")
        val request = requestBuilder.build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }

    fun delete(url: String, @Nullable token: String?, callback: JSONCallback) {
        val requestBuilder = Request.Builder()
            .url(url)
            .delete()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", token)
        }
        val request = requestBuilder.build()

        val call = httpClient.newCall(request)
        call.enqueue(ResponseCallback(callback))
    }
}