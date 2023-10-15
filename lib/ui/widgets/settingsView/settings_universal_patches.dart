import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SUniversalPatches extends StatefulWidget {
  const SUniversalPatches({super.key});

  @override
  State<SUniversalPatches> createState() =>
      _SUniversalPatchesState();
}

final _settingsViewModel = SettingsViewModel();
final _patchesSelectorViewModel = PatchesSelectorViewModel();
final _patcherViewModel = PatcherViewModel();

class _SUniversalPatchesState
    extends State<SUniversalPatches> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.universalPatchesLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.universalPatchesHint'),
      value: _settingsViewModel.areUniversalPatchesEnabled(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.showUniversalPatches(value);
        });
        if (!value) {
          _patcherViewModel.selectedPatches
              .removeWhere((patch) => patch.compatiblePackages.isEmpty);
          _patchesSelectorViewModel.selectedPatches
              .removeWhere((patch) => patch.compatiblePackages.isEmpty);
        }
      },
    );
  }
}
