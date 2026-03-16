package app.revanced.manager.data.platform

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

class Filesystem(private val app: Application) {
    val contentResolver = app.contentResolver // TODO: move Content Resolver operations to here.

    /**
     * A directory that gets cleared when the app restarts.
     * Do not store paths to this directory in a parcel.
     */
    val tempDir: File = app.getDir("ephemeral", Context.MODE_PRIVATE).apply {
        deleteRecursively()
        mkdirs()
    }

    /**
     * A directory for storing temporary files related to UI.
     * This is the same as [tempDir], but does not get cleared on system-initiated process death.
     * Paths to this directory can be safely stored in parcels.
     */
    val uiTempDir: File = File(app.filesDir, "ui_ephemeral").apply { mkdirs() }

    fun openFileDocument(uri: Uri): DocumentFile? {
        return DocumentFile.fromSingleUri(app, uri)
    }

    fun openFolderDocument(uri: Uri): DocumentFile? {
        return DocumentFile.fromTreeUri(app, uri)
    }
}