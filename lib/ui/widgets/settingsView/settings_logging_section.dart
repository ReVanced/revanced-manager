import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch_tile.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';

final _settingsViewModel = SettingsViewModel();

class SLoggingSection extends StatelessWidget {
  const SLoggingSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: 'settingsView.logsSectionTitle',
      children: <Widget>[
        CustomSwitchTile(
          padding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.sentryLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.sentryHint'),
          value: _settingsViewModel.isSentryEnabled(),
          onTap: (value) => _settingsViewModel.useSentry(value),
        ),
      ],
    );
  }
}
