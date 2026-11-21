import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../controllers/location_controller.dart';
import '../controllers/navigation_controller.dart';
import '../models/navigation.dart';
import '../theme/car_theme.dart';

/// flutter_map's dark-mode matrix: colors inverted, then the hue turned by 180 degrees.
const _darkTiles = ColorFilter.matrix(<double>[
  0.574, -1.43, -0.144, 0, 255, //
  -0.426, -0.43, -0.144, 0, 255, //
  -0.426, -1.43, 0.856, 0, 255, //
  0, 0, 0, 1, 0, //
]);

/// Dark street map that follows the car, with the route while navigation is set.
///
/// Tiles come from OpenStreetMap, shown dark; the attribution must stay visible.
/// Following stops when the driver pans and resumes with the recenter button. The camera
/// only moves while [active], so a map in a hidden menu does not keep fetching tiles.
///
/// With a [navigation] the map draws the route (travelled grey, remaining in the accent
/// or in traffic colors), the destination, fits the routes in preview when [fitRoutes],
/// and while navigating turns into a driving view: the car low in the free area, the map
/// turned so the road ahead points up, until the driver picks north-up.
class CarMap extends StatefulWidget {
  const CarMap({
    super.key,
    required this.location,
    this.navigation,
    this.active = true,
    this.interactive = true,
    this.showControls = true,
    this.tilesEnabled = true,
    this.fitRoutes = false,
    this.overlayInsets = EdgeInsets.zero,
    this.onTap,
    this.onLongPress,
  });

  final LocationController location;
  final NavigationController? navigation;

  /// Tap anywhere on the map (the dashboard card opens the Maps menu with it).
  final VoidCallback? onTap;

  /// Long press on the map: the Maps menu sets point B with it.
  final ValueChanged<LatLng>? onLongPress;
  final bool active;
  final bool interactive;
  final bool showControls;

  /// False in widget tests, which have no network for tiles.
  final bool tilesEnabled;

  /// Moves the camera to show the whole route and its alternatives in preview.
  final bool fitRoutes;

  /// Parts of the map covered by panels; the car and fitted routes stay out of them.
  final EdgeInsets overlayInsets;

  @override
  State<CarMap> createState() => _CarMapState();
}

class _CarMapState extends State<CarMap> with SingleTickerProviderStateMixin {
  /// Jakarta, only until the first position arrives.
  static const _fallbackCenter = LatLng(-6.2000, 106.8167);
  static const _followZoom = 16.0;
  static const _drivingZoom = 17.0;

  /// Where the car sits in the free area while driving heading-up, from its top: low,
  /// so most of the screen shows the road ahead.
  static const _headingUpCarHeight = 0.72;

  /// North-up driving keeps the road ahead in view by placing the car back from the
  /// free area's centre against the travel direction, by these fractions of its width
  /// and height (driving south puts the car high, east puts it left).
  static const _lookAheadX = 0.30;
  static const _lookAheadY = 0.25;

  /// Farther than this between two fixes (GPS reacquired, first fix after a pause) the
  /// car jumps: gliding across a city would pull in every tile on the way.
  static const _maxGlideMeters = 300.0;

  final MapController _map = MapController();
  bool _ready = false;
  bool _follow = true;

  /// North-up while navigating, the default: heading-up rotates the tile layer every
  /// frame, which cost ~22 ms of raster per frame on the A6+ against ~10 ms north-up.
  /// Heading-up is the driver's one-tap choice.
  bool _northUp = true;

  /// The car as drawn. GPS fixes arrive about once a second; the marker, and the camera
  /// while following, glide from one fix to the next over that second instead of jumping.
  /// While navigating the target is the fix snapped onto the route, so the car sits on
  /// the line where travelled turns into remaining.
  final ValueNotifier<LatLng?> _shown = ValueNotifier(null);
  late final AnimationController _glide;
  LatLng? _glideFrom;
  LatLng? _glideTo;

  /// Heading-up map rotation in degrees (minus the travel direction), gliding with the
  /// position; north-up driving uses it to look ahead.
  double _rotation = 0;
  double _rotationFrom = 0;
  double _rotationTo = 0;

  /// Direction the car arrow points, degrees from north; null when unknown.
  double? _markerHeading;

  LocationStatus? _status;
  bool _hasPosition = false;
  NavState _navState = NavState.idle;
  List<NavRoute>? _fittedRoutes;
  bool _drivingZoomPending = false;

  NavigationController? get _nav => widget.navigation;

  /// Navigating shows the driving view: the car low on screen, heading-up unless the
  /// driver chose north-up.
  bool get _navigating => _nav?.state == NavState.navigating;

  @override
  void initState() {
    super.initState();
    _glide = AnimationController(vsync: this, duration: const Duration(seconds: 1))
      ..addListener(_onGlide);
    _shown.value = widget.location.position;
    _markerHeading = widget.location.heading;
    _status = widget.location.status;
    _hasPosition = widget.location.position != null;
    widget.location.addListener(_onLocation);
    _attach(widget.navigation);
  }

  void _attach(NavigationController? nav) {
    if (nav == null) return;
    _navState = nav.state;
    nav.addListener(_onNavigation);
    nav.progress.addListener(_onProgress);
  }

  void _detach(NavigationController? nav) {
    nav?.removeListener(_onNavigation);
    nav?.progress.removeListener(_onProgress);
  }

  @override
  void didUpdateWidget(CarMap oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.location != widget.location) {
      oldWidget.location.removeListener(_onLocation);
      widget.location.addListener(_onLocation);
    }
    if (oldWidget.navigation != widget.navigation) {
      _detach(oldWidget.navigation);
      _attach(widget.navigation);
    }
    if (widget.active && !oldWidget.active) {
      // The glide's ticker is muted while the menu is hidden; start from the latest fix.
      _glide.stop();
      _shown.value = _target();
      _rotation = _rotationTo;
      if (!_fitPreview(force: true)) _followCamera();
    } else if (widget.overlayInsets != oldWidget.overlayInsets) {
      if (!_fitPreview(force: true)) _followCamera();
    }
  }

  /// Where the car should be drawn: on the route while navigating, else the fix.
  LatLng? _target() {
    final progress = _nav?.progress.value;
    if (_navigating && progress != null && !progress.offRoute) {
      return progress.snapped;
    }
    return widget.location.position;
  }

  void _onLocation() {
    if (!mounted) return;
    final location = widget.location;
    // While navigating with progress, the snapped position drives the car (_onProgress).
    if (!(_navigating && _nav?.progress.value != null)) {
      final heading = location.heading;
      _markerHeading = heading;
      if (_navigating && heading != null) {
        _rotationTo = _nearestAngle(_rotation, -heading);
      }
      _moveTo(location.position);
    }
    final status = location.status;
    final hasPosition = location.position != null;
    if (status != _status || hasPosition != _hasPosition) {
      // Permission prompt and the recenter button; not rebuilt on every fix.
      setState(() {
        _status = status;
        _hasPosition = hasPosition;
      });
    }
  }

  void _onProgress() {
    if (!mounted || !_navigating) return;
    final progress = _nav?.progress.value;
    if (progress == null) return;
    final heading = progress.offRoute ? widget.location.heading : progress.bearing;
    if (heading != null) {
      _markerHeading = heading;
      _rotationTo = _nearestAngle(_rotation, -heading);
    }
    _moveTo(_target());
  }

  void _onNavigation() {
    if (!mounted) return;
    final nav = _nav!;
    final state = nav.state;
    if (state != _navState) {
      final was = _navState;
      _navState = state;
      if (state == NavState.navigating) {
        // Every guidance starts in the default view, whatever the last one used.
        _northUp = true;
        _follow = true;
        _drivingZoomPending = true;
        final heading = nav.progress.value?.bearing ?? widget.location.heading;
        if (heading != null) _rotationTo = _rotation = _nearestAngle(_rotation, -heading);
        _glide.stop();
        _shown.value = _target();
        _followCamera(force: true);
      } else if (state == NavState.idle && was != NavState.idle) {
        _follow = true;
        _fittedRoutes = null;
        _rotation = _rotationTo = 0;
        _glide.stop();
        _shown.value = widget.location.position;
        _followCamera(force: true);
      }
      setState(() {}); // controls and route layers
    }
    _fitPreview();
  }

  /// Fits the routes (or the car and the destination while routes load) in preview.
  /// Returns whether the map is in preview and fitting applies.
  bool _fitPreview({bool force = false}) {
    final nav = _nav;
    if (nav == null || nav.state != NavState.preview || !widget.fitRoutes) {
      return false;
    }
    if (!force && identical(nav.routes, _fittedRoutes)) return true;
    if (!_ready || !widget.active) return true;
    _fittedRoutes = nav.routes;
    final points = <LatLng>[
      for (final route in nav.routes) ...route.points,
      if (nav.destination case final place?) place.location,
      if (widget.location.position case final car?) car,
    ];
    if (points.isEmpty) return true;
    _follow = false;
    _rotation = _rotationTo = 0;
    if (_map.camera.rotation != 0) _map.rotate(0);
    if (points.length == 1) {
      _map.move(points.single, _followZoom);
    } else {
      _map.fitCamera(
        CameraFit.coordinates(
          coordinates: points,
          maxZoom: _drivingZoom,
          padding: widget.overlayInsets +
              EdgeInsets.fromLTRB(32, 40, widget.showControls ? 88 : 32, 40),
        ),
      );
    }
    setState(() {}); // recenter button highlight
    return true;
  }

  void _moveTo(LatLng? target) {
    if (target == null) return;
    final from = _shown.value;
    if (target == _glideTo && from != null) {
      // Same point, maybe a new direction.
      if (_rotation != _rotationTo && !_glide.isAnimating) {
        _rotation = _rotationTo;
        _followCamera();
      }
      return;
    }
    if (from == null ||
        !widget.active ||
        const Distance().as(LengthUnit.Meter, from, target) > _maxGlideMeters) {
      _glide.stop();
      _glideTo = target;
      _rotation = _rotationTo;
      _shown.value = target;
      _followCamera();
    } else {
      _glideFrom = from;
      _glideTo = target;
      _rotationFrom = _rotation;
      _glide.forward(from: 0);
    }
  }

  void _onGlide() {
    final from = _glideFrom;
    final to = _glideTo;
    if (from == null || to == null) return;
    final t = _glide.value;
    _rotation = _rotationFrom + (_rotationTo - _rotationFrom) * t;
    _shown.value = LatLng(
      from.latitude + (to.latitude - from.latitude) * t,
      from.longitude + (to.longitude - from.longitude) * t,
    );
    _followCamera();
  }

  void _followCamera({bool force = false}) {
    final position = _shown.value;
    if (position == null || !_ready || !widget.active) return;
    if (!_follow && !force) return;
    final camera = _map.camera;
    if (_navigating) {
      var zoom = camera.zoom;
      if (_drivingZoomPending || zoom < 12) {
        zoom = _drivingZoom;
        _drivingZoomPending = false;
      }
      final rotation = _northUp ? 0.0 : _rotation;
      if (camera.rotation != rotation) _map.rotate(rotation);
      _map.move(
        position,
        zoom,
        offset: _northUp ? _lookAheadOffset() : _carOffset(_headingUpCarHeight),
      );
    } else {
      if (camera.rotation != 0) _map.rotate(0);
      final zoom = camera.zoom < 12 ? _followZoom : camera.zoom;
      _map.move(position, zoom, offset: _carOffset(0.5));
    }
  }

  /// Screen offset from the map's center to where the car is drawn: horizontally
  /// centered in the area not covered by [CarMap.overlayInsets], [height] of the way
  /// down it.
  Offset _carOffset(double height) {
    final size = _map.camera.nonRotatedSize;
    final insets = widget.overlayInsets;
    if (insets == EdgeInsets.zero && height == 0.5) return Offset.zero;
    final freeWidth = size.width - insets.horizontal;
    final freeHeight = size.height - insets.vertical;
    if (freeWidth <= 0 || freeHeight <= 0) return Offset.zero;
    return Offset(
      insets.left + freeWidth / 2 - size.width / 2,
      insets.top + freeHeight * height - size.height / 2,
    );
  }

  /// North-up driving: the car back from the free area's centre against the travel
  /// direction. The direction is the heading-up rotation, which glides between fixes,
  /// so the camera shifts smoothly through turns.
  Offset _lookAheadOffset() {
    final size = _map.camera.nonRotatedSize;
    final insets = widget.overlayInsets;
    final freeWidth = size.width - insets.horizontal;
    final freeHeight = size.height - insets.vertical;
    if (freeWidth <= 0 || freeHeight <= 0) return Offset.zero;
    final heading = -_rotation * math.pi / 180;
    return Offset(
      insets.left + freeWidth / 2 - size.width / 2 -
          math.sin(heading) * freeWidth * _lookAheadX,
      insets.top + freeHeight / 2 - size.height / 2 +
          math.cos(heading) * freeHeight * _lookAheadY,
    );
  }

  void _recenter() {
    setState(() => _follow = true);
    _followCamera(force: true);
  }

  void _toggleNorthUp() {
    setState(() => _northUp = !_northUp);
    _followCamera(force: true);
  }

  void _zoom(double delta) {
    if (!_ready) return;
    final camera = _map.camera;
    _map.move(camera.center, (camera.zoom + delta).clamp(4, 19));
  }

  @override
  void dispose() {
    widget.location.removeListener(_onLocation);
    _detach(widget.navigation);
    _glide.dispose();
    _shown.dispose();
    _map.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final location = widget.location;
    final position = location.position;
    final status = location.status;
    final nav = _nav;
    return Stack(
      children: [
        FlutterMap(
          mapController: _map,
          options: MapOptions(
            initialCenter: position ?? _fallbackCenter,
            initialZoom: position == null ? 11 : _followZoom,
            minZoom: 4,
            maxZoom: 19,
            backgroundColor: CarTheme.bg,
            interactionOptions: InteractionOptions(
              flags: widget.interactive
                  ? InteractiveFlag.all & ~InteractiveFlag.rotate
                  : InteractiveFlag.none,
            ),
            onTap: widget.onTap == null ? null : (_, __) => widget.onTap!(),
            onLongPress: widget.onLongPress == null || !widget.interactive
                ? null
                : (_, point) => widget.onLongPress!(point),
            onMapReady: () {
              _ready = true;
              if (!_fitPreview(force: true)) _followCamera(force: true);
            },
            onPositionChanged: (camera, hasGesture) {
              if (hasGesture && _follow) setState(() => _follow = false);
            },
          ),
          children: [
            if (widget.tilesEnabled)
              // Standard OpenStreetMap tiles, darkened on the device: free without an API
              // key (CARTO's dark tiles now watermark keyless requests). OSM's tile policy
              // asks for a real User-Agent and no bulk or simulated-retina loading.
              // One filter over the whole layer: flutter_map's darkModeTileBuilder filters
              // each tile, an offscreen pass per tile on every frame.
              ColorFiltered(
                colorFilter: _darkTiles,
                child: TileLayer(
                  urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                  userAgentPackageName: 'com.carheadunit.youtube_car_headunit',
                ),
              ),
            if (nav != null)
              // Rebuilt per fix while navigating (the split point moves), not per frame.
              ListenableBuilder(
                listenable: Listenable.merge([nav, nav.progress]),
                builder: (context, _) => _RouteLayer(navigation: nav),
              ),
            if (nav != null)
              ListenableBuilder(
                listenable: nav,
                builder: (context, _) => _DestinationLayer(navigation: nav),
              ),
            // Only the marker rebuilds on every glide frame, not the whole map.
            ValueListenableBuilder<LatLng?>(
              valueListenable: _shown,
              builder: (context, shown, _) => shown == null
                  ? const SizedBox.shrink()
                  : MarkerLayer(
                      markers: [
                        Marker(
                          point: shown,
                          width: 56,
                          height: 56,
                          child: _CarMarker(heading: _markerHeading),
                        ),
                      ],
                    ),
            ),
          ],
        ),
        Positioned(
          left: 10,
          bottom: 8,
          child: nav == null
              ? const _Attribution(_osmAttribution)
              : ListenableBuilder(
                  listenable: nav,
                  builder: (context, _) => _Attribution(
                    'Rute: ${_providerName(nav.route?.provider ?? nav.provider)}'
                    ' · Pencarian: Photon · $_osmAttribution',
                  ),
                ),
        ),
        if (widget.showControls)
          Positioned(
            right: 12,
            top: 0,
            bottom: 0,
            // Sized for all four buttons, top-aligned: the compass appearing with
            // guidance must not move the others, or a tap aimed at the recenter
            // button lands on the compass.
            child: Center(
              child: SizedBox(
                height: _MapControls.fullHeight,
                child: Align(
                  alignment: Alignment.topCenter,
                  child: _MapControls(
                    following: _follow,
                    onZoomIn: () => _zoom(1),
                    onZoomOut: () => _zoom(-1),
                    onRecenter: position == null ? null : _recenter,
                    northUp: _navigating ? _northUp : null,
                    onToggleNorthUp: _toggleNorthUp,
                  ),
                ),
              ),
            ),
          ),
        if (status != LocationStatus.granted &&
            status != LocationStatus.unknown)
          Positioned.fill(
            child: _PermissionPrompt(location: widget.location),
          ),
      ],
    );
  }
}

const _osmAttribution = '© OpenStreetMap contributors';

String _providerName(RouteProvider provider) => switch (provider) {
      RouteProvider.mapbox => 'Mapbox',
      RouteProvider.osrm => 'OSRM',
    };

/// [to] shifted by whole turns so a glide from [from] takes the short way round.
double _nearestAngle(double from, double to) {
  var delta = (to - from) % 360;
  if (delta > 180) delta -= 360;
  if (delta < -180) delta += 360;
  return from + delta;
}

/// Line colors: travelled grey, remaining in the accent; with traffic data the
/// remaining line takes amber, red and dark red where Mapbox reports congestion.
abstract final class _RouteColors {
  static const travelled = Color(0xFF5B6270);
  static const alternative = Color(0xFF4A5261);
  static const moderate = Color(0xFFF2A93B);
  static const heavy = Color(0xFFE5484D);
  static const severe = Color(0xFF9E1F23);

  /// A thin dark edge keeps the line readable over bright road labels.
  static final casing = CarTheme.bg.withValues(alpha: 0.7);

  static Color of(Congestion level) => switch (level) {
        Congestion.unknown || Congestion.low => CarTheme.accent,
        Congestion.moderate => moderate,
        Congestion.heavy => heavy,
        Congestion.severe => severe,
      };
}

class _RouteLayer extends StatelessWidget {
  const _RouteLayer({required this.navigation});

  final NavigationController navigation;

  static const _width = 7.0;

  @override
  Widget build(BuildContext context) {
    final nav = navigation;
    final route = nav.route;
    if (nav.state == NavState.idle || route == null) return const SizedBox.shrink();
    final progress = nav.state == NavState.preview ? null : nav.progress.value;
    final lines = <Polyline>[
      if (nav.state == NavState.preview)
        for (var i = 0; i < nav.routes.length; i++)
          if (i != nav.selectedIndex)
            Polyline(
              points: nav.routes[i].points,
              strokeWidth: _width - 1,
              color: _RouteColors.alternative,
              borderStrokeWidth: 1.5,
              borderColor: _RouteColors.casing,
            ),
      if (progress != null)
        Polyline(
          points: route.travelled(progress),
          strokeWidth: _width,
          color: _RouteColors.travelled,
          borderStrokeWidth: 1.5,
          borderColor: _RouteColors.casing,
        ),
      for (final run in route.remainingRuns(progress))
        Polyline(
          points: run.points,
          strokeWidth: _width,
          color: _RouteColors.of(run.congestion),
          borderStrokeWidth: 1.5,
          borderColor: _RouteColors.casing,
        ),
    ];
    return PolylineLayer(polylines: lines);
  }
}

class _DestinationLayer extends StatelessWidget {
  const _DestinationLayer({required this.navigation});

  final NavigationController navigation;

  @override
  Widget build(BuildContext context) {
    final place = navigation.destination;
    if (place == null) return const SizedBox.shrink();
    return MarkerLayer(
      // Upright whatever the map's rotation, the pin's tip on the point.
      rotate: true,
      markers: [
        Marker(
          point: place.location,
          width: 28,
          height: 40,
          alignment: Alignment.topCenter,
          child: const _DestinationPin(),
        ),
      ],
    );
  }
}

class _DestinationPin extends StatelessWidget {
  const _DestinationPin();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 28,
          height: 28,
          decoration: BoxDecoration(
            color: CarTheme.text,
            shape: BoxShape.circle,
            border: Border.all(color: CarTheme.bg, width: 2),
          ),
          child: const Icon(LucideIcons.flag, size: 14, color: CarTheme.bg),
        ),
        Container(width: 3, height: 12, color: CarTheme.text),
      ],
    );
  }
}

class _CarMarker extends StatelessWidget {
  const _CarMarker({required this.heading});

  final double? heading;

  @override
  Widget build(BuildContext context) {
    final heading = this.heading;
    return Stack(
      alignment: Alignment.center,
      children: [
        const DecoratedBox(
          decoration: BoxDecoration(
            color: CarTheme.accentSoft,
            shape: BoxShape.circle,
          ),
          child: SizedBox.square(dimension: 56),
        ),
        Container(
          width: 26,
          height: 26,
          decoration: BoxDecoration(
            color: CarTheme.accent,
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: 3),
          ),
          child: heading == null
              ? null
              : Transform.rotate(
                  angle: heading * math.pi / 180,
                  child: const Icon(
                    LucideIcons.navigation2,
                    size: 13,
                    color: Colors.white,
                  ),
                ),
        ),
      ],
    );
  }
}

class _MapControls extends StatelessWidget {
  const _MapControls({
    required this.following,
    required this.onZoomIn,
    required this.onZoomOut,
    required this.onRecenter,
    required this.northUp,
    required this.onToggleNorthUp,
  });

  final bool following;
  final VoidCallback onZoomIn;
  final VoidCallback onZoomOut;
  final VoidCallback? onRecenter;

  /// Null outside the driving view, where the map is always north-up.
  final bool? northUp;
  final VoidCallback onToggleNorthUp;

  /// Zoom in, zoom out, recenter and the compass with their gaps.
  static const fullHeight = 56.0 * 4 + 8 + 16 + 8;

  @override
  Widget build(BuildContext context) {
    Widget button(
      IconData icon,
      VoidCallback? onTap, {
      bool highlight = false,
      String? tooltip,
    }) {
      final button = Material(
        color: highlight ? CarTheme.accent : CarTheme.surfaceHigh,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(CarTheme.radius),
          side: const BorderSide(color: CarTheme.border),
        ),
        child: InkWell(
          onTap: onTap,
          customBorder: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(CarTheme.radius),
          ),
          child: SizedBox.square(
            dimension: 56,
            child: Icon(
              icon,
              color: highlight ? CarTheme.onAccent : CarTheme.text,
            ),
          ),
        ),
      );
      return tooltip == null ? button : Tooltip(message: tooltip, child: button);
    }

    final northUp = this.northUp;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        button(LucideIcons.plus, onZoomIn),
        const SizedBox(height: 8),
        button(LucideIcons.minus, onZoomOut),
        const SizedBox(height: 16),
        // Highlighted when the map no longer follows the car.
        button(
          LucideIcons.locateFixed,
          onRecenter,
          highlight: !following && onRecenter != null,
          tooltip: 'Ikuti mobil',
        ),
        if (northUp != null) ...[
          const SizedBox(height: 8),
          // Highlighted while heading-up; north-up is the driving default.
          button(
            LucideIcons.compass,
            onToggleNorthUp,
            highlight: !northUp,
            tooltip: northUp ? 'Arah jalan di atas' : 'Utara di atas',
          ),
        ],
      ],
    );
  }
}

class _Attribution extends StatelessWidget {
  const _Attribution(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: CarTheme.bg.withValues(alpha: 0.7),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        child: Text(
          text,
          style: const TextStyle(fontSize: 10, color: CarTheme.textFaint),
        ),
      ),
    );
  }
}

class _PermissionPrompt extends StatelessWidget {
  const _PermissionPrompt({required this.location});

  final LocationController location;

  @override
  Widget build(BuildContext context) {
    final (message, action, onAction) = switch (location.status) {
      LocationStatus.serviceOff => (
          'Lokasi HP mati. Nyalakan untuk melihat posisi mobil.',
          'Buka pengaturan',
          location.openSettings,
        ),
      LocationStatus.deniedForever => (
          'Izin lokasi ditolak. Aktifkan di pengaturan app.',
          'Buka pengaturan',
          location.openSettings,
        ),
      _ => (
          'Izinkan lokasi untuk menampilkan posisi mobil di peta.',
          'Izinkan lokasi',
          location.requestPermission,
        ),
    };
    return ColoredBox(
      color: CarTheme.bg.withValues(alpha: 0.72),
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(LucideIcons.mapPin, size: 32, color: CarTheme.accent),
              const SizedBox(height: 12),
              Text(
                message,
                textAlign: TextAlign.center,
                style: CarTheme.body,
              ),
              const SizedBox(height: 16),
              FilledButton(onPressed: onAction, child: Text(action)),
            ],
          ),
        ),
      ),
    );
  }
}
