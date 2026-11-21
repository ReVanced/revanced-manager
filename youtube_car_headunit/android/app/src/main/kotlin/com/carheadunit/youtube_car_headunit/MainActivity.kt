package com.carheadunit.youtube_car_headunit

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import com.ryanheise.audioservice.AudioServiceActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

// AudioServiceActivity shares audio_service's cached FlutterEngine, so the media session
// (steering-wheel buttons, notification) keeps running after this activity is destroyed.
class MainActivity : AudioServiceActivity() {
    private val channelName = "com.carheadunit.youtube_car_headunit/pip"
    private var methodChannel: MethodChannel? = null
    private var isAutoPipEnabled = false

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
        methodChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "enterPip" -> result.success(enterPipMode())
                "isPipSupported" -> result.success(isPipSupported())
                "setAutoPip" -> {
                    isAutoPipEnabled = call.argument<Boolean>("enabled") ?: false
                    result.success(true)
                }
                // Back on the dashboard: behave like Home and keep the activity alive.
                "minimize" -> result.success(moveTaskToBack(true))
                else -> result.notImplemented()
            }
        }
    }

    // The engine outlives this activity; drop the handler so it does not keep a destroyed
    // activity alive. A recreated activity registers a fresh one.
    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        super.cleanUpFlutterEngine(flutterEngine)
    }

    private fun isPipSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun enterPipMode(): Boolean {
        if (!isPipSupported()) return false
        return try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (e: RuntimeException) {
            // IllegalStateException when not resumed, IllegalArgumentException on bad params.
            false
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isAutoPipEnabled) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        methodChannel?.invokeMethod("onPipModeChanged", isInPictureInPictureMode)
    }
}
