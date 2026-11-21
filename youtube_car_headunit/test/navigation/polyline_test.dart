import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:youtube_car_headunit/services/polyline.dart';

void main() {
  test('decodes Google\'s reference polyline at precision 5', () {
    final points = decodePolyline('_p~iF~ps|U_ulLnnqC_mqNvxq`@', precision: 5);
    expect(points, hasLength(3));
    expect(points[0].latitude, closeTo(38.5, 1e-9));
    expect(points[0].longitude, closeTo(-120.2, 1e-9));
    expect(points[1].latitude, closeTo(40.7, 1e-9));
    expect(points[1].longitude, closeTo(-120.95, 1e-9));
    expect(points[2].latitude, closeTo(43.252, 1e-9));
    expect(points[2].longitude, closeTo(-126.453, 1e-9));
  });

  test('polyline6 round trip keeps every point to the micro-degree', () {
    const points = [
      LatLng(-6.187505, 106.82292),
      LatLng(-6.187043, 106.822941),
      LatLng(-6.2257, 106.7997),
      LatLng(0, 0),
      LatLng(52.508068, 13.426579),
    ];
    final decoded = decodePolyline(encodePolyline(points));
    expect(decoded, hasLength(points.length));
    for (var i = 0; i < points.length; i++) {
      expect(decoded[i].latitude, closeTo(points[i].latitude, 1e-6));
      expect(decoded[i].longitude, closeTo(points[i].longitude, 1e-6));
    }
  });

  test('empty and truncated input do not throw', () {
    expect(decodePolyline(''), isEmpty);
    final full = encodePolyline(const [LatLng(1, 2), LatLng(3, 4)]);
    expect(decodePolyline(full.substring(0, full.length - 1)), hasLength(1));
  });
}
