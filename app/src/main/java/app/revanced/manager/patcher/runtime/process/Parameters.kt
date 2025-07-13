package app.revanced.manager.patcher.runtime.process

import android.os.Parcelable
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Parameters(
    val cacheDir: String,
    val aaptPath: String,
    val frameworkDir: String,
    val packageName: String,
    val inputFile: String,
    val outputFile: String,
    val configurations: List<PatchConfiguration>,
) : Parcelable

@Parcelize
data class PatchConfiguration(
    val bundle: PatchBundle,
    val patches: Set<String>,
    val options: @RawValue Map<String, Map<String, Any?>>
) : Parcelable