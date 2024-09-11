package com.tsu.to_do_list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TaskViewModel : ViewModel() {

    var currentTaskList = TaskList()
    var taskToEdit = MutableLiveData(-1)
    var taskToDelete = MutableLiveData(-1)
    var taskStatusToSwap = MutableLiveData(-1)
    var taskListChanged = MutableLiveData(false)
    var needToCloseInputs = MutableLiveData(false)

    fun triggerTaskListChange() {
        taskListChanged.value = !taskListChanged.value!!
    }
}