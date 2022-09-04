import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/utils/about_info.dart';
import 'package:flutter/services.dart';

class AboutWidget extends StatefulWidget {
  const AboutWidget({Key? key}) : super(key: key);

  @override
  State<AboutWidget> createState() => _AboutWidgetState();
}

class _AboutWidgetState extends State<AboutWidget> {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          I18nText(
            'settingsView.aboutLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          const SizedBox(height: 4),
          FutureBuilder<Map<String, dynamic>>(
            future: AboutInfo.getInfo(),
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return GestureDetector(
                  onLongPress: () {
                    Clipboard.setData(
                      ClipboardData(
                        text: 'Version: ${snapshot.data!['version']}\n'
                            'Build: ${snapshot.data!['buildNumber']}\n'
                            'Model: ${snapshot.data!['model']}\n'
                            'Android Version: ${snapshot.data!['androidVersion']}\n'
                            'Arch: ${snapshot.data!['arch']}\n',
                      ),
                    );
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: const Text('Copied to clipboard'),
                        backgroundColor:
                            Theme.of(context).colorScheme.secondary,
                      ),
                    );
                  },
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'Version: ${snapshot.data!['version']}',
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w300,
                        ),
                      ),
                      Text(
                        'Build: ${snapshot.data!['buildNumber']}',
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w300,
                        ),
                      ),
                      Text(
                        'Model: ${snapshot.data!['model']}',
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w300,
                        ),
                      ),
                      Text(
                        'Android Version: ${snapshot.data!['androidVersion']}',
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w300,
                        ),
                      ),
                      Text(
                        'Arch: ${snapshot.data!['arch']}',
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w300,
                        ),
                      ),
                    ],
                  ),
                );
              } else {
                return Container();
              }
            },
          ),
        ],
      ),
    );
  }
}
