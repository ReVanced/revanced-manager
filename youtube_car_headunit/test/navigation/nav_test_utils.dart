import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:latlong2/latlong.dart';
import 'package:youtube_car_headunit/controllers/location_controller.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/geo.dart';
import 'package:youtube_car_headunit/services/geocoding_service.dart';
import 'package:youtube_car_headunit/services/polyline.dart';
import 'package:youtube_car_headunit/services/routing_service.dart';

String fixture(String name) =>
    File('test/fixtures/navigation/$name').readAsStringSync();

/// Lets queued microtasks and futures complete.
Future<void> settle() async {
  for (var i = 0; i < 10; i++) {
    await Future<void>.delayed(Duration.zero);
  }
}

const routeStart = LatLng(-6.2000, 106.8000);

/// One leg of a synthetic route: a heading, a length and the maneuver at its start.
class Leg {
  const Leg(this.bearing, this.meters,
      {this.type = ManeuverType.turn,
      this.modifier = ManeuverModifier.none,
      this.instruction = '',
      this.seconds});

  final double bearing;
  final double meters;
  final ManeuverType type;
  final ManeuverModifier modifier;
  final String instruction;
  final double? seconds;
}

/// A route made of straight legs with a point every ~50 m, like a real full geometry.
/// Steps: the first leg's depart, one maneuver at the start of every further leg, and
/// arrive at the end. Leg durations default to 10 m/s.
NavRoute syntheticRoute(
  List<Leg> legs, {
  LatLng start = routeStart,
  RouteProvider provider = RouteProvider.osrm,
  Congestion? congestion,
  double secondsPerSegment = 5,
}) {
  final points = <LatLng>[start];
  final legStart = <int>[];
  for (final leg in legs) {
    legStart.add(points.length - 1);
    final n = (leg.meters / 50).ceil();
    for (var i = 0; i < n; i++) {
      points.add(offsetPoint(points.last, leg.bearing, leg.meters / n));
    }
  }
  final cumulative = cumulativeDistances(points);
  final steps = <RouteStep>[
    for (var i = 0; i < legs.length; i++)
      RouteStep(
        type: i == 0 ? ManeuverType.depart : legs[i].type,
        modifier: legs[i].modifier,
        name: 'Jalan ${i + 1}',
        instruction: legs[i].instruction.isEmpty
            ? (i == 0 ? 'Arah utara di Jalan 1' : 'Belok ${i + 1}')
            : legs[i].instruction,
        location: points[legStart[i]],
        pointIndex: legStart[i],
        distance: legs[i].meters,
        duration: Duration(
            milliseconds: ((legs[i].seconds ?? legs[i].meters / 10) * 1000).round()),
      ),
    RouteStep(
      type: ManeuverType.arrive,
      instruction: 'Anda telah tiba di tujuan',
      location: points.last,
      pointIndex: points.length - 1,
      distance: 0,
      duration: Duration.zero,
    ),
  ];
  final seconds = legs.fold<double>(0, (s, l) => s + (l.seconds ?? l.meters / 10));
  return NavRoute(
    points: points,
    cumulative: cumulative,
    distance: cumulative.last,
    duration: Duration(milliseconds: (seconds * 1000).round()),
    steps: steps,
    provider: provider,
    congestion: congestion == null
        ? null
        : List.filled(points.length - 1, congestion),
    segmentDurations: congestion == null
        ? null
        : List.filled(points.length - 1, secondsPerSegment),
  );
}

/// North 1000 m, right 600 m, left 400 m.
NavRoute lRoute({RouteProvider provider = RouteProvider.osrm, Congestion? congestion}) =>
    syntheticRoute(
      const [
        Leg(0, 1000),
        Leg(90, 600,
            modifier: ManeuverModifier.right,
            instruction: 'Belok kanan ke Jalan Satu'),
        Leg(0, 400,
            modifier: ManeuverModifier.left,
            instruction: 'Belok kiri ke Jalan Dua'),
      ],
      provider: provider,
      congestion: congestion,
    );

/// The point [meters] along [route], moved [side] metres to the right of it.
LatLng pointAlong(NavRoute route, double meters, {double side = 0}) {
  final cum = route.cumulative;
  var i = 0;
  while (i < cum.length - 2 && cum[i + 1] < meters) {
    i++;
  }
  final a = route.points[i];
  final b = route.points[i + 1];
  final bearing = bearingDegrees(a, b);
  final p = offsetPoint(a, bearing, meters - cum[i]);
  return side == 0 ? p : offsetPoint(p, (bearing + 90) % 360, side);
}

double bearingAlong(NavRoute route, double meters) {
  final cum = route.cumulative;
  var i = 0;
  while (i < cum.length - 2 && cum[i + 1] < meters) {
    i++;
  }
  return bearingDegrees(route.points[i], route.points[i + 1]);
}

/// A LocationController without the plugin that records background-tracking requests.
class TestLocation extends LocationController {
  TestLocation() : super(enabled: false);

  final List<bool> backgroundCalls = [];

  @override
  Future<void> setBackgroundTracking(bool on) async => backgroundCalls.add(on);
}

class RouteCall {
  RouteCall(this.from, this.to, this.provider, this.avoidTolls, this.alternatives,
      this.heading);

  final LatLng from;
  final LatLng to;
  final RouteProvider provider;
  final bool avoidTolls;
  final bool alternatives;
  final double? heading;
}

/// Answers route requests from a queue: a [RouteResult], a [NavException], or a
/// [Completer] for replies the test releases later.
class FakeRouting implements RoutingService {
  FakeRouting({this.mapbox = false});

  final bool mapbox;
  final List<RouteCall> calls = [];
  final List<Object> replies = [];

  @override
  bool get hasMapbox => mapbox;

  @override
  Future<RouteResult> route(LatLng from, LatLng to,
      {RouteProvider provider = RouteProvider.osrm,
      bool avoidTolls = false,
      bool alternatives = true,
      double? heading}) async {
    calls.add(RouteCall(from, to, provider, avoidTolls, alternatives, heading));
    if (replies.isEmpty) throw const NavException('no reply queued');
    final reply = replies.removeAt(0);
    if (reply is Completer<RouteResult>) return reply.future;
    if (reply is NavException) throw reply;
    return reply as RouteResult;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class FakeGeocoding implements GeocodingService {
  final Map<String, List<Place>> results = {};
  Place? reverseResult;
  final List<LatLng> reverseCalls = [];

  @override
  Future<List<Place>> search(String query, {LatLng? near, int limit = 8}) async =>
      results[query] ?? const [];

  @override
  Future<Place?> reverse(LatLng point) async {
    reverseCalls.add(point);
    return reverseResult;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

RouteResult osrmResult(List<NavRoute> routes,
        {bool tollsAvoided = false, bool fellBack = false}) =>
    RouteResult(
      routes: routes,
      provider: routes.first.provider,
      tollsAvoided: tollsAvoided,
      fellBack: fellBack,
    );

/// A minimal hand-written Mapbox Directions response (Mapbox's terms forbid storing
/// real ones): a straight route north then east, Indonesian text when [locale] is `id`,
/// congestion and duration annotations per segment.
String mapboxResponse({
  String locale = 'id',
  bool annotations = true,
  int annotationTrim = 0,
  bool toll = false,
}) {
  final points = <LatLng>[routeStart];
  for (var i = 0; i < 4; i++) {
    points.add(offsetPoint(points.last, 0, 100));
  }
  for (var i = 0; i < 3; i++) {
    points.add(offsetPoint(points.last, 90, 100));
  }
  List<double> ll(LatLng p) => [p.longitude, p.latitude];
  final segments = points.length - 1;
  const levels = ['low', 'low', 'moderate', 'heavy', 'severe', 'unknown', 'low'];
  return jsonEncode({
    'code': 'Ok',
    'uuid': 'test-uuid',
    'routes': [
      {
        'geometry': encodePolyline(points),
        'distance': 700.0,
        'duration': 95.5,
        'voiceLocale': locale,
        'legs': [
          {
            'summary': 'Jalan Utara, Jalan Timur',
            'distance': 700.0,
            'duration': 95.5,
            if (annotations)
              'annotation': {
                'congestion': levels.take(segments - annotationTrim).toList(),
                'duration': List.filled(segments - annotationTrim, 13.5),
                'distance': List.filled(segments - annotationTrim, 100.0),
              },
            'steps': [
              {
                'name': 'Jalan Utara',
                'distance': 400.0,
                'duration': 50.0,
                'driving_side': 'right',
                'mode': 'driving',
                'geometry': encodePolyline(points.sublist(0, 5)),
                'maneuver': {
                  'type': 'depart',
                  'location': ll(points[0]),
                  'bearing_after': 0,
                  'instruction': 'Berkendara ke utara di Jalan Utara',
                },
                'intersections': [
                  {
                    'location': ll(points[0]),
                    'geometry_index': 0,
                    if (toll) 'classes': ['toll'],
                  },
                ],
              },
              {
                'name': 'Jalan Timur',
                'distance': 300.0,
                'duration': 45.5,
                'driving_side': 'right',
                'mode': 'driving',
                'geometry': encodePolyline(points.sublist(4)),
                'maneuver': {
                  'type': 'turn',
                  'modifier': 'right',
                  'location': ll(points[4]),
                  'bearing_after': 90,
                  'instruction': locale == 'id'
                      ? 'Belok kanan ke Jalan Timur'
                      : 'Turn right onto Jalan Timur',
                },
                'intersections': [
                  {'location': ll(points[4]), 'geometry_index': 4},
                ],
              },
              {
                'name': 'Jalan Timur',
                'distance': 0.0,
                'duration': 0.0,
                'driving_side': 'right',
                'mode': 'driving',
                'geometry': encodePolyline([points.last, points.last]),
                'maneuver': {
                  'type': 'arrive',
                  'location': ll(points.last),
                  'instruction': locale == 'id'
                      ? 'Anda telah tiba di tujuan'
                      : 'You have arrived at your destination',
                },
                'intersections': [
                  {'location': ll(points.last), 'geometry_index': segments},
                ],
              },
            ],
          },
        ],
      },
    ],
  });
}
