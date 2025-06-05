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
import java.util.Date
import kotlin.time.Duration.Companion.days

class KeystoreManager(app: Application, private val prefs: PreferencesManager) {
    companion object Constants {
        /**
         * Default alias and password for the keystore.
         */
        const val DEFAULT = "ReVanced"
        private val eightYearsFromNow get() = Date(System.currentTimeMillis() + (365.days * 8).inWholeMilliseconds * 24)
    }

    private val keystorePath =
        app.getDir("signing", Context.MODE_PRIVATE).resolve("manager.keystore")

    private suspend fun updatePrefs(cn: String, pass: String) = prefs.edit {
        prefs.keystoreCommonName.value = cn
        prefs.keystorePass.value = pass
    }

    private suspend fun signingDetails(path: File = keystorePath) = ApkUtils.KeyStoreDetails(
        keyStore = path,
        keyStorePassword = null,
        alias = prefs.keystoreCommonName.get(),
        password = prefs.keystorePass.get()
    )

    suspend fun sign(input: File, output: File) = withContext(Dispatchers.Default) {
        ApkUtils.signApk(input, output, prefs.keystoreCommonName.get(), signingDetails())
    }

    suspend fun regenerate() = withContext(Dispatchers.Default) {
        val keyCertPair = ApkSigner.newPrivateKeyCertificatePair(
            prefs.keystoreCommonName.get(),
            eightYearsFromNow
        )
        val ks = ApkSigner.newKeyStore(
            setOf(
                ApkSigner.KeyStoreEntry(
                    DEFAULT, DEFAULT, keyCertPair
                )
            )
        )
        withContext(Dispatchers.IO) {
            keystorePath.outputStream().use {
                ks.store(it, null)
            }
        }

        updatePrefs(DEFAULT, DEFAULT)
    }

    suspend fun import(cn: String, pass: String, keystore: InputStream): Boolean {
        val keystoreData = withContext(Dispatchers.IO) { keystore.readBytes() }

        try {
            val ks = ApkSigner.readKeyStore(ByteArrayInputStream(keystoreData), null)

            ApkSigner.readPrivateKeyCertificatePair(ks, cn, pass)
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