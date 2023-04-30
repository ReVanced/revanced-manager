package app.revanced.manager.compose.destination

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Destination: Parcelable {

    @Parcelize
    object Dashboard: Destination

}