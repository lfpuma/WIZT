package com.wizt.common.http

import com.google.gson.JsonObject

interface HttpResonseCallback {

    fun onResponse200() {}

    fun onResponse201() {}

    fun onResponse204() {}

    fun onResponse400() {}

    fun onResponse401() {}

    fun onResponse405() {}
}