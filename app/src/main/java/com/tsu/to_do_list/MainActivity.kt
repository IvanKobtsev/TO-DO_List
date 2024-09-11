package com.tsu.to_do_list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tsu.to_do_list.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    private var bIsEditingTask = false
    private var taskToEdit = -1

    private var gson: Gson? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        taskAdapter = TaskAdapter(this, taskViewModel.currentTaskList.tasks)
        binding.taskListView.adapter = taskAdapter

        binding.fab.setOnClickListener {
            binding.taskNameInput.text!!.clear()
            binding.taskDescrInput.text!!.clear()
            binding.taskInput.visibility = View.VISIBLE
            bIsEditingTask = false
        }

        binding.doneButton.setOnClickListener {

            if (binding.taskNameInput.text.toString() != "") {

                if (!bIsEditingTask) {
                    taskViewModel.currentTaskList.tasks.add(
                        Task(
                            binding.taskNameInput.text.toString(),
                            binding.taskDescrInput.text.toString()
                        ))
                }
                else {
                    taskViewModel.currentTaskList.tasks[taskToEdit].name = binding.taskNameInput.text.toString()
                    taskViewModel.currentTaskList.tasks[taskToEdit].description = binding.taskDescrInput.text.toString()
                }

                taskViewModel.taskListChanged.value = true
                binding.taskInput.visibility = View.INVISIBLE
            }
            else {
                Toast.makeText(this, "Введите название дела!", Toast.LENGTH_SHORT).show()
            }
        }

        taskViewModel.needToCloseInputs.observe(this) {
            binding.taskInput.visibility = View.INVISIBLE
        }

        taskViewModel.taskStatusToSwap.observe(this) {
            if (taskViewModel.taskStatusToSwap.value!! != -1 && taskViewModel.taskStatusToSwap.value!! < taskViewModel.currentTaskList.tasks.count()) {
                taskViewModel.currentTaskList.tasks[taskViewModel.taskStatusToSwap.value!!].state = !taskViewModel.currentTaskList.tasks[taskViewModel.taskStatusToSwap.value!!].state
                taskViewModel.taskListChanged.value = true
            }
        }

        taskViewModel.taskToEdit.observe(this) {
            if (taskViewModel.taskToEdit.value!! != -1 && taskViewModel.taskToEdit.value!! < taskViewModel.currentTaskList.tasks.count()) {
                taskToEdit = taskViewModel.taskToEdit.value!!
                bIsEditingTask = true
                binding.taskNameInput.setText(taskViewModel.currentTaskList.tasks[taskViewModel.taskToEdit.value!!].name, TextView.BufferType.EDITABLE)
                binding.taskDescrInput.setText(taskViewModel.currentTaskList.tasks[taskViewModel.taskToEdit.value!!].description, TextView.BufferType.EDITABLE)
                binding.taskInput.visibility = View.VISIBLE
            }
        }

        taskViewModel.taskToDelete.observe(this) {
            if (taskViewModel.taskToDelete.value!! != -1 && taskViewModel.taskToDelete.value!! < taskViewModel.currentTaskList.tasks.count()) {
                taskViewModel.currentTaskList.tasks.removeAt(taskViewModel.taskToDelete.value!!)
                taskViewModel.taskListChanged.value = true
            }
        }

        taskViewModel.taskListChanged.observe(this) {
            taskAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {

                writeTextToFile(gson?.toJson(taskViewModel.currentTaskList))

                true
            }
            R.id.action_load -> {

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                getDataFromFile.launch(intent)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun writeTextToFile(jsonResponse: String?) {
        if (jsonResponse != "") {
            val dir = File("//sdcard//Download//")

            var listName = ""
            val builder = AlertDialog.Builder(this)
            val dialogLayout = layoutInflater.inflate(R.layout.file_name_dialog, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.fileNameEditor)

            with (builder) {
                setTitle("Введите имя списка")
                setPositiveButton("Готово") { _, _ ->
                    listName = editText.text.toString()

                    if (listName == "") {
                        val currentDate = Date()
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                        listName = dateFormat.format(currentDate)
                    }

                    val myExternalFile = File(dir, listName)
                    var fos: FileOutputStream? = null
                    try {
                        fos = FileOutputStream(myExternalFile)
                        fos.write(jsonResponse?.toByteArray())
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    Toast.makeText(this.context, "Успешно!", Toast.LENGTH_SHORT).show()
                }
                setNegativeButton("Отменить") { _, _ ->
                    // Do nothing
                }
                setView(dialogLayout)
                show()
            }


        }
    }

    private fun readTextFromUri(uri: Uri): String {
        var inputStream: InputStream? = null
        val stringBuilder = StringBuilder()
        try {
            inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line = reader.readLine()
            while (line != null) {
                stringBuilder.append(line).append('\n')
                line = reader.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }

    private var getDataFromFile =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                val fileContents = readTextFromUri(uri!!)
                taskViewModel.currentTaskList = gson?.fromJson(fileContents, TaskList::class.java)!!

                taskAdapter = TaskAdapter(this, taskViewModel.currentTaskList.tasks)
                binding.taskListView.adapter = taskAdapter

                taskViewModel.taskListChanged.value = true
            }
        }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}