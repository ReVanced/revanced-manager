// ignore_for_file: prefer_const_constructors

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_update_theme.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_advanced_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_debug_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_export_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_team_section.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({Key? key}) : super(key: key);

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
              title: I18nText(
                'settingsView.widgetTitle',
                child: Text(
                  '',
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.titleLarge!.color,
                  ),
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
                      // SUpdateLanguageUI(),
                      _settingsDivider,
                      SAdvancedSection(),
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
