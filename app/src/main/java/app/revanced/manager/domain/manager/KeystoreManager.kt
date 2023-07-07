package app.revanced.manager.domain.manager

import android.app.Application
import android.content.Context
import app.revanced.manager.util.signing.Signer
import app.revanced.manager.util.signing.SigningOptions
import java.io.File
import java.io.InputStream
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

    private fun options(
        cn: String = prefs.keystoreCommonName!!,
        pass: String = prefs.keystorePass!!,
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

    fun import(cn: String, pass: String, keystore: Path): Boolean {
        if (!Signer(SigningOptions(cn, pass, keystore)).canUnlock()) {
            return false
        }
        Files.copy(keystore, keystorePath, StandardCopyOption.REPLACE_EXISTING)

        updatePrefs(cn, pass)
        return true
    }

    fun export(target: OutputStream) {
        Files.copy(keystorePath, target)
    }
}