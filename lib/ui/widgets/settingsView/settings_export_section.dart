import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_manage_keystore_password.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';

final _settingsViewModel = SettingsViewModel();

class SExportSection extends StatelessWidget {
  const SExportSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: 'settingsView.exportSectionTitle',
      children: <Widget>[
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.exportPatchesLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.exportPatchesHint'),
          onTap: () => _settingsViewModel.exportPatches(),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.importPatchesLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.importPatchesHint'),
          onTap: () => _settingsViewModel.importPatches(context),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.resetStoredPatchesLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.resetStoredPatchesHint'),
          onTap: () => _showResetDialog(
            context,
            'settingsView.resetStoredPatchesDialogTitle',
            'settingsView.resetStoredPatchesDialogText',
            _settingsViewModel.resetSelectedPatches,
          ),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.resetStoredOptionsLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.resetStoredOptionsHint'),
          onTap: () => _showResetDialog(
            context,
            'settingsView.resetStoredOptionsDialogTitle',
            'settingsView.resetStoredOptionsDialogText',
            _settingsViewModel.resetAllOptions,
          ),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.exportKeystoreLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.exportKeystoreHint'),
          onTap: () => _settingsViewModel.exportKeystore(),
        ),
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.importKeystoreLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.importKeystoreHint'),
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
          title: I18nText(
            'settingsView.regenerateKeystoreLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.regenerateKeystoreHint'),
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
        title: I18nText(dialogTitle),
        content: I18nText(dialogText),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: I18nText('noButton'),
          ),
          FilledButton(
            onPressed: () => {
              Navigator.of(context).pop(),
              dialogAction(),
            },
            child: I18nText('yesButton'),
          ),
        ],
      ),
    );
  }

  Future<void> _showDeleteKeystoreDialog(context) {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.regenerateKeystoreDialogTitle'),
        content: I18nText('settingsView.regenerateKeystoreDialogText'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: I18nText('noButton'),
          ),
          FilledButton(
            onPressed: () => {
              Navigator.of(context).pop(),
              _settingsViewModel.deleteKeystore(),
            },
            child: I18nText('yesButton'),
          ),
        ],
      ),
    );
  }
}
