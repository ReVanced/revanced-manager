import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/homeView/available_updates_card.dart';
import 'package:revanced_manager/ui/widgets/homeView/installed_apps_card.dart';
import 'package:revanced_manager/ui/widgets/homeView/latest_commit_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_chip.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class HomeView extends StatelessWidget {
  const HomeView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<HomeViewModel>.reactive(
      disposeViewModel: false,
      onModelReady: (model) => model.initialize(),
      viewModelBuilder: () => locator<HomeViewModel>(),
      builder: (context, model, child) => Scaffold(
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              title: I18nText(
                'homeView.widgetTitle',
                child: Text(
                  '',
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    I18nText(
                      'homeView.updatesSubtitle',
                      child: Text(
                        '',
                        style: Theme.of(context).textTheme.headline6!,
                      ),
                    ),
                    const SizedBox(height: 10),
                    LatestCommitCard(
                      onPressed: () =>
                          model.showUpdateConfirmationDialog(context),
                    ),
                    const SizedBox(height: 23),
                    I18nText(
                      'homeView.patchedSubtitle',
                      child: Text(
                        '',
                        style: Theme.of(context).textTheme.headline6!,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: <Widget>[
                        DashboardChip(
                          label: I18nText('homeView.updatesAvailable'),
                          isSelected: model.showUpdatableApps,
                          onSelected: (value) {
                            model.toggleUpdatableApps(true);
                          },
                        ),
                        const SizedBox(width: 10),
                        DashboardChip(
                          label: I18nText('homeView.installed'),
                          isSelected: !model.showUpdatableApps,
                          onSelected: (value) {
                            model.toggleUpdatableApps(false);
                          },
                        )
                      ],
                    ),
                    const SizedBox(height: 14),
                    model.showUpdatableApps
                        ? AvailableUpdatesCard()
                        : InstalledAppsCard(),
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
