package com.tsu.to_do_list

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyPreviewsResponse(
    @SerialName("\$id") val id: String,
    @SerialName("\$values") val values: List<TaskListPreview>
)
