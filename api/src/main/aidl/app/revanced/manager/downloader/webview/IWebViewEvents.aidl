// IWebViewEvents.aidl
package app.revanced.manager.downloader.webview;

import app.revanced.manager.downloader.webview.IWebView;

@JavaPassthrough(annotation="@app.revanced.manager.downloader.DownloaderHostApi")
oneway interface IWebViewEvents {
    void ready(IWebView iface);
    void pageLoad(String url);
    void download(String url, String mimetype, String userAgent);
}