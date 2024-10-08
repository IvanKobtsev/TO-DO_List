package com.tsu.to_do_list

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    var name: String = "Новое дело",
    var description: String = "",
    @SerialName("status") var state: Boolean = false,
    var id: Int = 0,
    @SerialName("\$id") var jsonId: String = ""
)
