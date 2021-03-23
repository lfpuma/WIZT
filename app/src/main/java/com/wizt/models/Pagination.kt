package com.wizt.models

data class Pagination (
    var count: Int,
    var next: String?,
    var previous: String?
)