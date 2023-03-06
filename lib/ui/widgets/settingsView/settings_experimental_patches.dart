import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SExperimentalPatches extends StatefulWidget {
  const SExperimentalPatches({super.key});

  @override
  State<SExperimentalPatches> createState() => _SExperimentalPatchesState();
}

final _settingsViewModel = SettingsViewModel();

class _SExperimentalPatchesState extends State<SExperimentalPatches> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.experimentalPatchesLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.experimentalPatchesHint'),
      value: _settingsViewModel.areExperimentalPatchesEnabled(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.useExperimentalPatches(value);
        });
      },
    );
  }
}
