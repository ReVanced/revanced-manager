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
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              TextField(
                controller: _hostSourceController,
                autocorrect: false,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  hintText: hostRepository,
                  labelText: FlutterI18n.translate(
                    context,
                    'settingsView.hostRepositoryLabel',
                  ),
                ),
              ),
              const SizedBox(height: 20),
              TextField(
                controller: _orgPatSourceController,
                autocorrect: false,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  hintText: patchesRepo.split('/')[0],
                  labelText: FlutterI18n.translate(
                    context,
                    'settingsView.orgPatchesLabel',
                  ),
                ),
              ),
              const SizedBox(height: 15),
              TextField(
                controller: _patSourceController,
                autocorrect: false,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  hintText: patchesRepo.split('/')[1],
                  labelText: FlutterI18n.translate(
                    context,
                    'settingsView.sourcesPatchesLabel',
                  ),
                ),
              ),
              const SizedBox(height: 20),
              TextField(
                controller: _orgIntSourceController,
                autocorrect: false,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  hintText: integrationsRepo.split('/')[0],
                  labelText: FlutterI18n.translate(
                    context,
                    'settingsView.orgIntegrationsLabel',
                  ),
                ),
              ),
              const SizedBox(height: 15),
              TextField(
                controller: _intSourceController,
                autocorrect: false,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  hintText: integrationsRepo.split('/')[1],
                  labelText: FlutterI18n.translate(
                    context,
                    'settingsView.sourcesIntegrationsLabel',
                  ),
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
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: I18nText('settingsView.sourcesResetDialogText'),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
            },
            child: I18nText('cancelButton'),
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
