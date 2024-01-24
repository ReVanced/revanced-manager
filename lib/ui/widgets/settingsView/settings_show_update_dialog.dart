import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
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
      title: I18nText(
        'settingsView.showUpdateDialogLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.showUpdateDialogHint'),
      value: _settingsViewModel.showUpdateDialog(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.setShowUpdateDialog(value);
        });
      },
    );
  }
}
