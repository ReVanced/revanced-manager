import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/geo.dart';

NavRoute _route(List<Congestion>? congestion) {
  final points = <LatLng>[const LatLng(-6.2, 106.8)];
  for (var i = 0; i < 5; i++) {
    points.add(offsetPoint(points.last, 0, 100));
  }
  return NavRoute(
    points: points,
    cumulative: cumulativeDistances(points),
    distance: 500,
    duration: const Duration(seconds: 50),
    steps: const [],
    congestion: congestion,
  );
}

NavProgress _at(NavRoute route, int segment, double fraction) {
  final a = route.points[segment];
  final b = route.points[segment + 1];
  final snapped = LatLng(a.latitude + fraction * (b.latitude - a.latitude),
      a.longitude + fraction * (b.longitude - a.longitude));
  const step = RouteStep(
      type: ManeuverType.arrive,
      instruction: '',
      location: LatLng(0, 0),
      pointIndex: 0,
      distance: 0,
      duration: Duration.zero);
  return NavProgress(
    stepIndex: 0,
    nextStep: step,
    distanceToManeuver: 0,
    distanceTravelled: 0,
    distanceRemaining: 0,
    timeRemaining: Duration.zero,
    eta: DateTime(2026),
    snapped: snapped,
    segmentIndex: segment,
    bearing: 0,
    speed: 0,
    distanceFromRoute: 0,
  );
}

void main() {
  test('travelled and remaining split at the snapped point', () {
    final route = _route(null);
    final p = _at(route, 2, 0.5);
    final travelled = route.travelled(p);
    final remaining = route.remaining(p);
    expect(travelled, [...route.points.take(3), p.snapped]);
    expect(remaining, [p.snapped, ...route.points.skip(3)]);
    expect(route.remaining(null), route.points);
  });

  test('remaining runs follow congestion and share their joints', () {
    final route = _route(const [
      Congestion.low,
      Congestion.low,
      Congestion.heavy,
      Congestion.heavy,
      Congestion.severe,
    ]);
    final runs = route.remainingRuns(_at(route, 1, 0.5));
    expect(runs.map((r) => r.congestion),
        [Congestion.low, Congestion.heavy, Congestion.severe]);
    expect(runs[0].points, [_at(route, 1, 0.5).snapped, route.points[2]]);
    expect(runs[1].points, [route.points[2], route.points[3], route.points[4]]);
    expect(runs[2].points, [route.points[4], route.points[5]]);

    final whole = route.remainingRuns(null);
    expect(whole.first.points.first, route.points.first);
    expect(whole, hasLength(3));
  });

  test('a route without traffic is one unknown run', () {
    final route = _route(null);
    final runs = route.remainingRuns(_at(route, 3, 0.2));
    expect(runs.single.congestion, Congestion.unknown);
    expect(runs.single.points.length, 3);
  });

  test('places compare by name and position and survive JSON', () {
    const a = Place(name: 'Monas', address: 'Gambir', location: LatLng(-6.17, 106.82));
    const b = Place(name: 'Monas', location: LatLng(-6.17, 106.82));
    expect(a, b);
    expect(a.hashCode, b.hashCode);
    expect(Place.fromJson(a.toJson()), a);
    expect(Place.fromJson(a.toJson()).address, 'Gambir');
    expect(a.copyWith(name: 'Monumen Nasional'), isNot(a));
  });
}
