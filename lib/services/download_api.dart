import 'dart:io';

abstract class AppDownloader {
  Stream<AppDownloaderApp> getAvailableVersions(String packageName, Set<String> versionFilter);
}

abstract class AppDownloaderApp {
  String get packageName;
  String get version;

  Future<File> download(
    String saveDirectory,
    bool preferSplit
  );
}