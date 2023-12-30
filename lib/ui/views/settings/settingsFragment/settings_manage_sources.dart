// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:stacked/stacked.dart';

class SManageSources extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final Toast _toast = locator<Toast>();

  final TextEditingController _hostSourceController = TextEditingController();
  final TextEditingController _orgPatSourceController = TextEditingController();
  final TextEditingController _patSourceController = TextEditingController();
  final TextEditingController _orgIntSourceController = TextEditingController();
  final TextEditingController _intSourceController = TextEditingController();

  Future<void> showSourcesDialog(BuildContext context) async {
    final String hostRepository = _managerAPI.getRepoUrl();
    final String patchesRepo = _managerAPI.getPatchesRepo();
    final String integrationsRepo = _managerAPI.getIntegrationsRepo();
    _hostSourceController.text = hostRepository;
    _orgPatSourceController.text = patchesRepo.split('/')[0];
    _patSourceController.text = patchesRepo.split('/')[1];
    _orgIntSourceController.text = integrationsRepo.split('/')[0];
    _intSourceController.text = integrationsRepo.split('/')[1];
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: <Widget>[
            I18nText('settingsView.sourcesLabel'),
            const Spacer(),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => showResetConfirmationDialog(context),
              color: Theme.of(context).colorScheme.secondary,
            ),
          ],
        ),
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              /*
              API for accessing the specified repositories
              If default is used, will use the ReVanced API
              */
              TextField(
                controller: _hostSourceController,
                autocorrect: false,
                onChanged: (value) => notifyListeners(),
                decoration: InputDecoration(
                  icon: Icon(
                    Icons.rocket_launch_outlined,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                  border: const OutlineInputBorder(),
                  labelText: I18nText(
                    'settingsView.hostRepositoryLabel',
                  ).toString(),
                  hintText: hostRepository,
                ),
              ),
              const SizedBox(height: 8),
              // Patches owner's name
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
                  labelText: I18nText(
                    'settingsView.orgPatchesLabel',
                  ).toString(),
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
                  labelText: I18nText(
                    'settingsView.sourcesPatchesLabel',
                  ).toString(),
                  hintText: patchesRepo.split('/')[1],
                ),
              ),
              const SizedBox(height: 8),
              // Integrations owner's name
              TextField(
                controller: _orgIntSourceController,
                autocorrect: false,
                onChanged: (value) => notifyListeners(),
                decoration: InputDecoration(
                  icon: Icon(
                    Icons.merge_outlined,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                  border: const OutlineInputBorder(),
                  labelText: I18nText(
                    'settingsView.orgIntegrationsLabel',
                  ).toString(),
                  hintText: integrationsRepo.split('/')[0],
                ),
              ),
              const SizedBox(height: 8),
              // Integrations repository's name
              TextField(
                controller: _intSourceController,
                autocorrect: false,
                onChanged: (value) => notifyListeners(),
                decoration: InputDecoration(
                  icon: const Icon(
                    Icons.merge_outlined,
                    color: Colors.transparent,
                  ),
                  border: const OutlineInputBorder(),
                  labelText: I18nText(
                    'settingsView.sourcesIntegrationsLabel',
                  ).toString(),
                  hintText: integrationsRepo.split('/')[1],
                ),
              ),
              const SizedBox(height: 20),
              I18nText('settingsView.sourcesUpdateNote'),
            ],
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              _orgPatSourceController.clear();
              _patSourceController.clear();
              _orgIntSourceController.clear();
              _intSourceController.clear();
              Navigator.of(context).pop();
            },
            child: I18nText('cancelButton'),
          ),
          FilledButton(
            onPressed: () {
              _managerAPI.setRepoUrl(_hostSourceController.text.trim());
              _managerAPI.setPatchesRepo(
                '${_orgPatSourceController.text.trim()}/${_patSourceController.text.trim()}',
              );
              _managerAPI.setIntegrationsRepo(
                '${_orgIntSourceController.text.trim()}/${_intSourceController.text.trim()}',
              );
              _managerAPI.setCurrentPatchesVersion('0.0.0');
              _managerAPI.setCurrentIntegrationsVersion('0.0.0');
              _toast.showBottom('settingsView.restartAppForChanges');
              Navigator.of(context).pop();
            },
            child: I18nText('okButton'),
          ),
        ],
      ),
    );
  }

  Future<void> showResetConfirmationDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.sourcesResetDialogTitle'),
        content: I18nText('settingsView.sourcesResetDialogText'),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: I18nText('noButton'),
          ),
          FilledButton(
            onPressed: () {
              _managerAPI.setRepoUrl('');
              _managerAPI.setPatchesRepo('');
              _managerAPI.setIntegrationsRepo('');
              _managerAPI.setCurrentPatchesVersion('0.0.0');
              _managerAPI.setCurrentIntegrationsVersion('0.0.0');
              _toast.showBottom('settingsView.restartAppForChanges');
              Navigator.of(context)
                ..pop()
                ..pop();
            },
            child: I18nText('yesButton'),
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
      title: 'settingsView.sourcesLabel',
      subtitle: 'settingsView.sourcesLabelHint',
      onTap: () => sManageSources.showSourcesDialog(context),
    );
  }
}
