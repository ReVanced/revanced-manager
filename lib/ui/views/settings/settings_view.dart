import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/contributors/contributors_view.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/about_widget.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_switch_tile.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_tile_dialog.dart';
import 'package:revanced_manager/ui/widgets/settingsView/settings_section.dart';
import 'package:revanced_manager/ui/widgets/settingsView/social_media_widget.dart';
import 'package:revanced_manager/ui/widgets/settingsView/sources_widget.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:revanced_manager/ui/widgets/shared/open_container_wrapper.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_themes/stacked_themes.dart';

class SettingsView extends StatelessWidget {
  final TextEditingController organizationController = TextEditingController();
  final TextEditingController patchesSourceController = TextEditingController();
  final TextEditingController integrationsSourceController =
      TextEditingController();

  SettingsView({Key? key}) : super(key: key);

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
                    color: Theme.of(context).textTheme.headline5!.color,
                    fontWeight: FontWeight.w500,
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
                            'settingsView.themeLabel',
                            child: Text(
                              '',
                              style: kSettingItemTextStyle,
                            ),
                          ),
                          subtitle: I18nText('settingsView.themeHint'),
                          value: isDark,
                          onTap: (value) {
                            isDark = value;
                            getThemeManager(context).toggleDarkLightTheme();
                          },
                        ),
                      ],
                    ),
                    SettingsTileDialog(
                        title: 'settingsView.languageLabel',
                        subtitle: 'English',
                        children: <Widget>[
                          RadioListTile<String>(
                            title: I18nText('settingsView.englishOption'),
                            value: 'en',
                            groupValue: 'en',
                            onChanged: (value) {
                              model.updateLanguage(context, value);
                              Navigator.of(context).pop();
                            },
                          ),
                          RadioListTile<String>(
                            title: I18nText('settingsView.frenchOption'),
                            value: 'fr',
                            groupValue: 'en',
                            onChanged: (value) {
                              model.updateLanguage(context, value);
                              Navigator.of(context).pop();
                            },
                          ),
                        ]),
                    const Divider(thickness: 1.0),
                    SettingsSection(
                      title: 'settingsView.patcherSectionTitle',
                      children: <Widget>[
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: I18nText(
                            'settingsView.rootModeLabel',
                            child: Text(
                              '',
                              style: kSettingItemTextStyle,
                            ),
                          ),
                          subtitle: I18nText('settingsView.rootModeHint'),
                          onTap: () => model.navigateToRootChecker(),
                        ),
                        SourcesWidget(
                          title: 'settingsView.sourcesLabel',
                          organizationController: organizationController,
                          patchesSourceController: patchesSourceController,
                          integrationsSourceController:
                              integrationsSourceController,
                        ),
                      ],
                    ),
                    const Divider(thickness: 1.0),
                    SettingsSection(
                      title: 'settingsView.teamSectionTitle',
                      children: <Widget>[
                        OpenContainerWrapper(
                          openBuilder: (_, __) => const ContributorsView(),
                          closedBuilder: (_, openContainer) => ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: I18nText(
                              'settingsView.contributorsLabel',
                              child: Text('', style: kSettingItemTextStyle),
                            ),
                            subtitle: I18nText('settingsView.contributorsHint'),
                            onTap: openContainer,
                          ),
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
