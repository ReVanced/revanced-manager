import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:latlong2/latlong.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/geo.dart';
import 'package:youtube_car_headunit/services/geocoding_service.dart';

import 'nav_test_utils.dart';

const _car = LatLng(-6.2, 106.82);

void main() {
  late List<http.Request> requests;

  GeocodingService service(
    String Function(http.Request) body, {
    int status = 200,
    Duration minInterval = const Duration(milliseconds: 1),
  }) {
    requests = [];
    return GeocodingService(
      client: MockClient((request) async {
        requests.add(request);
        return http.Response.bytes(
          utf8.encode(body(request)),
          status,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      }),
      minInterval: minInterval,
    );
  }

  test('search parses Photon results with a readable address line', () async {
    final s = service((_) => fixture('photon_search_monas.json'));
    final places = await s.search('monas', near: _car);
    expect(places, isNotEmpty);
    final bekasi = places.firstWhere((p) => p.name == 'MONAS');
    expect(bekasi.address, 'Bekasi Jaya, Bekasi, Jawa Barat');
    final halte = places.firstWhere((p) => p.name == 'Monas 1');
    expect(halte.address, 'Jalan Medan Merdeka Barat, Gambir, Jakarta, Jawa');
    expect(halte.location.latitude, lessThan(0));

    final request = requests.single;
    expect(request.url.host, 'photon.komoot.io');
    expect(request.url.path, '/api/');
    expect(request.url.queryParameters, {
      'q': 'monas',
      'limit': '8',
      'lat': '-6.20000',
      'lon': '106.82000',
    });
    expect(request.headers['User-Agent'], kNavUserAgent);
  });

  test('short queries return nothing without a request', () async {
    final s = service((_) => fixture('photon_search_monas.json'));
    expect(await s.search('mo', near: _car), isEmpty);
    expect(await s.search('   ', near: _car), isEmpty);
    expect(requests, isEmpty);
  });

  test('a repeated query near the same place is served from the cache', () async {
    final s = service((_) => fixture('photon_search_monas.json'));
    await s.search('monas', near: _car);
    await s.search('Monas ', near: const LatLng(-6.2001, 106.8202));
    expect(requests, hasLength(1));
  });

  test('requests are spaced and a superseded search is never sent', () async {
    final s = service((r) => fixture('photon_search_monas.json'),
        minInterval: const Duration(milliseconds: 120));
    final watch = Stopwatch()..start();
    final first = s.search('mona');
    await Future<void>.delayed(const Duration(milliseconds: 10)); // first is sent
    final second = s.search('monas'); // waits for the slot, then is superseded
    final third = s.search('monas n');
    final results = await Future.wait([first, second, third]);
    expect(results[1], isEmpty);
    expect(requests.map((r) => r.url.queryParameters['q']), ['mona', 'monas n']);
    expect(watch.elapsedMilliseconds, greaterThanOrEqualTo(110));
  });

  test('reverse names the point but keeps its position', () async {
    final s = service((_) => fixture('photon_reverse_monas.json'));
    const point = LatLng(-6.1754, 106.8272);
    final place = await s.reverse(point);
    expect(place!.name, 'Monumen Nasional');
    expect(place.address, 'Jalan Medan Merdeka Utara, Gambir, Jakarta, Jawa');
    expect(place.location, point);
    expect(requests.single.url.path, '/reverse');
    expect(await s.reverse(point), isNotNull);
    expect(requests, hasLength(1), reason: 'second reverse comes from the cache');
  });

  test('reverse with nothing there returns null', () async {
    final s = service((_) => '{"type":"FeatureCollection","features":[]}');
    expect(await s.reverse(_car), isNull);
  });

  test('server and network failures become driver-facing errors', () async {
    final down = service((_) => 'oops', status: 503);
    await expectLater(down.search('monas'), throwsA(isA<NavException>()));

    final offline = GeocodingService(
      client: MockClient((_) async => throw http.ClientException('offline')),
      minInterval: const Duration(milliseconds: 1),
    );
    await expectLater(
      offline.search('monas'),
      throwsA(isA<NavException>()
          .having((e) => e.message, 'message', contains('koneksi'))),
    );
  });

  test('parser skips unusable features and duplicate rows', () {
    final places = parsePhotonPlaces('''
      {"features":[
        {"geometry":{"coordinates":[106.8,-6.2]},"properties":{"name":"A","city":"Jakarta"}},
        {"geometry":{"coordinates":[106.81,-6.21]},"properties":{"name":"A","city":"Jakarta"}},
        {"geometry":{"coordinates":[106.8]},"properties":{"name":"B"}},
        {"geometry":{"coordinates":[106.8,-6.2]},"properties":{}},
        {"geometry":{"coordinates":[106.8,-6.2]},"properties":{"street":"Jalan C","housenumber":"5","city":"Bogor"}}
      ]}''');
    expect(places.map((p) => p.name), ['A', 'Jalan C 5']);
    expect(places.last.address, 'Bogor');
  });

  group('rankByDistance', () {
    Place at(String name, double km) =>
        Place(name: name, location: offsetPoint(_car, 90, km * 1000));

    test('nearby places first by distance, far ones after in Photon order', () {
      final photon = [
        at('Alun-Alun far exact', 133),
        at('Alun-Alun Kota', 1.1),
        at('Far B', 80),
        at('Near 12 km', 12),
        at('Near 0.5 km', 0.5),
        at('Far C', 45),
      ];
      expect(rankByDistance(photon, _car).map((p) => p.name), [
        'Near 0.5 km',
        'Alun-Alun Kota',
        'Near 12 km',
        'Alun-Alun far exact',
        'Far B',
        'Far C',
      ]);
    });

    test('equal distances keep Photon order; no position keeps it all', () {
      final photon = [at('A', 2), at('B', 2), at('C', 1)];
      expect(rankByDistance(photon, _car).map((p) => p.name), ['C', 'A', 'B']);
      expect(rankByDistance(photon, null), photon);
    });

    test('search applies it', () async {
      final s = service((_) => fixture('photon_search_monas.json'));
      final places = await s.search('monas', near: const LatLng(-6.1754, 106.8272));
      // Photon put the Bekasi street named "MONAS" (21 km) first; Monas itself is near.
      expect(places.first.address, contains('Gambir'));
      expect(places.indexWhere((p) => p.name == 'MONAS'),
          greaterThan(places.indexWhere((p) => p.name == 'Monas 1')));
    });
  });
}
