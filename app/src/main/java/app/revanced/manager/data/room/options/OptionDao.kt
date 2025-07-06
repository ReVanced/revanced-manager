package app.revanced.manager.data.room.options

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class OptionDao {
    @Transaction
    @Query(
        "SELECT patch_bundle, `group`, patch_name, `key`, value FROM option_groups" +
                " LEFT JOIN options ON uid = options.`group`" +
                " WHERE package_name = :packageName"
    )
    abstract suspend fun getOptions(packageName: String): Map<@MapColumn("patch_bundle") Int, List<Option>>

    @Query("SELECT uid FROM option_groups WHERE patch_bundle = :bundleUid AND package_name = :packageName")
    abstract suspend fun getGroupId(bundleUid: Int, packageName: String): Int?

    @Query("SELECT package_name FROM option_groups")
    abstract fun getPackagesWithOptions(): Flow<List<String>>

    @Insert
    abstract suspend fun createOptionGroup(group: OptionGroup)

    @Query("DELETE FROM option_groups WHERE patch_bundle = :uid")
    abstract suspend fun resetOptionsForPatchBundle(uid: Int)

    @Query("DELETE FROM option_groups WHERE package_name = :packageName")
    abstract suspend fun resetOptionsForPackage(packageName: String)

    @Query("DELETE FROM option_groups")
    abstract suspend fun reset()

    @Insert
    protected abstract suspend fun insertOptions(patches: List<Option>)

    @Query("DELETE FROM options WHERE `group` = :groupId")
    protected abstract suspend fun clearGroup(groupId: Int)

    @Transaction
    open suspend fun updateOptions(options: Map<Int, List<Option>>) =
        options.forEach { (groupId, options) ->
            clearGroup(groupId)
            insertOptions(options)
        }
}