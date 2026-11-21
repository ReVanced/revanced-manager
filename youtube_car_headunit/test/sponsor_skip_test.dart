import 'package:flutter_test/flutter_test.dart';
import 'package:youtube_car_headunit/models/car_video.dart';
import 'package:youtube_car_headunit/utils/sponsor_skip.dart';

void main() {
  const segments = [
    SponsorSegment(category: 'intro', start: 0, end: 10),
    SponsorSegment(category: 'sponsor', start: 60, end: 90),
  ];

  test('returns the segment the position is in', () {
    final tracker = SponsorSkipTracker();
    expect(tracker.segmentToSkip(const Duration(seconds: 2), segments)?.category,
        'intro');
    expect(
      tracker.segmentToSkip(const Duration(seconds: 61), segments)?.category,
      'sponsor',
    );
  });

  test('nothing outside segments', () {
    final tracker = SponsorSkipTracker();
    expect(tracker.segmentToSkip(const Duration(seconds: 30), segments), isNull);
    expect(tracker.segmentToSkip(const Duration(seconds: 90), segments), isNull);
  });

  test('each segment is skipped once until reset', () {
    final tracker = SponsorSkipTracker();
    expect(tracker.segmentToSkip(const Duration(seconds: 65), segments),
        isNotNull);
    // Seek back into the same segment: the driver chose to hear it.
    expect(tracker.segmentToSkip(const Duration(seconds: 65), segments), isNull);
    tracker.reset();
    expect(tracker.segmentToSkip(const Duration(seconds: 65), segments),
        isNotNull);
  });

  test('no skip right at the segment end, avoiding a pointless seek', () {
    final tracker = SponsorSkipTracker();
    expect(
      tracker.segmentToSkip(const Duration(milliseconds: 89700), segments),
      isNull,
    );
  });

  test('ignores segments shorter than a second', () {
    final tracker = SponsorSkipTracker();
    const tiny = [SponsorSegment(category: 'sponsor', start: 5, end: 5.5)];
    expect(tracker.segmentToSkip(const Duration(seconds: 5), tiny), isNull);
  });

  test('category labels are Indonesian where needed', () {
    expect(sponsorCategoryLabel('selfpromo'), 'promosi');
    expect(sponsorCategoryLabel('music_offtopic'), 'bagian non-musik');
    expect(sponsorCategoryLabel('unknown_cat'), 'unknown_cat');
  });
}
