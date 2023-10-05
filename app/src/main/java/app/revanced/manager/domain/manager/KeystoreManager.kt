package app.revanced.manager.domain.manager

import android.app.Application
import android.content.Context
import app.revanced.library.ApkSigner
import app.revanced.library.ApkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.security.UnrecoverableKeyException

class KeystoreManager(app: Application, private val prefs: PreferencesManager) {
    companion object Constants {
        /**
         * Default alias and password for the keystore.
         */
        const val DEFAULT = "ReVanced"
    }

    private val keystorePath =
        app.getDir("signing", Context.MODE_PRIVATE).resolve("manager.keystore")

    private suspend fun updatePrefs(cn: String, pass: String) = prefs.edit {
        prefs.keystoreCommonName.value = cn
        prefs.keystorePass.value = pass
    }

    private suspend fun signingOptions(path: File = keystorePath) = ApkUtils.SigningOptions(
        keyStore = path,
        keyStorePassword = null,
        alias = prefs.keystoreCommonName.get(),
        signer = prefs.keystoreCommonName.get(),
        password = prefs.keystorePass.get()
    )

    suspend fun sign(input: File, output: File) = withContext(Dispatchers.Default) {
        ApkUtils.sign(input, output, signingOptions())
    }

    suspend fun regenerate() = withContext(Dispatchers.Default) {
        val ks = ApkSigner.newKeyStore(
            listOf(
                ApkSigner.KeyStoreEntry(
                    DEFAULT, DEFAULT
                )
            )
        )
        keystorePath.outputStream().use {
            ks.store(it, null)
        }

        updatePrefs(DEFAULT, DEFAULT)
    }

    suspend fun import(cn: String, pass: String, keystore: InputStream): Boolean {
        val keystoreData = keystore.readBytes()

        try {
            val ks = ApkSigner.readKeyStore(ByteArrayInputStream(keystoreData), null)

            ApkSigner.readKeyCertificatePair(ks, cn, pass)
        } catch (_: UnrecoverableKeyException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }

        withContext(Dispatchers.IO) {
            Files.write(keystorePath.toPath(), keystoreData)
        }

        updatePrefs(cn, pass)
        return true
    }

    fun hasKeystore() = keystorePath.exists()

    suspend fun export(target: OutputStream) {
        withContext(Dispatchers.IO) {
            Files.copy(keystorePath.toPath(), target)
        }
    }
}