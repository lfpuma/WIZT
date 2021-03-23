package com.wizt.models

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.json.JSONException
import kotlin.collections.ArrayList


data class FriendRequest(
    val id: String,
    val status: Boolean,
    var from_user: User,
    var to_user: User,
    val created_at: String,
    val updated_at: String
) {

    companion object {
        fun parse(obj: JsonElement): FriendRequest {
            val json = obj.asJsonObject
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            val id = json["id"].asString
            val user = json["status"].asBoolean
            val from_user = gson.fromJson(json["from_user"].asJsonObject, User::class.java)
            val to_user = gson.fromJson(json["to_user"].asJsonObject, User::class.java)
            val created_at = json["created_at"].asString
            val updated_at = json["updated_at"].asString

            return FriendRequest(id, user, from_user, to_user, created_at, updated_at)
        }

        fun parseArray(obj: JsonObject): ArrayList<FriendRequest> {
            val friendRequestList = arrayListOf<FriendRequest>()
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            try {
                val jsonArr = obj["results"].asJsonArray
                for (json in jsonArr) {
                    friendRequestList.add(parse(json))
                }
            } catch (exception: JSONException) {
                Log.e("FriendRequest Parse", exception.message)
            }

            return friendRequestList
        }
    }
}