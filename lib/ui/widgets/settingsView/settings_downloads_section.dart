import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_manage_plugins.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';

class SDownloadsSection extends StatelessWidget {
  const SDownloadsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: t.settingsView.downloadsSectionTitle,
      children: const <Widget>[
        SManagePlugins(),
      ],
    );
  }
}