package com.tsu.to_do_list

data class Task(
    var name: String = "Новое дело",
    var description: String = "",
    var state: Boolean = false
)
