package com.wizt.models

data class User (
    var id: String,
    var name: String,
    var username: String,
    var email: String,
    var phone_number: String?,
    var picture: String?,
    var password: String?

) {
    companion object {
        fun emptyUser(): User {
            return User("", "", "", "", "", "","")
        }
    }
}
