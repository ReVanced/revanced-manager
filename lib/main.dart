import 'package:flutter/material.dart';
import 'package:revanced_manager_flutter/app/app.locator.dart';
import 'package:revanced_manager_flutter/app/app.router.dart';
import 'package:revanced_manager_flutter/main_viewmodel.dart';
import 'package:revanced_manager_flutter/theme.dart';
import 'package:revanced_manager_flutter/ui/views/home/home_view.dart';
import 'package:revanced_manager_flutter/ui/views/patcher/patcher_view.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  setupLocator();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'ReVanced Manager',
      theme: lightTheme,
      darkTheme: darkTheme,
      navigatorKey: StackedService.navigatorKey,
      onGenerateRoute: StackedRouter().onGenerateRoute,
      home: const Navigation(),
    );
  }
}

class Navigation extends StatelessWidget {
  const Navigation({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<MainViewModel>.reactive(
      viewModelBuilder: () => MainViewModel(),
      builder: (context,MainViewModel model, child) => Scaffold(
        body: getViewForIndex(model.currentIndex),
        bottomNavigationBar: NavigationBar(
          onDestinationSelected: model.setIndex,
          selectedIndex: model.currentIndex,
          destinations: const <Widget>[
            NavigationDestination(
              icon: Icon(Icons.dashboard),
              label: "Dashboard",
            ),
            NavigationDestination(
              icon: Icon(Icons.build),
              label: "Patcher",
            ),
          ],
        ),
      ),
    );
  }
  Widget getViewForIndex(int index) {
    switch (index) {
      case 0:
        return const HomeView();
      case 1:
        return const PatcherView();
      default:
        return const HomeView();
    }
  }
}
