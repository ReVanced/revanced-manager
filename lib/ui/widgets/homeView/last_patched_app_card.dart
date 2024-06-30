import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/application_item.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

//ignore: must_be_immutable
class LastPatchedAppCard extends StatelessWidget {
  LastPatchedAppCard({super.key});
  PatchedApplication? app = locator<HomeViewModel>().lastPatchedApp;

  @override
  Widget build(BuildContext context) {
    return app == null
      ? CustomCard(
          child: Center(
            child: Column(
              children: <Widget>[
                Icon(
                  size: 40,
                  Icons.update_disabled,
                  color: Theme.of(context).colorScheme.secondary,
                ),
                const SizedBox(height: 16),
                Text(
                  t.homeView.noSavedAppFound,
                  style: Theme.of(context)
                      .textTheme
                      .titleMedium!
                      .copyWith(
                        color:
                            Theme.of(context).colorScheme.secondary,
                      ),
                ),
              ],
            ),
          ),
        )
      : ApplicationItem(
        icon: app!.icon,
        name: app!.name,
        patchDate: app!.patchDate,
        onPressed: () =>
            locator<HomeViewModel>().navigateToAppInfo(app!, true),
      );
  }
}
