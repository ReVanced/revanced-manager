import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_switch_list_tile.dart';

class SEnablePatchesSelection extends StatefulWidget {
  const SEnablePatchesSelection({super.key});

  @override
  State<SEnablePatchesSelection> createState() =>
      _SEnablePatchesSelectionState();
}

final _settingsViewModel = SettingsViewModel();

class _SEnablePatchesSelectionState extends State<SEnablePatchesSelection> {
  @override
  Widget build(BuildContext context) {
    return HapticSwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: Text(
        t.settingsView.enablePatchesSelectionLabel,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: Text(t.settingsView.enablePatchesSelectionHint),
      value: _settingsViewModel.isPatchesChangeEnabled(),
      onChanged: (value) async {
        await _settingsViewModel.showPatchesChangeEnableDialog(value, context);
        setState(() {});
      },
    );
  }
}
