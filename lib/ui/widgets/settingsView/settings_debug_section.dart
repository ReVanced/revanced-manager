import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/about_widget.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';

final _settingsViewModel = SettingsViewModel();

class SDebugSection extends StatelessWidget {
  const SDebugSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: 'settingsView.debugSectionTitle',
      children: <Widget>[
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.logsLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.logsHint'),
          onTap: () => _settingsViewModel.exportLogcatLogs(),
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
        const AboutWidget(
          padding: EdgeInsets.symmetric(horizontal: 20.0),
        ),
      ],
    );
  }
}
