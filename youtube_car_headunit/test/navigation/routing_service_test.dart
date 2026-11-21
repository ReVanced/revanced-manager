import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/geo.dart';
import 'package:youtube_car_headunit/services/routing_service.dart';

import 'nav_test_utils.dart';

const _from = LatLng(-6.1875, 106.8228);
const _to = LatLng(-6.2257, 106.7997);

/// A GET that answers from a script keyed by host and records every URI.
class _Server {
  final List<Uri> requests = [];
  final Map<String, List<Object>> byHost = {};

  void reply(String host, Object reply) => byHost.putIfAbsent(host, () => []).add(reply);

  Future<HttpReply> get(Uri uri) async {
    requests.add(uri);
    final queue = byHost[uri.host];
    if (queue == null || queue.isEmpty) throw SocketException('no route to ${uri.host}');
    final r = queue.removeAt(0);
    if (r is Exception) throw r;
    return r as HttpReply;
  }
}

RoutingService _service(_Server server, {String token = ''}) => RoutingService(
      mapboxToken: token,
      get: server.get,
      osrmInterval: const Duration(milliseconds: 1),
      mapboxInterval: const Duration(milliseconds: 1),
    );

const _demo = 'router.project-osrm.org';
const _fossgis = 'routing.openstreetmap.de';
const _mapbox = 'api.mapbox.com';

void main() {
  group('parsing recorded OSRM responses', () {
    test('Sarinah to Senayan: two routes, steps on the geometry', () {
      final parsed =
          parseRouteResponse(fixture('osrm_thamrin_senayan.json'), RouteProvider.osrm);
      expect(parsed.code, 'Ok');
      expect(parsed.routes, hasLength(2));
      final route = parsed.routes.first;
      expect(route.provider, RouteProvider.osrm);
      expect(route.congestion, isNull);
      expect(route.points, hasLength(305));
      // Our own length of the line agrees with OSRM's to within half a percent.
      expect(route.cumulative.last, closeTo(route.distance, route.distance * 0.005));
      expect(route.steps.first.type, ManeuverType.depart);
      expect(route.steps.last.type, ManeuverType.arrive);
      expect(route.steps.last.pointIndex, route.points.length - 1);
      for (final step in route.steps) {
        expect(distanceMeters(step.location, route.points[step.pointIndex]),
            lessThan(1), reason: step.instruction);
      }
      expect(route.steps[1].instruction, 'Belok kiri ke Jalan Kyai Haji Wahid Hasyim');
      expect(route.steps[1].modifier, ManeuverModifier.left);
      expect(route.steps[1].roadLabel, 'Jalan Kyai Haji Wahid Hasyim');
      expect(route.steps.last.instruction, 'Anda telah tiba di tujuan, di sebelah kanan');
      expect(route.summary, 'Jalan Kyai Haji Mas Mansyur, Jalan Asia Afrika');
      expect(route.duration.inSeconds, 569);
    });

    test('Monas to the airport: roundabout exit and toll road by name', () {
      final parsed =
          parseRouteResponse(fixture('osrm_monas_airport.json'), RouteProvider.osrm);
      expect(parsed.routes, hasLength(2));
      final viaToll = parsed.routes[0];
      final viaDaanMogot = parsed.routes[1];
      expect(viaToll.hasTolls, isTrue);
      expect(viaDaanMogot.hasTolls, isFalse);
      final roundabout =
          viaToll.steps.firstWhere((s) => s.type == ManeuverType.roundabout);
      expect(roundabout.exit, 1);
      expect(roundabout.instruction,
          'Masuk bundaran dan ambil jalan keluar pertama arah Jalan C3');
      final exitRoundabout =
          viaToll.steps.firstWhere((s) => s.type == ManeuverType.exitRoundabout);
      expect(exitRoundabout.instruction, 'Keluar dari bundaran ke arah Jalan C3');
    });

    test('error codes come back without routes', () {
      final parsed =
          parseRouteResponse('{"code":"NoRoute","message":"x"}', RouteProvider.osrm);
      expect(parsed.code, 'NoRoute');
      expect(parsed.routes, isEmpty);
    });
  });

  group('parsing Mapbox responses (hand-written)', () {
    test('Indonesian text, congestion and durations per segment', () {
      final parsed = parseRouteResponse(mapboxResponse(), RouteProvider.mapbox);
      final route = parsed.routes.single;
      expect(route.provider, RouteProvider.mapbox);
      expect(route.uuid, 'test-uuid');
      expect(route.points, hasLength(8));
      expect(route.congestion, [
        Congestion.low,
        Congestion.low,
        Congestion.moderate,
        Congestion.heavy,
        Congestion.severe,
        Congestion.unknown,
        Congestion.low,
      ]);
      expect(route.segmentDurations, everyElement(13.5));
      expect(route.steps[1].instruction, 'Belok kanan ke Jalan Timur');
      expect(route.steps[1].pointIndex, 4);
      expect(route.steps.last.pointIndex, 7);
    });

    test('a locale other than Indonesian falls back to our templates', () {
      final route =
          parseRouteResponse(mapboxResponse(locale: 'en'), RouteProvider.mapbox)
              .routes
              .single;
      expect(route.steps[1].instruction, 'Belok kanan ke Jalan Timur');
      expect(route.steps.first.instruction, 'Arah utara di Jalan Utara');
    });

    test('annotations that do not cover every segment are dropped', () {
      final route = parseRouteResponse(
              mapboxResponse(annotationTrim: 1), RouteProvider.mapbox)
          .routes
          .single;
      expect(route.congestion, isNull);
      expect(route.segmentDurations, isNull);
    });

    test('toll class on an intersection marks the route', () {
      final route = parseRouteResponse(mapboxResponse(toll: true), RouteProvider.mapbox)
          .routes
          .single;
      expect(route.hasTolls, isTrue);
    });
  });

  group('RoutingService with OSRM', () {
    test('asks the demo server for full polyline6 geometry, steps and two alternatives',
        () async {
      final server = _Server()
        ..reply(_demo, HttpReply(200, fixture('osrm_thamrin_senayan.json')));
      final result = await _service(server).route(_from, _to, heading: 359.6);
      expect(result.provider, RouteProvider.osrm);
      expect(result.routes, hasLength(2));
      expect(result.fellBack, isFalse);
      final uri = server.requests.single;
      expect(uri.host, _demo);
      expect(uri.path,
          '/route/v1/driving/106.822800,-6.187500;106.799700,-6.225700');
      expect(uri.queryParameters, {
        'overview': 'full',
        'geometries': 'polyline6',
        'steps': 'true',
        'alternatives': '2',
        'bearings': '0,45;',
      });
    });

    test('never sends exclude=toll to OSRM, which rejects it', () async {
      final server = _Server()
        ..reply(_demo, HttpReply(200, fixture('osrm_thamrin_senayan.json')));
      final result = await _service(server).route(_from, _to, avoidTolls: true);
      expect(server.requests.single.queryParameters.containsKey('exclude'), isFalse);
      expect(result.tollsAvoided, isFalse);
    });

    test('falls back to FOSSGIS when the demo server is down', () async {
      final server = _Server()
        ..reply(_demo, const HttpReply(503, 'busy'))
        ..reply(_fossgis, HttpReply(200, fixture('osrm_thamrin_senayan.json')));
      final result = await _service(server).route(_from, _to);
      expect(result.routes, isNotEmpty);
      expect(server.requests.map((u) => u.host), [_demo, _fossgis]);
      expect(server.requests.last.path, startsWith('/routed-car/route/v1/driving/'));
    });

    test('no server reachable is a driver-facing error', () async {
      final server = _Server();
      await expectLater(
        _service(server).route(_from, _to),
        throwsA(isA<NavException>().having(
            (e) => e.message, 'message', contains('tidak dapat dihubungi'))),
      );
      expect(server.requests.map((u) => u.host), [_demo, _fossgis]);
    });

    test('NoRoute is final: no fallback server is asked', () async {
      final server = _Server()
        ..reply(_demo, const HttpReply(400, '{"code":"NoRoute","message":"x"}'));
      await expectLater(
        _service(server).route(_from, _to),
        throwsA(isA<NavException>()
            .having((e) => e.message, 'message', contains('tidak ditemukan'))),
      );
      expect(server.requests, hasLength(1));
    });

    test('requests to one host are spaced by the interval', () async {
      final server = _Server();
      for (var i = 0; i < 3; i++) {
        server.reply(_demo, HttpReply(200, fixture('osrm_thamrin_senayan.json')));
      }
      final service = RoutingService(
        mapboxToken: '',
        get: server.get,
        osrmInterval: const Duration(milliseconds: 150),
      );
      final watch = Stopwatch()..start();
      await Future.wait([
        service.route(_from, _to),
        service.route(_from, _to),
        service.route(_from, _to),
      ]);
      // Three starts need two intervals between them.
      expect(watch.elapsedMilliseconds, greaterThanOrEqualTo(290));
    });
  });

  group('RoutingService with Mapbox', () {
    test('asks driving-traffic with annotations, Indonesian and the token', () async {
      final server = _Server()..reply(_mapbox, HttpReply(200, mapboxResponse()));
      final service = _service(server, token: 'secret-test-token');
      expect(service.hasMapbox, isTrue);
      final result = await service.route(_from, _to,
          provider: RouteProvider.mapbox, avoidTolls: true);
      expect(result.provider, RouteProvider.mapbox);
      expect(result.tollsAvoided, isTrue);
      final uri = server.requests.single;
      expect(uri.path, startsWith('/directions/v5/mapbox/driving-traffic/'));
      expect(uri.queryParameters, containsPair('annotations', 'congestion,duration,distance'));
      expect(uri.queryParameters, containsPair('language', 'id'));
      expect(uri.queryParameters, containsPair('alternatives', 'true'));
      expect(uri.queryParameters, containsPair('exclude', 'toll'));
      expect(uri.queryParameters, containsPair('voice_units', 'metric'));
      expect(uri.queryParameters, containsPair('access_token', 'secret-test-token'));
    });

    test('a toll route despite exclude=toll is reported as not avoided', () async {
      final server = _Server()..reply(_mapbox, HttpReply(200, mapboxResponse(toll: true)));
      final result = await _service(server, token: 't')
          .route(_from, _to, provider: RouteProvider.mapbox, avoidTolls: true);
      expect(result.tollsAvoided, isFalse);
    });

    test('Mapbox refusing the token falls back to OSRM', () async {
      final server = _Server()
        ..reply(_mapbox, const HttpReply(401, '{"message":"Not Authorized"}'))
        ..reply(_demo, HttpReply(200, fixture('osrm_thamrin_senayan.json')));
      final result = await _service(server, token: 't')
          .route(_from, _to, provider: RouteProvider.mapbox);
      expect(result.fellBack, isTrue);
      expect(result.provider, RouteProvider.osrm);
      expect(server.requests.map((u) => u.host), [_mapbox, _demo]);
    });

    test('Mapbox unreachable falls back too, and nothing leaks the token', () async {
      final server = _Server()
        ..reply(_mapbox,
            const SocketException('failed: https://api.mapbox.com/?access_token=leak'));
      Object? error;
      try {
        await _service(server, token: 'leak')
            .route(_from, _to, provider: RouteProvider.mapbox);
      } catch (e) {
        error = e;
      }
      // OSRM is down as well in this script, so the final error is the generic one.
      expect(error, isA<NavException>());
      expect(error.toString(), isNot(contains('leak')));
    });

    test('without a token Mapbox is never asked', () async {
      final server = _Server()
        ..reply(_demo, HttpReply(200, fixture('osrm_thamrin_senayan.json')));
      final service = _service(server);
      expect(service.hasMapbox, isFalse);
      final result = await service.route(_from, _to, provider: RouteProvider.mapbox);
      expect(result.provider, RouteProvider.osrm);
      expect(result.fellBack, isFalse);
      expect(server.requests.single.host, _demo);
    });
  });
}
