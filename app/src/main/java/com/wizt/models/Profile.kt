package com.wizt.models

data class Profile(
    val id:Int,
    val subscribed_plan:Int,
    val name: String,
    val username: String,
    val phone_number: String,
    val phone_number_verified: Boolean,
    val email: String,
    val email_verified: Boolean,
    var picture: String?,
    val label_cnt: Int,
    val label_in_use: Int,
    val friends_count: Int,
    val photo_in_use: Int,
    val photo_cnt : Int,
    val fybe_cnt : Int,
    val created_at: String
) {
    companion object {
        fun emptyProfile(): Profile {
            return Profile(0,0,"", "", "", true, "", true, "", 0, 0, 0, 0,0,0,"")
        }
    }

    fun isSubscribe() : Boolean {
        return Global.isSubscription
    }
}