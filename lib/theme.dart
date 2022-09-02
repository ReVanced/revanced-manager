import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:stacked_themes/stacked_themes.dart';

import 'app/app.locator.dart';

final _themeService = locator<ThemeService>();
bool isDark = _themeService.isDarkMode;

var lightTheme = ThemeData.light().copyWith(
  backgroundColor: const Color.fromARGB(255, 243, 243, 243),
  navigationBarTheme: NavigationBarThemeData(
    indicatorColor: const Color.fromRGBO(75, 129, 210, 0.20),
    backgroundColor: const Color(0xffCBDFFC),
    labelTextStyle: MaterialStateProperty.all(
      GoogleFonts.roboto(
        fontSize: 12,
      ),
    ),
  ),
  useMaterial3: true,
  textButtonTheme: TextButtonThemeData(
    style: ButtonStyle(
      padding: MaterialStateProperty.all<EdgeInsetsGeometry>(
        const EdgeInsets.symmetric(
          vertical: 8,
          horizontal: 14,
        ),
      ),
      backgroundColor: MaterialStateProperty.all<Color>(
        const Color(0xff4B7CC6),
      ),
    ),
  ),
  toggleableActiveColor: const Color(0xff3868AF),
  colorScheme: const ColorScheme.light(
    primary: Color.fromRGBO(154, 193, 252, 0.18),
    secondary: Color(0xff3868AF),
    tertiary: Color(0xff485A74),
    background: Color(0xffDFD5EC),
  ),
);

var darkTheme = ThemeData.dark().copyWith(
  backgroundColor: const Color(0xff1E1E1E),
  navigationBarTheme: NavigationBarThemeData(
    iconTheme: MaterialStateProperty.all(const IconThemeData(
      color: Colors.white,
    )),
    indicatorColor: const Color(0xff223144),
    backgroundColor: const Color(0x1b222b6b),
    labelTextStyle: MaterialStateProperty.all(
      GoogleFonts.roboto(
        fontSize: 12,
      ),
    ),
  ),
  useMaterial3: true,
  scaffoldBackgroundColor: const Color(0xff0A0D11),
  textButtonTheme: TextButtonThemeData(
    style: ButtonStyle(
      padding: MaterialStateProperty.all<EdgeInsetsGeometry>(
        const EdgeInsets.symmetric(
          vertical: 8,
          horizontal: 12,
        ),
      ),
      backgroundColor: MaterialStateProperty.all<Color>(
        const Color.fromRGBO(119, 146, 168, 1),
      ),
    ),
  ),
  toggleableActiveColor: const Color(0xff7792BA),
  colorScheme: const ColorScheme.dark(
    primary: Color(0xff11161C),
    secondary: Color(0xff7792BA),
    tertiary: Color(0xff8691A0),
    background: Color(0xff0A0D11),
  ),
);
