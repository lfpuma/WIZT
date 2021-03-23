package com.wizt.models


import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

data class PredictionModel (
    var log_reg_predictions : MutableMap<String, Double>,
    var knn_predictions : MutableMap<String, Double>,
    var nn_predictions : MutableMap<String, Double>
) {
    companion object {
        fun parse(json: JsonObject): PredictionModel {
            val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

            val log_reg_predictions : MutableMap<String, Double> = mutableMapOf()
            val knn_predictions : MutableMap<String, Double> = mutableMapOf()

            val nn_json = json["nn_predictions"].asJsonObject
            val nn_predictions : MutableMap<String, Double> = gson.fromJson(nn_json, object : TypeToken<MutableMap<String, Double>>() {}.type)

            return PredictionModel(log_reg_predictions,knn_predictions,nn_predictions)
        }
    }
}