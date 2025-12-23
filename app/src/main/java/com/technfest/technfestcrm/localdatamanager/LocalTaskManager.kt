package com.technfest.technfestcrm.localdatamanager

import android.content.Context
import com.technfest.technfestcrm.model.LocalTask

object LocalTaskManager {

    private const val PREF_NAME = "LocalTasks"
    private const val KEY_TASKS = "task_list"

    fun saveTask(context: Context, task: LocalTask) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()

        val existingJson = prefs.getString(KEY_TASKS, null)
        val type = object : com.google.gson.reflect.TypeToken<MutableList<LocalTask>>() {}.type

        val taskList: MutableList<LocalTask> =
            if (existingJson != null) gson.fromJson(existingJson, type)
            else mutableListOf()

        taskList.add(task)

        prefs.edit()
            .putString(KEY_TASKS, gson.toJson(taskList))
            .apply()
    }
}
