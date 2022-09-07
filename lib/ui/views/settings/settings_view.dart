import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/about_widget.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch_tile.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/social_media_widget.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<SettingsViewModel>.reactive(
      viewModelBuilder: () => SettingsViewModel(),
      builder: (context, SettingsViewModel model, child) => Scaffold(
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              title: I18nText(
                'settingsView.widgetTitle',
                child: Text(
                  '',
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(
                vertical: 10.0,
                horizontal: 20.0,
              ),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    SettingsSection(
                      title: 'settingsView.appearanceSectionTitle',
                      children: <Widget>[
                        CustomSwitchTile(
                          title: I18nText(
                            'settingsView.darkThemeLabel',
                            child: const Text(
                              '',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                          subtitle: I18nText('settingsView.darkThemeHint'),
                          value: model.getDarkThemeStatus(),
                          onTap: (value) => model.setUseDarkTheme(
                            context,
                            value,
                          ),
                        ),
                        CustomSwitchTile(
                          title: I18nText(
                            'settingsView.dynamicThemeLabel',
                            child: const Text(
                              '',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                          subtitle: I18nText('settingsView.dynamicThemeHint'),
                          value: model.getDynamicThemeStatus(),
                          onTap: (value) => model.setUseDynamicTheme(
                            context,
                            value,
                          ),
                        ),
                      ],
                    ),
                    SettingsTileDialog(
                      title: 'settingsView.languageLabel',
                      subtitle: 'English',
                      onTap: () => model.showLanguagesDialog(context),
                    ),
                    const Divider(thickness: 1.0),
                    SettingsSection(
                      title: 'settingsView.patcherSectionTitle',
                      children: <Widget>[
                        SettingsTileDialog(
                          title: 'settingsView.sourcesLabel',
                          subtitle: 'settingsView.sourcesLabelHint',
                          onTap: () => model.showSourcesDialog(context),
                        ),
                      ],
                    ),
                    const Divider(thickness: 1.0),
                    SettingsSection(
                      title: 'settingsView.teamSectionTitle',
                      children: <Widget>[
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: I18nText(
                            'settingsView.contributorsLabel',
                            child: const Text(
                              '',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                          subtitle: I18nText('settingsView.contributorsHint'),
                          onTap: () => model.navigateToContributors(),
                        ),
                        const SocialMediaWidget(),
                      ],
                    ),
                    const Divider(thickness: 1.0),
                    const SettingsSection(
                      title: 'settingsView.infoSectionTitle',
                      children: <Widget>[
                        AboutWidget(),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
