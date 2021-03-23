package com.wizt.models

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class FloorPlan (
    var id: String,
    var user: User,
    var name: String,
    var tags: String,
    var image: String,
    var thumbnail: String,
    var created_at: String,
    var updated_at: String
) {
    companion object {
        fun parse(obj: JsonObject): FloorPlan {
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
            val id = obj["id"].asString
            val user = gson.fromJson(obj["user"].asJsonObject, User::class.java)
            val name = obj["name"].asString
            val tags = obj["tags"].asString
            val image = obj["image"].asString
            val thumbnail = obj["thumbnail"].asString
            val created_at = obj["created_at"].asString
            val updated_at = obj["updated_at"].asString

            return FloorPlan(id, user, name, tags, image, thumbnail, created_at, updated_at)
        }

        fun parseArray(arr: JsonArray): ArrayList<FloorPlan> {
            val floorPlanList = arrayListOf<FloorPlan>()
            for (obj in arr) {
                val floorPlan = parse(obj.asJsonObject)
                floorPlanList.add(floorPlan)
            }

            return floorPlanList
        }

        fun emptyFloorPlan(): FloorPlan {
            return FloorPlan("", User.emptyUser(), "", "", "", "", "", "")
        }
    }

    fun getJsonString(): String {
        val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        val map: MutableMap<String, Any> = mutableMapOf()
        map["name"] = name
        map["tags"] = tags
        map["image"] = image
        map["thumbnail"] = thumbnail

        return gson.toJson(map)
    }
}