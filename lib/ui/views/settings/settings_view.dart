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
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
              ),
            ),
            SliverList(
              delegate: SliverChildListDelegate.fixed(
                <Widget>[
                  SettingsSection(
                    title: 'settingsView.appearanceSectionTitle',
                    children: <Widget>[
                      CustomSwitchTile(
                        padding: const EdgeInsets.symmetric(horizontal: 20.0),
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
                      FutureBuilder<int>(
                        future: model.getSdkVersion(),
                        builder: (context, snapshot) => Visibility(
                          visible: snapshot.hasData &&
                              snapshot.data! >= ANDROID_12_SDK_VERSION,
                          child: CustomSwitchTile(
                            padding:
                                const EdgeInsets.symmetric(horizontal: 20.0),
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
                        ),
                      ),
                    ],
                  ),
                  SettingsTileDialog(
                    padding: const EdgeInsets.symmetric(horizontal: 20.0),
                    title: 'settingsView.languageLabel',
                    subtitle: 'English',
                    onTap: () => model.showLanguagesDialog(context),
                  ),
                  _settingsDivider,
                  SettingsSection(
                    title: 'settingsView.teamSectionTitle',
                    children: <Widget>[
                      ListTile(
                        contentPadding:
                            const EdgeInsets.symmetric(horizontal: 20.0),
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
                      const SocialMediaWidget(
                        padding: EdgeInsets.symmetric(horizontal: 20.0),
                      ),
                    ],
                  ),
                  _settingsDivider,
                  SettingsSection(
                    title: 'settingsView.advancedSectionTitle',
                    children: <Widget>[
                      SettingsTileDialog(
                        padding: const EdgeInsets.symmetric(horizontal: 20.0),
                        title: 'settingsView.apiURLLabel',
                        subtitle: 'settingsView.apiURLHint',
                        onTap: () => model.showApiUrlDialog(context),
                      ),
                      SettingsTileDialog(
                        padding: const EdgeInsets.symmetric(horizontal: 20.0),
                        title: 'settingsView.sourcesLabel',
                        subtitle: 'settingsView.sourcesLabelHint',
                        onTap: () => model.showSourcesDialog(context),
                      ),
                      CustomSwitchTile(
                          padding: const EdgeInsets.symmetric(horizontal: 20.0),
                          title: I18nText(
                            'settingsView.experimentalPatchesLabel',
                            child: const Text(
                              '',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                          subtitle:
                              I18nText('settingsView.experimentalPatchesHint'),
                          value: model.areExperimentalPatchesEnabled(),
                          onTap: (value) =>
                              model.useExperimentalPatches(value)),
                      ListTile(
                        contentPadding:
                            const EdgeInsets.symmetric(horizontal: 20.0),
                        title: I18nText(
                          'settingsView.deleteKeystoreLabel',
                          child: const Text(
                            '',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                        subtitle: I18nText('settingsView.deleteKeystoreHint'),
                        onTap: () => model.deleteKeystore,
                      ),
                      ListTile(
                        contentPadding:
                            const EdgeInsets.symmetric(horizontal: 20.0),
                        title: I18nText(
                          'settingsView.deleteTempDirLabel',
                          child: const Text(
                            '',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                        subtitle: I18nText('settingsView.deleteTempDirHint'),
                        onTap: () => model.deleteTempDir(),
                      ),
                      ListTile(
                        contentPadding:
                            const EdgeInsets.symmetric(horizontal: 20.0),
                        title: I18nText(
                          'settingsView.deleteLogsLabel',
                          child: const Text(
                            '',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                        subtitle: I18nText('settingsView.deleteLogsHint'),
                        onTap: () => model.deleteLogs(),
                      ),
                    ],
                  ),
                  _settingsDivider,
                  // SettingsSection(
                  //   title: 'settingsView.logsSectionTitle',
                  //   children: <Widget>[
                  //     CustomSwitchTile(
                  //       padding: const EdgeInsets.symmetric(horizontal: 20.0),
                  //       title: I18nText(
                  //         'settingsView.sentryLabel',
                  //         child: const Text(
                  //           '',
                  //           style: TextStyle(
                  //             fontSize: 20,
                  //             fontWeight: FontWeight.w500,
                  //           ),
                  //         ),
                  //       ),
                  //       subtitle: I18nText('settingsView.sentryHint'),
                  //       value: model.isSentryEnabled(),
                  //       onTap: (value) => model.useSentry(value),
                  //     ),
                  //   ],
                  // ),
                  // _settingsDivider,
                  SettingsSection(
                    title: 'settingsView.infoSectionTitle',
                    children: <Widget>[
                      ListTile(
                        contentPadding:
                            const EdgeInsets.symmetric(horizontal: 20.0),
                        title: I18nText(
                          'settingsView.logsLabel',
                          child: const Text(
                            '',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                        subtitle: I18nText('settingsView.logsHint'),
                        onTap: () => model.exportLogcatLogs(),
                      ),
                      const AboutWidget(
                        padding: EdgeInsets.symmetric(horizontal: 20.0),
                      ),
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
