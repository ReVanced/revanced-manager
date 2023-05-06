package app.revanced.manager.compose.ui.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Destination: Parcelable {

    @Parcelize
    object Dashboard: Destination

    @Parcelize
    object AppSelector: Destination

}