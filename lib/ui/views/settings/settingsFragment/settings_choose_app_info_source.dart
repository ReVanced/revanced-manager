// ignore_for_file: use_build_context_synchronously

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_checkbox_list_tile.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class SChooseAppInfoSources extends BaseViewModel {
  final ManagerAPI _managerAPI = locator<ManagerAPI>();
  final _settingsViewModel = SettingsViewModel();
  final Toast _toast = locator<Toast>();

  Future<void> showAppInfoSourcesDialog(BuildContext context) async {
    List<String> sources = _managerAPI.getAppInfoSources();
    final ValueNotifier<List<String>> selectedSources = ValueNotifier(sources);
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: I18nText('settingsView.fetchAppInfoDialogTitle'),
        backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
        content: SingleChildScrollView(
          child: ValueListenableBuilder(
            valueListenable: selectedSources,
            builder: (context, value, child) {
              sources = [...value];
              return Column(
                children: <Widget>[
                  CustomCheckboxListTile(
                    selectedSources: selectedSources,
                    value: 'Google Play Store',
                    sources: sources,
                  ),
                  CustomCheckboxListTile(
                    selectedSources: selectedSources,
                    value: 'APKPure',
                    sources: sources,
                  ),
                  CustomCheckboxListTile(
                    selectedSources: selectedSources,
                    value: 'APKMirror',
                    sources: sources,
                  ),
                  I18nText('settingsView.fetchAppInfoDialogText'),
                ],
              );
            },
          ),
        ),
        actions: <Widget>[
          CustomMaterialButton(
            isFilled: false,
            label: I18nText('cancelButton'),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
          CustomMaterialButton(
            label: I18nText('okButton'),
            onPressed: () {
              _managerAPI.setAppInfoSources(sources);
              _settingsViewModel.toggleFetchAppInfo(sources.isNotEmpty);
              _toast.showBottom('settingsView.restartAppForChanges');
              Navigator.of(context).pop();
            },
          )
        ],
      ),
    );
  }
}
