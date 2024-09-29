package com.tsu.to_do_list

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskListResponse(
    @SerialName("\$id") var jsonId: String,
    var name: String,
    var xMin: Long,
    var taskList: TasksResponse
)
