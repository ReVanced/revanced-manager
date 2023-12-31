// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:stacked/stacked.dart';

class SManageKeystorePassword extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();

  final TextEditingController _keystorePasswordController =
      TextEditingController();

  Future<void> showKeystoreDialog(BuildContext context) async {
    final String keystorePasswordText = _managerAPI.getKeystorePassword();
    _keystorePasswordController.text = keystorePasswordText;
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: <Widget>[
            I18nText('settingsView.selectKeystorePassword'),
            const Spacer(),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => _keystorePasswordController.text =
                  _managerAPI.defaultKeystorePassword,
              color: Theme.of(context).colorScheme.secondary,
            ),
          ],
        ),
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              TextField(
                controller: _keystorePasswordController,
                autocorrect: false,
                obscureText: true,
                onChanged: (value) => notifyListeners(),
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  labelText: I18nText(
                    'settingsView.selectKeystorePassword',
                  ).toString(),
                ),
              ),
            ],
          ),
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () {
              _keystorePasswordController.clear();
              Navigator.of(context).pop();
            },
            child: I18nText('cancelButton'),
          ),
          FilledButton(
            onPressed: () {
              final String passwd = _keystorePasswordController.text;
              _managerAPI.setKeystorePassword(passwd);
              Navigator.of(context).pop();
            },
            child: I18nText('okButton'),
          ),
        ],
      ),
    );
  }
}

final sManageKeystorePassword = SManageKeystorePassword();

class SManageKeystorePasswordUI extends StatelessWidget {
  const SManageKeystorePasswordUI({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsTileDialog(
      padding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: 'settingsView.selectKeystorePassword',
      subtitle: 'settingsView.selectKeystorePasswordHint',
      onTap: () => sManageKeystorePassword.showKeystoreDialog(context),
    );
  }
}
