// ignore_for_file: prefer_const_constructors

import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_auto_update_patches.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_enable_patches_selection.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_last_patched_app.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_require_suggested_app_version.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_show_update_dialog.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_universal_patches.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_version_compatibility_check.dart';

class SAdvancedSection extends StatelessWidget {
  const SAdvancedSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: t.settingsView.advancedSectionTitle,
      children: const <Widget>[
        SAutoUpdatePatches(),
        SShowUpdateDialog(),
        SEnablePatchesSelection(),
        SRequireSuggestedAppVersion(),
        SVersionCompatibilityCheck(),
        SUniversalPatches(),
        SLastPatchedApp(),
      ],
    );
  }
}
