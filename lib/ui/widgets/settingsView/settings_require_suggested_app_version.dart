import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SRequireSuggestedAppVersion extends StatefulWidget {
  const SRequireSuggestedAppVersion({super.key});

  @override
  State<SRequireSuggestedAppVersion> createState() => _SRequireSuggestedAppVersionState();
}

final _settingsViewModel = SettingsViewModel();

class _SRequireSuggestedAppVersionState extends State<SRequireSuggestedAppVersion> {
  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
      title: I18nText(
        'settingsView.requireSuggestedAppVersionLabel',
        child: const Text(
          '',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      subtitle: I18nText('settingsView.requireSuggestedAppVersionHint'),
      value: _settingsViewModel.isRequireSuggestedAppVersionEnabled(),
      onChanged: (value) async {
          await _settingsViewModel.showRequireSuggestedAppVersionDialog(context, value);
          setState(() {});
      },
    );
  }
}
