package com.wizt.models

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.json.JSONException

data class TrainObject (
    var id: String,
    var user: Int,
    var name: String,
    var train_class: String,
    var images: ArrayList<FybeImage>,
    var imageCount: Int
) {
    companion object {
        fun parse(json: JsonObject): TrainObject {
            val images: ArrayList<FybeImage> = arrayListOf()
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            val id = json["id"].asString
            val user = json["user"].asInt
            val name = json["name"].asString
            val train_class = json["label"].asString
            val imgArr = json["images"].asJsonArray
            for (imgObj in imgArr) {
                val image: FybeImage = gson.fromJson(imgObj, FybeImage::class.java)
                images.add(image)
            }

            return TrainObject(id, user, name, train_class, images, 0)
        }

        fun parseArray(json: JsonObject): ArrayList<TrainObject> {
            val objectArr: ArrayList<TrainObject> = arrayListOf()

            try {
                val arr = json["results"].asJsonArray
                for (obj in arr) {
                    val Tobject = parse(obj.asJsonObject)
                    objectArr.add(Tobject)
                }
            } catch (exception: JSONException) {
                Log.e("Train Object Parse", exception.message)
            }

            return objectArr
        }
    }

    fun getJsonString(): String {
        val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        val map: MutableMap<String, Any> = mutableMapOf()
        map["name"] = name
        map["label"] = train_class
        map["images"] = images
        map["image_count"] = imageCount

        return gson.toJson(map)
    }
}