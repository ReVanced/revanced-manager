import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:latlong2/latlong.dart';

import '../models/navigation.dart';
import '../services/geo.dart';
import '../services/geocoding_service.dart';
import '../services/recent_destinations.dart';
import '../services/route_instructions.dart';
import '../services/routing_service.dart';
import 'location_controller.dart';

/// Turn-by-turn guidance: destination, route preview, guidance along the route,
/// rerouting and arrival.
///
/// Listeners are notified only when [state], the routes, the destination, loading,
/// rerouting or [error] change. Per-fix data goes through [progress], so only the
/// guidance widgets rebuild once a second while driving. Voice prompts are scheduled
/// here and published as [cues] with their text; the voice only speaks them.
class NavigationController extends ChangeNotifier {
  NavigationController({
    required LocationController location,
    GeocodingService? geocoding,
    RoutingService? routing,
    NavigationStore? store,
    DateTime Function()? now,
  })  : _location = location,
        _geocoding = geocoding ?? GeocodingService(),
        _routing = routing ?? RoutingService(),
        _store = store,
        _now = now ?? DateTime.now {
    _avoidTolls = store?.avoidTolls ?? false;
    final saved = store?.provider;
    _provider =
        _routing.hasMapbox ? (saved ?? RouteProvider.mapbox) : RouteProvider.osrm;
    _location.addListener(_onLocation);
  }

  final LocationController _location;
  final GeocodingService _geocoding;
  final RoutingService _routing;
  final NavigationStore? _store;
  final DateTime Function() _now;

  // Thresholds of the guidance; distances in metres.
  static const _minOffRoute = 45.0;
  static const _maxOffRoute = 120.0;

  /// Off route has to last this long before a reroute: one wild fix must not trigger it.
  static const _offRouteHold = Duration(seconds: 4);
  static const _rerouteCooldown = Duration(seconds: 10);
  static const _maxRerouteCooldown = Duration(seconds: 60);
  static const _arrivalRadius = 30.0;

  /// Within this much remaining route, being near the destination counts as arrived.
  static const _arrivalApproach = 150.0;

  /// Fixes worse than this say nothing about being off route.
  static const _uselessAccuracy = 150.0;

  /// How often a Mapbox route is refreshed for traffic and ETA while navigating.
  static const trafficRefreshInterval = Duration(minutes: 5);

  NavState _state = NavState.idle;
  Place? _destination;
  List<NavRoute> _routes = const [];
  int _selectedIndex = 0;
  bool _avoidTolls = false;
  late RouteProvider _provider;
  bool _loading = false;
  bool _rerouting = false;
  String? _error;
  bool _disposed = false;

  /// Bumped by every route request and by [stop]; a reply with an older number is stale.
  int _requestSeq = 0;

  final ValueNotifier<NavProgress?> _progress = ValueNotifier(null);
  final StreamController<VoiceCue> _cues = StreamController.broadcast();
  final StreamController<NavEvent> _events = StreamController.broadcast();

  _Guide? _guide;
  LatLng? _lastFix;
  DateTime? _lastFixTime;
  double _speed = 0;
  DateTime? _offSince;
  DateTime? _lastRerouteAt;
  Duration _cooldown = _rerouteCooldown;
  DateTime? _lastTrafficRefresh;
  bool _refreshing = false;

  /// The toll notice is shown once per destination, not after every reroute.
  bool _tollNoticeShown = false;

  /// Prompt stages already spoken, keyed by maneuver position and stage, so a reroute or
  /// traffic refresh that keeps the same maneuver does not repeat its prompt.
  final Set<String> _spoken = {};

  /// Distance to each maneuver when it became the next one; a stage whose distance the
  /// step began inside is skipped.
  final Map<String, double> _firstSeen = {};

  NavState get state => _state;
  Place? get destination => _destination;

  /// Preview: the best route and up to two alternatives. Navigating: the active route only.
  List<NavRoute> get routes => _routes;
  int get selectedIndex => _selectedIndex;
  NavRoute? get route => _routes.isEmpty ? null : _routes[_selectedIndex];
  bool get avoidTolls => _avoidTolls;

  /// True when the build carries a Mapbox token; the provider toggle shows only then.
  bool get hasMapbox => _routing.hasMapbox;

  /// The provider the driver chose; [route]'s own provider tells who computed it, which
  /// differs after a fallback.
  RouteProvider get provider => _provider;
  bool get loading => _loading;
  bool get rerouting => _rerouting;

  /// Driver-facing message of the last failure; null when fine.
  String? get error => _error;
  List<Place> get recentDestinations => _store?.recents ?? const [];

  /// Where the car is along the route: updated on every fix while navigating, frozen at
  /// the destination (nothing remaining) while arrived, null otherwise.
  ValueListenable<NavProgress?> get progress => _progress;

  /// Prompts to speak, text ready.
  Stream<VoiceCue> get cues => _cues.stream;

  /// Notices for the screen: rerouting, toll fallback, provider fallback, arrival.
  Stream<NavEvent> get events => _events.stream;

  /// Places matching [query], biased to the car; empty under three characters.
  /// Throws [NavException].
  Future<List<Place>> search(String query) =>
      _geocoding.search(query, near: _location.position);

  /// Shows routes to [place] for the driver to pick and start.
  Future<void> previewTo(Place place) async {
    if (_state == NavState.navigating) _endGuidance();
    _tollNoticeShown = false;
    _destination = place;
    _state = NavState.preview;
    _routes = const [];
    _selectedIndex = 0;
    _progress.value = null;
    _notify();
    await _fetchPreview();
  }

  /// Shows routes to a point picked on the map. It is named "Titik di peta" until
  /// reverse geocoding, which runs alongside the route request, names it.
  Future<void> previewPoint(LatLng point) async {
    final pin = Place(name: Place.pinName, location: point);
    unawaited(_nameDestination(pin));
    await previewTo(pin);
  }

  Future<void> _nameDestination(Place pin) async {
    try {
      final named = await _geocoding.reverse(pin.location);
      if (named == null || _destination != pin) return;
      _destination = pin.copyWith(name: named.name, address: named.address);
      _notify();
    } on NavException catch (e) {
      debugPrint('Reverse geocoding failed: ${e.message}');
    }
  }

  void selectRoute(int index) {
    if (_state != NavState.preview) return;
    if (index < 0 || index >= _routes.length || index == _selectedIndex) return;
    _selectedIndex = index;
    _notify();
  }

  /// Avoids tolls from now on; refetches the preview or reroutes the drive. Only Mapbox
  /// honours it.
  Future<void> setAvoidTolls(bool value) async {
    if (value == _avoidTolls) return;
    _avoidTolls = value;
    _tollNoticeShown = false;
    _notify();
    await _store?.setAvoidTolls(value);
    // Kept for when Mapbox is active again; OSRM ignores it, so nothing to refetch.
    if (_provider == RouteProvider.mapbox) await _routeAgain();
  }

  /// Switches between Mapbox and OSRM (only with a Mapbox token).
  Future<void> setProvider(RouteProvider value) async {
    if (!hasMapbox || value == _provider) return;
    _provider = value;
    _notify();
    await _store?.setProvider(value);
    await _routeAgain();
  }

  /// Tries the failed request again.
  Future<void> retry() => _routeAgain();

  Future<void> _routeAgain() async {
    if (_state == NavState.preview) {
      await _fetchPreview();
    } else if (_state == NavState.navigating) {
      await _reroute(announce: false);
    }
  }

  /// Starts guidance on the selected route.
  void start() {
    final selected = route;
    if (_state != NavState.preview || selected == null || _destination == null) return;
    _routes = [selected];
    _selectedIndex = 0;
    _state = NavState.navigating;
    _error = null;
    _spoken.clear();
    _firstSeen.clear();
    _offSince = null;
    _lastRerouteAt = null;
    _cooldown = _rerouteCooldown;
    _lastTrafficRefresh = _now();
    _guide = _Guide(selected);
    _lastFix = null;
    unawaited(_store?.addRecent(_destination!));
    unawaited(_location.setBackgroundTracking(true));
    _notify();
    _cue(VoiceCueKind.depart, selected.steps.first.instruction);
    _onLocation();
  }

  /// Ends guidance or closes the preview.
  void stop() {
    final guiding = _state == NavState.navigating;
    _requestSeq++;
    _state = NavState.idle;
    _destination = null;
    _routes = const [];
    _selectedIndex = 0;
    _loading = false;
    _rerouting = false;
    _error = null;
    _guide = null;
    _progress.value = null;
    if (guiding) unawaited(_location.setBackgroundTracking(false));
    _notify();
  }

  Future<void> removeRecent(Place place) async {
    await _store?.removeRecent(place);
    _notify();
  }

  void _endGuidance() {
    _guide = null;
    _progress.value = null;
    unawaited(_location.setBackgroundTracking(false));
  }

  Future<void> _fetchPreview() async {
    final dest = _destination;
    if (dest == null) return;
    final from = _location.position;
    if (from == null) {
      _error = 'Lokasi belum tersedia. Tunggu sinyal GPS.';
      _notify();
      return;
    }
    final seq = ++_requestSeq;
    _loading = true;
    _error = null;
    _notify();
    try {
      final result = await _routing.route(
        from,
        dest.location,
        provider: _provider,
        avoidTolls: _avoidTolls,
        alternatives: true,
        heading: _location.heading,
      );
      if (seq != _requestSeq || _state != NavState.preview) return;
      _routes = result.routes;
      _selectedIndex = 0;
      _announceResult(result);
    } on NavException catch (e) {
      if (seq != _requestSeq) return;
      _routes = const [];
      _error = e.message;
    } finally {
      if (seq == _requestSeq) {
        _loading = false;
        _notify();
      }
    }
  }

  void _announceResult(RouteResult result) {
    if (result.fellBack) {
      _event(NavEventKind.providerFallback,
          'Mapbox tidak tersedia, rute dihitung dengan OSRM.');
    }
    // Only Mapbox can avoid tolls; under OSRM the switch is disabled and says so.
    if (_avoidTolls &&
        result.provider == RouteProvider.mapbox &&
        !result.tollsAvoided &&
        !_tollNoticeShown) {
      _tollNoticeShown = true;
      _event(NavEventKind.tollsNotAvoided,
          'Tol tidak bisa dihindari, rute ini mungkin lewat tol.');
    }
  }

  // ---------------------------------------------------------------- guidance per fix

  void _onLocation() {
    if (_state != NavState.navigating) return;
    final guide = _guide;
    final position = _location.position;
    if (guide == null || position == null || position == _lastFix) return;
    final now = _now();
    _updateSpeed(position, now);
    _lastFix = position;
    _lastFixTime = now;
    _processFix(guide, position, now);
  }

  void _updateSpeed(LatLng position, DateTime now) {
    final reported = _location.speed;
    if (reported > 0) {
      _speed = reported;
      return;
    }
    final last = _lastFix;
    final lastTime = _lastFixTime;
    if (last == null || lastTime == null) return;
    final seconds = now.difference(lastTime).inMilliseconds / 1000;
    if (seconds <= 0) return;
    final raw = distanceMeters(last, position) / seconds;
    _speed = _speed * 0.6 + raw * 0.4;
  }

  void _processFix(_Guide guide, LatLng position, DateTime now) {
    final heading = _location.heading;
    final accuracy = _location.accuracy ?? 15;
    final threshold = math.max(_minOffRoute, 2 * accuracy).clamp(_minOffRoute, _maxOffRoute);

    final match = guide.match(position, heading, _speed, threshold);
    final route = guide.route;
    final wrongWay = heading != null &&
        _speed > 3 &&
        bearingDifference(heading, guide.segmentBearing(match.segment)) > 135;
    final offNow = accuracy < _uselessAccuracy &&
        (match.distance > threshold || wrongWay);

    // Arrival: nearly no route left, or at the destination while near the route's end
    // or off it. A route that passes close to its own destination earlier (a loop, the
    // other carriageway) must not end there.
    final dest = _destination!;
    final toDest = math.min(distanceMeters(position, dest.location),
        distanceMeters(position, route.points.last));
    final remaining = math.max(0.0, route.cumulative.last - match.along);
    final atDestination =
        toDest <= _arrivalRadius && (offNow || remaining <= _arrivalApproach);
    if (atDestination || (!offNow && remaining <= _arrivalRadius)) {
      _arrive(guide, now);
      return;
    }

    final progress = guide.progress(match, now, _speed, offNow);
    _progress.value = progress;

    if (offNow) {
      _offSince ??= now;
      final heldLongEnough = now.difference(_offSince!) >= _offRouteHold;
      final last = _lastRerouteAt;
      final cooledDown = last == null || now.difference(last) >= _cooldown;
      if (heldLongEnough && cooledDown && !_rerouting) unawaited(_reroute());
      return;
    }
    _offSince = null;
    _scheduleCues(progress);

    if (route.provider == RouteProvider.mapbox &&
        !_rerouting &&
        !_refreshing &&
        now.difference(_lastTrafficRefresh ?? now) >= trafficRefreshInterval) {
      unawaited(_refreshTraffic());
    }
  }

  void _arrive(_Guide guide, DateTime now) {
    _state = NavState.arrived;
    _guide = null;
    _requestSeq++;
    _rerouting = false;
    // Frozen at the destination until stop(): fixes are ignored from here on, so a GPS
    // jump (the simulator handing back to real GPS) cannot move the numbers.
    final route = guide.route;
    final lastSegment = route.points.length - 2;
    _progress.value = NavProgress(
      stepIndex: route.steps.length - 1,
      nextStep: route.steps.last,
      distanceToManeuver: 0,
      distanceTravelled: route.cumulative.last,
      distanceRemaining: 0,
      timeRemaining: Duration.zero,
      eta: now,
      snapped: route.points.last,
      segmentIndex: lastSegment,
      bearing: guide.segmentBearing(lastSegment),
      speed: _speed,
      distanceFromRoute: 0,
    );
    unawaited(_location.setBackgroundTracking(false));
    _notify();
    _cue(VoiceCueKind.arrive, 'Anda telah tiba di tujuan');
    _event(NavEventKind.arrived, 'Anda telah tiba di ${_destination?.name ?? 'tujuan'}');
  }

  Future<void> _reroute({bool announce = true}) async {
    final dest = _destination;
    final from = _location.position;
    if (dest == null || from == null || _state != NavState.navigating) return;
    final seq = ++_requestSeq;
    _rerouting = true;
    _lastRerouteAt = _now();
    _notify();
    if (announce) {
      _event(NavEventKind.rerouting, 'Menghitung ulang rute…');
      _cue(VoiceCueKind.reroute, 'Menghitung ulang rute');
    }
    try {
      final result = await _routing.route(
        from,
        dest.location,
        provider: _provider,
        avoidTolls: _avoidTolls,
        alternatives: false,
        heading: _location.heading,
      );
      if (seq != _requestSeq || _state != NavState.navigating) return;
      _adopt(result.routes.first);
      _cooldown = _rerouteCooldown;
      _event(NavEventKind.rerouted, 'Rute baru ditemukan');
      _announceResult(result);
    } on NavException catch (e) {
      if (seq != _requestSeq) return;
      // Keep guiding on the old route; try again later, less often while it keeps failing.
      _cooldown = _cooldown * 2 > _maxRerouteCooldown ? _maxRerouteCooldown : _cooldown * 2;
      _event(NavEventKind.rerouteFailed, e.message);
    } finally {
      if (seq == _requestSeq) {
        _rerouting = false;
        _notify();
      }
    }
  }

  /// Asks Mapbox again from the car's position for fresh traffic and ETA. One request;
  /// the reply replaces the route, and a different path is announced as a new route.
  Future<void> _refreshTraffic() async {
    final dest = _destination;
    final from = _location.position;
    final old = _guide;
    if (dest == null || from == null || old == null) return;
    _refreshing = true;
    _lastTrafficRefresh = _now();
    final seq = _requestSeq;
    try {
      final result = await _routing.route(
        from,
        dest.location,
        provider: RouteProvider.mapbox,
        avoidTolls: _avoidTolls,
        alternatives: false,
        heading: _location.heading,
      );
      // A refresh that fell back to OSRM would trade traffic for nothing: keep the route.
      if (seq != _requestSeq ||
          _state != NavState.navigating ||
          result.provider != RouteProvider.mapbox ||
          _guide != old) {
        return;
      }
      final fresh = result.routes.first;
      final samePath = old.follows(fresh);
      _adopt(fresh);
      if (!samePath) {
        _event(NavEventKind.rerouted, 'Rute diperbarui mengikuti lalu lintas');
      }
    } on NavException catch (e) {
      debugPrint('Traffic refresh failed: ${e.message}');
    } finally {
      _refreshing = false;
    }
  }

  void _adopt(NavRoute route) {
    _routes = [route];
    _selectedIndex = 0;
    _guide = _Guide(route);
    _offSince = null;
    _notify();
    final position = _location.position;
    if (position != null) _processFix(_guide!, position, _now());
  }

  // ---------------------------------------------------------------- voice

  void _scheduleCues(NavProgress p) {
    final step = p.nextStep;
    if (!_speaks(step)) return;
    final key = _key(step.location);
    final firstSeen = _firstSeen.putIfAbsent(key, () => p.distanceToManeuver);
    final v = p.speed;
    final far = (v * 45).clamp(400.0, 2000.0);
    final near = (v * 12).clamp(120.0, 600.0);
    final atTurn = (v * 3).clamp(25.0, 80.0);
    final d = p.distanceToManeuver;
    final arrive = step.type == ManeuverType.arrive;

    // Only the nearest stage crossed is spoken, so a fix that jumps past two speaks once.
    if (!arrive && d <= atTurn) {
      if (_spoken.add('$key|turn')) {
        _spoken.addAll(['$key|far', '$key|near']);
        final after = p.stepAfter;
        if (after != null &&
            _speaks(after) &&
            step.distance < math.max(v * 10, 100)) {
          // The next maneuver follows closely: say both now, and not its approach later.
          _cue(VoiceCueKind.maneuver, linked(step.instruction, after.instruction));
          final afterKey = _key(after.location);
          _spoken.addAll(['$afterKey|far', '$afterKey|near']);
        } else {
          _cue(VoiceCueKind.maneuver, step.instruction);
        }
      }
    } else if (d <= near && d > atTurn) {
      if (firstSeen > near * 1.1 || firstSeen > near + 30) {
        if (_spoken.add('$key|near')) {
          _spoken.add('$key|far');
          _cue(VoiceCueKind.approach, _approachText(step, d));
        }
      }
    } else if (d <= far && d > near) {
      if (firstSeen > far * 1.1) {
        if (_spoken.add('$key|far')) {
          _cue(VoiceCueKind.approach, _approachText(step, d));
        }
      }
    }
  }

  static String _approachText(RouteStep step, double meters) =>
      step.type == ManeuverType.arrive
          ? 'Tujuan Anda ${spokenDistance(meters)} lagi'
          : inDistance(step.instruction, meters);

  /// Name changes, straight continuations and notices are shown but not spoken.
  static bool _speaks(RouteStep step) {
    switch (step.type) {
      case ManeuverType.depart:
      case ManeuverType.notification:
        return false;
      case ManeuverType.newName:
      case ManeuverType.continueOn:
        return step.modifier != ManeuverModifier.straight &&
            step.modifier != ManeuverModifier.none;
      default:
        return true;
    }
  }

  static String _key(LatLng p) =>
      '${p.latitude.toStringAsFixed(4)},${p.longitude.toStringAsFixed(4)}';

  void _cue(VoiceCueKind kind, String text) {
    if (!_cues.isClosed) _cues.add(VoiceCue(kind, text));
  }

  void _event(NavEventKind kind, String message) {
    if (!_events.isClosed) _events.add(NavEvent(kind, message));
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _requestSeq++;
    _location.removeListener(_onLocation);
    if (_state == NavState.navigating) {
      unawaited(_location.setBackgroundTracking(false));
    }
    _cues.close();
    _events.close();
    _progress.dispose();
    super.dispose();
  }
}

/// A fix matched onto the route.
class _Match {
  const _Match(this.segment, this.fraction, this.distance, this.point, this.along);

  final int segment;
  final double fraction;
  final double distance;
  final LatLng point;
  final double along;
}

/// One route prepared for guidance: per-segment bearings, maneuver positions along the
/// line, remaining-time sums, and the last matched segment.
class _Guide {
  _Guide(this.route)
      : _maneuverAlong = [
          for (final s in route.steps) route.cumulative[s.pointIndex],
        ],
        _bearings = List<double>.generate(
          route.points.length - 1,
          (i) => bearingDegrees(route.points[i], route.points[i + 1]),
        ) {
    final durations = route.segmentDurations;
    if (durations != null && durations.length == route.points.length - 1) {
      _segmentSuffix = List<double>.filled(durations.length + 1, 0);
      for (var i = durations.length - 1; i >= 0; i--) {
        _segmentSuffix![i] = _segmentSuffix![i + 1] + durations[i];
      }
    }
    _stepSuffix = List<double>.filled(route.steps.length + 1, 0);
    for (var i = route.steps.length - 1; i >= 0; i--) {
      _stepSuffix[i] =
          _stepSuffix[i + 1] + route.steps[i].duration.inMilliseconds / 1000;
    }
  }

  final NavRoute route;
  final List<double> _maneuverAlong;
  final List<double> _bearings;
  List<double>? _segmentSuffix;
  late final List<double> _stepSuffix;
  int _segment = 0;
  double _along = 0;

  /// A heading this far from a segment's direction makes it an unlikely match.
  static const _wrongDirection = 100.0;
  static const _wrongDirectionPenalty = 30.0;

  double segmentBearing(int segment) =>
      _bearings[segment.clamp(0, _bearings.length - 1)];

  /// Nearest segment around the last match: a little behind it for GPS jitter and as far
  /// ahead as ~20 s of driving. Segments against the car's heading are penalised, so a
  /// road that runs back next to itself (a U-turn, a flyover) does not steal the match.
  /// Only when nothing in the window is within [threshold] is the whole route searched,
  /// as after a GPS gap.
  _Match match(LatLng p, double? heading, double speed, double threshold) {
    final cum = route.cumulative;
    final segments = _bearings.length;
    var lo = _segment;
    while (lo > 0 && _along - cum[lo] < 60) {
      lo--;
    }
    final ahead = _along + math.max(300.0, speed * 20);
    var hi = _segment;
    while (hi < segments - 1 && cum[hi + 1] < ahead) {
      hi++;
    }
    var best = _search(p, heading, speed, lo, hi);
    if (best.distance > threshold) {
      final global = _search(p, heading, speed, 0, segments - 1);
      if (global.distance < best.distance) best = global;
    }
    _segment = best.segment;
    _along = best.along;
    return best;
  }

  _Match _search(LatLng p, double? heading, double speed, int lo, int hi) {
    final pts = route.points;
    final cum = route.cumulative;
    final useHeading = heading != null && speed > 3;
    _Match? best;
    var bestCost = double.infinity;
    for (var i = lo; i <= hi; i++) {
      final proj = projectOnSegment(p, pts[i], pts[i + 1]);
      var cost = proj.distance;
      if (useHeading && bearingDifference(heading, _bearings[i]) > _wrongDirection) {
        cost += _wrongDirectionPenalty;
      }
      // Ties go to the earlier segment: the car reaches it first.
      if (cost < bestCost) {
        bestCost = cost;
        best = _Match(i, proj.fraction, proj.distance, proj.point,
            cum[i] + proj.fraction * (cum[i + 1] - cum[i]));
      }
    }
    return best!;
  }

  NavProgress progress(_Match m, DateTime now, double speed, bool offRoute) {
    final steps = route.steps;
    // The step being driven: the last maneuver at or behind the car (3 m of slack, so
    // the banner moves on as the car reaches the turn).
    var current = 0;
    for (var i = 1; i < steps.length; i++) {
      if (_maneuverAlong[i] <= m.along + 3) {
        current = i;
      } else {
        break;
      }
    }
    final nextIndex = math.min(current + 1, steps.length - 1);
    final next = steps[nextIndex];
    final after = nextIndex + 1 < steps.length ? steps[nextIndex + 1] : null;
    final total = route.cumulative.last;

    final double seconds;
    final suffix = _segmentSuffix;
    if (suffix != null) {
      final segDuration = suffix[m.segment] - suffix[m.segment + 1];
      seconds = suffix[m.segment + 1] + segDuration * (1 - m.fraction);
    } else {
      final start = _maneuverAlong[current];
      final end = current + 1 < steps.length ? _maneuverAlong[current + 1] : total;
      final left = end > start ? ((end - m.along) / (end - start)).clamp(0.0, 1.0) : 0.0;
      final currentSeconds = steps[current].duration.inMilliseconds / 1000;
      seconds = _stepSuffix[current + 1] + currentSeconds * left;
    }
    final timeRemaining = Duration(milliseconds: (seconds * 1000).round());

    return NavProgress(
      stepIndex: current,
      nextStep: next,
      stepAfter: after,
      distanceToManeuver: math.max(0, _maneuverAlong[nextIndex] - m.along),
      distanceTravelled: m.along,
      distanceRemaining: math.max(0, total - m.along),
      timeRemaining: timeRemaining,
      eta: now.add(timeRemaining),
      snapped: m.point,
      segmentIndex: m.segment,
      bearing: segmentBearing(m.segment),
      speed: speed,
      distanceFromRoute: m.distance,
      offRoute: offRoute,
    );
  }

  /// True when [other] runs along this route's remaining part, within 50 m all the way.
  bool follows(NavRoute other) {
    final pts = route.points;
    var seg = _segment;
    final cum = other.cumulative;
    var nextSample = 0.0;
    for (var i = 0; i < other.points.length; i++) {
      if (cum[i] < nextSample && i != other.points.length - 1) continue;
      nextSample = cum[i] + 100;
      final p = other.points[i];
      var bestDistance = double.infinity;
      var bestSeg = seg;
      // Samples are 100 m apart, so the match is within the next few hundred metres;
      // bounded by distance, not points, so dense geometry stays cheap.
      final limit = route.cumulative[seg] + 400;
      for (var s = seg;
          s < pts.length - 1 && route.cumulative[s] <= limit;
          s++) {
        final d = projectOnSegment(p, pts[s], pts[s + 1]).distance;
        if (d < bestDistance) {
          bestDistance = d;
          bestSeg = s;
        }
      }
      if (bestDistance > 50) return false;
      seg = bestSeg;
    }
    return true;
  }
}
