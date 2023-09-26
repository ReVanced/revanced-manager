package app.revanced.manager.flutter

import android.app.Activity
import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.Serializable

class ExportSettingsActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val settingsChannel = "app.revanced.manager.flutter/settings"

        val mainChannel =
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, settingsChannel)

        mainChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "accept" -> {
                    val data = call.argument<String>("data")
                    val resultIntent = Intent()
                    resultIntent.putExtra("data", data as Serializable)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                "deny" -> {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun getDartEntrypointFunctionName(): String {
        return "mainExportSettings"
    }
}
