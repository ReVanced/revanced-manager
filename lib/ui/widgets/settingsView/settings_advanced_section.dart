// ignore_for_file: prefer_const_constructors

import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragement/settings_manage_api_url.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragement/settings_manage_sources.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_experimental_patches.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_experimental_universal_patches.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';

final _settingsViewModel = SettingsViewModel();

class SAdvancedSection extends StatelessWidget {
  const SAdvancedSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: 'settingsView.advancedSectionTitle',
      children: <Widget>[
        SManageApiUrlUI(),
        SManageSourcesUI(),
        SExperimentalUniversalPatches(),
        SExperimentalPatches(),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.deleteKeystoreLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.deleteKeystoreHint'),
          onTap: () => _settingsViewModel.deleteKeystore,
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.deleteTempDirLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.deleteTempDirHint'),
          onTap: () => _settingsViewModel.deleteTempDir(),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.deleteLogsLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.deleteLogsHint'),
          onTap: () => _settingsViewModel.deleteLogs(),
        ),
      ],
    );
  }
}
