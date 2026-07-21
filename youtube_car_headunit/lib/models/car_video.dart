class CarVideo {
  final String id;
  final String title;
  final String uploader;
  final String thumbnail;
  final Duration duration;
  final String? videoStreamUrl;
  final String? audioStreamUrl;
  final int views;
  final String uploadDate;

  CarVideo({
    required this.id,
    required this.title,
    required this.uploader,
    required this.thumbnail,
    required this.duration,
    this.videoStreamUrl,
    this.audioStreamUrl,
    this.views = 0,
    this.uploadDate = 'Terbaru',
  });

  CarVideo copyWith({
    String? videoStreamUrl,
    String? audioStreamUrl,
    bool clearVideoStream = false,
  }) {
    return CarVideo(
      id: id,
      title: title,
      uploader: uploader,
      thumbnail: thumbnail,
      duration: duration,
      videoStreamUrl:
          clearVideoStream ? null : (videoStreamUrl ?? this.videoStreamUrl),
      audioStreamUrl: audioStreamUrl ?? this.audioStreamUrl,
      views: views,
      uploadDate: uploadDate,
    );
  }
}

class SponsorSegment {
  final String category;
  final double start;
  final double end;

  SponsorSegment({
    required this.category,
    required this.start,
    required this.end,
  });
}
