// ignore_for_file: prefer_const_constructors

import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_manage_api_url.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_use_alternative_sources.dart';

class SDataSection extends StatelessWidget {
  const SDataSection({super.key});

  @override
  Widget build(BuildContext context) {
    return SettingsSection(
      title: t.settingsView.dataSectionTitle,
      children: const <Widget>[
        SManageApiUrlUI(),
        SUseAlternativeSources(),
      ],
    );
  }
}
