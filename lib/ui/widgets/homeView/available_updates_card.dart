import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class AvailableUpdatesCard extends StatelessWidget {
  AvailableUpdatesCard({Key? key}) : super(key: key);

  final List<PatchedApplication> apps =
      locator<HomeViewModel>().patchedUpdatableApps;

  @override
  Widget build(BuildContext context) {
    return CustomCard(
      child: Center(
        child: Column(
          children: <Widget>[
            Icon(
              size: 40,
              Icons.update_disabled,
              color: Theme.of(context).colorScheme.secondary,
            ),
            const SizedBox(height: 16),
            I18nText(
              'homeView.WIP',
              child: Text(
                '',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.subtitle1!.copyWith(
                      color: Theme.of(context).colorScheme.secondary,
                    ),
              ),
            )
          ],
        ),
      ),
    );
    // return apps.isEmpty
    //     ? CustomCard(
    //         child: Center(
    //           child: Column(
    //             children: <Widget>[
    //               Icon(
    //                 size: 40,
    //                 Icons.update_disabled,
    //                 color: Theme.of(context).colorScheme.secondary,
    //               ),
    //               const SizedBox(height: 16),
    //               I18nText(
    //                 'homeView.noUpdates',
    //                 child: Text(
    //                   '',
    //                   textAlign: TextAlign.center,
    //                   style: Theme.of(context).textTheme.subtitle1!.copyWith(
    //                         color: Theme.of(context).colorScheme.secondary,
    //                       ),
    //                 ),
    //               )
    //             ],
    //           ),
    //         ),
    //       )
    //     : ListView(
    //         shrinkWrap: true,
    //         padding: EdgeInsets.zero,
    //         physics: const NeverScrollableScrollPhysics(),
    //         children: apps
    //             .map((app) => ApplicationItem(
    //                 icon: app.icon,
    //                 name: app.name,
    //                 patchDate: app.patchDate,
    //                 changelog: app.changelog,
    //                 isUpdatableApp: true,
    //                 //TODO: Find a better way to do update functionality
    //                 onPressed: () {}
    //                 // () =>
    //                 //     locator<HomeViewModel>().navigateToPatcher(
    //                 //   app,
    //                 // ),
    //                 ))
    //             .toList(),
    //       );
  }
}
