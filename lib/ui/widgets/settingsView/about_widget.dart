import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/utils/about_info.dart';
import 'package:flutter/services.dart';

class AboutWidget extends StatefulWidget {
  const AboutWidget({Key? key, this.padding}) : super(key: key);

  final EdgeInsetsGeometry? padding;

  @override
  State<AboutWidget> createState() => _AboutWidgetState();
}

class _AboutWidgetState extends State<AboutWidget> {
  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Map<String, dynamic>>(
      future: AboutInfo.getInfo(),
      builder: (context, snapshot) {
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 8.0),
          child: ListTile(
            contentPadding: widget.padding ?? EdgeInsets.zero,
            onLongPress: snapshot.hasData
                ? () {
                    Clipboard.setData(
                      ClipboardData(
                        text: 'Version: ${snapshot.data!['version']}\n'
                            'Model: ${snapshot.data!['model']}\n'
                            'Android Version: ${snapshot.data!['androidVersion']}\n'
                            'Arch: ${snapshot.data!['arch']}\n',
                      ),
                    );
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: I18nText('settingsView.snackbarMessage'),
                        backgroundColor:
                            Theme.of(context).colorScheme.secondary,
                      ),
                    );
                  }
                : null,
            title: I18nText(
              'settingsView.aboutLabel',
              child: const Text(
                '',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            subtitle: snapshot.hasData
                ? Column(
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
                        'Build: ${snapshot.data!['flavor']}',
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
                  )
                : const SizedBox(),
          ),
        );
      },
    );
  }
}
