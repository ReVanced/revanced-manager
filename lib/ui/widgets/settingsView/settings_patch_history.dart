import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SPatchHistory extends StatefulWidget {
  const SPatchHistory({super.key});

  @override
  State<SPatchHistory> createState() =>
      _SPatchHistoryState();
}

final _settingsViewModel = SettingsViewModel();

class _SPatchHistoryState
    extends State<SPatchHistory> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: Text(
        t.settingsView.patchHistoryLabel,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: Text(t.settingsView.patchHistoryHint),
      value: _settingsViewModel.isPatchHistoryEnabled(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.usePatchHistory(value);
        });
      },
    );
  }
}
