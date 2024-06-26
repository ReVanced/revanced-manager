package app.revanced.manager.patcher.patch

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class PatchBundle(val patchesJar: File, val integrations: File?) : Parcelable