import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/patcher_api.dart';
import 'package:revanced_manager/services/revanced_api.dart';
import 'package:revanced_manager/ui/theme/dynamic_theme_builder.dart';
import 'package:revanced_manager/ui/views/navigation/navigation_view.dart';
import 'package:revanced_manager/utils/environment.dart';
import 'package:sentry_flutter/sentry_flutter.dart';
import 'package:timezone/data/latest.dart' as tz;

Future main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await setupLocator();
  final manager = locator<ManagerAPI>();
  await manager.initialize();
  final String apiUrl = manager.getApiUrl();
  final bool isSentryEnabled = manager.isSentryEnabled();
  final String repoUrl = manager.getRepoUrl();

  await Future.wait([
    locator<RevancedAPI>().initialize(apiUrl),
    locator<PatcherAPI>().initialize(),
  ]);
  locator<GithubAPI>().initialize(repoUrl);
  tz.initializeTimeZones();

  return SentryFlutter.init(
    (options) {
      options
        ..dsn = isSentryEnabled ? Environment.sentryDSN : ''
        ..environment = 'alpha'
        ..release = '0.1'
        ..tracesSampleRate = 1.0
        ..anrEnabled = true
        ..enableOutOfMemoryTracking = true
        ..sampleRate = isSentryEnabled ? 1.0 : 0.0
        ..beforeSend = (event, hint) {
          if (isSentryEnabled) {
            return event;
          } else {
            return null;
          }
        } as BeforeSendCallback?;
    },
    appRunner: () => runApp(const MyApp()),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // String rawLocale = prefs.getString('language') ?? 'en_US';
    // String replaceLocale = rawLocale.replaceAll('_', '-');
    // List<String> localeList = replaceLocale.split('-');
    // Locale locale = Locale(localeList[0], localeList[1]);
    const Locale locale = Locale('en', 'US');

    return DynamicThemeBuilder(
      title: 'ReVanced Manager',
      home: const NavigationView(),
      localizationsDelegates: [
        FlutterI18nDelegate(
          translationLoader: FileTranslationLoader(
            fallbackFile: 'en_US',
            forcedLocale: locale,
            basePath: 'assets/i18n',
            useCountryCode: true,
          ),
          missingTranslationHandler: (key, locale) {
            log(
              '--> Missing translation: key: $key, languageCode: ${locale?.languageCode}',
            );
          },
        ),
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate
      ],
    );
  }
}
