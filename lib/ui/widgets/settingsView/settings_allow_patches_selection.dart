import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SAllowPatchesSelection extends StatefulWidget {
  const SAllowPatchesSelection({super.key});

  @override
  State<SAllowPatchesSelection> createState() => _SAllowPatchesSelectionState();
}

final _settingsViewModel = SettingsViewModel();

class _SAllowPatchesSelectionState extends State<SAllowPatchesSelection> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.allowPatchesSelectionLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.allowPatchesSelectionHint'),
      value: _settingsViewModel.isPatchesChangeAllowed(),
      onChanged: (value) async {
        await _settingsViewModel.showPatchesChangeAllowDialog(value, context);
        setState(() {});
      },
    );
  }
}
