import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/utils/about_info.dart';

class AboutWidget extends StatefulWidget {
  const AboutWidget({super.key, this.padding});

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
                            '${snapshot.data!['supportedArch'].length > 1 ? 'Supported Archs' : 'Supported Arch'}: ${snapshot.data!['supportedArch'].join(", ")}\n',
                      ),
                    );
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(t.settingsView.snackbarMessage),
                        backgroundColor:
                            Theme.of(context).colorScheme.secondary,
                      ),
                    );
                  }
                : null,
            title: Text(
              t.settingsView.aboutLabel,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
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
                        snapshot.data!['supportedArch'].length > 1
                            ? 'Supported Archs: ${snapshot.data!['supportedArch'].join(", ")}'
                            : 'Supported Arch: ${snapshot.data!['supportedArch']}',
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
