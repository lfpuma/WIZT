package com.wizt.models

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.json.JSONException
import kotlin.arrayOf as arrayOf1

data class Label (
    var id: Int,
    var user: Int,
    var name: String,
    var location: String,
    var floor_plan : String,
    var tags: String,
    var ar_mark_image: String,
    var images: ArrayList<Image>,
    var created_at: String,
    var reminder_time: String,
    var imageCount: Int
) {
    companion object {
        fun parse(json: JsonObject): Label {
            val images: ArrayList<Image> = arrayListOf()
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            val id = json["id"].asInt
            val user = json["user"].asInt
            val name = json["name"].asString
            val location = json["location"].asString
            var floorplanID = ""
            floorplanID = json["floor_plan"].toString()
            floorplanID = floorplanID.replace("\"","")
            val tags = json["tags"].asString
            val ar_mark_image = json["ar_mark_image"].asString
            val imgArr = json["images"].asJsonArray
            for (imgObj in imgArr) {
                val image: Image = gson.fromJson(imgObj, Image::class.java)
                images.add(image)
            }
            val created_at = json["updated_at"].toString().replace("\"","")

            var reminder_time = ""
            if(json.toString().contains("reminder_time")) {
                reminder_time = json["reminder_time"].toString().replace("\"","")
            }
            return Label(id, user, name, location, floorplanID, tags, ar_mark_image, images, created_at,reminder_time, 0)
        }

        fun parseArray(json: JsonObject): ArrayList<Label> {
            val labelArr: ArrayList<Label> = arrayListOf()

            try {
                val arr = json["results"].asJsonArray
                for (obj in arr) {
                    val label = parse(obj.asJsonObject)
                    labelArr.add(label)
                }
            } catch (exception: JSONException) {
                Log.e("Label Parse", exception.message)
            }

            return labelArr
        }
    }

    fun getJsonString(): String {
        val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        val map: MutableMap<String, Any> = mutableMapOf()
        map["name"] = name
        map["location"] = location
        map["tags"] = tags
        map["ar_mark_image"] = ar_mark_image
        map["images"] = images
        if(floor_plan == null) floor_plan = ""
        map["floor_plan"] = floor_plan
        map["reminder_time"] = reminder_time
        map["image_count"] = imageCount

        return gson.toJson(map)
    }
}