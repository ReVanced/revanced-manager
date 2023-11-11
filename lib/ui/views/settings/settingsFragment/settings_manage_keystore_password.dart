// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_text_field.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
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
            Text(t.settingsView.selectKeystorePassword),
            const Spacer(),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => _keystorePasswordController.text =
                  _managerAPI.defaultKeystorePassword,
              color: Theme.of(context).colorScheme.secondary,
            ),
          ],
        ),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              CustomTextField(
                inputController: _keystorePasswordController,
                label: Text(t.settingsView.selectKeystorePassword),
                hint: '',
                onChanged: (value) => notifyListeners(),
              ),
            ],
          ),
        ),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: Text(t.cancelButton),
            onPressed: () {
              _keystorePasswordController.clear();
              Navigator.of(context).pop();
            },
          ),
          CustomMaterialButton(
            label: Text(t.okButton),
            onPressed: () {
              final String passwd = _keystorePasswordController.text;
              _managerAPI.setKeystorePassword(passwd);
              Navigator.of(context).pop();
            },
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
      title: t.settingsView.selectKeystorePassword,
      subtitle: t.settingsView.selectKeystorePasswordHint,
      onTap: () => sManageKeystorePassword.showKeystoreDialog(context),
    );
  }
}
