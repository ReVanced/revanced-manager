import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_switch_list_tile.dart';

class SShowUpdateDialog extends StatefulWidget {
  const SShowUpdateDialog({super.key});

  @override
  State<SShowUpdateDialog> createState() => _SShowUpdateDialogState();
}

final _settingsViewModel = SettingsViewModel();

class _SShowUpdateDialogState extends State<SShowUpdateDialog> {
  @override
  Widget build(BuildContext context) {
    return HapticSwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: Text(
        t.settingsView.showUpdateDialogLabel,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: Text(t.settingsView.showUpdateDialogHint),
      value: _settingsViewModel.showUpdateDialog(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.setShowUpdateDialog(value);
        });
      },
    );
  }
}
