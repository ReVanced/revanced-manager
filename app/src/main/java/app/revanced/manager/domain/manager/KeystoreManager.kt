package app.revanced.manager.domain.manager

import android.app.Application
import android.content.Context
import app.revanced.manager.util.signing.Signer
import app.revanced.manager.util.signing.SigningOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

class KeystoreManager(app: Application, private val prefs: PreferencesManager) {
    companion object {
        /**
         * Default alias and password for the keystore.
         */
        const val DEFAULT = "ReVanced"
    }

    private val keystorePath =
        app.getDir("signing", Context.MODE_PRIVATE).resolve("manager.keystore").toPath()

    private suspend fun updatePrefs(cn: String, pass: String) = prefs.edit {
        prefs.keystoreCommonName.value = cn
        prefs.keystorePass.value = pass
    }

    suspend fun sign(input: File, output: File) = withContext(Dispatchers.Default) {
        Signer(
            SigningOptions(
                prefs.keystoreCommonName.get(),
                prefs.keystorePass.get(),
                keystorePath
            )
        ).signApk(
            input,
            output
        )
    }

    suspend fun regenerate() = withContext(Dispatchers.Default) {
        Signer(SigningOptions(DEFAULT, DEFAULT, keystorePath)).regenerateKeystore()
        updatePrefs(DEFAULT, DEFAULT)
    }

    suspend fun import(cn: String, pass: String, keystore: Path): Boolean {
        if (!Signer(SigningOptions(cn, pass, keystore)).canUnlock()) {
            return false
        }
        withContext(Dispatchers.IO) {
            Files.copy(keystore, keystorePath, StandardCopyOption.REPLACE_EXISTING)
        }

        updatePrefs(cn, pass)
        return true
    }

    fun hasKeystore() = keystorePath.exists()

    suspend fun export(target: OutputStream) {
        withContext(Dispatchers.IO) {
            Files.copy(keystorePath, target)
        }
    }
}