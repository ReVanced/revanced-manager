// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:stacked/stacked.dart';

class SManageSources extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final Toast _toast = locator<Toast>();

  final TextEditingController _orgPatSourceController = TextEditingController();
  final TextEditingController _patSourceController = TextEditingController();

  Future<void> showSourcesDialog(BuildContext context) async {
    final String patchesRepo = _managerAPI.getPatchesRepo();
    _orgPatSourceController.text = patchesRepo.split('/')[0];
    _patSourceController.text = patchesRepo.split('/')[1];
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        scrollable: true,
        title: Row(
          children: <Widget>[
            Expanded(
              child: Text(t.settingsView.sourcesLabel),
            ),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => showResetConfirmationDialog(context),
              color: Theme.of(context).colorScheme.secondary,
            ),
          ],
        ),
        content: Column(
          children: <Widget>[
            TextField(
              controller: _orgPatSourceController,
              autocorrect: false,
              onChanged: (value) => notifyListeners(),
              decoration: InputDecoration(
                icon: Icon(
                  Icons.extension_outlined,
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
                border: const OutlineInputBorder(),
                labelText: t.settingsView.orgPatchesLabel,
                hintText: patchesRepo.split('/')[0],
              ),
            ),
            const SizedBox(height: 8),
            // Patches repository's name
            TextField(
              controller: _patSourceController,
              autocorrect: false,
              onChanged: (value) => notifyListeners(),
              decoration: InputDecoration(
                icon: const Icon(
                  Icons.extension_outlined,
                  color: Colors.transparent,
                ),
                border: const OutlineInputBorder(),
                labelText: t.settingsView.sourcesPatchesLabel,
                hintText: patchesRepo.split('/')[1],
              ),
            ),
            const SizedBox(height: 20),
            Text(t.settingsView.sourcesUpdateNote),
          ],
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              _orgPatSourceController.clear();
              _patSourceController.clear();
              Navigator.of(context).pop();
            },
            child: Text(t.cancelButton),
          ),
          FilledButton(
            onPressed: () {
              _managerAPI.setPatchesRepo(
                '${_orgPatSourceController.text.trim()}/${_patSourceController.text.trim()}',
              );
              _managerAPI.setCurrentPatchesVersion('0.0.0');
              _managerAPI.setLastUsedPatchesVersion();
              _toast.showBottom(t.settingsView.restartAppForChanges);
              Navigator.of(context).pop();
            },
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }

  Future<void> showResetConfirmationDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.settingsView.sourcesResetDialogTitle),
        content: Text(t.settingsView.sourcesResetDialogText),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.noButton),
          ),
          FilledButton(
            onPressed: () {
              _managerAPI.setPatchesRepo('');
              _managerAPI.setCurrentPatchesVersion('0.0.0');
              _toast.showBottom(t.settingsView.restartAppForChanges);
              Navigator.of(context)
                ..pop()
                ..pop();
            },
            child: Text(t.yesButton),
          ),
        ],
      ),
    );
  }
}

final sManageSources = SManageSources();

class SManageSourcesUI extends StatelessWidget {
  const SManageSourcesUI({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsTileDialog(
      padding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: t.settingsView.sourcesLabel,
      subtitle: t.settingsView.sourcesLabelHint,
      onTap: () => sManageSources.showSourcesDialog(context),
    );
  }
}
