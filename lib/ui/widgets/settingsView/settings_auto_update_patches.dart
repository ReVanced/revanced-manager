import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/services/haptics.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SAutoUpdatePatches extends StatefulWidget {
  const SAutoUpdatePatches({super.key});

  @override
  State<SAutoUpdatePatches> createState() => _SAutoUpdatePatchesState();
}

final _settingsViewModel = SettingsViewModel();

class _SAutoUpdatePatchesState extends State<SAutoUpdatePatches> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.autoUpdatePatchesLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.autoUpdatePatchesHint'),
      value: _settingsViewModel.isPatchesAutoUpdate(),
      onChanged: (value) {
        hapticSwitch(value);
        setState(() {
          _settingsViewModel.setPatchesAutoUpdate(value);
        });
      },
    );
  }
}
