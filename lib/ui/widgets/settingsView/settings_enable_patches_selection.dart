import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SEnablePatchesSelection extends StatefulWidget {
  const SEnablePatchesSelection({super.key});

  @override
  State<SEnablePatchesSelection> createState() => _SEnablePatchesSelectionState();
}

final _settingsViewModel = SettingsViewModel();

class _SEnablePatchesSelectionState extends State<SEnablePatchesSelection> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.enablePatchesSelectionLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.enablePatchesSelectionHint'),
      value: _settingsViewModel.isPatchesChangeEnabled(),
      onChanged: (value) async {
        await _settingsViewModel.showPatchesChangeEnableDialog(value, context);
        setState(() {});
      },
    );
  }
}
