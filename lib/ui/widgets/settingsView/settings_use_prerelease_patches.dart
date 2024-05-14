import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_switch_list_tile.dart';

class SUsePrereleasePatches extends StatefulWidget {
  const SUsePrereleasePatches({super.key});

  @override
  State<SUsePrereleasePatches> createState() => _SUsePrereleasePatchesState();
}

final _settingsViewModel = SettingsViewModel();

class _SUsePrereleasePatchesState extends State<SUsePrereleasePatches> {
  @override
  Widget build(BuildContext context) {
    return HapticSwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: Text(
        t.settingsView.usePrereleasePatchesLabel,
        style: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.w500,
        ),
      ),
      subtitle: Text(t.settingsView.usePrereleasePatchesHint),
      value: _settingsViewModel.isPreReleasePatchesEnabled(),
      onChanged: (value) async {
        await _settingsViewModel.showUsePreReleasePatchesDialog(context, value);
        setState(() {});
      },
    );
  }
}
