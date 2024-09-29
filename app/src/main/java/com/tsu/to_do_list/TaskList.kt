package com.tsu.to_do_list

data class TaskList(
    var id: Int = 0,
    var xMin: Long = 0,
    var name: String = "Новый список",
    var tasks: ArrayList<Task> = arrayListOf()
)
