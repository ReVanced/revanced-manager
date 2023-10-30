import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_manage_keystore_password.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';

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
        // SManageKeystorePasswordUI(),
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
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: Text(dialogText),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: Text(t.noButton),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: Text(t.yesButton),
            onPressed: () => {
              Navigator.of(context).pop(),
              dialogAction(),
            },
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
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: Text(t.settingsView.regenerateKeystoreDialogText),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: Text(t.noButton),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: Text(t.yesButton),
            onPressed: () => {
              Navigator.of(context).pop(),
              _settingsViewModel.deleteKeystore(),
            },
          ),
        ],
      ),
    );
  }
}
