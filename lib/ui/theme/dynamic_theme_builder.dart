import 'package:dynamic_color/dynamic_color.dart';
import 'package:dynamic_themes/dynamic_themes.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.router.dart';
import 'package:revanced_manager/theme.dart';
import 'package:stacked_services/stacked_services.dart';

class DynamicThemeBuilder extends StatelessWidget {
  final String title;
  final Widget home;
  final Iterable<LocalizationsDelegate> localizationsDelegates;

  const DynamicThemeBuilder({
    Key? key,
    required this.title,
    required this.home,
    required this.localizationsDelegates,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(
      builder: (lightColorScheme, darkColorScheme) {
        ThemeData lightDynamicTheme = ThemeData(
          useMaterial3: true,
          canvasColor: lightColorScheme?.background,
          navigationBarTheme: NavigationBarThemeData(
            backgroundColor: lightColorScheme?.background,
            indicatorColor: lightColorScheme?.primary.withAlpha(150),
            labelTextStyle: MaterialStateProperty.all(
              GoogleFonts.roboto(
                color: lightColorScheme?.secondary,
                fontWeight: FontWeight.w500,
              ),
            ),
            iconTheme: MaterialStateProperty.all(
              IconThemeData(
                color: lightColorScheme?.secondary,
              ),
            ),
          ),
          scaffoldBackgroundColor: lightColorScheme?.background,
          colorScheme: lightColorScheme?.harmonized(),
          toggleableActiveColor: lightColorScheme?.primary,
          textTheme: GoogleFonts.robotoTextTheme(ThemeData.light().textTheme),
        );
        ThemeData darkDynamicTheme = ThemeData(
          useMaterial3: true,
          canvasColor: darkColorScheme?.background,
          navigationBarTheme: NavigationBarThemeData(
            backgroundColor: darkColorScheme?.background,
            indicatorColor: darkColorScheme?.primary.withOpacity(0.4),
            labelTextStyle: MaterialStateProperty.all(
              GoogleFonts.roboto(
                color: darkColorScheme?.secondary,
                fontWeight: FontWeight.w500,
              ),
            ),
            iconTheme: MaterialStateProperty.all(
              IconThemeData(
                color: darkColorScheme?.secondary,
              ),
            ),
          ),
          scaffoldBackgroundColor: darkColorScheme?.background,
          colorScheme: darkColorScheme?.harmonized(),
          toggleableActiveColor: darkColorScheme?.primary,
          textTheme: GoogleFonts.robotoTextTheme(ThemeData.dark().textTheme),
        );
        return DynamicTheme(
          themeCollection: ThemeCollection(
            themes: {
              0: lightCustomTheme,
              1: darkCustomTheme,
              2: lightDynamicTheme,
              3: darkDynamicTheme,
            },
            fallbackTheme: darkCustomTheme,
          ),
          builder: (context, theme) => MaterialApp(
            debugShowCheckedModeBanner: false,
            title: title,
            navigatorKey: StackedService.navigatorKey,
            onGenerateRoute: StackedRouter().onGenerateRoute,
            theme: theme,
            home: home,
            localizationsDelegates: localizationsDelegates,
          ),
        );
      },
    );
  }
}
