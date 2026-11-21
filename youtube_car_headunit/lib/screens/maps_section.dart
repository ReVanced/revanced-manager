import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../controllers/location_controller.dart';
import '../controllers/navigation_controller.dart';
import '../controllers/playback_controller.dart';
import '../models/navigation.dart';
import '../services/voice_guidance.dart';
import '../theme/car_theme.dart';
import '../widgets/car_map.dart';
import '../widgets/navigation/guidance_bar.dart';
import '../widgets/navigation/maneuver_banner.dart';
import '../widgets/navigation/route_preview.dart';
import '../widgets/navigation/search_panel.dart';
import '../widgets/now_playing.dart';

/// Full-screen map with destination search, route preview and guidance on the left and
/// the car in the free area on the right; media controls float on top.
///
/// The section rebuilds when the navigation state changes, never per GPS fix: the
/// banner, the guidance bar and the map's route layer listen to progress themselves.
class MapsSection extends StatelessWidget {
  const MapsSection({
    super.key,
    required this.playback,
    required this.location,
    required this.navigation,
    required this.searchOpen,
    required this.active,
    required this.tilesEnabled,
    required this.onOpenMusic,
    required this.onGoogleMaps,
    this.voice,
  });

  /// Width of the left column (search, preview, guidance); the map keeps the car and the
  /// fitted routes to the right of it.
  static const panelWidth = 360.0;
  static const _margin = 12.0;

  final PlaybackController playback;
  final LocationController location;
  final NavigationController navigation;

  /// Whether the destination search is open; the shell opens it from Home's Navigasi.
  final ValueNotifier<bool> searchOpen;
  final bool active;
  final bool tilesEnabled;
  final VoidCallback onOpenMusic;

  /// Hands navigation to Google Maps, to the destination when one is set.
  final ValueChanged<Place?> onGoogleMaps;
  final VoiceGuidance? voice;

  void _pickPoint(LatLng point) {
    final state = navigation.state;
    if (state != NavState.idle && state != NavState.preview) return;
    searchOpen.value = false;
    navigation.previewPoint(point);
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([navigation, searchOpen]),
      builder: (context, _) {
        final state = navigation.state;
        final searching = state == NavState.idle && searchOpen.value;
        final panel = searching || state != NavState.idle;
        return Stack(
          children: [
            Positioned.fill(
              child: CarMap(
                location: location,
                navigation: navigation,
                active: active,
                tilesEnabled: tilesEnabled,
                fitRoutes: true,
                overlayInsets: panel
                    ? const EdgeInsets.only(left: panelWidth + _margin)
                    : EdgeInsets.zero,
                onLongPress: _pickPoint,
              ),
            ),
            ...switch (state) {
              NavState.idle when searching => [_search(context)],
              NavState.idle => _idle(),
              NavState.preview => [_preview()],
              NavState.navigating || NavState.arrived => [_guidance()],
            },
          ],
        );
      },
    );
  }

  List<Widget> _idle() => [
        Positioned(
          top: _margin,
          left: _margin,
          child: _SearchPill(onTap: () => searchOpen.value = true),
        ),
        Positioned(
          top: _margin,
          right: _margin,
          child: FilledButton.icon(
            onPressed: () => onGoogleMaps(null),
            style: FilledButton.styleFrom(
              backgroundColor: CarTheme.surfaceHigh,
              foregroundColor: CarTheme.text,
              side: const BorderSide(color: CarTheme.border),
            ),
            icon: const Icon(LucideIcons.externalLink, size: 18),
            label: const Text('Google Maps'),
          ),
        ),
        Positioned(
          left: _margin,
          bottom: 30,
          child: NowPlayingMini(playback: playback, onOpen: onOpenMusic),
        ),
      ];

  Widget _search(BuildContext context) {
    // Results stay above the keyboard, which leaves the panel ~160 dp: compact then.
    final keyboard = MediaQuery.viewInsetsOf(context).bottom;
    final compact = keyboard > 0;
    return Positioned(
      top: compact ? 4 : _margin,
      left: _margin,
      width: panelWidth,
      bottom: math.max(_margin, keyboard + 4),
      child: SearchPanel(
        navigation: navigation,
        location: location,
        compact: compact,
        onClose: () => searchOpen.value = false,
      ),
    );
  }

  Widget _preview() => Positioned(
        top: _margin,
        left: _margin,
        bottom: _margin,
        width: panelWidth,
        child: Align(
          alignment: Alignment.topLeft,
          child: RoutePreview(navigation: navigation, onGoogleMaps: onGoogleMaps),
        ),
      );

  Widget _guidance() {
    final voice = this.voice;
    return Positioned(
      top: _margin,
      left: _margin,
      bottom: 30,
      width: panelWidth,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ManeuverBanner(navigation: navigation),
          const Spacer(),
          NowPlayingMini(playback: playback, onOpen: onOpenMusic),
          // Arrived: the arrival card alone, no remaining time or Akhiri.
          if (navigation.state == NavState.navigating) ...[
            const SizedBox(height: 8),
            GuidanceBar(
              navigation: navigation,
              muted: voice?.muted,
              onToggleMute: voice?.toggleMute,
            ),
          ],
        ],
      ),
    );
  }
}

class _SearchPill extends StatelessWidget {
  const _SearchPill({required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final shape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(CarTheme.radius),
      side: const BorderSide(color: CarTheme.border),
    );
    return Semantics(
      button: true,
      label: 'Cari tujuan',
      child: Material(
        color: CarTheme.surfaceHigh,
        shape: shape,
        child: InkWell(
          onTap: onTap,
          customBorder: shape,
          child: const SizedBox(
            width: 300,
            height: 52,
            child: ExcludeSemantics(
              child: Row(
                children: [
                  SizedBox(width: 16),
                  Icon(LucideIcons.search, size: 20, color: CarTheme.textMuted),
                  SizedBox(width: 12),
                  Text(
                    'Cari tujuan',
                    style: TextStyle(fontSize: 15, color: CarTheme.textFaint),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
