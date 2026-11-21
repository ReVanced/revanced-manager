import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Design tokens for the head-unit look: graphite surfaces, one calm accent, red only for
/// LIVE, one type scale and one radius family. Every screen takes its colors, text styles
/// and corner radii from here so the app reads as one product.
///
/// System font (Roboto on Android): no runtime font download, no startup cost on a
/// mid-range phone.
abstract final class CarTheme {
  // Surfaces, darkest to lightest.
  static const Color bg = Color(0xFF0B0D10);
  static const Color surface = Color(0xFF14171C);
  static const Color surfaceHigh = Color(0xFF1D2128);
  static const Color border = Color(0x17FFFFFF);

  static const Color text = Color(0xFFF2F4F7);
  static const Color textMuted = Color(0xFF9BA3AF);
  static const Color textFaint = Color(0xFF6B7280);

  /// The only accent: active menu item, primary buttons, progress.
  static const Color accent = Color(0xFF4C9EFF);
  static const Color accentSoft = Color(0x264C9EFF);
  static const Color onAccent = Color(0xFF06111F);

  /// Reserved for LIVE badges.
  static const Color live = Color(0xFFE5484D);

  /// SponsorBlock skip notice.
  static const Color positive = Color(0xFF3DD68C);

  static const double radiusSmall = 10;
  static const double radius = 16;
  static const double radiusLarge = 22;

  static const List<FontFeature> _tabular = [FontFeature.tabularFigures()];

  static const TextStyle clock = TextStyle(
    fontSize: 34,
    height: 1.0,
    fontWeight: FontWeight.w300,
    color: text,
    fontFeatures: _tabular,
  );
  static const TextStyle title = TextStyle(
    fontSize: 22,
    height: 1.2,
    fontWeight: FontWeight.w600,
    color: text,
  );
  static const TextStyle heading = TextStyle(
    fontSize: 17,
    height: 1.25,
    fontWeight: FontWeight.w600,
    color: text,
  );
  static const TextStyle body = TextStyle(
    fontSize: 15,
    height: 1.3,
    fontWeight: FontWeight.w500,
    color: text,
  );
  static const TextStyle caption = TextStyle(
    fontSize: 13,
    height: 1.3,
    fontWeight: FontWeight.w500,
    color: textMuted,
  );
  static const TextStyle label = TextStyle(
    fontSize: 12,
    height: 1.2,
    fontWeight: FontWeight.w600,
    color: textMuted,
  );
  static const TextStyle time = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w600,
    color: textMuted,
    fontFeatures: _tabular,
  );

  static ThemeData get darkTheme {
    const scheme = ColorScheme.dark(
      primary: accent,
      onPrimary: onAccent,
      secondary: accent,
      onSecondary: onAccent,
      surface: surface,
      onSurface: text,
      error: live,
    );
    final rounded = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(radius),
    );
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: scheme,
      scaffoldBackgroundColor: bg,
      canvasColor: bg,
      splashFactory: InkRipple.splashFactory,
      textTheme: ThemeData.dark().textTheme.apply(
            bodyColor: text,
            displayColor: text,
          ),
      iconTheme: const IconThemeData(color: text, size: 24),
      dividerTheme: const DividerThemeData(color: border, thickness: 1),
      sliderTheme: const SliderThemeData(
        activeTrackColor: accent,
        inactiveTrackColor: Color(0x29FFFFFF),
        thumbColor: text,
        overlayColor: accentSoft,
        trackHeight: 4,
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: accent,
          foregroundColor: onAccent,
          minimumSize: const Size(56, 48),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
          shape: rounded,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: accent,
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
          shape: rounded,
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: surfaceHigh,
        contentTextStyle: body,
        actionTextColor: accent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radiusSmall),
        ),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: surfaceHigh,
        surfaceTintColor: Colors.transparent,
        showDragHandle: false,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surfaceHigh,
        hintStyle: const TextStyle(color: textFaint, fontSize: 15),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radius),
          borderSide: BorderSide.none,
        ),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(color: accent),
    );
  }
}

/// System bars as a head unit shows them: no phone status bar (the menu rail has the
/// clock), navigation buttons kept for Back and Home. Fullscreen video hides both and
/// restores this when it closes.
void applyCarSystemUi() {
  SystemChrome.setEnabledSystemUIMode(
    SystemUiMode.manual,
    overlays: const [SystemUiOverlay.bottom],
  );
}

/// Flat card on [CarTheme.surface] with a hairline border, the base of every panel.
class SurfaceCard extends StatelessWidget {
  const SurfaceCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.color = CarTheme.surface,
    this.onTap,
    this.clip = false,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color color;
  final VoidCallback? onTap;

  /// Clips children to the rounded shape, for maps and artwork that touch the edges.
  final bool clip;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: color,
      clipBehavior: clip ? Clip.antiAlias : Clip.none,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(CarTheme.radiusLarge),
        side: const BorderSide(color: CarTheme.border),
      ),
      child: InkWell(
        onTap: onTap,
        child: Padding(padding: padding, child: child),
      ),
    );
  }
}

/// Round transport button (play/pause, next, previous) in three sizes.
class ControlButton extends StatelessWidget {
  const ControlButton({
    super.key,
    required this.icon,
    required this.onPressed,
    this.size = 48,
    this.primary = false,
    this.loading = false,
    this.tooltip,
  });

  final IconData icon;
  final VoidCallback? onPressed;

  /// Outer diameter in dp; the icon scales with it.
  final double size;
  final bool primary;
  final bool loading;
  final String? tooltip;

  @override
  Widget build(BuildContext context) {
    final foreground = primary ? CarTheme.onAccent : CarTheme.text;
    final button = Material(
      color: primary ? CarTheme.accent : Colors.transparent,
      shape: const CircleBorder(),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onPressed,
        child: SizedBox.square(
          dimension: size,
          child: Center(
            child: loading
                ? SizedBox.square(
                    dimension: size * 0.38,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: foreground,
                    ),
                  )
                : Icon(
                    icon,
                    size: size * (primary ? 0.42 : 0.5),
                    color: onPressed == null
                        ? foreground.withValues(alpha: 0.35)
                        : foreground,
                  ),
          ),
        ),
      ),
    );
    return tooltip == null ? button : Tooltip(message: tooltip, child: button);
  }
}

/// Small rounded tag, e.g. LIVE or the served quality.
class Tag extends StatelessWidget {
  const Tag(this.text, {super.key, this.color = CarTheme.surfaceHigh, this.textColor});

  final String text;
  final Color color;
  final Color? textColor;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
        child: Text(
          text,
          style: TextStyle(
            fontSize: 11,
            height: 1.1,
            fontWeight: FontWeight.w700,
            color: textColor ?? CarTheme.text,
            letterSpacing: 0.3,
          ),
        ),
      ),
    );
  }
}
