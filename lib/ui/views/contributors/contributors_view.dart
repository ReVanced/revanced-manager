import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/contributors/contributors_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/contributorsView/contributors_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class ContributorsView extends StatelessWidget {
  const ContributorsView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<ContributorsViewModel>.reactive(
      viewModelBuilder: () => ContributorsViewModel(),
      onModelReady: (model) => model.getContributors(),
      builder: (context, model, child) => Scaffold(
        body: CustomScrollView(
          slivers: <Widget>[
            CustomSliverAppBar(
              title: I18nText(
                'contributorsView.widgetTitle',
                child: Text(
                  '',
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.headline6!.color,
                  ),
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.all(20.0),
              sliver: SliverList(
                delegate: SliverChildListDelegate.fixed(
                  <Widget>[
                    ContributorsCard(
                      title: 'contributorsView.patcherContributors',
                      contributors: model.patcherContributors,
                    ),
                    const SizedBox(height: 20),
                    ContributorsCard(
                      title: 'contributorsView.patchesContributors',
                      contributors: model.patchesContributors,
                    ),
                    const SizedBox(height: 20),
                    ContributorsCard(
                      title: 'contributorsView.integrationsContributors',
                      contributors: model.integrationsContributors,
                    ),
                    const SizedBox(height: 20),
                    ContributorsCard(
                      title: 'contributorsView.cliContributors',
                      contributors: model.cliContributors,
                    ),
                    const SizedBox(height: 20),
                    ContributorsCard(
                      title: 'contributorsView.managerContributors',
                      contributors: model.managerContributors,
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
