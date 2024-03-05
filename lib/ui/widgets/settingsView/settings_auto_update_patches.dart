import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_switch_list_tile.dart';

class SAutoUpdatePatches extends StatefulWidget {
  const SAutoUpdatePatches({super.key});

  @override
  State<SAutoUpdatePatches> createState() => _SAutoUpdatePatchesState();
}

final _settingsViewModel = SettingsViewModel();

class _SAutoUpdatePatchesState extends State<SAutoUpdatePatches> {
  @override
  Widget build(BuildContext context) {
    return HapticSwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: Text(
        t.settingsView.autoUpdatePatchesLabel,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: Text(t.settingsView.autoUpdatePatchesHint),
      value: _settingsViewModel.isPatchesAutoUpdate(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.setPatchesAutoUpdate(value);
        });
      },
    );
  }
}
