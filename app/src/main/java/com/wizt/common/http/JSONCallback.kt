package com.wizt.common.http

import com.google.gson.JsonArray
import com.google.gson.JsonObject

interface JSONCallback : HttpResonseCallback {

    fun onResponseJSONObjectSuccess(obj: JsonObject) {}

    fun onResponseJSONArraySuccess(array: JsonArray) {}

    fun onResponseTextSuccess(text: String) {}

    fun onResponseEmptySuccess(text: String) {}

    fun onFailed(message: String) {}

    fun onFinal() {}

    fun onException() {}

}