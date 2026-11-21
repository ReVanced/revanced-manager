/// Formats a playback time as `m:ss`, switching to `h:mm:ss` from one hour on so long
/// mixes and 24/7 streams stay readable.
String formatDuration(Duration duration) {
  final total = duration.isNegative ? 0 : duration.inSeconds;
  final hours = total ~/ 3600;
  final minutes = (total % 3600) ~/ 60;
  final seconds = (total % 60).toString().padLeft(2, '0');
  if (hours > 0) {
    return '$hours:${minutes.toString().padLeft(2, '0')}:$seconds';
  }
  return '$minutes:$seconds';
}

/// Parses a YouTube clock string (`3:34`, `1:03:40`) into a duration; null for anything
/// else, such as the empty or "LIVE" text live results carry.
Duration? parseClockDuration(String text) {
  final parts = text.trim().split(':');
  if (parts.length < 2 || parts.length > 3) return null;
  final numbers = parts.map(int.tryParse).toList();
  if (numbers.any((n) => n == null || n < 0)) return null;
  var seconds = 0;
  for (final n in numbers) {
    seconds = seconds * 60 + n!;
  }
  return Duration(seconds: seconds);
}

const _days = ['Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab', 'Min'];
const _months = [
  'Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun',
  'Jul', 'Agu', 'Sep', 'Okt', 'Nov', 'Des',
];

/// 24-hour clock, `HH:mm`, as Indonesian dashboards show it.
String formatClock(DateTime time) =>
    '${time.hour.toString().padLeft(2, '0')}:'
    '${time.minute.toString().padLeft(2, '0')}';

/// Short Indonesian date, e.g. `Sab, 21 Nov`. Kept local instead of pulling in `intl`
/// for one format.
String formatShortDate(DateTime time) =>
    '${_days[time.weekday - 1]}, ${time.day} ${_months[time.month - 1]}';
