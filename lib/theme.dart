import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

var lightCustomColorScheme = ColorScheme.fromSeed(
  seedColor: Colors.pink,
  primary: const Color(0xFFB31869),
);

var lightCustomTheme = ThemeData(
  useMaterial3: true,
  colorScheme: lightCustomColorScheme,
  navigationBarTheme: NavigationBarThemeData(
    labelTextStyle: MaterialStateProperty.all(
      TextStyle(
        color: lightCustomColorScheme.onSurface,
        fontWeight: FontWeight.w500,
      ),
    ),
  ),
  textTheme: GoogleFonts.robotoTextTheme(ThemeData.light().textTheme),
);

var darkCustomColorScheme = ColorScheme.fromSeed(
  seedColor: Colors.pink,
  brightness: Brightness.dark,
  primary: const Color(0xFFFFB0CB),
  surface: const Color(0xFF171213),
);

var darkCustomTheme = ThemeData(
  useMaterial3: true,
  colorScheme: darkCustomColorScheme,
  navigationBarTheme: NavigationBarThemeData(
    labelTextStyle: MaterialStateProperty.all(
      TextStyle(
        color: darkCustomColorScheme.onSurface,
        fontWeight: FontWeight.w500,
      ),
    ),
  ),
  canvasColor: const Color(0xFF171213),
  scaffoldBackgroundColor: const Color(0xFF171213),
  textTheme: GoogleFonts.robotoTextTheme(ThemeData.dark().textTheme),
);
