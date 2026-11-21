import '../models/car_video.dart';

/// Decides which SponsorBlock segment to skip at a playback position.
///
/// Each segment is skipped at most once per video, so a driver who seeks back into a
/// skipped segment gets to hear it, and a seek that lands a little before the segment end
/// cannot loop. Call [reset] whenever a new video starts.
class SponsorSkipTracker {
  final Set<int> _skipped = <int>{};

  /// Shorter segments are not worth a seek; the rebuffer costs more than it saves.
  static const double minSegmentSeconds = 1.0;

  /// No skip this close to a segment end; the seek would land on almost the same spot.
  static const double endToleranceSeconds = 0.5;

  void reset() => _skipped.clear();

  /// Returns the segment to skip now, or null. A returned segment counts as skipped.
  SponsorSegment? segmentToSkip(
    Duration position,
    List<SponsorSegment> segments,
  ) {
    final seconds = position.inMilliseconds / 1000.0;
    for (var i = 0; i < segments.length; i++) {
      if (_skipped.contains(i)) continue;
      final segment = segments[i];
      if (segment.end - segment.start < minSegmentSeconds) continue;
      if (seconds >= segment.start &&
          seconds < segment.end - endToleranceSeconds) {
        _skipped.add(i);
        return segment;
      }
    }
    return null;
  }
}

/// Short Indonesian label for a SponsorBlock category, shown in the skip notice.
String sponsorCategoryLabel(String category) {
  return switch (category) {
    'sponsor' => 'sponsor',
    'intro' => 'intro',
    'outro' => 'outro',
    'selfpromo' => 'promosi',
    'music_offtopic' => 'bagian non-musik',
    _ => category,
  };
}
