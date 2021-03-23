package com.wizt.common.http

import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.wizt.R
import com.wizt.common.constants.Constants
import com.wizt.models.*
import com.wizt.utils.PreferenceUtils
import java.io.File

class APIManager {

    companion object {
        const val TAG = "WIZT:APIManager"
        var share = APIManager()
    }

    private var sessionManager = SessionManager.share
    private val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    constructor() {
        sessionManager.token_api = "Token 0e837f0438c84127ae6e7f6ab1b984833b6f4426"
    }


    fun login(jwt: String, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        HttpManager.share.postWithJWT(URLProvider.login, jwt, object : JSONCallback {
            override fun onResponseTextSuccess(text: String) {
                sessionManager.token_api = "Token $text".replace("\"", "")
                PreferenceUtils.saveString(Constants.PREF_CURRENT_TOKEN, sessionManager.token_api)
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun register(user: User, successCallback: () -> Unit, existUserCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        HttpManager.share.postWithUserInfo(URLProvider.register, user, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                successCallback()
            }

            override fun onResponse400() {
                existUserCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun login(user: User, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        HttpManager.share.post( URLProvider.login, user, object : JSONCallback {
            override fun onResponseTextSuccess(text: String) {
                sessionManager.token_api = "Token $text".replace("\"", "")
                PreferenceUtils.saveString(Constants.PREF_CURRENT_TOKEN, sessionManager.token_api)
                Log.d(TAG," Token Data with CustomLogin: " + sessionManager.token_api)
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun resetPassword(user: User, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        HttpManager.share.resetpass( URLProvider.resetpassword, user, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun loginFaceBook(user: User, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        HttpManager.share.postWithUserInfo("facebook", URLProvider.facebook_login, user, object : JSONCallback {
            override fun onResponseTextSuccess(text: String) {
                sessionManager.token_api = "Token $text".replace("\"", "")
                PreferenceUtils.saveString(Constants.PREF_CURRENT_TOKEN, sessionManager.token_api)
                Log.d(TAG," Token Data with FaceBook: " + sessionManager.token_api)
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun loginGoogle(user: User, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        HttpManager.share.postWithUserInfo("google", URLProvider.google_login, user, object : JSONCallback {
            override fun onResponseTextSuccess(text: String) {
                sessionManager.token_api = "Token $text".replace("\"", "")
                PreferenceUtils.saveString(Constants.PREF_CURRENT_TOKEN, sessionManager.token_api)
                Log.d(TAG," Token Data with Google: " + sessionManager.token_api)
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun logout() {
        HttpManager.share.post(URLProvider.logout, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
            }

            override fun onFailed(message: String) {
            }
        })
    }

    fun getMyProfile(successCallback: (myProfile: Profile) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.get_my_profile
        sessionManager.token_api = PreferenceUtils.getString(Constants.PREF_CURRENT_TOKEN)
        Log.d(TAG,"Token From Auto Login " + sessionManager.token_api)
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val profile = gson.fromJson(obj, Profile::class.java)
                successCallback(profile)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun updateProfilePicture(picture: String, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.get_my_profile
        val params = "{\n\t\"picture\": \"$picture\"\n}"
        HttpManager.share.patch(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                successCallback()
            }

            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * Label APIs
     */

    fun getLabels(
        successCallback: (paginate: Pagination, list: List<Label>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        getLabels(URLProvider.get_labels, successCallback, errorCallback)
    }

    fun getTrainedObjects(
        successCallback: (paginate: Pagination, list: List<TrainObject>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        getTrains(URLProvider.get_trainobjs, successCallback, errorCallback)
    }

    fun getTrains(
        url: String,
        successCallback: (paginate: Pagination, list: List<TrainObject>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                val objList: ArrayList<TrainObject> = TrainObject.parseArray(obj)
                successCallback(pagination, objList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }


    fun getLabels(
        url: String,
        successCallback: (paginate: Pagination, list: List<Label>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                val labelList: ArrayList<Label> = Label.parseArray(obj)
                successCallback(pagination, labelList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun getLabel(id: Int, successCallback: (label: Label) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.get_label.replace("@", "$id")

        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val label = Label.parse(obj)
                successCallback(label)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun postLabel(label: Label, successCallback: (label: Label) -> Unit, subscribeCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.post_label.replace("@", "$label.id")
        val params = label.getJsonString()

        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val label = gson.fromJson(obj, Label::class.java)
                successCallback(label)
            }

            override fun onResponse405() {
                subscribeCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun postPredictionModel(id:Int,file: File, successCallback: (predictionModel: PredictionModel) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.post_prediction_model.replace("@", "$id")

        HttpManager.share.postWithImageFile(url, file, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val predictionModel = gson.fromJson(obj, PredictionModel::class.java)
                successCallback(predictionModel)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun postTrainObject(trainObject: TrainObject, successCallback: (trainObject : TrainObject) -> Unit, subscribeCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.post_fybe_train
        val params = trainObject.getJsonString()

        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val trainObject = gson.fromJson(obj, TrainObject::class.java)
                successCallback(trainObject)
            }

            override fun onResponse405() {
                subscribeCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun deleteLabel(label: Label, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.delete_label.replace("@", "${label.id}")
        HttpManager.share.delete(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun deleteTrain(trainObject: TrainObject, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.delete_train.replace("@", "${trainObject.id}")
        HttpManager.share.delete(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * Friend and User APIs
     */
    fun getFriendList(
        successCallback: (paginate: Pagination, list: List<User>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.get_friends
        getFriendList(url, successCallback, errorCallback)
    }

    fun getFriendList(
        url: String,
        successCallback: (paginate: Pagination, list: List<User>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                val userList = gson.fromJson(obj["results"].asJsonArray, Array<User>::class.java).toList()
                successCallback(pagination, userList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * Plan APIs
     */
    fun getPlanList(successCallback: (paginate: Pagination, planList: List<Plan>) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.get_plan_list
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                var planList = gson.fromJson(obj["results"].asJsonArray, Array<Plan>::class.java).toList()
                planList = planList.filter { s -> s.is_free == false }
                successCallback(pagination, planList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun getFreePlanID(successCallback: (id: Int) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.get_plan_list
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                var planList = gson.fromJson(obj["results"].asJsonArray, Array<Plan>::class.java).toList()
                planList = planList.filter { s -> s.is_free == true }
                if (planList.size == 1)
                    successCallback(planList[0].id)
                else successCallback(-1)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun getUserList(
        query: String?,
        successCallback: (paginate: Pagination, list: List<User>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        var url = URLProvider.get_user_list
        var key = ""
        if (!query.isNullOrEmpty()) {
            key = query
        }
        url = url.replace("@", key)

        getUserListWithURL(url, successCallback, errorCallback)
    }

    fun getUserListWithURL(
        url: String,
        successCallback: (paginate: Pagination, list: List<User>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                val userList = gson.fromJson(obj["results"].asJsonArray, Array<User>::class.java).toList()
                successCallback(pagination, userList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * FriendRequest APIs
     */
    fun getFriendRequestList(
        successCallback: (pagination: Pagination, friendRequestList: ArrayList<FriendRequest>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.get_friend_request_list
        getFriendRequestList(url, successCallback, errorCallback)
    }

    fun getFriendRequestList(
        url: String,
        successCallback: (pagination: Pagination, friendRequestList: ArrayList<FriendRequest>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                val friendRequestList = FriendRequest.parseArray(obj)
                successCallback(pagination, friendRequestList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun sendFriendRequest(
        user: User,
        successCallback: (friendRequest: FriendRequest) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.post_friend_request
        val params = "{\n\t\"to_user\": ${user.id}\n}"

        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val friendRequest = FriendRequest.parse(obj)
                successCallback(friendRequest)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun acceptFriendRequest(
        request: FriendRequest,
        successCallback: () -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.update_friend_request.replace("@", "${request.id}")
        val params = "{\n" + "\t\"status\": true\n" + "}"
        HttpManager.share.patch(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
//                val request = FriendRequest.parse(obj)
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun deleteFriendRequest(
        request: FriendRequest,
        successCallback: () -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.delete_friend_request.replace("@", "${request.id}")
        HttpManager.share.delete(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * Notification APIs
     */
    fun getNotificationList(
        successCallback: (pagination: Pagination, notifications: ArrayList<Notification>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.get_notification_list
        getNotificationList(url, successCallback, errorCallback)
    }

    fun getNotificationList(
        url: String,
        successCallback: (pagination: Pagination, notifications: ArrayList<Notification>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                val notificationList = Notification.parseArray(obj)
                successCallback(pagination, notificationList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun sendNotification(
        user_id: Int,
        message: String,
        successCallback: (notification: Notification) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.post_notification
        val params = Notification.getJsonString(user_id, message)
        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val notification = Notification.parse(obj)
                successCallback(notification!!)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun clearAllNotification(successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.delete_all_notifications
        HttpManager.share.delete(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun clearNotification(
        notification: Notification,
        successCallback: () -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.delete_notification.replace("@", "${notification.id}")
        HttpManager.share.delete(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * ShareLabel and ShareLog APIs
     */
    fun getShareLabelList(
        successCallback: (pagination: Pagination, labelList: ArrayList<ShareLabel>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.get_share_label_list
        getShareLabelList(url, successCallback, errorCallback)
    }

    fun getShareLabelList(
        url: String,
        successCallback: (pagination: Pagination, labelList: ArrayList<ShareLabel>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val pagination = gson.fromJson(obj, Pagination::class.java)
                val labelList = ShareLabel.parseArr(obj)
                successCallback(pagination, labelList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun getLabelLogList(
        successCallback: (pagination: Pagination, labelList: ArrayList<ShareLabel>) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.get_share_log_list
        getShareLabelList(url, successCallback, errorCallback)
    }

    fun shareLabel(
        label: Label,
        isEdit: Boolean,
        to_user: User,
        successCallback: (shareLabel: ShareLabel) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.post_share_label
        val params = ShareLabel.getJsonString(label, isEdit, to_user)
        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val shareLabel = ShareLabel.parse(obj)
                successCallback(shareLabel)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun updateShareLabel(
        shareLabel: ShareLabel,
        successCallback: (shareLabel: ShareLabel) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {

        val url = URLProvider.update_share_label.replace("@", shareLabel.id)
        val params = shareLabel.getJsonString()
        HttpManager.share.put(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val shareLabel = ShareLabel.parse(obj)
                successCallback(shareLabel)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun updateLabel(label: Label, successCallback: (label: Label) -> Unit, subscribeCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.update_label.replace("@", "${label.id}")
        //val params = gson.toJson(label)
        val params = label.getJsonString()

        HttpManager.share.put(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val label = gson.fromJson(obj, Label::class.java)
                successCallback(label)
            }
            override fun onResponse405() {
                subscribeCallback()
            }
            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun updateTrainObject(trainObject: TrainObject, successCallback: (trainObject: TrainObject) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.update_fybe_train.replace("@", "${trainObject.id}")
        val params = trainObject.getJsonString()

        HttpManager.share.put(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val trainObject = gson.fromJson(obj, TrainObject::class.java)
                successCallback(trainObject)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun deleteShareLabel(
        shareLabel: ShareLabel,
        successCallback: () -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.delete_share_label.replace("@", shareLabel.id)
        HttpManager.share.delete(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }


    /**
     * Address APIs
     */
    fun getMyAddress(successCallback: (address: Address) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.get_my_address
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val address = gson.fromJson(obj, Address::class.java)
                successCallback(address)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun postMyAddress(
        address: Address,
        successCallback: (address: Address) -> Unit,
        errorCallback: (message: String) -> Unit
    ) {
        val url = URLProvider.post_my_address
        val params = address.getJsonStrring()
        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val address = gson.fromJson(obj, Address::class.java)
                successCallback(address)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * Payment APIs
     */
    fun subscribe(token: String, planID: Int, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.post_subscribe
        val params = "{\n" + "\t\"plan\":$planID,\n" + "\t\"tokenId\":\"$token\"\n" + "}"
        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                successCallback()
            }

            override fun onResponseTextSuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun checkSubscribe(successCallback: (message: String) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.post_checksubscribe
        val params = "{\n" +"}"
        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {

            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                successCallback(obj["status"].toString())
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * FloorPlan APIs
     */
    fun getFloorPlanList(successCallback: (arr: ArrayList<FloorPlan>) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.get_floor_plan_list
        HttpManager.share.get(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseJSONArraySuccess(array: JsonArray) {
                val floorPlanList = FloorPlan.parseArray(array)
                successCallback(floorPlanList)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun postFloorPlan(floorPlan: FloorPlan, successCallback: (floorPlan: FloorPlan) -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.post_floor_plan
        val params = floorPlan.getJsonString()
        HttpManager.share.post(url, sessionManager.token_api, params, object : JSONCallback {
            override fun onResponseJSONObjectSuccess(obj: JsonObject) {
                val floorPlan = FloorPlan.parse(obj)
                successCallback(floorPlan)
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    fun deleteFloorPlan(floorPlan: FloorPlan, successCallback: () -> Unit, errorCallback: (message: String) -> Unit) {
        val url = URLProvider.delete_floor_plan.replace("@", "${floorPlan.id}")
        HttpManager.share.delete(url, sessionManager.token_api, object : JSONCallback {
            override fun onResponseEmptySuccess(text: String) {
                successCallback()
            }

            override fun onFailed(message: String) {
                errorCallback(message)
            }
        })
    }

    /**
     * FYBE Training
     */

    //fun postFYBETrainItem()
}
