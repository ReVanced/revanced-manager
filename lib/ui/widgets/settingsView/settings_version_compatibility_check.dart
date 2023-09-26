import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_viewmodel.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/utils/check_for_supported_patch.dart';

class SVersionCompatibilityCheck extends StatefulWidget {
  const SVersionCompatibilityCheck({super.key});

  @override
  State<SVersionCompatibilityCheck> createState() => _SVersionCompatibilityCheckState();
}

final _settingsViewModel = SettingsViewModel();
final _patchesSelectorViewModel = PatchesSelectorViewModel();
final _patcherViewModel = PatcherViewModel();

class _SVersionCompatibilityCheckState extends State<SVersionCompatibilityCheck> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.versionCompatibilityCheckLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.versionCompatibilityCheckHint'),
      value: _settingsViewModel.isVersionCompatibilityCheckEnabled(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.useVersionCompatibilityCheck(value);
        });
        if (!value) {
          _patcherViewModel.selectedPatches
              .removeWhere((patch) => !isPatchSupported(patch));
          _patchesSelectorViewModel.selectedPatches
              .removeWhere((patch) => !isPatchSupported(patch));
        }
      },
    );
  }
}
