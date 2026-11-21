/// Remaining travel time the way Indonesian navigation apps write it: `7 mnt`,
/// `1 j 05 mnt`. Rounded up, so the last minute never shows as `0 mnt`.
String formatTravelTime(Duration duration) {
  final minutes = (duration.inSeconds / 60).ceil().clamp(1, 100000);
  if (minutes < 60) return '$minutes mnt';
  final hours = minutes ~/ 60;
  final rest = (minutes % 60).toString().padLeft(2, '0');
  return '$hours j $rest mnt';
}

/// Distance on screen: `40 m`, `350 m`, `1,2 km`, `12 km` (Indonesian decimal comma).
/// Short distances snap to round steps so the banner does not flicker every fix.
String formatDistance(double meters) {
  final m = meters.isFinite && meters > 0 ? meters : 0.0;
  if (m < 100) return '${(m / 10).round() * 10} m';
  if (m < 975) return '${(m / 50).round() * 50} m';
  if (m < 9950) return '${(m / 1000).toStringAsFixed(1).replaceAll('.', ',')} km';
  return '${(m / 1000).round()} km';
}
