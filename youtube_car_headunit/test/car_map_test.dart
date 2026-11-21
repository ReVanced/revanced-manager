import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:youtube_car_headunit/controllers/location_controller.dart';
import 'package:youtube_car_headunit/widgets/car_map.dart';

void main() {
  testWidgets('the car glides between GPS fixes and jumps after a big gap',
      (tester) async {
    final location = LocationController(enabled: false);
    addTearDown(location.dispose);
    await tester.pumpWidget(
      MaterialApp(
        home: Center(
          child: SizedBox(
            width: 800,
            height: 400,
            child: CarMap(location: location, tilesEnabled: false),
          ),
        ),
      ),
    );

    LatLng marker() =>
        tester.widget<MarkerLayer>(find.byType(MarkerLayer)).markers.single.point;

    // First fix: drawn at once.
    const a = LatLng(-6.2, 106.8);
    location.injectPosition(a);
    await tester.pump();
    expect(marker(), a);

    // ~55 m further: halfway after half a second, there after one.
    const b = LatLng(-6.2, 106.8005);
    location.injectPosition(b);
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));
    expect(marker().latitude, closeTo(-6.2, 1e-9));
    expect(marker().longitude, closeTo(106.80025, 1e-6));
    await tester.pump(const Duration(milliseconds: 600));
    expect(marker(), b);

    // ~1.1 km away (GPS reacquired): jumps instead of gliding across the city.
    const c = LatLng(-6.21, 106.8005);
    location.injectPosition(c);
    await tester.pump();
    expect(marker(), c);
  });
}
