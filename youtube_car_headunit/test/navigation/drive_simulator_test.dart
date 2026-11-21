import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/controllers/drive_simulator.dart';
import 'package:youtube_car_headunit/controllers/navigation_controller.dart';
import 'package:youtube_car_headunit/models/navigation.dart';
import 'package:youtube_car_headunit/services/geo.dart';

import 'nav_test_utils.dart';

void main() {
  test('simulation is compiled out unless asked for', () {
    expect(kSimulateDrive, isFalse);
    expect(kSimulateDetourSeconds, 0);
  });

  // testWidgets runs in fake time, so the simulator's periodic timer only fires when the
  // test pumps.
  testWidgets('drives the route at 40 km/h, leaves it, follows the reroute',
      (tester) async {
    final location = TestLocation();
    final routing = FakeRouting();
    var now = DateTime(2026, 11, 21, 8);
    final nav = NavigationController(
        location: location, routing: routing, now: () => now);
    final sim = DriveSimulator(location: location, navigation: nav)..attach();
    final route = lRoute();

    location.injectPosition(route.points.first);
    routing.replies.add(osrmResult([route]));
    await nav.previewTo(Place(name: 'Tujuan', location: route.points.last));
    expect(sim.running, isFalse);
    nav.start();
    expect(sim.running, isTrue);

    for (var i = 0; i < 10; i++) {
      now = now.add(const Duration(seconds: 1));
      await tester.pump(const Duration(seconds: 1));
    }
    var p = nav.progress.value!;
    // 40 km/h is 11.1 m/s.
    expect(p.distanceTravelled, closeTo(111, 2));
    expect(location.speed, closeTo(11.1, 0.1));
    expect(location.heading, closeTo(0, 1));

    sim.leaveRoute(meters: 150);
    final detourStart = location.position!;
    routing.replies.add(osrmResult([
      syntheticRoute(const [Leg(270, 100), Leg(0, 1500)], start: detourStart)
    ]));
    for (var i = 0; i < 10; i++) {
      now = now.add(const Duration(seconds: 1));
      await tester.pump(const Duration(seconds: 1));
    }
    expect(routing.calls, hasLength(2), reason: 'left the route, rerouted');
    final rerouted = nav.route!;
    expect(rerouted, isNot(same(route)));
    // On the new route now, not still driving off into the fields.
    p = nav.progress.value!;
    expect(p.offRoute, isFalse);
    expect(p.distanceFromRoute, lessThan(5));
    expect(distanceMeters(location.position!, rerouted.points.first),
        lessThan(200));

    nav.stop();
    expect(sim.running, isFalse);
    sim.dispose();
    nav.dispose();
  });
}
