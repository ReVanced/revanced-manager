package app.revanced.manager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PatchEntity(
    @PrimaryKey val name: String? = null,
    val version: String?,
    val enabled: Boolean?
)
// i want to ship metadata here!!!!!!!!!!!!!!!!!!!!!!!!!!