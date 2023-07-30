package app.revanced.manager.network.downloader

import android.os.Build.SUPPORTED_ABIS
import app.revanced.manager.network.service.HttpService
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import it.skrape.selects.html5.a
import it.skrape.selects.html5.div
import it.skrape.selects.html5.form
import it.skrape.selects.html5.h5
import it.skrape.selects.html5.input
import it.skrape.selects.html5.p
import it.skrape.selects.html5.span
import kotlinx.coroutines.flow.flow
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.io.File

class APKMirror : AppDownloader, KoinComponent {
    private val httpClient: HttpService = get()

    enum class APKType {
        APK,
        BUNDLE
    }

    data class Variant(
        val apkType: APKType,
        val arch: String,
        val link: String
    )

    private suspend fun getAppLink(packageName: String): String {
        val searchResults = httpClient.getHtml { url("$apkMirror/?post_type=app_release&searchtype=app&s=$packageName") }
            .div {
                withId = "content"
                findFirst {
                    div {
                        withClass = "listWidget"
                        findAll {

                            find {
                                it.children.first().text.contains(packageName)
                            }!!.children.mapNotNull {
                                if (it.classNames.isEmpty()) {
                                    it.h5 {
                                        withClass = "appRowTitle"
                                        findFirst {
                                            a {
                                                findFirst {
                                                    attribute("href")
                                                }
                                            }
                                        }
                                    }
                                } else null
                            }

                        }
                    }
                }
            }

        return searchResults.find { url ->
            httpClient.getHtml { url(apkMirror + url) }
                .div {
                    withId = "primary"
                    findFirst {
                        div {
                            withClass = "tab-buttons"
                            findFirst {
                                div {
                                    withClass = "tab-button-positioning"
                                    findFirst {
                                        children.any {
                                            it.attribute("href") == "https://play.google.com/store/apps/details?id=$packageName"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        } ?: throw Exception("App isn't available for download")
    }

    override fun getAvailableVersions(packageName: String, versionFilter: Set<String>) = flow<AppDownloader.App> {

        // Vanced music uses the same package name so we have to hardcode...
        val appCategory = if (packageName == "com.google.android.apps.youtube.music")
            "youtube-music"
        else
            getAppLink(packageName).split("/")[3]

        var page = 1

        val versions = mutableListOf<String>()

        while (
            if (versionFilter.isNotEmpty())
                versions.size < versionFilter.size && page <= 7
            else
                page <= 1
        ) {
            httpClient.getHtml {
                url("$apkMirror/uploads/page/$page/")
                parameter("appcategory", appCategory)
            }.div {
                withClass = "widget_appmanager_recentpostswidget"
                findFirst {
                    div {
                        withClass = "listWidget"
                        findFirst {
                            children.mapNotNull { element ->
                                if (element.className.isEmpty()) {

                                    APKMirrorApp(
                                        packageName = packageName,
                                        version = element.div {
                                            withClass = "infoSlide"
                                            findFirst {
                                                p {
                                                    findFirst {
                                                        span {
                                                            withClass = "infoSlide-value"
                                                            findFirst {
                                                                text
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }.also {
                                            if (it in versionFilter)
                                                versions.add(it)
                                        },
                                        downloadLink = element.findFirst {
                                            a {
                                                withClass = "downloadLink"
                                                findFirst {
                                                    attribute("href")
                                                }
                                            }
                                        }
                                    )

                                } else null
                            }
                        }
                    }
                }
            }.onEach { version -> emit(version) }

            page++
        }
    }

    @Parcelize
    private class APKMirrorApp(
        override val packageName: String,
        override val version: String,
        private val downloadLink: String,
    ) : AppDownloader.App, KoinComponent {
        @IgnoredOnParcel private val httpClient: HttpService by inject()

        override suspend fun download(
            saveDirectory: File,
            preferSplit: Boolean,
            onDownload: suspend (downloadProgress: Pair<Float, Float>?) -> Unit
        ): File {
            val variants = httpClient.getHtml { url(apkMirror + downloadLink) }
                .div {
                    withClass = "variants-table"
                    findFirst { // list of variants
                        children.drop(1).map {
                            Variant(
                                apkType = it.div {
                                    findFirst {
                                        span {
                                            findFirst {
                                                enumValueOf(text)
                                            }
                                        }
                                    }
                                },
                                arch = it.div {
                                    findSecond {
                                        text
                                    }
                                },
                                link = it.div {
                                    findFirst {
                                        a {
                                            findFirst {
                                                attribute("href")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

            val orderedAPKTypes = mutableListOf(APKType.APK, APKType.BUNDLE)
                .also { if (preferSplit) it.reverse() }

            val variant = orderedAPKTypes.firstNotNullOfOrNull { apkType ->
                supportedArches.firstNotNullOfOrNull { arch ->
                    variants.find { it.arch == arch && it.apkType == apkType }
                }
            } ?: throw Exception("No compatible variant found")

            if (variant.apkType == APKType.BUNDLE) throw Exception("Split apks are not supported yet") // TODO

            val downloadPage = httpClient.getHtml { url(apkMirror + variant.link) }
                .a {
                    withClass = "downloadButton"
                    findFirst {
                        attribute("href")
                    }
                }

            val downloadLink = httpClient.getHtml { url(apkMirror + downloadPage) }
                .form {
                    withId = "filedownload"
                    findFirst {
                        val apkLink = attribute("action")
                        val id = input {
                            withAttribute = "name" to "id"
                            findFirst {
                                attribute("value")
                            }
                        }
                        val key = input {
                            withAttribute = "name" to "key"
                            findFirst {
                                attribute("value")
                            }
                        }
                        "$apkLink?id=$id&key=$key"
                    }
                }

            val saveLocation = if (variant.apkType == APKType.BUNDLE)
                saveDirectory.resolve(version).also { it.mkdirs() }
            else
                saveDirectory.resolve("$version.apk")

            try {
                val downloadLocation = if (variant.apkType == APKType.BUNDLE)
                    saveLocation.resolve("temp.zip")
                else
                    saveLocation

                httpClient.download(downloadLocation) {
                    url(apkMirror + downloadLink)
                    onDownload { bytesSentTotal, contentLength ->
                        onDownload(bytesSentTotal.div(100000).toFloat().div(10) to contentLength.div(100000).toFloat().div(10))
                    }
                }

                if (variant.apkType == APKType.BUNDLE) {
                    // TODO: Extract temp.zip

                    downloadLocation.delete()
                }
            } catch (e: Exception) {
                saveLocation.deleteRecursively()
                throw e
            } finally {
                onDownload(null)
            }

            return saveLocation
        }
    }

    companion object {
        const val apkMirror = "https://www.apkmirror.com"

        val supportedArches = listOf("universal", "noarch") + SUPPORTED_ABIS
    }

}