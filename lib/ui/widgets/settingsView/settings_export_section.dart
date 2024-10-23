import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_manage_keystore_password.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';

final _settingsViewModel = SettingsViewModel();

class SExportSection extends StatelessWidget {
  const SExportSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: t.settingsView.exportSectionTitle,
      children: <Widget>[
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.exportSettingsLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.exportSettingsHint),
          onTap: () => _settingsViewModel.exportSettings(),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.importSettingsLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.importSettingsHint),
          onTap: () => _settingsViewModel.importSettings(),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.exportPatchesLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.exportPatchesHint),
          onTap: () => _settingsViewModel.exportPatches(),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.importPatchesLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.importPatchesHint),
          onTap: () => _settingsViewModel.importPatches(context),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.resetStoredPatchesLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.resetStoredPatchesHint),
          onTap: () => _showResetDialog(
            context,
            t.settingsView.resetStoredPatchesDialogTitle,
            t.settingsView.resetStoredPatchesDialogText,
            _settingsViewModel.resetSelectedPatches,
          ),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.resetStoredOptionsLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.resetStoredOptionsHint),
          onTap: () => _showResetDialog(
            context,
            t.settingsView.resetStoredOptionsDialogTitle,
            t.settingsView.resetStoredOptionsDialogText,
            _settingsViewModel.resetAllOptions,
          ),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.exportKeystoreLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.exportKeystoreHint),
          onTap: () => _settingsViewModel.exportKeystore(),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.importKeystoreLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.importKeystoreHint),
          onTap: () async {
            await _settingsViewModel.importKeystore();
            final sManageKeystorePassword = SManageKeystorePassword();
            if (context.mounted) {
              sManageKeystorePassword.showKeystoreDialog(context);
            }
          },
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: Text(
            t.settingsView.regenerateKeystoreLabel,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
          subtitle: Text(t.settingsView.regenerateKeystoreHint),
          onTap: () => _showDeleteKeystoreDialog(context),
        ),
      ],
    );
  }

  Future<void> _showResetDialog(
    context,
    dialogTitle,
    dialogText,
    dialogAction,
  ) {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(dialogTitle),
        content: Text(dialogText),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.noButton),
          ),
          FilledButton(
            onPressed: () => {
              Navigator.of(context).pop(),
              dialogAction(),
            },
            child: Text(t.yesButton),
          ),
        ],
      ),
    );
  }

  Future<void> _showDeleteKeystoreDialog(context) {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.settingsView.regenerateKeystoreDialogTitle),
        content: Text(t.settingsView.regenerateKeystoreDialogText),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.noButton),
          ),
          FilledButton(
            onPressed: () => {
              Navigator.of(context).pop(),
              _settingsViewModel.deleteKeystore(),
            },
            child: Text(t.yesButton),
          ),
        ],
      ),
    );
  }
}
