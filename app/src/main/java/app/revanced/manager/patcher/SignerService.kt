package app.revanced.manager.patcher

import android.app.Application
import app.revanced.manager.util.signing.Signer
import app.revanced.manager.util.signing.SigningOptions

class SignerService(app: Application) {
    private val options = SigningOptions("ReVanced", "ReVanced", app.dataDir.resolve("manager.keystore").path)

    fun createSigner() = Signer(options)
}