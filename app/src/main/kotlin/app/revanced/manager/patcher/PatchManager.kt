package app.revanced.manager.patcher

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.signature.Signature
import java.io.File

class PatchManager (
    private val apk: File,
    private val patches: Array<Patch>,
    private val signatures: Array<Signature>
    )
{

}