import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_switch_list_tile.dart';

import '../../views/settings/settingsFragment/settings_manage_sources.dart';

class SUseAlternativeSources extends StatefulWidget {
  const SUseAlternativeSources({super.key});

  @override
  State<SUseAlternativeSources> createState() =>
      _SUseAlternativeSourcesState();
}

final _settingsViewModel = SettingsViewModel();

class _SUseAlternativeSourcesState extends State<SUseAlternativeSources> {
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        HapticSwitchListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.useAlternativeSources',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.useAlternativeSourcesHint'),
          value: _settingsViewModel.isUsingAlternativeSources(),
          onChanged: (value) {
            _settingsViewModel.useAlternativeSources(value);
            setState(() {});
          },
        ),
        if (_settingsViewModel.isUsingAlternativeSources())
          const SManageSourcesUI(),
      ],
    );
  }
}
