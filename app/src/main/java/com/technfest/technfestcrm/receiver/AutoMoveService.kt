package com.technfest.technfestcrm.receiver

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.technfest.technfestcrm.utils.AllRecordingsAutoMover

class AutoMoveService : IntentService("AutoMoveService") {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onHandleIntent(intent: Intent?) {
        val prefs = getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        val treeUri = prefs.getString("folder_path", null) ?: return

        val fragment = AllRecordingsAutoMover(this)
        fragment.autoMoveRecordings()
    }
}
