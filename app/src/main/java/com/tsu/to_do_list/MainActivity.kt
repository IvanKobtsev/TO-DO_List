package com.tsu.to_do_list

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.tsu.to_do_list.databinding.ActivityMainBinding
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    private var previewIds: ArrayList<Int> = ArrayList()
    private var previewStrings: ArrayList<String> = ArrayList()
    private var bIsEditingTask = false
    private var taskToEdit = -1
    private var currentURL = "https://f875-85-192-63-92.ngrok-free.app"
    private val secretToken = "2mgI8UtE3CoFPfld6fcwGa04mX3_5MB68eH1WTZWRB5sTMcEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Important settings
        taskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = "Новый список"

        // Task adapter
        taskAdapter = TaskAdapter(this, taskViewModel.currentTaskList.tasks)
        binding.taskListView.adapter = taskAdapter

        // TaskListPreviews
        binding.taskListPreviews.visibility = View.INVISIBLE
        
        binding.taskListPreviews.setOnItemClickListener { _, _, position, _ ->
            binding.taskListPreviews.visibility = View.INVISIBLE
            loadList(previewIds[position])
        }

        // Bindings and other stuff
        binding.fab.setOnClickListener {
            binding.taskNameInput.text!!.clear()
            binding.taskDescrInput.text!!.clear()
            binding.taskInput.visibility = View.VISIBLE
            bIsEditingTask = false
        }

        binding.doneButton.setOnClickListener {

            if (binding.taskNameInput.text.toString() != "") {

                if (!bIsEditingTask) {
                    if (taskViewModel.currentTaskList.tasks.isNotEmpty()) {
                        addTaskHTTP()
                    }
                    else {
                        newListHTTP()
                    }
                }
                else {
                    editTaskHTTP()
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

                val url = "$currentURL/api/ToDoLists/updateTaskState"
                val requestQueue = Volley.newRequestQueue(this.applicationContext)
                val headers = HashMap<String, String>()
                headers["Access-Control-Allow-Origin"] = "*"
                headers["Authorization"] = "Bearer $secretToken"
                headers["ngrok-skip-browser-warning"] = "any"
                headers["Content-Type"] = "application/json;charset=UTF-8"

                val jsonTask = JSONObject()
                jsonTask.put("status", !taskViewModel.currentTaskList.tasks[taskViewModel.taskStatusToSwap.value!!].state)
                jsonTask.put("id", taskViewModel.currentTaskList.tasks[taskViewModel.taskStatusToSwap.value!!].id)
                jsonTask.put("taskListId", taskViewModel.currentTaskList.id)

                val stringRequest = object : JsonObjectRequest(Method.PUT, url, jsonTask,
                    Response.Listener { _ ->

                        loadList(taskViewModel.currentTaskList.id)
                    },
                    Response.ErrorListener { _ ->
                        stopLoading()
                        Toast.makeText(this.applicationContext, "Чёт нету коннекта...", Toast.LENGTH_LONG).show()
                    }) {
                    override fun getHeaders(): Map<String, String> {
                        return headers
                    }
                    override fun getBodyContentType(): String {
                        return "application/json"
                    }
                }

                startLoading()
                requestQueue.add(stringRequest)

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

                val url = "$currentURL/api/ToDoLists/deleteTask/" + taskViewModel.currentTaskList.id + "&" + taskViewModel.currentTaskList.tasks[taskViewModel.taskToDelete.value!!].id
                val requestQueue = Volley.newRequestQueue(this.applicationContext)
                val headers = HashMap<String, String>()
                headers["Access-Control-Allow-Origin"] = "*"
                headers["Authorization"] = "Bearer $secretToken"
                headers["ngrok-skip-browser-warning"] = "any"

                val stringRequest = object : StringRequest(Method.DELETE, url,
                    Response.Listener { _ ->

                        loadList(taskViewModel.currentTaskList.id)
                    },
                    Response.ErrorListener { _ ->
                        stopLoading()
                        Toast.makeText(this.applicationContext, "Чёт нету коннекта...", Toast.LENGTH_LONG).show()
                    }) {
                    override fun getHeaders(): Map<String, String> {
                        return headers
                    }
                }

                startLoading()
                requestQueue.add(stringRequest)

                taskViewModel.taskListChanged.value = true
            }
        }

        taskViewModel.taskListChanged.observe(this) {
//            updateListView()
        }

        val builder = AlertDialog.Builder(this)
        val dialogLayout = layoutInflater.inflate(R.layout.url_dialog, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.fileNameEditor)

        with (builder) {
            setTitle("Введите URL для подключения:")
            setPositiveButton("Готово") { _, _ ->

                currentURL = editText.text.toString()

                Toast.makeText(this.context, "Успешно! (наверное)", Toast.LENGTH_SHORT).show()
            }
            setNegativeButton("Отменить") { _, _ ->
                // Do nothing
            }
            setView(dialogLayout)
            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new -> {

                val builder = AlertDialog.Builder(this)
                val dialogLayout = layoutInflater.inflate(R.layout.file_name_dialog, null)
                val editText = dialogLayout.findViewById<EditText>(R.id.fileNameEditor)

                with (builder) {
                    setTitle("Введите имя нового списка:")
                    setPositiveButton("Готово") { _, _ ->

                        taskViewModel.currentTaskList.name = editText.text.toString()
                        supportActionBar!!.title = taskViewModel.currentTaskList.name

                        previewStrings.clear()
                        previewIds.clear()

                        taskViewModel.currentTaskList.tasks.clear()
                        taskViewModel.currentTaskList.id = 0
                        taskViewModel.currentTaskList.xMin = -1
                        updateListView()
                        binding.taskListPreviews.visibility = View.INVISIBLE
                    }
                    setNegativeButton("Отменить") { _, _ ->
                        // Do nothing
                    }
                    setView(dialogLayout)
                    show()
                }

                true
            }
            R.id.action_load -> {

                val url = "$currentURL/api/ToDoLists/all"
                val requestQueue = Volley.newRequestQueue(this.applicationContext)
                val headers = HashMap<String, String>()
                headers["Access-Control-Allow-Origin"] = "*"
                headers["Authorization"] = "Bearer $secretToken"
                headers["ngrok-skip-browser-warning"] = "any"

                val stringRequest = object : StringRequest(Method.GET, url,
                    Response.Listener { response ->
                        Log.e("1", response)

                        val json = Json { ignoreUnknownKeys = true }
                        val previewsResponse: MyPreviewsResponse = json.decodeFromString<MyPreviewsResponse>(response)

                        previewStrings.clear()
                        previewIds.clear()

                        previewsResponse.values.forEach { el ->
                            previewStrings.add(el.name)
                            previewIds.add(el.id)
                        }

                        val previewsArrayAdapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_1,
                            previewStrings
                        )

                        binding.taskListPreviews.adapter = previewsArrayAdapter
                        binding.taskListPreviews.visibility = View.VISIBLE
                        stopLoading()
                    },
                    Response.ErrorListener { _ ->
                        stopLoading()
                        Toast.makeText(this.applicationContext, "Чёт нету коннекта...", Toast.LENGTH_LONG).show()
                    }) {
                    override fun getHeaders(): Map<String, String> {
                        return headers
                    }
                }

                startLoading()
                requestQueue.add(stringRequest)

                true
            }
            R.id.action_delete -> {

                val url = "$currentURL/api/ToDoLists/deleteList/" + taskViewModel.currentTaskList.id
                val requestQueue = Volley.newRequestQueue(this.applicationContext)
                val headers = HashMap<String, String>()
                headers["Access-Control-Allow-Origin"] = "*"
                headers["Authorization"] = "Bearer $secretToken"
                headers["ngrok-skip-browser-warning"] = "any"

                val stringRequest = object : StringRequest(Method.DELETE, url,
                    Response.Listener { response ->
                        Log.e("1", response)

                        previewStrings.clear()
                        previewIds.clear()

                        taskViewModel.currentTaskList.tasks.clear()
                        taskViewModel.currentTaskList.name = "Новый список"
                        taskViewModel.currentTaskList.id = 0
                        taskViewModel.currentTaskList.xMin = -1
                        updateListView()
                        supportActionBar!!.title = "Новый список"
                    },
                    Response.ErrorListener { _ ->
                        stopLoading()
                        Toast.makeText(this.applicationContext, "Чёт нету коннекта...", Toast.LENGTH_LONG).show()
                    }) {
                    override fun getHeaders(): Map<String, String> {
                        return headers
                    }
                }

                startLoading()
                requestQueue.add(stringRequest)

                true
            }
            R.id.action_rename -> {

                val builder = AlertDialog.Builder(this)
                val dialogLayout = layoutInflater.inflate(R.layout.file_name_dialog, null)
                val editText = dialogLayout.findViewById<EditText>(R.id.fileNameEditor)

                with (builder) {
                    setTitle("Введите новое имя:")
                    setPositiveButton("Готово") { _, _ ->

                        val url = "$currentURL/api/ToDoLists/updateListName"
                        val requestQueue = Volley.newRequestQueue(context)
                        val headers = HashMap<String, String>()
                        headers["Access-Control-Allow-Origin"] = "*"
                        headers["Authorization"] = "Bearer $secretToken"
                        headers["ngrok-skip-browser-warning"] = "any"
                        headers["Content-Type"] = "application/json;charset=UTF-8"

                        taskViewModel.currentTaskList.name = editText.text.toString()

                        val newName = JSONObject()
                        newName.put("name", taskViewModel.currentTaskList.name)
                        newName.put("id", taskViewModel.currentTaskList.id)

                        val stringRequest = object : JsonObjectRequest(Method.PUT, url, newName,
                            Response.Listener { _ ->

                                loadList(taskViewModel.currentTaskList.id)
                            },
                            Response.ErrorListener { _ ->

                                supportActionBar!!.title = taskViewModel.currentTaskList.name
                                stopLoading()
                            }) {
                            override fun getHeaders(): Map<String, String> {
                                return headers
                            }
                            override fun getBodyContentType(): String {
                                return "application/json"
                            }
                        }

                        startLoading()
                        requestQueue.add(stringRequest)
                    }
                    setNegativeButton("Отменить") { _, _ ->
                        // Do nothing
                    }
                    setView(dialogLayout)
                    show()
                }

                true
            }
            R.id.action_update -> {

                loadList(taskViewModel.currentTaskList.id)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startLoading() {
        binding.loadingScreen.visibility = View.VISIBLE
    }

    private fun stopLoading() {
        binding.loadingScreen.visibility = View.INVISIBLE
    }

    private fun loadList(listId: Int) {
        val url = "$currentURL/api/ToDoLists/$listId"
        val requestQueue = Volley.newRequestQueue(this.applicationContext)
        val headers = HashMap<String, String>()
        headers["Access-Control-Allow-Origin"] = "*"
        headers["Authorization"] = "Bearer $secretToken"
        headers["ngrok-skip-browser-warning"] = "any"
        taskViewModel.currentTaskList.id = listId

        val stringRequest = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                Log.e("1", response)

                val json = Json { ignoreUnknownKeys = true }
                val taskList = json.decodeFromString<TaskListResponse>(response)

                taskViewModel.currentTaskList.tasks.clear()

                taskViewModel.currentTaskList.xMin = taskList.xMin
                taskViewModel.currentTaskList.name = taskList.name
                supportActionBar!!.title = taskList.name

                taskList.taskList.values.forEach { el ->
                    taskViewModel.currentTaskList.tasks.add(el)
                }

                updateListView()
            },
            Response.ErrorListener { _ ->
                stopLoading()
            }) {
            override fun getHeaders(): Map<String, String> {
                return headers
            }
        }

        startLoading()
        requestQueue.add(stringRequest)
    }

    private fun updateListView() {
        taskAdapter = TaskAdapter(this, taskViewModel.currentTaskList.tasks)
        binding.taskListView.adapter = taskAdapter
        taskViewModel.taskListChanged.value = true
        stopLoading()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun newListHTTP() {

        val url = "$currentURL/api/ToDoLists/newList"
        val requestQueue = Volley.newRequestQueue(this.applicationContext)
        val headers = HashMap<String, String>()
        headers["Access-Control-Allow-Origin"] = "*"
        headers["Authorization"] = "Bearer $secretToken"
        headers["ngrok-skip-browser-warning"] = "any"
        headers["Content-Type"] = "application/json;charset=UTF-8"


        val jsonTask = JSONObject()
        jsonTask.put("name",binding.taskNameInput.text.toString())
        jsonTask.put("description", binding.taskDescrInput.text.toString())
        jsonTask.put("status", false)
        jsonTask.put("id", 0)

        val jsonList = JSONObject()
        val jsonArray = JSONArray()
        jsonArray.put(jsonTask)
        jsonList.put("name", taskViewModel.currentTaskList.name)
        jsonList.put("xMin", "0")
        jsonList.put("taskList", jsonArray)

        val stringRequest = object : JsonObjectRequest(Method.POST, url, jsonList,
            Response.Listener { response ->

                loadList(response.get("id") as Int)
            },
            Response.ErrorListener { _ ->
                stopLoading()
                Toast.makeText(this.applicationContext, "Чёт нету коннекта...", Toast.LENGTH_LONG).show()
            }) {
            override fun getHeaders(): Map<String, String> {
                return headers
            }
            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        startLoading()
        requestQueue.add(stringRequest)
    }

    private fun addTaskHTTP() {

        val url = "$currentURL/api/ToDoLists/newTask"
        val requestQueue = Volley.newRequestQueue(this.applicationContext)
        val headers = HashMap<String, String>()
        headers["Access-Control-Allow-Origin"] = "*"
        headers["Authorization"] = "Bearer $secretToken"
        headers["ngrok-skip-browser-warning"] = "any"
        headers["Content-Type"] = "application/json;charset=UTF-8"

        val jsonTask = JSONObject()
        jsonTask.put("name",binding.taskNameInput.text.toString())
        jsonTask.put("description", binding.taskDescrInput.text.toString())
        jsonTask.put("taskListId", taskViewModel.currentTaskList.id)

        val stringRequest = object : JsonObjectRequest(Method.POST, url, jsonTask,
            Response.Listener { response ->

                loadList(response.get("id") as Int)
            },
            Response.ErrorListener { _ ->
                stopLoading()
                Toast.makeText(this.applicationContext, "Чёт нету коннекта...", Toast.LENGTH_LONG).show()
            }) {
            override fun getHeaders(): Map<String, String> {
                return headers
            }
            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        startLoading()
        requestQueue.add(stringRequest)
    }

    private fun editTaskHTTP() {

        bIsEditingTask = false

        val url = "$currentURL/api/ToDoLists/updateTaskNameDescr"
        val requestQueue = Volley.newRequestQueue(this.applicationContext)
        val headers = HashMap<String, String>()
        headers["Access-Control-Allow-Origin"] = "*"
        headers["Authorization"] = "Bearer $secretToken"
        headers["ngrok-skip-browser-warning"] = "any"
        headers["Content-Type"] = "application/json;charset=UTF-8"

        val jsonTask = JSONObject()
        jsonTask.put("name",binding.taskNameInput.text.toString())
        jsonTask.put("description", binding.taskDescrInput.text.toString())
        jsonTask.put("id", taskViewModel.currentTaskList.tasks[taskToEdit].id)
        jsonTask.put("taskListId", taskViewModel.currentTaskList.id)

        val stringRequest = object : JsonObjectRequest(Method.PUT, url, jsonTask,
            Response.Listener { _ ->

                loadList(taskViewModel.currentTaskList.id)
            },
            Response.ErrorListener { _ ->
                stopLoading()
                Toast.makeText(this.applicationContext, "Чёт нету коннекта...", Toast.LENGTH_LONG).show()
            }) {
            override fun getHeaders(): Map<String, String> {
                return headers
            }
            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        startLoading()
        requestQueue.add(stringRequest)
    }
}