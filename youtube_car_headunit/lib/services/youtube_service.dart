import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
// Needed to ask the ios player endpoint for its HLS master without getManifest's stream
// check (see _resolveHls). youtube_explode_dart is pinned by pubspec.lock; recheck this
// internal API when upgrading.
// ignore: implementation_imports
import 'package:youtube_explode_dart/src/videos/video_controller.dart';
import 'package:youtube_explode_dart/youtube_explode_dart.dart';

import '../models/car_video.dart';
import '../utils/format.dart';
import 'stream_selection.dart';
import 'video_source.dart';

/// Desktop Chrome UA sent with every stream request; the stream URLs were verified to
/// answer with it (HTTP 206).
const String kStreamUserAgent =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
    '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';

const Map<String, String> kStreamHeaders = {'User-Agent': kStreamUserAgent};

enum StreamQuality {
  p360('360p', 360),
  p480('480p', 480),
  p720('720p', 720),
  audioOnly('Audio Only', 0);

  const StreamQuality(this.label, this.height);

  final String label;
  final int height;

  bool get isAudioOnly => this == audioOnly;

  static StreamQuality fromLabel(String? label) => values.firstWhere(
        (q) => q.label == label,
        orElse: () => p360,
      );
}

/// The URLs to play for one video at one quality.
class ResolvedStreams {
  const ResolvedStreams({
    required this.label,
    this.videoUri,
    this.videoIsHls = false,
    this.audioUri,
    this.audioIsHls = false,
    this.audioFallbackUri,
    this.downgraded = false,
  });

  /// Muxed audio+video: a progressive MP4, a live HLS master or a built HLS master.
  final Uri? videoUri;
  final bool videoIsHls;

  /// Audio for Audio Only, and the fallback when the video fails.
  final Uri? audioUri;
  final bool audioIsHls;

  /// Progressive stream to try when [audioUri] is refused. YouTube often answers the
  /// android client's audio-only URL with 403 while its muxed itag 18 still plays; the
  /// audio player then decodes only the sound of that file.
  final Uri? audioFallbackUri;

  /// What is actually served, e.g. `720p`, `360p`, `LIVE`, `Audio`.
  final String label;

  /// True when the requested quality was not available and a lower one is served.
  final bool downgraded;
}

/// Where playback gets what to play: stream URLs and SponsorBlock segments.
abstract interface class StreamSource {
  Future<ResolvedStreams> resolveStreams(CarVideo video, StreamQuality quality);

  Future<List<SponsorSegment>> getSponsorSegments(String videoId);
}

/// Everything the app asks YouTube. [YouTubeService] does the work; the app reaches it
/// through `YouTubeWorker`, which runs it in a background isolate.
abstract interface class YouTubeApi implements VideoSource, StreamSource {
  void dispose();
}

class YouTubeService implements YouTubeApi {
  final YoutubeHttpClient _http = YoutubeHttpClient();
  late final YoutubeExplode _yt = YoutubeExplode(_http);
  // ignore: invalid_use_of_internal_member
  late final VideoController _playerApi = VideoController(_http);

  /// Upper bound for each HD lookup step, so a slow or blocked lookup falls back to 360p
  /// quickly instead of leaving the driver on a spinner.
  static const _hdLookupTimeout = Duration(seconds: 6);

  // ---------------------------------------------------------------- search

  /// Search goes through `searchContent` and maps results here: 2.5.3's `search()`
  /// converts every result's duration and date while parsing and throws
  /// `FormatException` on pages with live videos (the "Live" chip, "lofi"), while the
  /// raw results are fine.
  @override
  Future<VideoPage> search(String query) async {
    var page = _page(await _search(query));
    // YouTube occasionally serves a results page the parser reads as empty; the same query
    // submitted again returned results on the phone. One retry, no loop (anti-bot).
    if (page.videos.isEmpty) page = _page(await _search(query));
    return page;
  }

  Future<SearchList> _search(String query) => _guard(
        () => _yt.search.searchContent(query, filter: TypeFilters.video),
      );

  @override
  Future<VideoPage> nextPage(VideoPage page) async {
    final cursor = page.cursor;
    if (cursor is! SearchList) return const VideoPage([]);
    final next = await _guard(() => cursor.nextPage());
    return next == null ? const VideoPage([]) : _page(next);
  }

  VideoPage _page(SearchList list) {
    final videos = list.whereType<SearchVideo>().map(_mapVideo).toList();
    return VideoPage(videos, cursor: list.isEmpty ? null : list);
  }

  CarVideo _mapVideo(SearchVideo video) {
    final duration = parseClockDuration(video.duration);
    return CarVideo(
      id: video.id.value,
      title: video.title,
      uploader: video.author,
      // mediumRes (~320px) is enough for the car grid; highRes wastes bandwidth and RAM.
      thumbnail: ThumbnailSet(video.id.value).mediumResUrl,
      duration: duration ?? Duration.zero,
      isLive: video.isLive,
      views: video.viewCount,
      uploadDate: video.uploadDate ?? 'Terbaru',
    );
  }

  // --------------------------------------------------------------- streams

  /// Resolves what to play for [video] at [quality].
  ///
  /// Client choice comes from probing youtube_explode_dart 2.5.3: the `android` client is
  /// fastest (~0.7 s) and has the only muxed progressive stream (itag 18, 360p) plus all
  /// audio; only the `ios` client offers HLS (variants up to 1080p), and only for some
  /// videos.
  @override
  Future<ResolvedStreams> resolveStreams(
    CarVideo video,
    StreamQuality quality,
  ) {
    return _guard(() async {
      if (video.isLive) {
        // Live has no progressive stream; the android manifest even crashes on it.
        final live = await _resolveHls(video.id, quality, live: true) ??
            await _resolveWatchPageLive(video.id, quality);
        if (live != null) return live;
        throw const YouTubeServiceException(
          'Siaran live ini tidak bisa diputar.',
        );
      }
      if (quality.height > 360) {
        final hd = await _resolveHls(video.id, quality);
        if (hd != null) return hd;
        return _resolveProgressive(video.id, wantVideo: true, downgraded: true);
      }
      return _resolveProgressive(video.id, wantVideo: !quality.isAudioOnly);
    });
  }

  /// Older route to a live HLS master, through the web watch page. YouTube's web player
  /// now often omits it even for streams that are live, so it is only a fallback.
  Future<ResolvedStreams?> _resolveWatchPageLive(
    String id,
    StreamQuality quality,
  ) async {
    try {
      final url = await _yt.videos.streamsClient.getHttpLiveStreamUrl(
        VideoId(id),
      );
      final hls = Uri.parse(url);
      return ResolvedStreams(
        label: 'LIVE',
        videoUri: quality.isAudioOnly ? null : hls,
        videoIsHls: true,
        audioUri: hls,
        audioIsHls: true,
      );
    } on YoutubeExplodeException catch (e) {
      debugPrint('Watch-page live HLS unavailable for $id: $e');
      return null;
    }
  }

  /// HLS for 480p/720p of normal videos and for live streams. Null when no client offers
  /// a master (normal for many uploads) or the lookup fails.
  ///
  /// Probed 2026-11-21: for uploads only the ios client returns `hlsManifestUrl`, and only
  /// for some videos; for live streams the android client does and ios does not. The
  /// master comes straight from the player response instead of getManifest, which
  /// HEAD-checks the first adaptive stream (YouTube often answers ios with 403 because it
  /// wants a PO token) and then retries for about 15 s.
  Future<ResolvedStreams?> _resolveHls(
    String id,
    StreamQuality quality, {
    bool live = false,
  }) async {
    try {
      final masterUri = await _hlsMasterUrl(
        id,
        live
            ? [YoutubeApiClient.android, YoutubeApiClient.ios]
            : [YoutubeApiClient.ios],
      );
      if (masterUri == null) {
        debugPrint('No HLS master for $id');
        return null;
      }
      final response = await http
          .get(masterUri, headers: kStreamHeaders)
          .timeout(_hdLookupTimeout);
      if (response.statusCode != 200) {
        debugPrint('HLS master HTTP ${response.statusCode} for $id');
        return null;
      }
      final master = parseHlsMaster(response.body, masterUri);
      // Live and 360p requests aim at 360p; HD requests at their own height.
      final height = quality.height == 0 ? 360 : quality.height;
      final pick = pickHlsVariant(master.playable, height);
      if (pick == null) {
        if (master.variants.isEmpty) return null;
        // Variants carry their own audio (no renditions): let ExoPlayer adapt over the
        // original master.
        return ResolvedStreams(
          label: live ? 'LIVE' : 'Auto',
          videoUri: quality.isAudioOnly ? null : masterUri,
          videoIsHls: true,
          audioUri: masterUri,
          audioIsHls: true,
        );
      }
      final audioUrl = master.audioGroups[pick.audioGroup]!;
      return ResolvedStreams(
        label: live ? 'LIVE' : '${pick.height}p',
        videoUri: quality.isAudioOnly
            ? null
            : hlsMasterDataUri(buildHlsMaster(variant: pick, audioUrl: audioUrl)),
        videoIsHls: true,
        audioUri: audioUrl,
        audioIsHls: true,
        downgraded: !live && pick.height < height,
      );
    } catch (e) {
      debugPrint('HLS lookup failed for $id: $e');
      return null;
    }
  }

  Future<Uri?> _hlsMasterUrl(String id, List<YoutubeApiClient> clients) async {
    for (final client in clients) {
      final player = await _playerApi
          .getPlayerResponse(VideoId(id), client)
          .timeout(_hdLookupTimeout);
      final url = player.hlsManifestUrl;
      if (player.isVideoPlayable && url != null) return Uri.parse(url);
    }
    return null;
  }

  Future<ResolvedStreams> _resolveProgressive(
    String id, {
    required bool wantVideo,
    bool downgraded = false,
  }) async {
    StreamManifest manifest;
    try {
      manifest = await _yt.videos.streamsClient.getManifest(
        id,
        ytClients: [YoutubeApiClient.android],
      );
    } catch (e) {
      // The library's default client list (android, ios, then tv) is slower but works for
      // some restricted videos the android client alone refuses.
      debugPrint('android manifest failed for $id, retrying defaults: $e');
      manifest = await _yt.videos.streamsClient.getManifest(id);
    }
    final audio = manifest.audioOnly.isEmpty
        ? null
        : manifest.audioOnly.withHighestBitrate();
    final muxed =
        manifest.muxed.isEmpty ? null : manifest.muxed.withHighestBitrate();
    if (muxed == null && audio == null) {
      throw const YouTubeServiceException(
        'Stream tidak tersedia. Coba video lain.',
      );
    }
    return ResolvedStreams(
      label: wantVideo && muxed != null ? muxed.qualityLabel : 'Audio',
      videoUri: wantVideo ? muxed?.url : null,
      audioUri: audio?.url ?? muxed?.url,
      audioFallbackUri: audio != null ? muxed?.url : null,
      downgraded: downgraded,
    );
  }

  // ----------------------------------------------------------- sponsorblock

  /// SponsorBlock segments to auto-skip; empty when the service is slow or offline.
  @override
  Future<List<SponsorSegment>> getSponsorSegments(String videoId) async {
    try {
      final url = Uri.https('sponsor.ajay.app', '/api/skipSegments', {
        'videoID': videoId,
        'categories': jsonEncode(
          ['sponsor', 'intro', 'outro', 'selfpromo', 'music_offtopic'],
        ),
      });
      final res = await http.get(url).timeout(const Duration(seconds: 3));
      if (res.statusCode != 200) return const [];
      final data = jsonDecode(res.body) as List<dynamic>;
      return [
        for (final item in data.cast<Map<String, dynamic>>())
          SponsorSegment(
            category: item['category'] as String? ?? 'sponsor',
            start: ((item['segment'] as List<dynamic>)[0] as num).toDouble(),
            end: ((item['segment'] as List<dynamic>)[1] as num).toDouble(),
          ),
      ]..sort((a, b) => a.start.compareTo(b.start));
    } catch (e) {
      return const [];
    }
  }

  @override
  void dispose() => _yt.close();

  // ----------------------------------------------------------------- errors

  /// Runs [call] and turns every failure into a [YouTubeServiceException] whose message
  /// can be shown as is.
  Future<T> _guard<T>(Future<T> Function() call) async {
    try {
      return await call();
    } on YouTubeServiceException {
      rethrow;
    } on SocketException catch (e) {
      throw YouTubeServiceException('Tidak ada koneksi internet.', e);
    } on TimeoutException catch (e) {
      throw YouTubeServiceException('YouTube tidak merespons. Coba lagi.', e);
    } on http.ClientException catch (e) {
      // YouTube answers bursts of requests with a redirect loop to its anti-bot page,
      // which the http package reports as "Redirect limit exceeded".
      if (e.message.contains('Redirect limit')) {
        throw YouTubeServiceException(
          'YouTube sedang membatasi permintaan. Coba lagi beberapa menit lagi.',
          e,
        );
      }
      throw YouTubeServiceException('Koneksi ke YouTube gagal.', e);
    } on RequestLimitExceededException catch (e) {
      throw YouTubeServiceException(
        'YouTube sedang membatasi permintaan. Coba lagi beberapa menit lagi.',
        e,
      );
    } on VideoRequiresPurchaseException catch (e) {
      throw YouTubeServiceException('Video ini berbayar.', e);
    } on VideoUnavailableException catch (e) {
      // Subtype of VideoUnplayableException, so it must be caught first.
      throw YouTubeServiceException('Video tidak tersedia.', e);
    } on VideoUnplayableException catch (e) {
      throw YouTubeServiceException(
        'Video ini tidak bisa diputar (dibatasi usia, wilayah, atau belum tayang).',
        e,
      );
    } on FormatException catch (e) {
      // youtube_explode_dart 2.5.3 fails to parse some result pages (e.g. "lofi").
      throw YouTubeServiceException(
        'Hasil dari YouTube tidak bisa dibaca. Coba kata kunci lain.',
        e,
      );
    } catch (e) {
      debugPrint('YouTube request failed: $e');
      throw YouTubeServiceException('Gagal memuat dari YouTube. Coba lagi.', e);
    }
  }
}
