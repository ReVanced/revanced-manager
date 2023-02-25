import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch_tile.dart';

class SExperimentalUniversalPatches extends StatefulWidget {
  const SExperimentalUniversalPatches({super.key});

  @override
  State<SExperimentalUniversalPatches> createState() =>
      _SExperimentalUniversalPatchesState();
}

final _settingsViewModel = SettingsViewModel();

class _SExperimentalUniversalPatchesState
    extends State<SExperimentalUniversalPatches> {
  @override
  Widget build(BuildContext context) {
    return CustomSwitchTile(
      padding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.experimentalUniversalPatchesLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.experimentalUniversalPatchesHint'),
      value: _settingsViewModel.areUniversalPatchesEnabled(),
      onTap: (value) {
        setState(() {
          _settingsViewModel.showUniversalPatches(value);
        });
      },
    );
  }
}
