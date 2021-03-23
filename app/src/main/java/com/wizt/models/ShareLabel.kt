package com.wizt.models

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

data class ShareLabel(
    val id: String,
    var edit_permission: Boolean,
    var label: Label,
    val share_by: User,
    val share_to: User,
    val created_at: String
) {
    companion object {
        fun parse(obj: JsonObject): ShareLabel {
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            val id = obj["id"].asString
            val edit_permission = obj["edit_permission"].asBoolean
            val label = Label.parse(obj["label"].asJsonObject)
            val share_by =  gson.fromJson(obj["share_by"].asJsonObject, User::class.java)
            val share_to =  gson.fromJson(obj["share_to"].asJsonObject, User::class.java)
            val created_at = obj["created_at"].asString

            return ShareLabel(id, edit_permission, label, share_by, share_to, created_at)
        }

        fun parseArr(json: JsonObject): ArrayList<ShareLabel> {
            val shareLabelList = arrayListOf<ShareLabel>()
            val arr = json["results"].asJsonArray
            for (obj in arr) {
                val shareLabel = parse(obj.asJsonObject)
                shareLabelList.add(shareLabel)
            }

            return shareLabelList
        }

        fun getJsonString(label: Label, isEdit: Boolean, user: User): String {
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            val map: MutableMap<String, Any> = mutableMapOf()
            map["label"] = label.id
            map["edit_permission"] = isEdit
            map["share_to"] = user.id

            return gson.toJson(map)
        }
    }

    fun getJsonString(): String {
        val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        val map: MutableMap<String, Any> = mutableMapOf()
        map["label_id"] = id
        map["edit_permission"] = edit_permission
        map["share_to"] = share_to.id

        return gson.toJson(map)
    }
}