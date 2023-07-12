import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_choose_app_info_source.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';

class SFetchAppInfo extends StatefulWidget {
  const SFetchAppInfo({super.key});

  @override
  State<SFetchAppInfo> createState() => _SFetchAppInfoState();
}

final _settingsViewModel = SettingsViewModel();

class _SFetchAppInfoState extends State<SFetchAppInfo> {
  @override
  Widget build(BuildContext context) {
    final bool isFetchAppInfoEnabled = _settingsViewModel.isFetchAppInfoEnabled();
    return Column(
      children: [
        SwitchListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
          title: I18nText(
            'settingsView.fetchAppInfoLabel',
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          subtitle: I18nText('settingsView.fetchAppInfoLabelHint'),
          value: isFetchAppInfoEnabled,
          onChanged: (value) async {
            if (value) {
              await SChooseAppInfoSources().showAppInfoSourcesDialog(context);
              setState(() {});
            } else{
              setState(() {
                _settingsViewModel.toggleFetchAppInfo(value);
              });
            }
          },
        ),
        if(isFetchAppInfoEnabled)
          ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 20.0),
            title: I18nText(
              'settingsView.fetchAppInfoDialogTitle',
              child: const Text(
                '',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            onTap: () async {
              await SChooseAppInfoSources().showAppInfoSourcesDialog(context);
              setState(() {});
            },
          ),
      ],
    );
  }
}
