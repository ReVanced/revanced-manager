import 'package:flutter/material.dart';

/// Dark car-headunit theme tuned for mid-range Android devices.
///
/// Uses system fonts (no Google Fonts runtime fetch) to avoid startup lag
/// and extra network/CPU on devices like Galaxy A6+ (Android 10).
class CarTheme {
  static const Color bgPrimary = Color(0xFF07090E);
  static const Color bgSurface = Color(0xFF10141E);
  static const Color bgCard = Color(0xFF181F2E);
  static const Color bgCardHover = Color(0xFF222B3D);
  static const Color accentRed = Color(0xFFFF1744);
  static const Color accentRedGlow = Color(0x66FF1744);
  static const Color accentGreen = Color(0xFF00E676);
  static const Color textMain = Color(0xFFF8FAFC);
  static const Color textMuted = Color(0xFFCBD5E1);

  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      scaffoldBackgroundColor: bgPrimary,
      primaryColor: accentRed,
      colorScheme: const ColorScheme.dark(
        primary: accentRed,
        surface: bgSurface,
      ),
      fontFamily: 'sans-serif',
      textTheme: ThemeData.dark().textTheme.apply(
            bodyColor: textMain,
            displayColor: textMain,
          ),
      cardTheme: CardThemeData(
        color: bgCard,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: Colors.white10),
        ),
      ),
    );
  }
}
