import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/homeView/available_updates_card.dart';
import 'package:revanced_manager/ui/widgets/homeView/dashboard_raw_chip.dart';
import 'package:revanced_manager/ui/widgets/homeView/installed_apps_card.dart';
import 'package:revanced_manager/ui/widgets/homeView/latest_commit_card.dart';
import 'package:stacked/stacked.dart';

class HomeView extends StatelessWidget {
  const HomeView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<HomeViewModel>.reactive(
      disposeViewModel: false,
      fireOnModelReadyOnce: true,
      onModelReady: (model) => model.initialize(),
      viewModelBuilder: () => locator<HomeViewModel>(),
      builder: (context, model, child) => Scaffold(
        body: CustomScrollView(
          slivers: <Widget>[
            SliverAppBar(
              pinned: true,
              snap: false,
              floating: false,
              expandedHeight: 100.0,
              automaticallyImplyLeading: false,
              backgroundColor: MaterialStateColor.resolveWith(
                (states) => states.contains(MaterialState.scrolledUnder)
                    ? isDark
                        ? Theme.of(context).colorScheme.primary
                        : Theme.of(context).navigationBarTheme.backgroundColor!
                    : Theme.of(context).scaffoldBackgroundColor,
              ),
              flexibleSpace: FlexibleSpaceBar(
                titlePadding: const EdgeInsets.symmetric(
                  vertical: 23.0,
                  horizontal: 20.0,
                ),
                title: I18nText(
                  'homeView.widgetTitle',
                  child: Text(
                    '',
                    style: GoogleFonts.inter(
                      color: Theme.of(context).textTheme.headline5!.color,
                      fontWeight: FontWeight.w500,
                    ),
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
                        style: GoogleFonts.inter(
                          fontSize: 20,
                          fontWeight: FontWeight.w500,
                          color: isDark
                              ? const Color(0xffD1E1FA)
                              : const Color(0xff384E6E),
                        ),
                      ),
                    ),
                    const SizedBox(height: 10),
                    LatestCommitCard(
                      onPressed: () => model.updateManager(context),
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(height: 23),
                    I18nText(
                      'homeView.patchedSubtitle',
                      child: Text(
                        '',
                        style: GoogleFonts.inter(
                          fontSize: 20,
                          color: isDark
                              ? const Color(0xffD1E1FA)
                              : const Color(0xff384E6E),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        DashboardChip(
                          label: "homeView.updatesAvailable",
                          isSelected: model.showUpdatableApps,
                          onSelected: (value) {
                            model.toggleUpdatableApps(true);
                          },
                        ),
                        const SizedBox(width: 10),
                        DashboardChip(
                          label: "homeView.installed",
                          isSelected: !model.showUpdatableApps,
                          onSelected: (value) {
                            model.toggleUpdatableApps(false);
                          },
                        )
                      ],
                    ),
                    const SizedBox(height: 14),
                    model.showUpdatableApps
                        ? const AvailableUpdatesCard()
                        : const InstalledAppsCard()
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
