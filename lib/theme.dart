import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:stacked_themes/stacked_themes.dart';

import 'app/app.locator.dart';

final _themeService = locator<ThemeService>();
bool isDark = _themeService.isDarkMode;

var lightTheme = ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(
    seedColor: Colors.blue,
    brightness: Brightness.light,
  ),
  textTheme: GoogleFonts.robotoTextTheme(ThemeData.light().textTheme),
);

var darkTheme = ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(
    seedColor: Colors.blue,
    brightness: Brightness.dark,
    primary: const Color(0xff7792BA),
    surface: const Color(0xff0A0D11),
  ),
  canvasColor: const Color(0xff0A0D11),
  scaffoldBackgroundColor: const Color(0xff0A0D11),
  toggleableActiveColor: const Color(0xff7792BA),
  textTheme: GoogleFonts.robotoTextTheme(ThemeData.dark().textTheme),
);
