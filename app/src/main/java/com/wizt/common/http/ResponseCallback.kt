package com.wizt.common.http

import android.util.Log
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import matteocrippa.it.karamba.toDate
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.properties.Delegates

class ResponseCallback : Callback {

    companion object {
        const val TAG = "ResponseCallback"
    }

    var successStatusCodes = intArrayOf(200, 201, 202, 203, 204, 205, 206, 207, 208, 209)
    //var serverErrorMessages = "Something went wrong"
    var serverErrorMessages = "Failed"
    var jsonCallback: JSONCallback by Delegates.notNull()

    constructor(jsonCallback: JSONCallback) {
        this.jsonCallback = jsonCallback
    }

    override fun onResponse(call: Call, response: Response) {
        val statusCode = response.code()
        val message = response.body()?.string()

        when (statusCode) {
            200 -> jsonCallback.onResponse200()
            201 -> jsonCallback.onResponse201()
            204 -> jsonCallback.onResponse204()
            400 -> jsonCallback.onResponse400()
            401 -> jsonCallback.onResponse401()
            405 -> {
                jsonCallback.onResponse405()
                return
            }
        }

        if (successStatusCodes.contains(statusCode)) {
            try {
                val element = JsonParser().parse(message)

                when {
                    element.isJsonObject -> this.jsonCallback.onResponseJSONObjectSuccess(element.asJsonObject)
                    element.isJsonArray -> this.jsonCallback.onResponseJSONArraySuccess(element.asJsonArray)
                    element.isJsonNull -> this.jsonCallback.onResponseEmptySuccess(message!!)
                    element.isJsonPrimitive -> this.jsonCallback.onResponseTextSuccess(message!!)
                    else -> this.jsonCallback.onResponseTextSuccess(message!!)
                }
            } catch (exception: JsonParseException) {
                this.jsonCallback.onResponseTextSuccess(message!!)
                Log.d(TAG, "error -> " + message)
            }
        } else {
            //jsonCallback.onFailed(serverErrorMessages)
            if (message != null) {
                jsonCallback.onFailed(message)
            }
            else jsonCallback.onFailed("")
            Log.d(TAG, "serverErrorMessages -> " + serverErrorMessages)
        }

        jsonCallback.onFinal()
    }

    override fun onFailure(call: Call, e: IOException) {
        jsonCallback.onException()
        jsonCallback.onFinal()
    }
}