package app.revanced.manager.flutter.utils.share

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.File

class LegacySettingsProvider : ContentProvider() {
    private val authority = "app.revanced.manager.flutter.provider"
    private val URI_CODE_SETTINGS = 1

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(authority, "settings", URI_CODE_SETTINGS)
    }

    override fun onCreate(): Boolean {
        return true
    }

    fun getSettings(): String {
        val json = JSONObject()

        // Default Data
        json.put("keystorePassword", "s3cur3p@ssw0rd")

        // Load Shared Preferences
        val sharedPreferences = context!!.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val allEntries: Map<String, *> = sharedPreferences.getAll()
        for ((key, value) in allEntries.entries) {
            json.put(key.replace("flutter.", ""), if (value is Boolean) if (value) 1 else 0 else value)
        }

        // Load keystore
        val keystoreFile = File(context!!.getExternalFilesDir(null), "/revanced-manager.keystore")
        if (keystoreFile.exists()) {
            val keystoreBytes = keystoreFile.readBytes()
            val keystoreBase64 = Base64.encodeToString(keystoreBytes, Base64.DEFAULT).replace("\n", "")
            json.put("keystore", keystoreBase64)
        }

        // Load saved patches
        val storedPatchesFile = File(context!!.filesDir.parentFile.absolutePath, "/app_flutter/selected-patches.json")
        if (storedPatchesFile.exists()) {
            val patchesBytes = storedPatchesFile.readBytes()
            val patches = String(patchesBytes, Charsets.UTF_8)
            json.put("savedPatches", patches)
        }

        return json.toString()
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ):Cursor? {
        when (uriMatcher.match(uri)) {
            URI_CODE_SETTINGS -> {
                val cursor = MatrixCursor(arrayOf("settings"))
                val row = arrayOf(getSettings())
                cursor.addRow(row)
                return cursor
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Insert operation is not supported")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Update operation is not supported")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Delete operation is not supported")
    }

    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Get type operation is not supported")
    }
}
