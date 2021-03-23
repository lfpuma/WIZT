package com.wizt.models

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlin.collections.ArrayList

data class Notification(
    val id: String,
    val message_type: Int,
    val send_by: User,
    val send_to: User,
    val message: String,
    val created_at: String,
    val updated_at: String
) {
    companion object {
        fun parse(obj: JsonObject): Notification {
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
            val id = obj["id"].asString
            val message_type = obj["message_type"].asInt
            val send_by = gson.fromJson(obj["send_by"].asJsonObject, User::class.java)
            val send_to = gson.fromJson(obj["send_to"].asJsonObject, User::class.java)
            val message = obj["message"].asString
            val created_at = obj["created_at"].asString
            val updated_at= obj["updated_at"].asString

            return Notification(id, message_type, send_by, send_to, message, created_at, updated_at)
        }

        fun parseArray(json: JsonObject): ArrayList<Notification> {
            val notificationArr = arrayListOf<Notification>()
            val arr = json["results"].asJsonArray
            for (obj in arr) {
                val notification = parse(obj.asJsonObject)
                notificationArr.add(notification)
            }

            return notificationArr
        }

        fun getJsonString(send_to: Int, message: String): String {
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            val map = mutableMapOf<String, Any>()
            map["send_to"] = send_to
            map["message_type"] = 1
            map["message"] = message

            return gson.toJson(map)
        }
    }
}