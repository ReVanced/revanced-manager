import '../models/car_video.dart';

/// A page of search results.
class VideoPage {
  const VideoPage(this.videos, {this.cursor});

  final List<CarVideo> videos;

  /// Opaque handle for [VideoSource.nextPage]; null when there are no more results.
  final Object? cursor;

  bool get hasMore => cursor != null;
}

/// Where the feed gets its videos from. Kept apart from YouTubeService so the feed logic
/// can be tested without the network.
abstract interface class VideoSource {
  Future<VideoPage> search(String query);

  Future<VideoPage> nextPage(VideoPage page);
}

/// A failure with a message ready to show the driver.
class YouTubeServiceException implements Exception {
  const YouTubeServiceException(this.message, [this.cause]);

  final String message;
  final Object? cause;

  @override
  String toString() => cause == null ? message : '$message ($cause)';
}
