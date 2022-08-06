import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/constants.dart';

var lightTheme = ThemeData.light().copyWith(
  navigationBarTheme: NavigationBarThemeData(
    labelTextStyle: MaterialStateProperty.all(
      GoogleFonts.roboto(
        fontSize: 12,
      ),
    ),
  ),
  backgroundColor: Colors.red,
  useMaterial3: true,
  colorScheme: const ColorScheme.light(
    primary: purple40,
    secondary: purpleGrey40,
    tertiary: pink40,
    background: Colors.red,
  ),
);

var darkTheme = ThemeData.dark().copyWith(
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
  backgroundColor: Colors.red,
  useMaterial3: true,
  scaffoldBackgroundColor: const Color(0xff0A0D11),
  colorScheme: const ColorScheme.dark(
    primary: purple80,
    secondary: purpleGrey80,
    tertiary: pink80,
    background: Colors.red,
  ),
);
