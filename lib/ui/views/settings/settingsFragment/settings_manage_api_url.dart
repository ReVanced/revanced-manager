// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_text_field.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class SManageApiUrl extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final Toast _toast = locator<Toast>();

  final TextEditingController _apiUrlController = TextEditingController();

  Future<void> showApiUrlDialog(BuildContext context) async {
    final String apiUrl = _managerAPI.getApiUrl();
    _apiUrlController.text = apiUrl.replaceAll('https://', '');
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: <Widget>[
            I18nText('settingsView.apiURLLabel'),
            const Spacer(),
            IconButton(
              icon: const Icon(Icons.manage_history_outlined),
              onPressed: () => showApiUrlResetDialog(context),
              color: Theme.of(context).colorScheme.secondary,
            )
          ],
        ),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: SingleChildScrollView(
          child: Column(
            children: <Widget>[
              CustomTextField(
                leadingIcon: Icon(
                  Icons.api_outlined,
                  color: Theme.of(context).colorScheme.secondary,
                ),
                inputController: _apiUrlController,
                label: I18nText('settingsView.selectApiURL'),
                hint: apiUrl,
                onChanged: (value) => notifyListeners(),
              ),
            ],
          ),
        ),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('cancelButton'),
            onPressed: () {
              _apiUrlController.clear();
              Navigator.of(context).pop();
            },
          ),
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () {
              String apiUrl = _apiUrlController.text;
              if (!apiUrl.startsWith('https')) {
                apiUrl = 'https://$apiUrl';
              }
              _managerAPI.setApiUrl(apiUrl);
              Navigator.of(context).pop();
            },
          )
        ],
      ),
    );
  }

  Future<void> showApiUrlResetDialog(BuildContext context) async {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.sourcesResetDialogTitle'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: I18nText('settingsView.apiURLResetDialogText'),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('noButton'),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CustomMaterialButton(
            label: I18nText('yesButton'),
            onPressed: () {
              _managerAPI.setApiUrl('');
              _toast.showBottom('settingsView.restartAppForChanges');
              Navigator.of(context).pop();
            },
          )
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
      title: 'settingsView.apiURLLabel',
      subtitle: 'settingsView.apiURLHint',
      onTap: () => sManageApiUrl.showApiUrlDialog(context),
    );
  }
}
