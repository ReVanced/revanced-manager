// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:stacked/stacked.dart';

class SManageApiUrl extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();

  final TextEditingController _apiUrlController = TextEditingController();

  Future<void> showApiUrlDialog(BuildContext context) async {
    final apiUrl = _managerAPI.getApiUrl();

    _apiUrlController.text = apiUrl;
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: <Widget>[
            Expanded(
              child: Text(t.settingsView.apiURLLabel),
            ),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => showApiUrlResetDialog(context),
              color: Theme.of(context).colorScheme.secondary,
            ),
          ],
        ),
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              TextField(
                controller: _apiUrlController,
                autocorrect: false,
                onChanged: (value) => notifyListeners(),
                decoration: InputDecoration(
                  icon: Icon(
                    Icons.api_outlined,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                  border: const OutlineInputBorder(),
                  labelText: t.settingsView.selectApiURL,
                  hintText: apiUrl,
                ),
              ),
            ],
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              _apiUrlController.clear();
              Navigator.of(context).pop();
            },
            child: Text(t.cancelButton),
          ),
          FilledButton(
            onPressed: () {
              _managerAPI.setApiUrl(_apiUrlController.text);
              Navigator.of(context).pop();
            },
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }

  Future<void> showApiUrlResetDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.settingsView.sourcesResetDialogTitle),
        content: Text(t.settingsView.apiURLResetDialogText),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.noButton),
          ),
          FilledButton(
            onPressed: () {
              _managerAPI.resetApiUrl();
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

final sManageApiUrl = SManageApiUrl();

class SManageApiUrlUI extends StatelessWidget {
  const SManageApiUrlUI({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsTileDialog(
      padding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: t.settingsView.apiURLLabel,
      subtitle: t.settingsView.apiURLHint,
      onTap: () => sManageApiUrl.showApiUrlDialog(context),
    );
  }
}
