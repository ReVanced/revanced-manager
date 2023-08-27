import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SLoadAllApps extends StatefulWidget {
  const SLoadAllApps({super.key});

  @override
  State<SLoadAllApps> createState() =>
      _SLoadAllAppsState();
}

final _settingsViewModel = SettingsViewModel();

class _SLoadAllAppsState
    extends State<SLoadAllApps> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.loadAllAppsLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.loadAllAppsHint'),
      value: _settingsViewModel.getLoadAllApps(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.enableLoadAllApps(value);
        });
      },
    );
  }
}
