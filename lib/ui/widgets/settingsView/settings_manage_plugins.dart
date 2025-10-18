import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_manage_plugins_view.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile.dart';

class SManagePlugins extends StatelessWidget {
  const SManagePlugins({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsTile(
      title: t.settingsView.managePluginsLabel,
      subtitle: t.settingsView.managePluginsHint,
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute(
          builder: (context) => const SettingsManagePluginsView(),
        ),
      ),
    );
  }
}