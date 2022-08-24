import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
// ignore: depend_on_referenced_packages
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/main_viewmodel.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/home/home_view.dart';
import 'package:revanced_manager/ui/views/patcher/patcher_view.dart';
import 'package:revanced_manager/ui/views/root_checker/root_checker_view.dart';
import 'package:revanced_manager/ui/views/settings/settings_view.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:stacked_themes/stacked_themes.dart';

Future main() async {
  await ThemeManager.initialise();
  await setupLocator();
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ThemeBuilder(
      defaultThemeMode: ThemeMode.dark,
      darkTheme: darkTheme,
      lightTheme: lightTheme,
      builder: (context, regularTheme, darkTheme, themeMode) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'ReVanced Manager',
        theme: lightTheme,
        darkTheme: darkTheme,
        themeMode: themeMode,
        navigatorKey: StackedService.navigatorKey,
        onGenerateRoute: StackedRouter().onGenerateRoute,
        home: FutureBuilder<Widget>(
          future: _init(),
          builder: (context, snapshot) {
            if (snapshot.hasData) {
              return snapshot.data!;
            } else {
              return Center(
                child: CircularProgressIndicator(
                  color: Theme.of(context).colorScheme.secondary,
                ),
              );
            }
          },
        ),
        localizationsDelegates: [
          FlutterI18nDelegate(
            translationLoader: FileTranslationLoader(
              fallbackFile: 'en',
              basePath: 'assets/i18n',
            ),
          ),
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate
        ],
      ),
    );
  }

  Future<Widget> _init() async {
    await locator<ManagerAPI>().initialize();
    bool? isRooted = locator<ManagerAPI>().isRooted();
    if (isRooted != null) {
      return const Navigation();
    }
    return const RootCheckerView();
  }
}

class Navigation extends StatelessWidget {
  const Navigation({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<MainViewModel>.reactive(
      viewModelBuilder: () => locator<MainViewModel>(),
      builder: (context, model, child) => Scaffold(
        body: getViewForIndex(model.currentIndex),
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

  Widget getViewForIndex(int index) {
    switch (index) {
      case 0:
        return const HomeView();
      case 1:
        return const PatcherView();
      case 2:
        return SettingsView();
      default:
        return const HomeView();
    }
  }
}
