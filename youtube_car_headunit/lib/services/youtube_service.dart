import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:youtube_explode_dart/youtube_explode_dart.dart';
import '../models/car_video.dart';

class YouTubeService {
  final YoutubeExplode _yt = YoutubeExplode();

  // Search Videos
  Future<List<CarVideo>> searchVideos(String query) async {
    try {
      final searchResults = await _yt.search.search(query);
      return searchResults.map((video) {
        return CarVideo(
          id: video.id.value,
          title: video.title,
          uploader: video.author,
          thumbnail: video.thumbnails.highResUrl,
          duration: video.duration ?? Duration.zero,
          views: video.engagement.viewCount,
          uploadDate: video.uploadDate != null
              ? '${video.uploadDate!.day}/${video.uploadDate!.month}/${video.uploadDate!.year}'
              : 'Terbaru',
        );
      }).toList();
    } catch (e) {
      return _getFallbackCuratedVideos();
    }
  }

  // Fetch Recommended / Car Trending Videos
  Future<List<CarVideo>> getCarTrendingVideos() async {
    try {
      final searchResults = await _yt.search.search('Lofi Drive Music Indonesia');
      if (searchResults.isNotEmpty) {
        return searchResults.map((video) {
          return CarVideo(
            id: video.id.value,
            title: video.title,
            uploader: video.author,
            thumbnail: video.thumbnails.highResUrl,
            duration: video.duration ?? Duration.zero,
            views: video.engagement.viewCount,
          );
        }).toList();
      }
    } catch (e) {
      // Fallback
    }
    return _getFallbackCuratedVideos();
  }

  // Extract Ad-Free Stream URLs with Quality Selector
  Future<CarVideo> getAdFreeStreams(CarVideo video, {String preferredQuality = '360p'}) async {
    try {
      final manifest = await _yt.videos.streamsClient.getManifest(video.id);
      
      final audioStream = manifest.audioOnly.withHighestBitrate();

      if (preferredQuality == 'Audio Only') {
        return video.copyWith(
          videoStreamUrl: null,
          audioStreamUrl: audioStream.url.toString(),
        );
      }

      VideoStreamInfo? videoStream;

      if (manifest.muxed.isNotEmpty) {
        final targetNum = int.tryParse(preferredQuality.replaceAll(RegExp(r'[^0-9]'), '')) ?? 360;
        
        final matches = manifest.muxed.where((s) => s.qualityLabel.contains('$targetNum')).toList();
        if (matches.isNotEmpty) {
          videoStream = matches.first;
        } else {
          final sorted = manifest.muxed.toList()
            ..sort((a, b) {
              final aRes = a.videoResolution.height;
              final bRes = b.videoResolution.height;
              return (aRes - targetNum).abs().compareTo((bRes - targetNum).abs());
            });
          videoStream = sorted.first;
        }
      } else {
        videoStream = manifest.muxed.withHighestBitrate();
      }

      return video.copyWith(
        videoStreamUrl: videoStream.url.toString(),
        audioStreamUrl: audioStream.url.toString(),
      );
    } catch (e) {
      return video;
    }
  }

  // SponsorBlock Integration (Auto-Skip Sponsors)
  Future<List<SponsorSegment>> getSponsorSegments(String videoId) async {
    try {
      const categories = '["sponsor", "intro", "outro", "selfpromo", "music_offtopic"]';
      final url = Uri.parse('https://sponsor.ajay.app/api/skipSegments?videoID=$videoId&categories=$categories');
      final res = await http.get(url).timeout(const Duration(seconds: 4));
      
      if (res.statusCode == 200) {
        final List data = jsonDecode(res.body);
        return data.map((item) {
          final List seg = item['segment'];
          return SponsorSegment(
            category: item['category'] ?? 'sponsor',
            start: (seg[0] as num).toDouble(),
            end: (seg[1] as num).toDouble(),
          );
        }).toList();
      }
    } catch (e) {
      // Ignore if SponsorBlock offline
    }
    return [];
  }

  void dispose() {
    _yt.close();
  }

  List<CarVideo> _getFallbackCuratedVideos() {
    return [
      CarVideo(
        id: 'jfKfPfyJRdk',
        title: 'Lofi Hip Hop Radio 📚 - Beats to Relax/Study to',
        uploader: 'Lofi Girl',
        thumbnail: 'https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg',
        duration: const Duration(hours: 10),
        views: 125000000,
      ),
      CarVideo(
        id: '5qap5aO4i9A',
        title: 'Lofi Beats for Highway & Night Driving 🚗🎶',
        uploader: 'Drive Beats',
        thumbnail: 'https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg',
        duration: const Duration(hours: 1),
        views: 8900000,
      ),
      CarVideo(
        id: '2g811Eo7K8U',
        title: 'Top Hits Indonesia 2026 - Lagu Santai Perjalanan Mobil',
        uploader: 'Indo Music Vibe',
        thumbnail: 'https://i.ytimg.com/vi/2g811Eo7K8U/hqdefault.jpg',
        duration: const Duration(hours: 2),
        views: 4500000,
      ),
    ];
  }
}
