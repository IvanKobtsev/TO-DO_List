package com.tsu.to_do_list

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskListPreview(
    @SerialName("\$id") var jsonId: String,
    var name: String = "",
    var id: Int = 0
)
