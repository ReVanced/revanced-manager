import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SLastPatchedApp extends StatefulWidget {
  const SLastPatchedApp({super.key});

  @override
  State<SLastPatchedApp> createState() =>
      _SLastPatchedAppState();
}

final _settingsViewModel = SettingsViewModel();

class _SLastPatchedAppState
    extends State<SLastPatchedApp> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: Text(
        t.settingsView.lastPatchedAppLabel,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: Text(t.settingsView.lastPatchedAppHint),
      value: _settingsViewModel.isLastPatchedAppEnabled(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.useLastPatchedApp(value);
        });
      },
    );
  }
}
