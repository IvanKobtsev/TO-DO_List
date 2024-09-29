package com.tsu.to_do_list

import kotlinx.serialization.Serializable

@Serializable
data class TaskToSend(
    var name: String = "Новое дело",
    var description: String = "",
    var status: Boolean = false,
    var id: Int = 0,
)
