package com.wizt.models

import com.google.gson.GsonBuilder

data class Address (
    var id: String,
    var user: Int,
    var country: String,
    var mobile: String,
    var name: String,
    var shipping_address: String,
    var state: String,
    var zip_code: String
) {
    fun getJsonStrring(): String {
        val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        val map: MutableMap<String, Any> = mutableMapOf()
        map["country"] = country
        map["email"] = mobile
        map["name"] = name
        map["shipping_address"] = shipping_address
        map["state"] = state
        map["zip_code"] = zip_code

        return gson.toJson(map)
    }
}