import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/geo.dart';
import 'package:youtube_car_headunit/services/recent_destinations.dart';

Place _place(String name, double lat) =>
    Place(name: name, address: 'Jakarta', location: LatLng(lat, 106.8));

void main() {
  test('defaults when nothing is stored', () async {
    SharedPreferences.setMockInitialValues({});
    final store = await NavigationStore.load();
    expect(store.recents, isEmpty);
    expect(store.provider, isNull);
    expect(store.avoidTolls, isFalse);
  });

  test('recents: newest first, capped, survive a reload', () async {
    SharedPreferences.setMockInitialValues({});
    final store = await NavigationStore.load();
    for (var i = 0; i < 10; i++) {
      await store.addRecent(_place('P$i', -6.0 - i * 0.01));
    }
    final reloaded = await NavigationStore.load();
    expect(reloaded.recents.map((p) => p.name),
        ['P9', 'P8', 'P7', 'P6', 'P5', 'P4', 'P3', 'P2']);
    expect(reloaded.recents.first.address, 'Jakarta');
  });

  test('the same place again moves to the top instead of repeating', () async {
    SharedPreferences.setMockInitialValues({});
    final store = await NavigationStore.load();
    final monas = _place('Monas', -6.1754);
    await store.addRecent(monas);
    await store.addRecent(_place('Senayan', -6.2257));
    // 20 m away and renamed by a later search: still the same place.
    await store.addRecent(Place(
        name: 'Monumen Nasional', location: offsetPoint(monas.location, 0, 20)));
    expect(store.recents.map((p) => p.name), ['Monumen Nasional', 'Senayan']);
  });

  test('remove and corrupt data', () async {
    SharedPreferences.setMockInitialValues({});
    final store = await NavigationStore.load();
    final a = _place('A', -6.1);
    await store.addRecent(a);
    await store.addRecent(_place('B', -6.2));
    await store.removeRecent(a);
    expect(store.recents.map((p) => p.name), ['B']);

    SharedPreferences.setMockInitialValues({'nav_recent_destinations': '[{bad'});
    expect((await NavigationStore.load()).recents, isEmpty);
  });

  test('provider and avoid tolls persist', () async {
    SharedPreferences.setMockInitialValues({});
    final store = await NavigationStore.load();
    await store.setProvider(RouteProvider.osrm);
    await store.setAvoidTolls(true);
    final reloaded = await NavigationStore.load();
    expect(reloaded.provider, RouteProvider.osrm);
    expect(reloaded.avoidTolls, isTrue);
  });
}
