package com.wizt.models

class Global {

    companion object {
        var email: String = ""
        var accessToken: String = ""
        var idToken: String = ""
        var isSeleted: Boolean = false
        var label: Label? = null
        var trainObject : TrainObject? = null
        var floorPlan: FloorPlan? = null
        var profile: Profile? = null
        var shareLabel: ShareLabel? = null
        var firebaseToken = ""
        var isSubscription = false
    }
}