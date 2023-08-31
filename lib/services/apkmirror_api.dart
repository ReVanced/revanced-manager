import 'dart:async';
import 'dart:io';
import 'package:collection/collection.dart';
import 'package:flutter/foundation.dart';
import 'package:html/dom.dart';
import 'package:html/parser.dart' show parse;
import 'package:http/http.dart' as http;
import 'package:revanced_manager/services/download_api.dart';

const String _urlAPKMirror = 'https://www.apkmirror.com';
final  List<String> _supportedArches = ['universal', 'noarch'].toList();

enum APKType {
  APK,
  BUNDLE
}

class Variant {
  Variant({
    required this.apkType,
    required this.arch,
    required this.link,
  });

  APKType apkType;
  String arch;
  String link;
}

class APKMirror extends AppDownloader {
  Future<String> getAppLink(String packageName) async {
    final downloadedHTML = await http.get(Uri.parse('$_urlAPKMirror/?post_type=app_release&searchtype=app&s=$packageName'));
    final Document document = parse(downloadedHTML.body);
    final Element listWidget = document.querySelectorAll('#content div.listWidget').firstWhere((element) => element.children.first.text.contains(packageName));

    final List<String?> searchResults = listWidget.children
        .map((element) {
          if (element.className.isEmpty) {
            return element.querySelector('h5.appRowTitle a')?.attributes['href'];
          } else {
            return null;
          }
        })
        .where((element) => element != null)
        .toList();

    if (searchResults.isEmpty) {
      throw Exception("App isn't available for download");
    }

    for (final String result in searchResults.cast<String>()) {
      final downloadedHTML = await http.get(Uri.parse('$_urlAPKMirror$result'));
      final Document document = parse(downloadedHTML.body);
      final List<Element> children = document.querySelector('#primary div.tab-buttons div.tab-button-positioning')!.children;
      if (children.any((element) => element.attributes['href'] == 'https://play.google.com/store/apps/details?id=$packageName')) {
        return result;
      }
    }

    throw Exception("App isn't available for download");
  }

  @override
  Stream<APKMirrorApp> getAvailableVersions(String packageName, Set<String> versionFilter) async* {
    // Vanced music uses the same package name so we have to hardcode...
    final String appCategory = (packageName == 'com.google.android.apps.youtube.music') ?
      'youtube-music' :
      (await getAppLink(packageName)).split('/')[3];

    int page = 1;
    final List<String> versions = <String>[];

    while (
      (versionFilter.isNotEmpty) ?
        versions.length < versionFilter.length && page <= 7 :
        page <= 1
    ) {
      final downloadedHTML = await http.get(Uri.parse('$_urlAPKMirror/uploads/page/$page/?appcategory=$appCategory'));
      final Document document = parse(downloadedHTML.body);
      final Element listWidget = document.querySelector('.widget_appmanager_recentpostswidget .listWidget')!;
      final List<APKMirrorApp> apkMirrorAppList = listWidget.children
        .map((element) {
          if (element.className.isEmpty) {
            final String version = element.querySelector('div.infoSlide p span.infoSlide-value')!.text.trim();
            if (versionFilter.contains(version)) {
              versions.add(version);
            } else {
              return null;
            }
            return APKMirrorApp(
              packageName: packageName,
              version: version,
              downloadLink: element.querySelector('a.downloadLink')!.attributes['href']!,
            );
          } else {
            return null;
          }
        })
        .where((element) => element != null)
        .toList().cast<APKMirrorApp>();

        for (final apkMirrorApp in apkMirrorAppList) {
          yield apkMirrorApp;
        }

      page++;
    }
  }
}

class APKMirrorApp extends AppDownloaderApp {
  APKMirrorApp({
    required this.packageName,
    required this.version,
    required this.downloadLink,
  });

  @override
  String packageName;
  @override
  String version;
  String downloadLink;

  @override
  Future<File> download(
    String saveDirectory,
    bool preferSplit
  ) async {
    final downloadedHTML = await http.get(Uri.parse(_urlAPKMirror + this.downloadLink));
    final Document document = parse(downloadedHTML.body);
    final Element variantsTable = document.querySelector('.variants-table')!;
    final variants = variantsTable.children.skip(1).map((element) {
      return Variant(
        apkType: (element.querySelector('div span')?.text == 'BUNDLE') ? APKType.BUNDLE : APKType.APK,
        arch: element.querySelectorAll('div')[1].text,
        link: element.querySelector('div a')?.attributes['href'] ?? '',
      );
    }).toList();

    final orderedAPKTypes = preferSplit ? [APKType.BUNDLE, APKType.APK].toList() : [APKType.APK, APKType.BUNDLE].toList();
    
    final Variant? variantMightBeNull = orderedAPKTypes.map((apkType) {
        for (final arch in _supportedArches) {
          return variants.firstWhereOrNull((element) => element.apkType == apkType && element.arch == arch);
        }
        return null;
    }).firstWhere((element) => element != null);

    if (variantMightBeNull == null) {
      throw Exception('No variant found');
    }

    final Variant variant = variantMightBeNull;

    if (variant.apkType == APKType.BUNDLE) {
      throw Exception('Split apks are not supported yet');
    }

    final downloadedHTML2 = await http.get(Uri.parse(_urlAPKMirror + variant.link));
    final Document document2 = parse(downloadedHTML2.body);
    final String downloadPage = document2.querySelector('.downloadButton')!.attributes['href']!;

    final downloadedHTML3 = await http.get(Uri.parse(_urlAPKMirror + downloadPage));
    final Document document3 = parse(downloadedHTML3.body);
    final Element downloadForm = document3.querySelector('#filedownload')!;

    final String apkLink = downloadForm.attributes['action']!;
    final String id = downloadForm.querySelector('input[name="id"]')!.attributes['value']!;
    final String key = downloadForm.querySelector('input[name="key"]')!.attributes['value']!;
    final String downloadLink = '$apkLink?id=$id&key=$key';

    final saveLocation = (variant.apkType == APKType.BUNDLE) ?
      File('$saveDirectory/$version') : 
      File('$saveDirectory/$version.apk');
    
    if (variant.apkType == APKType.BUNDLE) {
      saveLocation.createSync();
    }

    try {
      final downloadLocation = (variant.apkType == APKType.BUNDLE) ?
        File('${saveLocation.path}/temp.zip') :
        File(saveLocation.path);

        print('downloading $_urlAPKMirror$downloadLink');
        final response = await http.get(Uri.parse(_urlAPKMirror + downloadLink));
        downloadLocation.writeAsBytesSync(response.bodyBytes);

        if (variant.apkType == APKType.BUNDLE) {
          // TODO: Extract temp.zip

          downloadLocation.deleteSync();
        }
    } on Exception catch (e) {
      saveLocation.deleteSync(recursive: true);
      if (kDebugMode) {
        print(e);
      }
    }

    return saveLocation;
  }
}