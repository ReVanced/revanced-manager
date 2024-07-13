@file:Suppress("Unused")
package app.revanced.manager.plugin.downloader.example

import android.content.pm.PackageManager
import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloaderContext
import app.revanced.manager.plugin.downloader.downloader
import app.revanced.manager.plugin.downloader.paginatedDownloader
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

// TODO: document API, change dispatcher.

@Parcelize
class InstalledApp(
    override val packageName: String,
    override val version: String,
    internal val apkPath: String
) : App(packageName, version)

private fun installedAppDownloader(context: DownloaderContext) = downloader<InstalledApp> {
    val pm = context.androidContext.packageManager

    getVersions { packageName, _ ->
        val packageInfo = try {
            pm.getPackageInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return@getVersions emptyList()
        }

        listOf(
            InstalledApp(
                packageName,
                packageInfo.versionName,
                packageInfo.applicationInfo.sourceDir
            )
        )
    }

    download {
        Files.copy(Path(it.apkPath), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

private val Int.megaBytes get() = times(1_000_000)

val examplePaginatedDownloader = paginatedDownloader {
    versionPager { packageName, versionHint ->
        object : PagingSource<Int, App>() {
            override fun getRefreshKey(state: PagingState<Int, App>) = state.anchorPosition?.let {
                state.closestPageToPosition(it)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
            }

            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, App> {
                val page = params.key ?: 0
                if (page == 0 && versionHint != null) return LoadResult.Page(
                    listOf(
                        App(
                            packageName,
                            versionHint
                        )
                    ),
                    prevKey = null,
                    nextKey = 1
                )

                return LoadResult.Page(
                    data = List(params.loadSize) { App(packageName, "fake.$page.$it") },
                    prevKey = page.minus(1).takeIf { it >= 0 },
                    nextKey = page.plus(1).takeIf { it < 5 }
                )
            }
        }
    }

    download {
        for (i in 0..5) {
            reportProgress(i.megaBytes , 5.megaBytes)
            delay(1000L)
        }

        throw Exception("Download simulation complete")
    }
}