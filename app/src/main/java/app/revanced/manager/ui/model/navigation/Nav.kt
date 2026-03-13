package app.revanced.manager.ui.model.navigation

import android.os.Parcelable
import app.revanced.manager.ui.model.SelectedSource
import app.revanced.manager.ui.model.SelectedVersion
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.Serializable

interface ComplexParameter<T : Parcelable>

@Serializable
object Onboarding

@Serializable
object Dashboard

@Serializable
object AppSelector

@Serializable
data class InstalledApplicationInfo(val packageName: String)

@Serializable
data class BundleInformation(val uid: Int)

@Serializable
data class Update(val downloadOnScreenEntry: Boolean = false)

@Serializable
data object Announcements

@Serializable
data object Announcement : ComplexParameter<ReVancedAnnouncement>

@Serializable
data object SelectedAppInfo : ComplexParameter<SelectedAppInfo.ViewModelParams> {
    @Parcelize
    data class ViewModelParams(
        val packageName: String,
        val localPath: String? = null,
        val patches: PatchSelection? = null
    ) : Parcelable

    @Serializable
    object Main

    @Serializable
    data object PatchesSelector : ComplexParameter<PatchesSelector.ViewModelParams> {
        @Parcelize
        data class ViewModelParams(
            val packageName: String,
            val version: String?,
            val patchSelection: PatchSelection?,
            val options: @RawValue Options,
        ) : Parcelable
    }

    @Serializable
    data object VersionSelector : ComplexParameter<VersionSelector.ViewModelParams> {
        @Parcelize
        data class ViewModelParams(
            val packageName: String,
            val patchSelection: PatchSelection,
            val selectedVersion: SelectedVersion,
            val localPath: String? = null,
        ) : Parcelable
    }

    @Serializable
    data object SourceSelector : ComplexParameter<SourceSelector.ViewModelParams> {
        @Parcelize
        data class ViewModelParams(
            val packageName: String,
            val version: String?,
            val selectedSource: SelectedSource,
            val localPath: String? = null,
        ) : Parcelable
    }

    @Serializable
    data object RequiredOptions : ComplexParameter<PatchesSelector.ViewModelParams>
}

@Serializable
data object Patcher : ComplexParameter<Patcher.ViewModelParams> {
    @Parcelize
    data class ViewModelParams(
        val packageName: String,
        val version: String?,
        val selectedSource: SelectedSource,
        val selectedPatches: PatchSelection,
        val options: @RawValue Options
    ) : Parcelable
}

@Serializable
object Settings {
    sealed interface Destination

    @Serializable
    data object Main : Destination

    @Serializable
    data object General : Destination

    @Serializable
    data object Advanced : Destination

    @Serializable
    data object Updates : Destination

    @Serializable
    data object Downloads : Destination

    @Serializable
    data class DownloadersInfo(val packageName: String) : Destination

    @Serializable
    data object ImportExport : Destination

    @Serializable
    data object About : Destination

    @Serializable
    data object Changelogs : Destination

    @Serializable
    data object Contributors : Destination

    @Serializable
    data object Licenses : Destination

    @Serializable
    data object Developer : Destination
}
