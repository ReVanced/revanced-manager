import 'dart:math' as math;

import 'package:latlong2/latlong.dart';

const double _earthRadius = 6371008.8;
const double _rad = math.pi / 180;

/// Great-circle distance in metres.
double distanceMeters(LatLng a, LatLng b) {
  final dLat = (b.latitude - a.latitude) * _rad;
  final dLon = (b.longitude - a.longitude) * _rad;
  final s = math.sin(dLat / 2) * math.sin(dLat / 2) +
      math.cos(a.latitude * _rad) *
          math.cos(b.latitude * _rad) *
          math.sin(dLon / 2) *
          math.sin(dLon / 2);
  return 2 * _earthRadius * math.asin(math.min(1, math.sqrt(s)));
}

/// Initial bearing from [a] to [b], degrees clockwise from north in [0, 360).
double bearingDegrees(LatLng a, LatLng b) {
  final lat1 = a.latitude * _rad;
  final lat2 = b.latitude * _rad;
  final dLon = (b.longitude - a.longitude) * _rad;
  final y = math.sin(dLon) * math.cos(lat2);
  final x = math.cos(lat1) * math.sin(lat2) -
      math.sin(lat1) * math.cos(lat2) * math.cos(dLon);
  return (math.atan2(y, x) / _rad + 360) % 360;
}

/// Smallest angle between two bearings, 0..180.
double bearingDifference(double a, double b) {
  final d = ((a - b) % 360 + 360) % 360;
  return d > 180 ? 360 - d : d;
}

/// The point [meters] from [from] towards [bearing] degrees.
LatLng offsetPoint(LatLng from, double bearing, double meters) {
  final d = meters / _earthRadius;
  final b = bearing * _rad;
  final lat1 = from.latitude * _rad;
  final lon1 = from.longitude * _rad;
  final lat2 = math.asin(
      math.sin(lat1) * math.cos(d) + math.cos(lat1) * math.sin(d) * math.cos(b));
  final lon2 = lon1 +
      math.atan2(math.sin(b) * math.sin(d) * math.cos(lat1),
          math.cos(d) - math.sin(lat1) * math.sin(lat2));
  return LatLng(lat2 / _rad, ((lon2 / _rad + 540) % 360) - 180);
}

/// Metres along the line at each point; the first is 0, the last the line's length.
List<double> cumulativeDistances(List<LatLng> points) {
  final out = List<double>.filled(points.length, 0);
  for (var i = 1; i < points.length; i++) {
    out[i] = out[i - 1] + distanceMeters(points[i - 1], points[i]);
  }
  return out;
}

/// Where a point falls on a segment: the fraction along it and the distance to it.
class SegmentProjection {
  const SegmentProjection(this.fraction, this.distance, this.point);

  /// 0 at the segment's start, 1 at its end.
  final double fraction;

  /// Metres from the point to the segment.
  final double distance;

  /// The nearest point on the segment.
  final LatLng point;
}

/// Projects [p] onto the segment [a]-[b] in a local flat frame around [p], which is
/// accurate to centimetres over the few hundred metres a route segment spans.
SegmentProjection projectOnSegment(LatLng p, LatLng a, LatLng b) {
  final cosLat = math.cos(p.latitude * _rad);
  final kx = _earthRadius * _rad * cosLat;
  const ky = _earthRadius * _rad;
  final ax = (a.longitude - p.longitude) * kx;
  final ay = (a.latitude - p.latitude) * ky;
  final bx = (b.longitude - p.longitude) * kx;
  final by = (b.latitude - p.latitude) * ky;
  final dx = bx - ax;
  final dy = by - ay;
  final len2 = dx * dx + dy * dy;
  var t = len2 == 0 ? 0.0 : -(ax * dx + ay * dy) / len2;
  t = t.clamp(0.0, 1.0);
  final x = ax + t * dx;
  final y = ay + t * dy;
  return SegmentProjection(
    t,
    math.sqrt(x * x + y * y),
    LatLng(
      a.latitude + t * (b.latitude - a.latitude),
      a.longitude + t * (b.longitude - a.longitude),
    ),
  );
}
