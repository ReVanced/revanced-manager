import 'package:html/dom.dart';
import 'package:html/parser.dart' show parse;
import 'package:http/http.dart' as http;

class AppDownloader {
  final String _urlAPKMirror = 'https://www.apkmirror.com';

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
}

void main() {
  final downloader = AppDownloader();
  downloader.getAppLink('com.google.andrfoid.youtube').then((value) => print(value));
}
