package app.revanced.manager.plugin.downloader.webview

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import app.revanced.manager.plugin.downloader.DownloaderScope
import app.revanced.manager.plugin.downloader.GetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.net.HttpURLConnection
import java.net.URI

internal typealias PageLoadCallback<T> = suspend WebViewCallbackScope<T>.(url: String) -> Unit
internal typealias DownloadCallback<T> = suspend WebViewCallbackScope<T>.(url: String, mimeType: String, userAgent: String) -> Unit
internal typealias ReadyCallback<T> = suspend WebViewCallbackScope<T>.() -> Unit

@Parcelize
data class DownloadUrl(val url: String, val mimeType: String, val userAgent: String) : Parcelable {
    fun toResult() = with(URI.create(url).toURL().openConnection() as HttpURLConnection) {
        useCaches = false
        allowUserInteraction = false
        setRequestProperty("User-Agent", userAgent)
        connectTimeout = 10_000
        connect()
        inputStream to getHeaderField("Content-Length").toLong()
    }
}

interface WebViewCallbackScope<T : Parcelable> {
    suspend fun finish(result: GetResult<T>?)
    suspend fun load(url: String)
}

class WebViewScope<T : Parcelable> internal constructor(
    coroutineScope: CoroutineScope,
    setResult: (GetResult<T>?) -> Unit
) {
    private var onPageLoadCallback: PageLoadCallback<T> = {}
    private var onDownloadCallback: DownloadCallback<T> = { _, _, _ -> }
    private var onReadyCallback: ReadyCallback<T> =
        { throw Exception("Ready callback not set") }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private var current: IWebView? = null
    private val webView: IWebView
        inline get() = current ?: throw Exception("WebView interface unavailable")

    internal val binder = object : IWebViewEvents.Stub() {
        override fun ready(iface: IWebView?) {
            coroutineScope.launch(dispatcher) {
                val wasNull = current == null
                current = iface
                if (wasNull) onReadyCallback(callbackScope)
            }
        }

        override fun pageLoad(url: String?) {
            coroutineScope.launch(dispatcher) { onPageLoadCallback(callbackScope, url!!) }
        }

        override fun download(url: String?, mimetype: String?, userAgent: String?) {
            coroutineScope.launch(dispatcher) {
                onDownloadCallback(
                    callbackScope,
                    url!!,
                    mimetype!!,
                    userAgent!!
                )
            }
        }
    }

    private val callbackScope = object : WebViewCallbackScope<T> {
        override suspend fun finish(result: GetResult<T>?) {
            setResult(result)
            // Tell the WebViewActivity to finish
            webView.let { withContext(Dispatchers.IO) { it.finish() } }
        }

        override suspend fun load(url: String) {
            webView.let { withContext(Dispatchers.IO) { it.load(url) } }
        }

    }

    fun onDownload(block: DownloadCallback<T>) {
        onDownloadCallback = block
    }

    fun onPageLoad(block: PageLoadCallback<T>) {
        onPageLoadCallback = block
    }

    fun onReady(block: ReadyCallback<T>) {
        onReadyCallback = block
    }
}

fun <T : Parcelable> DownloaderScope<T>.webView(block: WebViewScope<T>.(packageName: String, version: String?) -> Unit) =
    get { pkgName, version ->
        var result: GetResult<T>? = null

        coroutineScope {
            val scope = WebViewScope(this) { result = it }
            scope.block(pkgName, version)
            requestStartActivity(Intent().apply {
                putExtras(Bundle().apply {
                    putBinder(WebViewActivity.BINDER_KEY, scope.binder)
                    val pm = context.packageManager
                    val label = pm.getPackageInfo(pluginPackageName, 0).applicationInfo.loadLabel(pm).toString()
                    putString(WebViewActivity.TITLE_KEY, label)
                })
                setClassName(
                    hostPackageName,
                    WebViewActivity::class.qualifiedName!!
                )
            })
        }
        result
    }