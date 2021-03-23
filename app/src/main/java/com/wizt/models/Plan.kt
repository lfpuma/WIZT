package com.wizt.models

data class Plan (
    val id: Int,
    val product_id: String,
    val icon: String,
    val price: Float,
    val currency: String,
    val name: String,
    val sub_name: String,
    val photo_count: Int,
    val label_count: Int,
    val description: String,
    val is_free : Boolean,
    val created_at : String,
    val updated_at : String

) {
    companion object {
        fun emptyPlan(): Plan {
            return Plan(0, "", "", 0.0f, "", "","",0,0,"",true,"","")
        }
    }
}