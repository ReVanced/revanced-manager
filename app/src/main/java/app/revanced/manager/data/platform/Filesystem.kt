package app.revanced.manager.data.platform

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import app.revanced.manager.util.RequestManageStorageContract

class Filesystem(private val app: Application) {
    val contentResolver = app.contentResolver // TODO: move Content Resolver operations to here.

    /**
     * A directory that gets cleared when the app restarts.
     * Do not store paths to this directory in a parcel.
     */
    val tempDir = app.getDir("ephemeral", Context.MODE_PRIVATE).apply {
        deleteRecursively()
        mkdirs()
    }

    fun externalFilesDir() = Environment.getExternalStorageDirectory().toPath()

    private fun usesManagePermission() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private val storagePermissionName = if (usesManagePermission()) Manifest.permission.MANAGE_EXTERNAL_STORAGE else Manifest.permission.READ_EXTERNAL_STORAGE

    fun permissionContract(): Pair<ActivityResultContract<String, Boolean>, String> {
        val contract = if (usesManagePermission()) RequestManageStorageContract() else ActivityResultContracts.RequestPermission()
        return contract to storagePermissionName
    }

    fun hasStoragePermission() = if (usesManagePermission()) Environment.isExternalStorageManager() else app.checkSelfPermission(storagePermissionName) == PackageManager.PERMISSION_GRANTED
}