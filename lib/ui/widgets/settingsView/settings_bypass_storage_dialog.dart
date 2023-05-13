import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SBypassStorageDialog extends StatefulWidget {
  const SBypassStorageDialog({super.key});

  @override
  State<SBypassStorageDialog> createState() => _SBypassStorageDialog();
}

final _settingsViewModel = SettingsViewModel();

class _SBypassStorageDialog extends State<SBypassStorageDialog> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.bypassSelectFromStorageLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.bypassSelectFromStorageHint'),
      value: _settingsViewModel.getBypassSelectFromStorage(),
      onChanged: (value) {
        setState(() {
          _settingsViewModel.setBypassSelectFromStorage(value);
        });
      },
    );
  }
}
