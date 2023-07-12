import 'dart:convert';
import 'package:http/http.dart' as http;

Future<Map<String, dynamic>> getAppInfo(
    String pkgName, List<String> sources,) async {
  String result = '{}';
  if (sources.contains('Google Play Store')) {
    final String url = 'https://play.google.com/store/apps/details?id=$pkgName';
    final Map<String, String> headers = {
      'Host': 'play.google.com',
    };
    final http.Response response = await http.get(
      Uri.parse(url),
      headers: headers,
    );
    if (response.statusCode == 200) {
      final String firstSplit = response.body.split('@context')[1];
      final String secondSplit = firstSplit.split(',"contentRating')[0];
      result = '{"@context$secondSplit, "pkgName":"$pkgName"}';
    }
  }
  if (sources.contains('APKPure') && result == '{}') {
    final String url =
        'https://tapi.pureapk.com/v3/search_query_new?hl=en-US&key=$pkgName';
    final Map<String, String> headers = {
      'Host': 'tapi.pureapk.com',
      'Ual-Access-Businessid': 'projecta',
    };
    final http.Response response = await http.get(
      Uri.parse(url),
      headers: headers,
    );
    if (response.statusCode == 200) {
      final data =
          jsonDecode(response.body)['data']['data'][0]['data'][0]['app_info'];
      if (data['package_name'] == pkgName) {
        final String iconUrl =
            '${data['icon']['thumbnail']['url']}'.replaceAll('\u0026', '&');
        result =
            '{"image":"$iconUrl", "name": "${data['title']}", "pkgName":"$pkgName"}';
      }
    }
  }
  if (sources.contains('APKMirror') && result == '{}') {
    String url = 'https://www.apkmirror.com/wp-json/apkm/v1/app_exists';
    final Map<String, String> headers = {
      'Host': 'www.apkmirror.com',
      'Content-Type': 'application/json',
      'Authorization':
          'Basic YXBpLXRvb2xib3gtZm9yLWdvb2dsZS1wbGF5OkNiVVcgQVVMZyBNRVJXIHU4M3IgS0s0SCBEbmJL'
    };
    final http.Response response = await http.post(
      Uri.parse(url),
      headers: headers,
      body: jsonEncode(
        {
          'pnames': [pkgName]
        },
      ),
    );
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      if (data['data'][0]['exists']) {
        url = 'https://www.apkmirror.com${data['data'][0]['release']['link']}';
        final http.Response request = await http.get(
          Uri.parse(url),
          headers: headers,
        );
        final String iconUrl = request.body.split('thumbnailUrl": "')[1].split(
              '",',
            )[0];
        result =
            '{"image":"$iconUrl", "name":"${data['data'][0]['app']['name']}", "pkgName":"$pkgName"}';
      }
    }
  }
  final Map<String, dynamic> body = jsonDecode(result);
  return body;
}
