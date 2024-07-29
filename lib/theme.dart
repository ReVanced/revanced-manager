import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

var lightCustomColorScheme = ColorScheme.fromSeed(
  seedColor: Colors.purple,
  primary: const Color(0xFF4C51C0),
);

var lightCustomTheme = ThemeData(
  useMaterial3: true,
  colorScheme: lightCustomColorScheme,
  navigationBarTheme: NavigationBarThemeData(
    labelTextStyle: WidgetStateProperty.all(
      TextStyle(
        color: lightCustomColorScheme.onSurface,
        fontWeight: FontWeight.w500,
      ),
    ),
  ),
  textTheme: GoogleFonts.robotoTextTheme(ThemeData.light().textTheme),
);

var darkCustomColorScheme = ColorScheme.fromSeed(
  seedColor: Colors.purple,
  brightness: Brightness.dark,
  primary: const Color(0xFFBFC1FF),
  surface: const Color(0xFF131316),
);

var darkCustomTheme = ThemeData(
  useMaterial3: true,
  colorScheme: darkCustomColorScheme,
  navigationBarTheme: NavigationBarThemeData(
    labelTextStyle: WidgetStateProperty.all(
      TextStyle(
        color: darkCustomColorScheme.onSurface,
        fontWeight: FontWeight.w500,
      ),
    ),
  ),
  canvasColor: const Color(0xFF131316),
  scaffoldBackgroundColor: const Color(0xFF131316),
  textTheme: GoogleFonts.robotoTextTheme(ThemeData.dark().textTheme),
);
