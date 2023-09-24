package app.revanced.manager.flutter.utils.share

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.json.JSONObject

import android.util.Log

class ShareProvider : ContentProvider() {
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

        // TODO: load default data

        // Load Shared Preferences
        val sharedPreferences = context!!.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        val allEntries: Map<String, *> = sharedPreferences.getAll()
        for ((key, value) in allEntries.entries) {
            Log.d("map values", key + ": " + value.toString())
            json.put(key.replace("flutter.", ""), value)
        }

        // TODO: Load keystore

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
