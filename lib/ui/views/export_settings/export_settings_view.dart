import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/views/export_settings/export_settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';

final _exportSettingsViewModel = ExportSettingsViewModel();

class ExportSettingsView extends StatelessWidget {
  const ExportSettingsView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        systemNavigationBarColor: Colors.black.withOpacity(0.002),
        statusBarColor: Colors.black.withOpacity(0.002),
      ),
    );
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

    return AlertDialog(
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
    );
  }
}
