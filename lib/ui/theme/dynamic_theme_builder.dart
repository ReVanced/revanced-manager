import 'dart:ui';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/theme.dart';
import 'package:stacked_services/stacked_services.dart';

class DynamicThemeBuilder extends StatefulWidget {
  const DynamicThemeBuilder({
    super.key,
    required this.title,
    required this.home,
  });
  final String title;
  final Widget home;

  @override
  State<DynamicThemeBuilder> createState() => _DynamicThemeBuilderState();
}

class _DynamicThemeBuilderState extends State<DynamicThemeBuilder>
    with WidgetsBindingObserver {
  late Brightness brightness;

  @override
  void initState() {
    super.initState();
    brightness = PlatformDispatcher.instance.platformBrightness;
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      final systemBrightness = PlatformDispatcher.instance.platformBrightness;
      if (brightness != systemBrightness) {
        brightness = systemBrightness;
        setState(() {});
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(
      builder: (lightColorScheme, darkColorScheme) {
        final ThemeData lightDynamicTheme = ThemeData(
          useMaterial3: true,
          navigationBarTheme: NavigationBarThemeData(
            labelTextStyle: WidgetStateProperty.all(
              GoogleFonts.roboto(
                color: lightColorScheme?.onSurface,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          colorScheme: lightColorScheme?.harmonized(),
          textTheme: GoogleFonts.robotoTextTheme(ThemeData.light().textTheme),
        );
        final ThemeData darkDynamicTheme = ThemeData(
          brightness: Brightness.dark,
          useMaterial3: true,
          navigationBarTheme: NavigationBarThemeData(
            labelTextStyle: WidgetStateProperty.all(
              GoogleFonts.roboto(
                color: darkColorScheme?.onSurface,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          colorScheme: darkColorScheme?.harmonized(),
          textTheme: GoogleFonts.robotoTextTheme(ThemeData.dark().textTheme),
        );
        return DynamicTheme(
          themeCollection: ThemeCollection(
            themes: {
              0: brightness == Brightness.light
                  ? lightCustomTheme
                  : darkCustomTheme,
              1: brightness == Brightness.light
                  ? lightDynamicTheme
                  : darkDynamicTheme,
              2: lightCustomTheme,
              3: lightDynamicTheme,
              4: darkCustomTheme,
              5: darkDynamicTheme,
            },
            fallbackTheme: PlatformDispatcher.instance.platformBrightness ==
                    Brightness.light
                ? lightCustomTheme
                : darkCustomTheme,
          ),
          builder: (context, theme) => MaterialApp(
            debugShowCheckedModeBanner: false,
            title: widget.title,
            navigatorKey: StackedService.navigatorKey,
            onGenerateRoute: StackedRouter().onGenerateRoute,
            theme: theme,
            home: widget.home,
            localizationsDelegates: GlobalMaterialLocalizations.delegates,
            locale: TranslationProvider.of(context).flutterLocale,
            supportedLocales: AppLocaleUtils.supportedLocales,
          ),
        );
      },
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }
}
