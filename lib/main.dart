import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/services/download_manager.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/services/root_api.dart';
import 'package:revanced_manager/ui/theme/dynamic_theme_builder.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_view.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timezone/data/latest.dart' as tz;

late SharedPreferences prefs;
Future main() async {
  await setupLocator();
  WidgetsFlutterBinding.ensureInitialized();
  await locator<ManagerAPI>().initialize();

  await locator<DownloadManager>().initialize();
  final String apiUrl = locator<ManagerAPI>().getApiUrl();
  await locator<RevancedAPI>().initialize(apiUrl);
  final String repoUrl = locator<ManagerAPI>().getRepoUrl();
  locator<GithubAPI>().initialize(repoUrl);
  tz.initializeTimeZones();

  // TODO(aAbed): remove in the future, keep it for now during migration.
  final rootAPI = RootAPI();
  if (await rootAPI.hasRootPermissions()) {
    await rootAPI.removeOrphanedFiles();
  }

  prefs = await SharedPreferences.getInstance();

  final managerAPI = locator<ManagerAPI>();
  final locale = managerAPI.getLocale();
  LocaleSettings.setLocaleRaw(locale);

  runApp(TranslationProvider(child: const MyApp()));
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const DynamicThemeBuilder(
      title: 'ReVanced Manager',
      home: NavigationView(),
    );
  }
}
