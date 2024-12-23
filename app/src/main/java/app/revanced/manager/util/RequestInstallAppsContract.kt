package app.revanced.manager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object RequestInstallAppsContract : ActivityResultContract<String, Boolean>(), KoinComponent {
    private val pm: PM by inject()
    override fun createIntent(context: Context, input: String) = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", input, null))

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return pm.canInstallPackages()
    }
}