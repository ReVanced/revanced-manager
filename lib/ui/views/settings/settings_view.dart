// ignore_for_file: prefer_const_constructors

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_update_language.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_update_theme.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_advanced_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_data_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_debug_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_export_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_team_section.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  static const _settingsDivider =
      Divider(thickness: 1.0, indent: 20.0, endIndent: 20.0);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<SettingsViewModel>.reactive(
      viewModelBuilder: () => SettingsViewModel(),
      builder: (context, model, child) => Scaffold(
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              isMainView: true,
              title: Text(
                t.settingsView.widgetTitle,
                style: GoogleFonts.inter(
                  color: Theme.of(context).textTheme.titleLarge!.color,
                ),
              ),
            ),
            SliverList(
              delegate: SliverChildListDelegate.fixed(
                <Widget>[
                  ListView(
                    padding: EdgeInsets.zero,
                    shrinkWrap: true,
                    physics: NeverScrollableScrollPhysics(),
                    children: const [
                      SUpdateThemeUI(),
                      // _settingsDivider,
                      SUpdateLanguageUI(),
                      _settingsDivider,
                      SAdvancedSection(),
                      _settingsDivider,
                      SDataSection(),
                      _settingsDivider,
                      SExportSection(),
                      _settingsDivider,
                      STeamSection(),
                      _settingsDivider,
                      SDebugSection(),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
