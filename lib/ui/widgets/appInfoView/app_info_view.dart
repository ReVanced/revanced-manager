import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/ui/widgets/appInfoView/app_info_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class AppInfoView extends StatelessWidget {
  final PatchedApplication app;

  const AppInfoView({
    Key? key,
    required this.app,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<AppInfoViewModel>.reactive(
      viewModelBuilder: () => AppInfoViewModel(),
      builder: (context, model, child) => Scaffold(
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              title: I18nText(
                'appInfoView.widgetTitle',
                child: Text(
                  '',
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(vertical: 20.0),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    SizedBox(
                      height: 64.0,
                      child: CircleAvatar(
                        backgroundColor: Colors.transparent,
                        child: Image.memory(
                          app.icon,
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    Text(
                      app.name,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.headline6,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      app.version,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.subtitle1,
                    ),
                    const SizedBox(height: 20),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20.0),
                      child: CustomCard(
                        padding: EdgeInsets.zero,
                        child: SizedBox(
                          height: 94.0,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: <Widget>[
                              Expanded(
                                child: Material(
                                  type: MaterialType.transparency,
                                  child: InkWell(
                                    borderRadius: BorderRadius.circular(16.0),
                                    onTap: () => model.openApp(app),
                                    child: Column(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: <Widget>[
                                        Icon(
                                          Icons.open_in_new_outlined,
                                          color: Theme.of(context)
                                              .colorScheme
                                              .primary,
                                        ),
                                        const SizedBox(height: 10),
                                        I18nText(
                                          'appInfoView.openButton',
                                          child: Text(
                                            '',
                                            style: TextStyle(
                                              color: Theme.of(context)
                                                  .colorScheme
                                                  .primary,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ),
                              VerticalDivider(
                                color: Theme.of(context).canvasColor,
                                indent: 12.0,
                                endIndent: 12.0,
                                width: 1.0,
                              ),
                              Expanded(
                                child: Material(
                                  type: MaterialType.transparency,
                                  child: InkWell(
                                    borderRadius: BorderRadius.circular(16.0),
                                    onTap: () => model.showUninstallDialog(
                                      context,
                                      app,
                                      false,
                                    ),
                                    child: Column(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: <Widget>[
                                        Icon(
                                          Icons.delete_outline,
                                          color: Theme.of(context)
                                              .colorScheme
                                              .primary,
                                        ),
                                        const SizedBox(height: 10),
                                        I18nText(
                                          'appInfoView.uninstallButton',
                                          child: Text(
                                            '',
                                            style: TextStyle(
                                              color: Theme.of(context)
                                                  .colorScheme
                                                  .primary,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ),
                              VerticalDivider(
                                color: Theme.of(context).canvasColor,
                                indent: 12.0,
                                endIndent: 12.0,
                                width: 1.0,
                              ),
                              Expanded(
                                child: Material(
                                  type: MaterialType.transparency,
                                  child: InkWell(
                                    borderRadius: BorderRadius.circular(16.0),
                                    onTap: () {
                                      model.navigateToPatcher(app);
                                      Navigator.of(context).pop();
                                    },
                                    child: Column(
                                      mainAxisAlignment:
                                          MainAxisAlignment.center,
                                      children: <Widget>[
                                        Icon(
                                          Icons.build_outlined,
                                          color: Theme.of(context)
                                              .colorScheme
                                              .primary,
                                        ),
                                        const SizedBox(height: 10),
                                        I18nText(
                                          'appInfoView.patchButton',
                                          child: Text(
                                            '',
                                            style: TextStyle(
                                              color: Theme.of(context)
                                                  .colorScheme
                                                  .primary,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ),
                              if (app.isRooted)
                                VerticalDivider(
                                  color: Theme.of(context).canvasColor,
                                  indent: 12.0,
                                  endIndent: 12.0,
                                  width: 1.0,
                                ),
                              if (app.isRooted)
                                Expanded(
                                  child: Material(
                                    type: MaterialType.transparency,
                                    child: InkWell(
                                      borderRadius: BorderRadius.circular(16.0),
                                      onTap: () => model.showUninstallDialog(
                                        context,
                                        app,
                                        true,
                                      ),
                                      child: Column(
                                        mainAxisAlignment:
                                            MainAxisAlignment.center,
                                        children: <Widget>[
                                          Icon(
                                            Icons
                                                .settings_backup_restore_outlined,
                                            color: Theme.of(context)
                                                .colorScheme
                                                .primary,
                                          ),
                                          const SizedBox(height: 10),
                                          I18nText(
                                            'appInfoView.unpatchButton',
                                            child: Text(
                                              '',
                                              style: TextStyle(
                                                color: Theme.of(context)
                                                    .colorScheme
                                                    .primary,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    ListTile(
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 20.0),
                      title: I18nText(
                        'appInfoView.packageNameLabel',
                        child: const Text(
                          '',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      subtitle: Text(app.packageName),
                    ),
                    const SizedBox(height: 4),
                    ListTile(
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 20.0),
                      title: I18nText(
                        'appInfoView.installTypeLabel',
                        child: const Text(
                          '',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      subtitle: app.isRooted
                          ? I18nText('appInfoView.rootTypeLabel')
                          : I18nText('appInfoView.nonRootTypeLabel'),
                    ),
                    const SizedBox(height: 4),
                    ListTile(
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 20.0),
                      title: I18nText(
                        'appInfoView.patchedDateLabel',
                        child: const Text(
                          '',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      subtitle: I18nText(
                        'appInfoView.patchedDateHint',
                        translationParams: {
                          'date': model.getPrettyDate(context, app.patchDate),
                          'time': model.getPrettyTime(context, app.patchDate),
                        },
                      ),
                    ),
                    const SizedBox(height: 4),
                    ListTile(
                      contentPadding:
                          const EdgeInsets.symmetric(horizontal: 20.0),
                      title: I18nText(
                        'appInfoView.appliedPatchesLabel',
                        child: const Text(
                          '',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                      subtitle: I18nText(
                        'appInfoView.appliedPatchesHint',
                        translationParams: {
                          'quantity': app.appliedPatches.length.toString(),
                        },
                      ),
                      onTap: () => model.showAppliedPatchesDialog(context, app),
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
