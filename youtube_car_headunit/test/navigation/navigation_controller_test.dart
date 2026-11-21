import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:youtube_car_headunit/controllers/navigation_controller.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/geo.dart';
import 'package:youtube_car_headunit/services/recent_destinations.dart';

import 'nav_test_utils.dart';

/// A controller on fakes, with a clock the test moves and every cue and event recorded.
class _Rig {
  _Rig({bool mapbox = false, NavigationStore? store}) {
    routing = FakeRouting(mapbox: mapbox);
    nav = NavigationController(
      location: location,
      geocoding: geocoding,
      routing: routing,
      store: store,
      now: () => now,
    );
    nav.cues.listen(cues.add);
    nav.events.listen(events.add);
  }

  final TestLocation location = TestLocation();
  final FakeGeocoding geocoding = FakeGeocoding();
  late final FakeRouting routing;
  late final NavigationController nav;
  DateTime now = DateTime(2026, 11, 21, 8);
  final List<VoiceCue> cues = [];
  final List<NavEvent> events = [];

  NavRoute get route => nav.route!;

  /// Puts the car [meters] along the route ([side] metres to its right), [seconds]
  /// after the previous fix.
  void driveTo(double meters,
      {double side = 0, double speed = 14, double? heading, int seconds = 1}) {
    now = now.add(Duration(seconds: seconds));
    location.injectPosition(
      pointAlong(route, meters, side: side),
      heading: heading ?? bearingAlong(route, meters),
      speed: speed,
      accuracy: 5,
    );
  }

  /// Preview to the end of [route] and start guidance on it.
  Future<void> startOn(NavRoute route) async {
    location.injectPosition(route.points.first, speed: 0);
    routing.replies.add(osrmResult([route]));
    await nav.previewTo(Place(name: 'Tujuan', location: route.points.last));
    nav.start();
    await settle();
  }

  void dispose() => nav.dispose();
}

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  group('preview', () {
    test('without a position there is nothing to route from', () async {
      final rig = _Rig();
      await rig.nav.previewTo(const Place(name: 'Monas', location: LatLng(-6.17, 106.82)));
      expect(rig.nav.state, NavState.preview);
      expect(rig.nav.error, contains('Lokasi belum tersedia'));
      expect(rig.routing.calls, isEmpty);
      rig.dispose();
    });

    test('routes from the car with alternatives, then one can be picked', () async {
      final rig = _Rig();
      final a = lRoute();
      final b = syntheticRoute(const [Leg(90, 600), Leg(0, 1400)]);
      rig.location.injectPosition(routeStart, heading: 10, speed: 3);
      rig.routing.replies.add(osrmResult([a, b]));
      final destination = Place(name: 'Kantor', location: a.points.last);
      final notified = <bool>[];
      rig.nav.addListener(() => notified.add(rig.nav.loading));

      await rig.nav.previewTo(destination);

      final call = rig.routing.calls.single;
      expect(call.from, routeStart);
      expect(call.to, destination.location);
      expect(call.alternatives, isTrue);
      expect(call.provider, RouteProvider.osrm);
      expect(call.heading, 10);
      expect(rig.nav.routes, [a, b]);
      expect(rig.nav.route, a);
      expect(rig.nav.loading, isFalse);
      expect(notified, contains(true));
      rig.nav.selectRoute(1);
      expect(rig.nav.route, b);
      expect(rig.nav.progress.value, isNull);
      rig.dispose();
    });

    test('a newer destination wins over a slower earlier reply', () async {
      final rig = _Rig();
      rig.location.injectPosition(routeStart);
      final slow = Completer<RouteResult>();
      final fast = Completer<RouteResult>();
      rig.routing.replies.addAll([slow, fast]);
      final first = rig.nav.previewTo(const Place(name: 'A', location: LatLng(-6.1, 106.8)));
      final second =
          rig.nav.previewTo(const Place(name: 'B', location: LatLng(-6.3, 106.8)));
      final newer = lRoute();
      fast.complete(osrmResult([newer]));
      await second;
      slow.complete(osrmResult([syntheticRoute(const [Leg(90, 300)])]));
      await first;
      expect(rig.nav.destination!.name, 'B');
      expect(rig.nav.routes, [newer]);
      rig.dispose();
    });

    test('a long-pressed point is named by reverse geocoding', () async {
      final rig = _Rig();
      rig.location.injectPosition(routeStart);
      rig.routing.replies.add(osrmResult([lRoute()]));
      rig.geocoding.reverseResult = const Place(
          name: 'Monumen Nasional', address: 'Gambir', location: LatLng(0, 0));
      const point = LatLng(-6.1754, 106.8272);
      await rig.nav.previewPoint(point);
      await settle();
      expect(rig.geocoding.reverseCalls, [point]);
      expect(rig.nav.destination!.name, 'Monumen Nasional');
      expect(rig.nav.destination!.address, 'Gambir');
      expect(rig.nav.destination!.location, point);
      rig.dispose();
    });

    test('route errors are shown and retry asks again', () async {
      final rig = _Rig();
      rig.location.injectPosition(routeStart);
      rig.routing.replies.add(const NavException('Rute tidak ditemukan ke tujuan ini.'));
      await rig.nav.previewTo(const Place(name: 'Pulau', location: LatLng(-5.6, 106.6)));
      expect(rig.nav.error, 'Rute tidak ditemukan ke tujuan ini.');
      expect(rig.nav.routes, isEmpty);
      rig.routing.replies.add(osrmResult([lRoute()]));
      await rig.nav.retry();
      expect(rig.nav.error, isNull);
      expect(rig.nav.routes, hasLength(1));
      rig.dispose();
    });
  });

  group('providers and tolls', () {
    test('without a Mapbox token the provider is OSRM and cannot change', () async {
      final rig = _Rig();
      expect(rig.nav.hasMapbox, isFalse);
      expect(rig.nav.provider, RouteProvider.osrm);
      await rig.nav.setProvider(RouteProvider.mapbox);
      expect(rig.nav.provider, RouteProvider.osrm);
      rig.dispose();
    });

    test('with a token Mapbox is the default; a switch is stored and refetches',
        () async {
      final store = await NavigationStore.load();
      final rig = _Rig(mapbox: true, store: store);
      expect(rig.nav.provider, RouteProvider.mapbox);
      rig.location.injectPosition(routeStart);
      rig.routing.replies
          .addAll([osrmResult([lRoute(provider: RouteProvider.mapbox)]), osrmResult([lRoute()])]);
      await rig.nav.previewTo(const Place(name: 'X', location: LatLng(-6.19, 106.8)));
      await rig.nav.setProvider(RouteProvider.osrm);
      expect(rig.routing.calls.map((c) => c.provider),
          [RouteProvider.mapbox, RouteProvider.osrm]);
      expect(store.provider, RouteProvider.osrm);
      expect(_Rig(mapbox: true, store: store).nav.provider, RouteProvider.osrm);
      rig.dispose();
    });

    test('Mapbox failing over to OSRM is announced', () async {
      final rig = _Rig(mapbox: true);
      rig.location.injectPosition(routeStart);
      rig.routing.replies.add(osrmResult([lRoute()], fellBack: true));
      await rig.nav.previewTo(const Place(name: 'X', location: LatLng(-6.19, 106.8)));
      await settle();
      expect(rig.events.map((e) => e.kind), [NavEventKind.providerFallback]);
      rig.dispose();
    });

    test('toll notice only for Mapbox, once per destination', () async {
      final rig = _Rig(mapbox: true);
      rig.location.injectPosition(routeStart);
      final mapboxTolls = RouteResult(
          routes: [lRoute(provider: RouteProvider.mapbox)],
          provider: RouteProvider.mapbox);
      rig.routing.replies.addAll([mapboxTolls, mapboxTolls]);
      await rig.nav.setAvoidTolls(true); // idle: nothing to refetch
      await rig.nav.previewTo(const Place(name: 'X', location: LatLng(-6.19, 106.8)));
      await rig.nav.retry();
      await settle();
      expect(rig.events.map((e) => e.kind), [NavEventKind.tollsNotAvoided]);
      expect(rig.routing.calls.every((c) => c.avoidTolls), isTrue);

      await rig.nav.setProvider(RouteProvider.osrm);
      rig.routing.replies.add(osrmResult([lRoute()]));
      await rig.nav.retry();
      await settle();
      expect(rig.events, hasLength(1), reason: 'OSRM never reports tolls');
      rig.dispose();
    });

    test('the toll switch under OSRM is stored but asks nothing', () async {
      final store = await NavigationStore.load();
      final rig = _Rig(store: store);
      rig.location.injectPosition(routeStart);
      rig.routing.replies.add(osrmResult([lRoute()]));
      await rig.nav.previewTo(const Place(name: 'X', location: LatLng(-6.19, 106.8)));
      await rig.nav.setAvoidTolls(true);
      expect(rig.routing.calls, hasLength(1));
      expect(store.avoidTolls, isTrue);
      rig.dispose();
    });
  });

  group('guidance', () {
    test('start: one route, recents, background tracking, depart prompt', () async {
      final store = await NavigationStore.load();
      final rig = _Rig(store: store);
      final route = lRoute();
      await rig.startOn(route);
      expect(rig.nav.state, NavState.navigating);
      expect(rig.nav.routes, [route]);
      expect(rig.location.backgroundCalls, [true]);
      expect(store.recents.single.name, 'Tujuan');
      expect(rig.nav.recentDestinations.single.name, 'Tujuan');
      expect(rig.cues.first, isA<VoiceCue>()
          .having((c) => c.kind, 'kind', VoiceCueKind.depart)
          .having((c) => c.text, 'text', 'Arah utara di Jalan 1'));
      expect(rig.nav.progress.value, isNotNull);
      rig.dispose();
    });

    test('progress per fix: step, distances, time and ETA', () async {
      final rig = _Rig();
      await rig.startOn(lRoute());
      var notifications = 0;
      rig.nav.addListener(() => notifications++);

      rig.driveTo(200);
      var p = rig.nav.progress.value!;
      expect(p.stepIndex, 0);
      expect(p.nextStep.instruction, 'Belok kanan ke Jalan Satu');
      expect(p.stepAfter!.instruction, 'Belok kiri ke Jalan Dua');
      expect(p.distanceToManeuver, closeTo(800, 2));
      expect(p.distanceTravelled, closeTo(200, 2));
      expect(p.distanceRemaining, closeTo(1800, 3));
      // 80 s left of the first leg's 100 s, then 60 s and 40 s.
      expect(p.timeRemaining.inSeconds, closeTo(180, 1));
      expect(p.eta, rig.now.add(p.timeRemaining));
      expect(p.bearing, closeTo(0, 1));
      expect(p.speed, 14);
      expect(p.offRoute, isFalse);

      rig.driveTo(1200);
      p = rig.nav.progress.value!;
      expect(p.stepIndex, 1);
      expect(p.nextStep.instruction, 'Belok kiri ke Jalan Dua');
      expect(p.stepAfter!.type, ManeuverType.arrive);
      expect(p.distanceToManeuver, closeTo(400, 3));
      expect(p.bearing, closeTo(90, 1));

      expect(notifications, 0, reason: 'fixes go through progress only');
      rig.dispose();
    });

    test('GPS noise beside the road stays on route, snapped onto the line', () async {
      final rig = _Rig();
      await rig.startOn(lRoute());
      for (var i = 1; i <= 6; i++) {
        rig.driveTo(100.0 * i, side: i.isEven ? 20 : -20);
        final p = rig.nav.progress.value!;
        expect(p.offRoute, isFalse);
        expect(p.distanceFromRoute, closeTo(20, 1));
        expect(distanceMeters(p.snapped, pointAlong(rig.route, 100.0 * i)), lessThan(2));
      }
      expect(rig.routing.calls, hasLength(1), reason: 'only the preview');
      rig.dispose();
    });

    test('a road running back beside itself does not steal the match', () async {
      final rig = _Rig();
      // North 500 m, east 20 m, back south 500 m: two carriageways 20 m apart.
      final hairpin = syntheticRoute(const [
        Leg(0, 500),
        Leg(90, 20, modifier: ManeuverModifier.right),
        Leg(180, 500, modifier: ManeuverModifier.right),
      ]);
      await rig.startOn(hairpin);
      // The destination is 20 m from the start, but 1 km of route away.
      expect(rig.nav.state, NavState.navigating);
      // Going north 12 m east of the road: closer to the southbound side (8 m).
      rig.driveTo(250, side: 12, heading: 0);
      var p = rig.nav.progress.value!;
      expect(p.distanceTravelled, closeTo(250, 3));
      expect(p.stepIndex, 0);
      rig.driveTo(400, side: 12, heading: 0);
      rig.driveTo(510, heading: 90);
      // Coming back south 12 m west of the southbound side.
      rig.driveTo(700, side: 12, heading: 180);
      p = rig.nav.progress.value!;
      expect(p.distanceTravelled, closeTo(700, 3));
      expect(p.stepIndex, 2);
      expect(p.offRoute, isFalse);
      rig.dispose();
    });

    test('off route for 4 s reroutes from the car, keeping its heading', () async {
      final rig = _Rig();
      await rig.startOn(lRoute());
      rig.driveTo(300);
      for (var i = 0; i < 4; i++) {
        rig.driveTo(300.0 + i, side: 80, heading: 0, speed: 10);
        expect(rig.nav.progress.value!.offRoute, isTrue);
      }
      expect(rig.routing.calls, hasLength(1), reason: 'held under 4 s: no reroute yet');

      final detour = syntheticRoute(const [Leg(270, 80), Leg(0, 1700)],
          start: pointAlong(rig.route, 305, side: 80));
      rig.routing.replies.add(osrmResult([detour]));
      rig.driveTo(305, side: 80, heading: 0, speed: 10);
      await settle();

      final call = rig.routing.calls.last;
      expect(call.alternatives, isFalse);
      expect(call.heading, 0);
      expect(call.from, pointAlong(lRoute(), 305, side: 80));
      expect(rig.nav.route, detour);
      expect(rig.nav.rerouting, isFalse);
      expect(rig.events.map((e) => e.kind),
          [NavEventKind.rerouting, NavEventKind.rerouted]);
      expect(rig.cues.map((c) => c.kind), contains(VoiceCueKind.reroute));
      expect(rig.nav.progress.value!.offRoute, isFalse);
      rig.dispose();
    });

    test('driving against the route counts as off route', () async {
      final rig = _Rig();
      await rig.startOn(lRoute());
      rig.routing.replies.add(osrmResult([lRoute()]));
      for (var i = 0; i < 6; i++) {
        rig.driveTo(500.0 - i * 10, heading: 180, speed: 10);
      }
      await settle();
      expect(rig.routing.calls, hasLength(2));
      rig.dispose();
    });

    test('a failed reroute keeps the old route and backs off', () async {
      final rig = _Rig();
      await rig.startOn(lRoute());
      final original = rig.route;
      rig.routing.replies.add(const NavException('Server rute tidak dapat dihubungi.'));
      for (var i = 0; i <= 4; i++) {
        rig.driveTo(300.0 + i, side: 90, heading: 0, speed: 10);
      }
      await settle();
      expect(rig.routing.calls, hasLength(2));
      expect(rig.nav.route, same(original));
      expect(rig.events.last.kind, NavEventKind.rerouteFailed);

      // Cooldown doubled to 20 s after the failure.
      rig.routing.replies.add(osrmResult([lRoute()]));
      for (var i = 5; i < 24; i++) {
        rig.driveTo(300.0 + i, side: 90, heading: 0, speed: 10);
      }
      await settle();
      expect(rig.routing.calls, hasLength(2));
      rig.driveTo(325, side: 90, heading: 0, speed: 10);
      await settle();
      expect(rig.routing.calls, hasLength(3));
      rig.dispose();
    });

    test('arrival ends guidance with a prompt and a notice', () async {
      final rig = _Rig();
      await rig.startOn(lRoute());
      rig.driveTo(1900);
      expect(rig.nav.state, NavState.navigating);
      rig.driveTo(1985);
      await settle();
      expect(rig.nav.state, NavState.arrived);
      final frozen = rig.nav.progress.value!;
      expect(frozen.distanceRemaining, 0);
      expect(frozen.timeRemaining, Duration.zero);
      expect(frozen.nextStep.type, ManeuverType.arrive);
      // A later fix far away (the simulator handing back to real GPS) changes nothing.
      rig.location.injectPosition(routeStart, speed: 0);
      expect(rig.nav.progress.value, same(frozen));
      expect(rig.location.backgroundCalls, [true, false]);
      expect(rig.cues.last.kind, VoiceCueKind.arrive);
      expect(rig.events.last.kind, NavEventKind.arrived);
      expect(rig.events.last.message, contains('Tujuan'));
      rig.nav.stop();
      expect(rig.nav.state, NavState.idle);
      expect(rig.nav.progress.value, isNull);
      expect(rig.location.backgroundCalls, [true, false], reason: 'already off');
      rig.dispose();
    });

    test('stop ends guidance and background tracking', () async {
      final rig = _Rig();
      final route = lRoute();
      await rig.startOn(route);
      rig.driveTo(100);
      rig.nav.stop();
      expect(rig.nav.state, NavState.idle);
      expect(rig.nav.destination, isNull);
      expect(rig.nav.routes, isEmpty);
      expect(rig.nav.progress.value, isNull);
      expect(rig.location.backgroundCalls, [true, false]);
      rig.location.injectPosition(pointAlong(route, 200), speed: 14);
      expect(rig.nav.progress.value, isNull);
      rig.dispose();
    });
  });

  group('voice prompts', () {
    test('far, near and at-turn by speed; stages a short step began inside are skipped',
        () async {
      final rig = _Rig();
      await rig.startOn(lRoute());
      for (var m = 14.0; m < 2000 && rig.nav.state == NavState.navigating; m += 14) {
        rig.driveTo(m, speed: 14);
      }
      await settle();
      final spoken = rig.cues.map((c) => '${c.kind.name}: ${c.text}').toList();
      // At 14 m/s: far 630 m, near 168 m, at the turn 42 m.
      expect(spoken, [
        'depart: Arah utara di Jalan 1',
        'approach: Dalam 600 meter, belok kanan ke Jalan Satu',
        'approach: Dalam 150 meter, belok kanan ke Jalan Satu',
        'maneuver: Belok kanan ke Jalan Satu',
        // The 600 m leg began inside the far distance: only near and at the turn.
        'approach: Dalam 150 meter, belok kiri ke Jalan Dua',
        'maneuver: Belok kiri ke Jalan Dua',
        'approach: Tujuan Anda 150 meter lagi',
        'arrive: Anda telah tiba di tujuan',
      ]);
      rig.dispose();
    });

    test('two maneuvers close together are announced as one', () async {
      final rig = _Rig();
      await rig.startOn(syntheticRoute(const [
        Leg(0, 800),
        Leg(90, 60,
            modifier: ManeuverModifier.right, instruction: 'Belok kanan ke Jalan A'),
        Leg(0, 600,
            modifier: ManeuverModifier.left, instruction: 'Belok kiri ke Jalan B'),
      ]));
      for (var m = 10.0; m < 1300; m += 10) {
        rig.driveTo(m, speed: 10);
      }
      await settle();
      final texts = rig.cues.map((c) => c.text).toList();
      expect(texts, contains('Belok kanan ke Jalan A, kemudian belok kiri ke Jalan B'));
      expect(texts.where((t) => t.contains('Jalan B') && t.startsWith('Dalam')), isEmpty);
      expect(texts, contains('Belok kiri ke Jalan B'));
      rig.dispose();
    });

    test('name changes are shown but not spoken', () async {
      final rig = _Rig();
      await rig.startOn(syntheticRoute(const [
        Leg(0, 800),
        Leg(0, 800,
            type: ManeuverType.newName,
            modifier: ManeuverModifier.straight,
            instruction: 'Terus ke Jalan Baru'),
      ]));
      for (var m = 10.0; m < 1500; m += 10) {
        rig.driveTo(m, speed: 10);
      }
      expect(rig.cues.where((c) => c.text.contains('Jalan Baru')), isEmpty);
      rig.dispose();
    });
  });

  group('Mapbox traffic refresh', () {
    test('every 5 minutes; the same path updates silently', () async {
      final rig = _Rig(mapbox: true);
      final route = lRoute(provider: RouteProvider.mapbox, congestion: Congestion.low);
      rig.location.injectPosition(route.points.first);
      rig.routing.replies.add(RouteResult(routes: [route], provider: RouteProvider.mapbox));
      await rig.nav.previewTo(Place(name: 'X', location: route.points.last));
      rig.nav.start();

      rig.driveTo(10, speed: 1);
      // Remaining time follows Mapbox's per-segment durations: 0.8 x 5 s + 39 x 5 s.
      expect(rig.nav.progress.value!.timeRemaining.inSeconds, closeTo(199, 1));
      rig.driveTo(11, speed: 1, seconds: 200);
      expect(rig.routing.calls, hasLength(1));

      final fresh = lRoute(provider: RouteProvider.mapbox, congestion: Congestion.heavy);
      rig.routing.replies.add(RouteResult(routes: [fresh], provider: RouteProvider.mapbox));
      rig.driveTo(12, speed: 1, seconds: 100);
      await settle();
      final call = rig.routing.calls.last;
      expect(rig.routing.calls, hasLength(2));
      expect(call.provider, RouteProvider.mapbox);
      expect(call.alternatives, isFalse);
      expect(rig.nav.route, same(fresh));
      expect(rig.events, isEmpty);
      rig.dispose();
    });

    test('a different path is announced; an OSRM fallback is ignored', () async {
      final rig = _Rig(mapbox: true);
      final route = lRoute(provider: RouteProvider.mapbox, congestion: Congestion.low);
      rig.location.injectPosition(route.points.first);
      rig.routing.replies.add(RouteResult(routes: [route], provider: RouteProvider.mapbox));
      await rig.nav.previewTo(Place(name: 'X', location: route.points.last));
      rig.nav.start();
      rig.driveTo(10, speed: 1);

      rig.routing.replies.add(osrmResult([lRoute()], fellBack: true));
      rig.driveTo(11, speed: 1, seconds: 301);
      await settle();
      expect(rig.nav.route, same(route));

      final other = syntheticRoute(const [Leg(90, 600), Leg(0, 1400)],
          provider: RouteProvider.mapbox, congestion: Congestion.low);
      rig.routing.replies.add(RouteResult(routes: [other], provider: RouteProvider.mapbox));
      rig.driveTo(12, speed: 1, seconds: 301);
      await settle();
      expect(rig.nav.route, same(other));
      expect(rig.events.map((e) => e.kind), [NavEventKind.rerouted]);
      rig.dispose();
    });
  });
}
