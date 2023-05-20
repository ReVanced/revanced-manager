package app.revanced.manager.compose.patcher

import android.app.Application
import app.revanced.manager.compose.util.signing.Signer
import app.revanced.manager.compose.util.signing.SigningOptions

class SignerService(app: Application) {
    private val options = SigningOptions("ReVanced", "ReVanced", app.dataDir.resolve("manager.keystore").path)

    fun createSigner() = Signer(options)
}