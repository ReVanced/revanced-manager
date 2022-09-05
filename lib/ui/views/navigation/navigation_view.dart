import 'package:animations/animations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_viewmodel.dart';
import 'package:stacked/stacked.dart';

class NavigationView extends StatelessWidget {
  const NavigationView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<NavigationViewModel>.reactive(
      viewModelBuilder: () => locator<NavigationViewModel>(),
      builder: (context, model, child) => Scaffold(
        body: PageTransitionSwitcher(
          duration: const Duration(milliseconds: 400),
          transitionBuilder: (
            Widget child,
            Animation<double> animation,
            Animation<double> secondaryAnimation,
          ) {
            return FadeThroughTransition(
              animation: animation,
              secondaryAnimation: secondaryAnimation,
              fillColor: Theme.of(context).colorScheme.surface,
              child: child,
            );
          },
          child: model.getViewForIndex(model.currentIndex),
        ),
        bottomNavigationBar: NavigationBar(
          onDestinationSelected: model.setIndex,
          selectedIndex: model.currentIndex,
          destinations: <Widget>[
            NavigationDestination(
              icon: const Icon(
                Icons.dashboard,
              ),
              label: FlutterI18n.translate(
                context,
                'main.dashboardTab',
              ),
            ),
            NavigationDestination(
              icon: const Icon(Icons.build),
              label: FlutterI18n.translate(
                context,
                'main.patcherTab',
              ),
            ),
            NavigationDestination(
              icon: const Icon(Icons.settings),
              label: FlutterI18n.translate(
                context,
                'main.settingsTab',
              ),
            ),
          ],
        ),
      ),
    );
  }
}
