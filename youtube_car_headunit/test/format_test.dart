import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/utils/format.dart';

void main() {
  test('minutes and seconds under an hour', () {
    expect(formatDuration(Duration.zero), '0:00');
    expect(formatDuration(const Duration(seconds: 7)), '0:07');
    expect(formatDuration(const Duration(minutes: 3, seconds: 45)), '3:45');
    expect(formatDuration(const Duration(minutes: 59, seconds: 59)), '59:59');
  });

  test('hours once past an hour (10 h stream is not 600:00)', () {
    expect(formatDuration(const Duration(hours: 1)), '1:00:00');
    expect(
      formatDuration(const Duration(hours: 1, minutes: 2, seconds: 3)),
      '1:02:03',
    );
    expect(formatDuration(const Duration(hours: 10)), '10:00:00');
  });

  test('negative clamps to zero', () {
    expect(formatDuration(const Duration(seconds: -5)), '0:00');
  });

  test('parses YouTube clock strings', () {
    expect(parseClockDuration('3:34'), const Duration(minutes: 3, seconds: 34));
    expect(parseClockDuration('1:03:40'),
        const Duration(hours: 1, minutes: 3, seconds: 40));
    expect(parseClockDuration(' 0:07 '), const Duration(seconds: 7));
  });

  test('anything else is null, including live results', () {
    expect(parseClockDuration(''), isNull);
    expect(parseClockDuration('LIVE'), isNull);
    expect(parseClockDuration('12'), isNull);
    expect(parseClockDuration('1:2:3:4'), isNull);
    expect(parseClockDuration('3:x4'), isNull);
  });

  test('clock is 24-hour with leading zeros', () {
    expect(formatClock(DateTime(2026, 11, 21, 7, 5)), '07:05');
    expect(formatClock(DateTime(2026, 11, 21, 23, 59)), '23:59');
  });

  test('short Indonesian date', () {
    expect(formatShortDate(DateTime(2026, 11, 21)), 'Sab, 21 Nov');
    expect(formatShortDate(DateTime(2026, 8, 17)), 'Sen, 17 Agu');
  });
}
