class CarVideo {
  final String id;
  final String title;
  final String uploader;
  final String thumbnail;
  final Duration duration;
  final bool isLive;
  final int views;
  final String uploadDate;

  const CarVideo({
    required this.id,
    required this.title,
    required this.uploader,
    required this.thumbnail,
    required this.duration,
    this.isLive = false,
    this.views = 0,
    this.uploadDate = 'Terbaru',
  });

  /// 1280x720 artwork for large displays (Musik). Not every video has it; callers fall
  /// back to [thumbnail] when it fails to load.
  String get largeThumbnail => 'https://i.ytimg.com/vi/$id/maxresdefault.jpg';

  /// Only the fields needed to show and replay the video after a restart are kept.
  Map<String, Object?> toJson() => {
        'id': id,
        'title': title,
        'uploader': uploader,
        'thumbnail': thumbnail,
        'durationSeconds': duration.inSeconds,
        'isLive': isLive,
      };

  factory CarVideo.fromJson(Map<String, Object?> json) {
    return CarVideo(
      id: json['id']! as String,
      title: json['title'] as String? ?? '',
      uploader: json['uploader'] as String? ?? '',
      thumbnail: json['thumbnail'] as String? ?? '',
      duration: Duration(seconds: json['durationSeconds'] as int? ?? 0),
      isLive: json['isLive'] as bool? ?? false,
    );
  }
}

class SponsorSegment {
  final String category;
  final double start;
  final double end;

  const SponsorSegment({
    required this.category,
    required this.start,
    required this.end,
  });
}
