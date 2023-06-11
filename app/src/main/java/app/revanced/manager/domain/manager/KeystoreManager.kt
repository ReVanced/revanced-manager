package app.revanced.manager.domain.manager

import android.app.Application
import app.revanced.manager.util.signing.Signer
import app.revanced.manager.util.signing.SigningOptions
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

class KeystoreManager(app: Application, private val prefs: PreferencesManager) {
    companion object {
        /**
         * Default common name and password for the keystore.
         */
        const val DEFAULT = "ReVanced"

        /**
         * The default password used by the Flutter version.
         */
        const val FLUTTER_MANAGER_PASSWORD = "s3cur3p@ssw0rd"
    }

    private val keystorePath = app.dataDir.resolve("manager.keystore").toPath()
    private fun options(
        cn: String = prefs.keystoreCommonName!!,
        pass: String = prefs.keystorePass!!
    ) = SigningOptions(cn, pass, keystorePath)

    private fun updatePrefs(cn: String, pass: String) {
        prefs.keystoreCommonName = cn
        prefs.keystorePass = pass
    }

    fun sign(input: File, output: File) = Signer(options()).signApk(input, output)

    init {
        if (!keystorePath.exists()) {
            regenerate()
        }
    }

    fun regenerate() = Signer(options(DEFAULT, DEFAULT)).regenerateKeystore().also {
        updatePrefs(DEFAULT, DEFAULT)
    }

    fun import(cn: String, pass: String, keystore: InputStream) {
        // TODO: check if the user actually provided the correct password
        Files.copy(keystore, keystorePath, StandardCopyOption.REPLACE_EXISTING)

        updatePrefs(cn, pass)
    }

    fun export(target: OutputStream) {
        Files.copy(keystorePath, target)
    }
}