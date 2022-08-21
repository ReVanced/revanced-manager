import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/utils/about_info.dart';

class AboutWidget extends StatefulWidget {
  const AboutWidget({Key? key}) : super(key: key);

  @override
  State<AboutWidget> createState() => _AboutWidgetState();
}

class _AboutWidgetState extends State<AboutWidget> {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          I18nText(
            'settingsView.aboutLabel',
            child: Text('', style: kSettingItemTextStyle),
          ),
          const SizedBox(height: 4),
          FutureBuilder<Map<String, dynamic>>(
            future: AboutInfo.getInfo(),
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Version: ${snapshot.data!['version']}',
                        style: kSettingItemSubtitleTextStyle),
                    Text('Build: ${snapshot.data!['buildNumber']}',
                        style: kSettingItemSubtitleTextStyle),
                    Text('Model: ${snapshot.data!['model']}',
                        style: kSettingItemSubtitleTextStyle),
                    Text('Android Version: ${snapshot.data!['androidVersion']}',
                        style: kSettingItemSubtitleTextStyle),
                    Text('Arch: ${snapshot.data!['arch']}',
                        style: kSettingItemSubtitleTextStyle),
                  ],
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
