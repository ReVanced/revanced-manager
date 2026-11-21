import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:latlong2/latlong.dart';

import '../models/navigation.dart';
import '../services/geo.dart';
import 'location_controller.dart';
import 'navigation_controller.dart';

/// True only in builds made with `--dart-define=SIMULATE_DRIVE=true`. Code guarded by
/// `if (kSimulateDrive)` is removed from other builds, the simulator included.
const bool kSimulateDrive = bool.fromEnvironment('SIMULATE_DRIVE');

/// Seconds after the start of guidance at which the simulated car leaves the route once,
/// from `--dart-define=SIMULATE_DETOUR_S=<n>`; 0 never.
const int kSimulateDetourSeconds = int.fromEnvironment('SIMULATE_DETOUR_S');

/// Dev-only simulated drive for testing guidance on the phone, whose Android 10 cannot
/// take mock locations from the shell.
///
/// While guidance runs it moves the car along the active route at [speedKmh], one
/// position per [tick], through [LocationController.injectPosition], so real GPS fixes
/// cannot pull the car back. It follows reroutes, and [leaveRoute] drives off the road
/// to make one happen. Guidance ending returns the location to real GPS.
class DriveSimulator {
  DriveSimulator({
    required this.location,
    required this.navigation,
    this.speedKmh = 40,
    this.tick = const Duration(seconds: 1),
    this.detourAfter = kSimulateDetourSeconds > 0
        ? const Duration(seconds: kSimulateDetourSeconds)
        : null,
  });

  final LocationController location;
  final NavigationController navigation;
  final double speedKmh;
  final Duration tick;

  /// Leaves the route once this long after guidance starts; null never.
  final Duration? detourAfter;

  Timer? _timer;
  NavRoute? _route;
  double _along = 0;
  LatLng? _position;
  double _heading = 0;
  double _detourLeft = 0;
  double _detourBearing = 0;
  Duration _driven = Duration.zero;
  bool _detoured = false;
  bool _attached = false;

  bool get running => _timer != null;

  double get _metersPerTick => speedKmh / 3.6 * tick.inMilliseconds / 1000;

  /// Starts following guidance: drives whenever navigation is navigating.
  void attach() {
    if (_attached) return;
    _attached = true;
    navigation.addListener(_onNavigation);
    _onNavigation();
  }

  /// Drives [meters] at a right angle off the route, which guidance sees as off route
  /// and answers with a reroute.
  void leaveRoute({double meters = 200}) {
    if (_route == null) return;
    _detourLeft = meters;
    _detourBearing = (_heading + 90) % 360;
  }

  void _onNavigation() {
    final route = navigation.route;
    if (navigation.state != NavState.navigating || route == null) {
      _stop();
      return;
    }
    if (!identical(route, _route)) _follow(route);
    _timer ??= Timer.periodic(tick, (_) => advance());
  }

  /// A new route (start, reroute, traffic refresh) begins where the car is: drive on from
  /// the car's position projected onto it.
  void _follow(NavRoute route) {
    _route = route;
    _detourLeft = 0;
    final from = _position;
    if (from == null) {
      _along = 0;
      _driven = Duration.zero;
      _detoured = false;
      return;
    }
    var best = double.infinity;
    for (var i = 0; i < route.points.length - 1; i++) {
      final proj = projectOnSegment(from, route.points[i], route.points[i + 1]);
      if (proj.distance < best) {
        best = proj.distance;
        _along = route.cumulative[i] +
            proj.fraction * (route.cumulative[i + 1] - route.cumulative[i]);
      }
    }
  }

  /// Moves the car one tick. Public for tests, which step it without a timer.
  @visibleForTesting
  void advance() {
    final route = _route;
    if (route == null) return;
    final step = _metersPerTick;
    _driven += tick;

    final detour = detourAfter;
    if (!_detoured && detour != null && _driven >= detour) {
      _detoured = true;
      leaveRoute();
    }

    if (_detourLeft > 0 && _position != null) {
      _position = offsetPoint(_position!, _detourBearing, step);
      _heading = _detourBearing;
      _detourLeft -= step;
    } else {
      _along = (_along + step).clamp(0.0, route.cumulative.last);
      final (point, bearing) = _pointAt(route, _along);
      _position = point;
      _heading = bearing;
    }
    location.injectPosition(
      _position!,
      heading: _heading,
      speed: speedKmh / 3.6,
      accuracy: 5,
    );
  }

  (LatLng, double) _pointAt(NavRoute route, double along) {
    final cum = route.cumulative;
    var lo = 0;
    var hi = cum.length - 1;
    while (hi - lo > 1) {
      final mid = (lo + hi) >> 1;
      if (cum[mid] <= along) {
        lo = mid;
      } else {
        hi = mid;
      }
    }
    final a = route.points[lo];
    final b = route.points[hi];
    final length = cum[hi] - cum[lo];
    final t = length > 0 ? ((along - cum[lo]) / length).clamp(0.0, 1.0) : 0.0;
    return (
      LatLng(a.latitude + t * (b.latitude - a.latitude),
          a.longitude + t * (b.longitude - a.longitude)),
      bearingDegrees(a, b),
    );
  }

  void _stop() {
    final wasDriving = _timer != null || _route != null;
    _timer?.cancel();
    _timer = null;
    _route = null;
    _position = null;
    _detourLeft = 0;
    if (wasDriving) location.stopInjecting();
  }

  void dispose() {
    if (_attached) navigation.removeListener(_onNavigation);
    _stop();
  }
}
