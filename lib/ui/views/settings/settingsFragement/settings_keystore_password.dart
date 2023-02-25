import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/widgets/settingsView/password_text_field.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class SKeyStorePassword extends BaseViewModel {
  final _keypassController = TextEditingController();
  final _managerAPI = locator<ManagerAPI>();

  Future<void> showConfirmDialog(BuildContext context, String newPass) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.customKeystoreConfirmTitle'),
        content: I18nText('settingsView.customKeystoreConfirmContent'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        actions: [
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('cancelButton'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () async {
              _managerAPI.setKeyStorePassword(newPass);
              _managerAPI.deleteKeystore();
              Navigator.of(context).pop();
            },
          ),
        ],
      ),
    );
  }

  Future<void> showKeyStorePasswordDialog(BuildContext context) {
    final keyStorePass = _managerAPI.getKeyStorePass();
    _keypassController.text = keyStorePass;

    return showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: I18nText('settingsView.customKeystoreTitle'),
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          content: SingleChildScrollView(
            child: Column(
              children: <Widget>[
                PasswordTextField(
                  leadingIcon: Icon(
                    Icons.password_outlined,
                    color: Theme.of(context).colorScheme.secondary,
                  ),
                  obscureText: true,
                  inputController: _keypassController,
                  label: I18nText('settingsView.customKeystoreLabel'),
                  hint: FlutterI18n.translate(
                      context, 'settingsView.customKeystoreHint'),
                  onChanged: (value) => notifyListeners(),
                ),
              ],
            ),
          ),
          actions: [
            CustomMaterialButton(
              isFilled: false,
              label: I18nText('cancelButton'),
              onPressed: () => Navigator.of(context).pop(),
            ),
            CustomMaterialButton(
              label: I18nText('okButton'),
              onPressed: () async {
                if (keyStorePass != _keypassController.text &&
                    _managerAPI.defaultKeyStorePass !=
                        _keypassController.text) {
                  await showConfirmDialog(context, _keypassController.text);
                }
                // ignore: use_build_context_synchronously
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }
}

class SKeyStorePasswordUI extends StatelessWidget {
  const SKeyStorePasswordUI({super.key});

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<SKeyStorePassword>.reactive(
      viewModelBuilder: () => SKeyStorePassword(),
      builder: (context, viewModel, child) {
        return SettingsTileDialog(
          padding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: 'settingsView.customKeystoreLabel',
          subtitle: 'settingsView.customKeystoreHint',
          onTap: () => viewModel.showKeyStorePasswordDialog(context),
        );
      },
    );
  }
}
