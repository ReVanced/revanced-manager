package app.revanced.manager.domain.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

abstract class Worker<ARGS>(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters)