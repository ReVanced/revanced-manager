import 'package:latlong2/latlong.dart';

/// Decodes a Google encoded polyline; OSRM and Mapbox send `polyline6` (precision 6).
List<LatLng> decodePolyline(String encoded, {int precision = 6}) {
  final factor = _pow10(precision);
  final points = <LatLng>[];
  var index = 0;
  var lat = 0;
  var lon = 0;
  final length = encoded.length;
  while (index < length) {
    var result = 0;
    var shift = 0;
    int b;
    do {
      // A value cut off mid-way ends the line rather than adding a wrong point.
      if (index >= length) return points;
      b = encoded.codeUnitAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

    result = 0;
    shift = 0;
    do {
      if (index >= length) return points;
      b = encoded.codeUnitAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    lon += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

    points.add(LatLng(lat / factor, lon / factor));
  }
  return points;
}

/// Encodes points as a polyline; the inverse of [decodePolyline], used by tests and the
/// drive simulator's fixtures.
String encodePolyline(List<LatLng> points, {int precision = 6}) {
  final factor = _pow10(precision);
  final out = StringBuffer();
  var lastLat = 0;
  var lastLon = 0;
  for (final p in points) {
    final lat = (p.latitude * factor).round();
    final lon = (p.longitude * factor).round();
    _encodeValue(lat - lastLat, out);
    _encodeValue(lon - lastLon, out);
    lastLat = lat;
    lastLon = lon;
  }
  return out.toString();
}

void _encodeValue(int value, StringBuffer out) {
  var v = value < 0 ? ~(value << 1) : value << 1;
  while (v >= 0x20) {
    out.writeCharCode((0x20 | (v & 0x1f)) + 63);
    v >>= 5;
  }
  out.writeCharCode(v + 63);
}

double _pow10(int precision) {
  var f = 1.0;
  for (var i = 0; i < precision; i++) {
    f *= 10;
  }
  return f;
}
