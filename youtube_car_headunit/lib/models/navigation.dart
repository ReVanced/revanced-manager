import 'package:latlong2/latlong.dart';

/// Where guidance is: nothing set, a route shown before "Mulai", driving it, or there.
enum NavState { idle, preview, navigating, arrived }

/// Which service computed a route. Mapbox is offered only when the build carries a token.
enum RouteProvider { mapbox, osrm }

/// Traffic on one segment of a route, as Mapbox reports it; OSRM routes have none.
enum Congestion { unknown, low, moderate, heavy, severe }

/// A destination: a search result, a long-pressed point or a recent destination.
class Place {
  const Place({required this.name, this.address, required this.location});

  /// Shown until reverse geocoding names a long-pressed point.
  static const String pinName = 'Titik di peta';

  final String name;

  /// Second line under the name (street, district, city); null when unknown.
  final String? address;
  final LatLng location;

  Place copyWith({String? name, String? address}) => Place(
        name: name ?? this.name,
        address: address ?? this.address,
        location: location,
      );

  Map<String, Object?> toJson() => {
        'name': name,
        if (address != null) 'address': address,
        'lat': location.latitude,
        'lon': location.longitude,
      };

  factory Place.fromJson(Map<String, Object?> json) => Place(
        name: json['name'] as String,
        address: json['address'] as String?,
        location: LatLng(
          (json['lat'] as num).toDouble(),
          (json['lon'] as num).toDouble(),
        ),
      );

  @override
  bool operator ==(Object other) =>
      other is Place && other.name == name && other.location == location;

  @override
  int get hashCode => Object.hash(name, location);

  @override
  String toString() => 'Place($name, ${location.latitude}, ${location.longitude})';
}

/// OSRM maneuver types (`maneuver.type`), shared by Mapbox.
enum ManeuverType {
  depart,
  arrive,
  turn,
  newName,
  continueOn,
  merge,
  onRamp,
  offRamp,
  fork,
  endOfRoad,
  roundabout,
  rotary,
  roundaboutTurn,
  exitRoundabout,
  exitRotary,
  notification,
}

/// OSRM maneuver modifiers (`maneuver.modifier`).
enum ManeuverModifier {
  none,
  uturn,
  sharpRight,
  right,
  slightRight,
  straight,
  slightLeft,
  left,
  sharpLeft,
}

/// One maneuver of a route and the stretch that follows it up to the next maneuver.
class RouteStep {
  const RouteStep({
    required this.type,
    this.modifier = ManeuverModifier.none,
    this.exit,
    this.name = '',
    this.ref,
    required this.instruction,
    required this.location,
    required this.pointIndex,
    required this.distance,
    required this.duration,
  });

  final ManeuverType type;
  final ManeuverModifier modifier;

  /// Roundabout exit to take, counted from the entry; null when not a roundabout.
  final int? exit;

  /// Street after the maneuver; empty when the road has no name.
  final String name;

  /// Road number, e.g. a toll road's code; null when none.
  final String? ref;

  /// Full Indonesian instruction, e.g. "Belok kiri ke Jalan Sudirman".
  final String instruction;

  /// The maneuver point.
  final LatLng location;

  /// Index of [location] in [NavRoute.points].
  final int pointIndex;

  /// Metres from this maneuver to the next one.
  final double distance;
  final Duration duration;

  /// Name, else road number, else empty: what the banner shows under the arrow.
  String get roadLabel => name.isNotEmpty ? name : (ref ?? '');
}

/// A consecutive run of the route line with one congestion level, drawn as one polyline.
class RouteRun {
  const RouteRun(this.congestion, this.points);

  final Congestion congestion;
  final List<LatLng> points;
}

/// A drivable route from the car to the destination.
class NavRoute {
  NavRoute({
    required this.points,
    required this.cumulative,
    required this.distance,
    required this.duration,
    required this.steps,
    this.summary = '',
    this.provider = RouteProvider.osrm,
    this.congestion,
    this.segmentDurations,
    this.hasTolls = false,
    this.uuid,
    this.routeIndex = 0,
  });

  /// Full geometry of the route.
  final List<LatLng> points;

  /// Metres along the route at each of [points]; the last one is the route length.
  final List<double> cumulative;

  /// Metres.
  final double distance;

  /// Provider's estimate: typical speeds for OSRM, live traffic for Mapbox.
  final Duration duration;

  /// Depart first, arrive last.
  final List<RouteStep> steps;

  /// Main roads on the way, e.g. "Jalan Sudirman, Jalan Thamrin"; may be empty.
  final String summary;
  final RouteProvider provider;

  /// Traffic per segment: `congestion[i]` covers `points[i]` to `points[i + 1]`, so the
  /// list is one shorter than [points]. Null when the provider has no traffic (OSRM).
  final List<Congestion>? congestion;

  /// Seconds per segment, aligned like [congestion]; null when the provider gives none.
  /// Remaining time follows these when present, else the step durations.
  final List<double>? segmentDurations;

  /// True when the route uses a toll road somewhere.
  final bool hasTolls;

  /// Mapbox response id and route index, for the Directions Refresh API; null for OSRM.
  final String? uuid;
  final int routeIndex;

  /// The driven part of the line: from the start to the car's snapped position.
  List<LatLng> travelled(NavProgress progress) {
    final end = progress.segmentIndex.clamp(0, points.length - 1);
    return [...points.sublist(0, end + 1), progress.snapped];
  }

  /// The part still ahead: from the car's snapped position to the destination.
  List<LatLng> remaining(NavProgress? progress) {
    if (progress == null) return points;
    final start = (progress.segmentIndex + 1).clamp(0, points.length);
    return [progress.snapped, ...points.sublist(start)];
  }

  /// The part still ahead split into runs of equal congestion, for a traffic-colored line.
  /// One [Congestion.unknown] run when the route has no traffic data.
  List<RouteRun> remainingRuns(NavProgress? progress) {
    final levels = congestion;
    if (levels == null || levels.length != points.length - 1) {
      return [RouteRun(Congestion.unknown, remaining(progress))];
    }
    final firstSegment = progress == null
        ? 0
        : progress.segmentIndex.clamp(0, points.length - 2);
    final runs = <RouteRun>[];
    var level = levels[firstSegment];
    var run = <LatLng>[progress?.snapped ?? points[firstSegment]];
    for (var i = firstSegment; i < levels.length; i++) {
      if (levels[i] != level) {
        runs.add(RouteRun(level, run));
        level = levels[i];
        // Runs share their joint point so the line has no gaps.
        run = [run.last];
      }
      run.add(points[i + 1]);
    }
    runs.add(RouteRun(level, run));
    return runs;
  }
}

/// What the routing services return for one request.
class RouteResult {
  const RouteResult({
    required this.routes,
    required this.provider,
    this.tollsAvoided = false,
    this.fellBack = false,
  });

  /// Best first; alternatives follow.
  final List<NavRoute> routes;
  final RouteProvider provider;

  /// True when tolls were asked to be avoided and the server honoured it.
  final bool tollsAvoided;

  /// True when Mapbox failed and OSRM answered instead.
  final bool fellBack;
}

/// Where the car is along the active route, recomputed on every GPS fix.
class NavProgress {
  const NavProgress({
    required this.stepIndex,
    required this.nextStep,
    this.stepAfter,
    required this.distanceToManeuver,
    required this.distanceTravelled,
    required this.distanceRemaining,
    required this.timeRemaining,
    required this.eta,
    required this.snapped,
    required this.segmentIndex,
    required this.bearing,
    required this.speed,
    required this.distanceFromRoute,
    this.offRoute = false,
  });

  /// Step being driven; its maneuver is behind the car.
  final int stepIndex;

  /// The maneuver ahead (the arrive step on the last stretch).
  final RouteStep nextStep;

  /// The maneuver after [nextStep], for the banner's second line.
  final RouteStep? stepAfter;

  /// Metres along the route to [nextStep].
  final double distanceToManeuver;
  final double distanceTravelled;
  final double distanceRemaining;
  final Duration timeRemaining;
  final DateTime eta;

  /// The fix projected onto the route line.
  final LatLng snapped;

  /// [snapped] lies between `points[segmentIndex]` and `points[segmentIndex + 1]`.
  final int segmentIndex;

  /// Direction of the route at [snapped], degrees clockwise from north.
  final double bearing;

  /// Metres per second.
  final double speed;

  /// Metres between the fix and the route line.
  final double distanceFromRoute;

  /// The fix is beyond the off-route threshold; a reroute follows if it stays there.
  final bool offRoute;
}

enum VoiceCueKind { depart, approach, maneuver, arrive, reroute }

/// A prompt to speak, in Indonesian. The controller decides when; the voice only speaks.
class VoiceCue {
  const VoiceCue(this.kind, this.text);

  final VoiceCueKind kind;
  final String text;

  @override
  String toString() => 'VoiceCue($kind, $text)';
}

enum NavEventKind {
  rerouting,
  rerouted,
  rerouteFailed,

  /// Avoiding tolls was asked but the server could not; the route may use tolls.
  tollsNotAvoided,

  /// Mapbox failed (network, 401/429, quota) and OSRM computed the route instead.
  providerFallback,
  arrived,
}

/// Something worth a short notice on screen; [message] is Indonesian.
class NavEvent {
  const NavEvent(this.kind, this.message);

  final NavEventKind kind;
  final String message;

  @override
  String toString() => 'NavEvent($kind, $message)';
}

/// A failure with a driver-facing Indonesian [message].
class NavException implements Exception {
  const NavException(this.message);

  final String message;

  @override
  String toString() => message;
}
