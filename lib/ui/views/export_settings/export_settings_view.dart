import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/views/export_settings/export_settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';

final _exportSettingsViewModel = ExportSettingsViewModel();

class ExportSettingsView extends StatelessWidget {
  const ExportSettingsView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    _exportSettingsViewModel.init(context);
    return Material(
        child: AlertDialog(
          title: I18nText('exportSettingsView.widgetTitle'),
          content: I18nText('exportSettingsView.description'),
          icon: const Icon(Icons.update),
          actions: <Widget> [
            CustomMaterialButton(
              isFilled: false,
              label: I18nText('exportSettingsView.dismissButton'),
              onPressed: _exportSettingsViewModel.deny,
            ),
            CustomMaterialButton(
              label: I18nText('exportSettingsView.exportButton'),
              onPressed: () async {
                await _exportSettingsViewModel.accept();
              },
            ),
          ],
        ),
    );
  }
}
