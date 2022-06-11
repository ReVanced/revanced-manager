package app.revanced.manager.data

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PatchEntity(
    @NonNull @PrimaryKey val name: String = "patch",
    val version: String?,
    val enabled: Boolean?
)
// i want to ship metadata here!!!!!!!!!!!!!!!!!!!!!!!!!!