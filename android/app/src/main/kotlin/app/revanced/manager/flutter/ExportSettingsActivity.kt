package app.revanced.manager.flutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import org.json.JSONObject
import java.io.File

import android.util.Log

class ExportSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val json = JSONObject()

        // Default Data
        json.put("keystorePassword", "s3cur3p@ssw0rd")

        // Load Shared Preferences
        val sharedPreferences = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val allEntries: Map<String, *> = sharedPreferences.getAll()
        for ((key, value) in allEntries.entries) {
            json.put(
                key.replace("flutter.", ""),
                if (value is Boolean) if (value) 1 else 0 else value
            )
        }

        // Load keystore
        val keystoreFile = File(getExternalFilesDir(null), "/revanced-manager.keystore")
        if (keystoreFile.exists()) {
            val keystoreBytes = keystoreFile.readBytes()
            val keystoreBase64 =
                Base64.encodeToString(keystoreBytes, Base64.DEFAULT).replace("\n", "")
            json.put("keystore", keystoreBase64)
        }

        // Load saved patches
        val storedPatchesFile = File(filesDir.parentFile.absolutePath, "/app_flutter/selected-patches.json")
        if (storedPatchesFile.exists()) {
            val patchesBytes = storedPatchesFile.readBytes()
            val patches = String(patchesBytes, Charsets.UTF_8)
            json.put("patches", patches)
        }

        // Send data back
        Log.e("ExportSettingsActivity", json.toString())
        val resultIntent = Intent()
        resultIntent.putExtra("data", json.toString())
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
