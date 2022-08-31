import 'package:flutter/material.dart';
import 'package:revanced_manager/ui/views/contributors/contributors_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/contributorsView/contributors_card.dart';
import 'package:stacked/stacked.dart';

class ContributorsView extends StatelessWidget {
  const ContributorsView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<ContributorsViewModel>.reactive(
      viewModelBuilder: () => ContributorsViewModel(),
      onModelReady: (model) => model.getContributors(),
      builder: (context, model, child) => Scaffold(
        body: SafeArea(
          child: SingleChildScrollView(
            child: Column(
              children: [
                ContributorsCard(
                  title: 'Patcher Contributors',
                  contributors: model.patcherContributors,
                  height: 60,
                ),
                ContributorsCard(
                  title: 'Patches Contributors',
                  contributors: model.patchesContributors,
                  height: 230,
                ),
                ContributorsCard(
                  title: 'Integrations Contributors',
                  contributors: model.integrationsContributors,
                  height: 230,
                ),
                ContributorsCard(
                  title: 'CLI Contributors',
                  contributors: model.cliContributors,
                  height: 180,
                ),
                ContributorsCard(
                  title: 'Manager Contributors',
                  contributors: model.managerContributors,
                  height: 130,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
