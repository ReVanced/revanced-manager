package app.revanced.manager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
class RequestManageStorageContract(private val forceLaunch: Boolean = false) : ActivityResultContract<String, Boolean>() {
    override fun createIntent(context: Context, input: String) = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.fromParts("package", context.packageName, null))

    override fun getSynchronousResult(context: Context, input: String): SynchronousResult<Boolean>? = if (!forceLaunch && Environment.isExternalStorageManager()) SynchronousResult(true) else null

    override fun parseResult(resultCode: Int, intent: Intent?) = Environment.isExternalStorageManager()
}