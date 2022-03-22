package app.revanced.manager.patcher.signatures

import app.revanced.patcher.signature.Signature
import com.beust.klaxon.Klaxon


class SignatureLoader {
    fun updateSignatures(fromUrl: String) {
        // TODO()
    }

    fun loadSignatures(path: String): Array<Signature>? {
        return Klaxon().parse<Array<Signature>>(path)
    }
}
