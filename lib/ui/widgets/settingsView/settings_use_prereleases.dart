import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_switch_list_tile.dart';

class SUsePrereleases extends StatefulWidget {
  const SUsePrereleases({super.key});

  @override
  State<SUsePrereleases> createState() => _SUsePrereleasesState();
}

final _settingsViewModel = SettingsViewModel();

class _SUsePrereleasesState extends State<SUsePrereleases> {
  @override
  Widget build(BuildContext context) {
    return HapticSwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: Text(
        t.settingsView.usePrereleasesLabel,
        style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w500),
      ),
      subtitle: Text(t.settingsView.usePrereleasesHint),
      value: _settingsViewModel.usePrereleases(),
      onChanged: (value) async {
        await _settingsViewModel.showUsePrereleasesDialog(context, value);
        setState(() {});
      },
    );
  }
}
