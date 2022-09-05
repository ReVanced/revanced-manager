import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

var lightCustomTheme = ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(
    seedColor: Colors.blue,
    brightness: Brightness.light,
  ),
  textTheme: GoogleFonts.robotoTextTheme(ThemeData.light().textTheme),
);

var darkCustomTheme = ThemeData(
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
