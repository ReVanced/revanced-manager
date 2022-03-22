package app.revanced.manager.installer.util

import android.content.Context

fun getVancedYoutubePath(
    version: String,
    variant: String,
    context: Context
) = context.getExternalFilesDirPath("vanced_youtube") + "/$version/$variant"

fun getVancedYoutubeMusicPath(
    version: String,
    variant: String,
    context: Context
) = context.getExternalFilesDirPath("vanced_music") + "/$version/$variant"

fun getMicrogPath(
    context: Context
) = context.getExternalFilesDirPath("microg")

fun getStockYoutubePath(
    version: String,
    context: Context
) = context.getExternalFilesDirPath("stock_youtube") + "/$version"

fun getStockYoutubeMusicPath(
    version: String,
    context: Context
) = context.getExternalFilesDirPath("stock_youtube_music") + "/$version"

private fun Context.getExternalFilesDirPath(
    type: String
): String {
    val filesDir = getExternalFilesDir(type)!!
    if (!filesDir.exists())
        filesDir.mkdirs()

    return filesDir.path
}
