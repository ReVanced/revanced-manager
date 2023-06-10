import 'dart:convert';
import 'package:http/http.dart' as http;

Future<Map<String, dynamic>> getPackageInfo(String pkgName) async {
  String result = '';
  if(pkgName == 'com.vanced.android.youtube') {
    result = '{"name":"YouTube Vanced","image":"https://downloadr2.apkmirror.com/wp-content/uploads/2019/02/youtube-vanced.jpg"}';
  }else{
    final String url = 'https://play.google.com/store/apps/details?id=$pkgName';
    final Map<String, String> headers = {
      'Host': 'play.google.com',
    };
    final http.Response response = await http.get(Uri.parse(url), headers: headers);
    final String firstSplit = response.body.split('@context')[1];
    final String secondSplit = firstSplit.split(',"contentRating')[0];
    result = '{"@context$secondSplit}';
  }
  final Map<String, dynamic> body = jsonDecode(result);
  return body;
}
