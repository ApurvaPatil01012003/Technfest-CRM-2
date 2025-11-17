package com.technfest.technfestcrm.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.technfest.technfestcrm.utils.AllRecordingsAutoMover

class MoveRecordingsWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        return try {
            Log.d("MoveWorker", "Starting auto-move recordings…")
            AllRecordingsAutoMover(ctx).autoMoveRecordings()
            Log.d("MoveWorker", "Auto-move completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("MoveWorker", "Error in auto-move", e)
            Result.retry()
        }
    }
}
