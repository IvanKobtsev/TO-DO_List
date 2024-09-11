package com.tsu.to_do_list

import android.app.Activity
import android.content.Context
import android.media.Image
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.currentCoroutineContext

class TaskAdapter(private val context: Context,
                  private val taskList: List<Task>) : BaseAdapter() {

    private lateinit var taskViewModel: TaskViewModel

    override fun getCount(): Int {
        return taskList.size
    }

    override fun getItem(position: Int): Any {
        return taskList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.task_layout, parent, false)
        }

        taskViewModel = ViewModelProvider(context as MainActivity)[TaskViewModel::class.java]

        // Get the current item
        val currentItem = taskList[position]

        // Set item data
        val nameView: TextView = view!!.findViewById(R.id.taskName)
        val descriptionView: TextView = view.findViewById(R.id.taskDescription)

        nameView.text = currentItem.name
        descriptionView.text = currentItem.description

        val taskWrapper: ConstraintLayout = view.findViewById(R.id.taskWrapper)
        val checkboxButton: ImageButton = view.findViewById(R.id.taskStatus)
        val editTaskButton: ImageButton = view.findViewById(R.id.editTask)
        val deleteTaskButton: ImageButton = view.findViewById(R.id.deleteTask)

        if (currentItem.state) {
            checkboxButton.setImageResource(android.R.drawable.checkbox_on_background)
        }
        else {
            checkboxButton.setImageResource(android.R.drawable.checkbox_off_background)
        }

        taskWrapper.setOnClickListener {
            taskViewModel.needToCloseInputs.value = true
        }

        checkboxButton.setOnClickListener {
            taskViewModel.taskStatusToSwap.value = position
            taskViewModel.needToCloseInputs.value = true
        }

        editTaskButton.setOnClickListener {
            taskViewModel.taskToEdit.value = position
        }

        deleteTaskButton.setOnClickListener {
            taskViewModel.taskToDelete.value = position
            taskViewModel.needToCloseInputs.value = true
        }

        return view
    }
}