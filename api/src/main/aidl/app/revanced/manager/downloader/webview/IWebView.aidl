// IWebView.aidl
package app.revanced.manager.downloader.webview;

@JavaPassthrough(annotation="@app.revanced.manager.downloader.DownloaderHostApi")
oneway interface IWebView {
    void load(String url);
    void finish();
}