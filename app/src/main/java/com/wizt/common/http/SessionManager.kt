package com.wizt.common.http

data class SessionManager(
    var accessTokenAWSUserPool: String,
    var idTokenAWSUserPool: String,
    var token_aws_user_pool: String,
    var token_api: String
) {
    companion object {
//        val share = SessionManager("", "Token 0e837f0438c84127ae6e7f6ab1b984833b6f4426", "", "")
        val share = SessionManager("", "", "", "")
    }
}

