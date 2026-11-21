import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/scheduler.dart';

/// On only in builds made with `--dart-define=FRAME_LOG=true`; otherwise the code is
/// compiled out.
const bool kFrameLog = bool.fromEnvironment('FRAME_LOG');

/// Prints frame costs to the device log (tag `flutter`) for measuring smoothness on the
/// phone: once per batch the UI-thread (build) and raster-thread times, plus every frame
/// that missed the 16.7 ms budget of a 60 Hz screen, and UI-isolate stalls over 50 ms.
void startFrameLog() {
  if (!kFrameLog) return;
  // Late 16 ms timer ticks show the UI isolate busy outside frames as well (response
  // parsing, TLS handshakes), which frame timings cannot see.
  final clock = Stopwatch()..start();
  var last = 0;
  Timer.periodic(const Duration(milliseconds: 16), (_) {
    final now = clock.elapsedMilliseconds;
    if (now - last > 50) debugPrint('STALL ${now - last} ms');
    last = now;
  });
  SchedulerBinding.instance.addTimingsCallback((timings) {
    final build = [for (final t in timings) t.buildDuration.inMicroseconds / 1000]
      ..sort();
    final raster = [for (final t in timings) t.rasterDuration.inMicroseconds / 1000]
      ..sort();
    String stats(List<double> ms) =>
        'p50 ${ms[ms.length ~/ 2].toStringAsFixed(1)} '
        'p90 ${ms[(ms.length * 9) ~/ 10].toStringAsFixed(1)} '
        'max ${ms.last.toStringAsFixed(1)}';
    debugPrint(
      'FRAMES n=${timings.length} build[${stats(build)}] raster[${stats(raster)}]',
    );
    for (final t in timings) {
      final b = t.buildDuration.inMicroseconds / 1000;
      final r = t.rasterDuration.inMicroseconds / 1000;
      if (b > 16.7 || r > 16.7) {
        debugPrint(
          'SLOW build ${b.toStringAsFixed(1)} raster ${r.toStringAsFixed(1)} '
          'total ${(t.totalSpan.inMicroseconds / 1000).toStringAsFixed(1)}',
        );
      }
    }
  });
}
