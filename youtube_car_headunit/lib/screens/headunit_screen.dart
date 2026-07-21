import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:just_audio/just_audio.dart' show ProcessingState;
import 'package:lucide_icons/lucide_icons.dart';
import 'package:video_player/video_player.dart';
import '../models/car_video.dart';
import '../services/car_audio_handler.dart';
import '../services/pip_service.dart';
import '../services/youtube_service.dart';
import '../theme/car_theme.dart';
import '../widgets/fullscreen_player.dart';
import '../widgets/player_widget.dart';
import '../widgets/video_card.dart';
import 'driving_mode_screen.dart';

class HeadunitScreen extends StatefulWidget {
  const HeadunitScreen({super.key});

  @override
  State<HeadunitScreen> createState() => _HeadunitScreenState();
}

class _HeadunitScreenState extends State<HeadunitScreen> {
  final YouTubeService _ytService = YouTubeService();
  final CarAudioHandler _audioHandler = CarAudioHandler();
  final PipService _pipService = PipService();
  final TextEditingController _searchController = TextEditingController();

  List<CarVideo> _videos = [];
  CarVideo? _currentVideo;
  List<SponsorSegment> _sponsorSegments = [];
  VideoPlayerController? _videoController;
  StreamSubscription? _audioStateSub;

  bool _isLoading = true;
  bool _isVideoLoading = false;
  bool _isDrivingMode = false;
  bool _isFullscreen = false;
  bool _isAudioPlaying = false;
  bool _forceAudioOnly = false;
  bool _didAdvanceOnComplete = false;
  bool _searchHasText = false;
  String? _loadError;
  String _selectedQuality = '360p';
  bool _isAutoplayEnabled = true;
  int _selectGeneration = 0;

  bool get _isAudioOnly => _selectedQuality == 'Audio Only' || _forceAudioOnly;

  static const Map<String, String> defaultHeaders = {
    'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
            '(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  };

  @override
  void initState() {
    super.initState();
    _audioStateSub = _audioHandler.player.playerStateStream.listen((state) {
      if (!mounted) return;
      final playing = state.playing;
      if (playing != _isAudioPlaying) {
        setState(() => _isAudioPlaying = playing);
      }
      // Autoplay for audio-only mode (one-shot per track)
      if (_isAudioOnly &&
          state.processingState == ProcessingState.completed &&
          _isAutoplayEnabled &&
          !_isVideoLoading &&
          !_didAdvanceOnComplete) {
        _didAdvanceOnComplete = true;
        _nextTrack();
      }
    });
    _loadTrending();
  }

  Future<void> _loadTrending({bool force = false}) async {
    if (!force && _videos.isNotEmpty && !_isLoading) {
      setState(() {
        _isFullscreen = false;
        _isDrivingMode = false;
      });
      return;
    }
    setState(() => _isLoading = true);
    final list = await _ytService.getCarTrendingVideos();
    if (!mounted) return;
    setState(() {
      _videos = list;
      _isLoading = false;
    });
    // Do not auto-start first video — saves battery/CPU on cold start (A6+)
  }

  Future<void> _search(String query) async {
    if (query.trim().isEmpty) return _loadTrending(force: true);
    setState(() => _isLoading = true);
    final list = await _ytService.searchVideos(query);
    if (!mounted) return;
    setState(() {
      _videos = list;
      _isLoading = false;
    });
  }

  Future<void> _selectVideo(CarVideo video, {Duration? seekToPosition}) async {
    final generation = ++_selectGeneration;
    await _audioHandler.stop();

    final oldController = _videoController;
    _videoController = null;

    setState(() {
      _currentVideo = video;
      _isVideoLoading = true;
      _sponsorSegments = [];
      _isAudioPlaying = false;
      _forceAudioOnly = false;
      _didAdvanceOnComplete = false;
      _loadError = null;
    });

    // Await dispose so A6+ never runs two video decoders at once
    if (oldController != null) {
      await oldController.dispose();
    }

    try {
      // Parallel fetch — cuts perceived load time ~30-50%
      final results = await Future.wait([
        _ytService.getAdFreeStreams(video, preferredQuality: _selectedQuality),
        _ytService.getSponsorSegments(video.id),
      ]);

      if (!mounted || generation != _selectGeneration) return;

      final videoWithStreams = results[0] as CarVideo;
      final segments = results[1] as List<SponsorSegment>;

      // --- Audio Only path (single decoder: just_audio) ---
      final useAudioOnly = _selectedQuality == 'Audio Only' ||
          videoWithStreams.videoStreamUrl == null;

      if (useAudioOnly) {
        if (videoWithStreams.audioStreamUrl == null) {
          setState(() {
            _isVideoLoading = false;
            _loadError = 'Stream tidak tersedia. Coba video lain.';
          });
          _showError(_loadError!);
          return;
        }
        setState(() {
          _currentVideo = videoWithStreams;
          _sponsorSegments = segments;
          _isVideoLoading = false;
          _forceAudioOnly = _selectedQuality != 'Audio Only';
        });
        if (_forceAudioOnly) {
          _showError('Video stream gagal — putar Audio Only');
        }
        await _audioHandler.playAudioOnly(videoWithStreams);
        if (seekToPosition != null) {
          await _audioHandler.seek(seekToPosition);
        }
        _pipService.setAutoPip(false);
        return;
      }

      // --- Video path (single decoder: VideoPlayer — muxed A/V) ---
      final newController = VideoPlayerController.networkUrl(
        Uri.parse(videoWithStreams.videoStreamUrl!),
        httpHeaders: defaultHeaders,
        videoPlayerOptions: VideoPlayerOptions(mixWithOthers: false),
      );

      await newController.initialize();

      if (!mounted || generation != _selectGeneration) {
        await newController.dispose();
        return;
      }

      newController.addListener(() {
        if (!mounted || _currentVideo?.id != video.id) return;
        final val = newController.value;
        if (val.isInitialized &&
            !val.isPlaying &&
            val.position >= val.duration &&
            val.duration > Duration.zero &&
            !_isVideoLoading &&
            _isAutoplayEnabled &&
            !_isAudioOnly &&
            !_didAdvanceOnComplete) {
          _didAdvanceOnComplete = true;
          _nextTrack();
        }
      });

      if (seekToPosition != null) {
        await newController.seekTo(seekToPosition);
      }
      await newController.play();

      if (!mounted || generation != _selectGeneration) {
        await newController.dispose();
        return;
      }

      setState(() {
        _videoController = newController;
        _currentVideo = videoWithStreams;
        _sponsorSegments = segments;
        _isVideoLoading = false;
      });
      // Metadata only — do NOT start a second audio decoder
      _audioHandler.updateMetadataOnly(videoWithStreams);
      _pipService.setAutoPip(true);
    } catch (e) {
      if (mounted && generation == _selectGeneration) {
        setState(() {
          _isVideoLoading = false;
          _loadError = 'Gagal memuat video. Coba lagi.';
        });
        _showError(_loadError!);
      }
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 3),
        action: _currentVideo != null
            ? SnackBarAction(
                label: 'Ulangi',
                onPressed: () => _selectVideo(_currentVideo!),
              )
            : null,
      ),
    );
  }

  void _togglePlay() {
    if (_isAudioOnly) {
      if (_isAudioPlaying) {
        _audioHandler.pause();
      } else {
        _audioHandler.play();
      }
      return;
    }
    final c = _videoController;
    if (c == null) return;
    if (c.value.isPlaying) {
      c.pause();
    } else {
      c.play();
    }
    setState(() {});
  }

  bool get _isPlayingNow {
    if (_isAudioOnly) return _isAudioPlaying;
    return _videoController?.value.isPlaying ?? false;
  }

  void _onQualityChanged(String newQuality) {
    if (_selectedQuality == newQuality || _currentVideo == null) return;
    final currentPos = _isAudioOnly
        ? _audioHandler.player.position
        : _videoController?.value.position;
    setState(() => _selectedQuality = newQuality);
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Kualitas → $newQuality'),
        duration: const Duration(seconds: 2),
      ),
    );
    _selectVideo(_currentVideo!, seekToPosition: currentPos);
  }

  void _nextTrack() {
    if (_videos.isEmpty || _currentVideo == null) return;
    final index = _videos.indexWhere((v) => v.id == _currentVideo!.id);
    final nextIndex = index < 0 ? 0 : (index + 1) % _videos.length;
    _selectVideo(_videos[nextIndex]);
  }

  void _prevTrack() {
    if (_videos.isEmpty || _currentVideo == null) return;
    final index = _videos.indexWhere((v) => v.id == _currentVideo!.id);
    final prevIndex =
        index < 0 ? 0 : (index - 1 + _videos.length) % _videos.length;
    _selectVideo(_videos[prevIndex]);
  }

  void _seek(Duration position) {
    if (_isAudioOnly) {
      _audioHandler.seek(position);
    } else {
      _videoController?.seekTo(position);
    }
  }

  Widget _buildSidebarButton({
    required IconData icon,
    required bool isActive,
    required Color color,
    required VoidCallback onTap,
    VoidCallback? onLongPress,
    String? tooltip,
  }) {
    return Tooltip(
      message: tooltip ?? '',
      child: Container(
        decoration: BoxDecoration(
          color: isActive ? color.withValues(alpha: 0.2) : Colors.transparent,
          borderRadius: BorderRadius.circular(14),
          border: isActive
              ? Border.all(color: color.withValues(alpha: 0.6), width: 1.5)
              : null,
        ),
        child: IconButton(
          iconSize: 26,
          padding: const EdgeInsets.all(12),
          constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
          icon: Icon(icon, color: isActive ? color : CarTheme.textMuted),
          onPressed: onTap,
          onLongPress: onLongPress,
        ),
      ),
    );
  }

  Widget _buildCategoryChip(String label, VoidCallback onTap) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ActionChip(
        label: Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: CarTheme.textMain,
          ),
        ),
        onPressed: onTap,
        backgroundColor: CarTheme.bgCard,
        side: const BorderSide(color: Colors.white12),
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 0),
        visualDensity: VisualDensity.compact,
      ),
    );
  }

  Widget _buildPlayer() {
    if (_currentVideo == null) {
      return const Center(
        child: Text(
          'Pilih video dari daftar',
          style: TextStyle(color: CarTheme.textMuted),
        ),
      );
    }
    return PlayerWidget(
      video: _currentVideo!,
      controller: _videoController,
      isVideoLoading: _isVideoLoading,
      isAudioOnly: _isAudioOnly,
      selectedQuality: _selectedQuality,
      onQualityChanged: _onQualityChanged,
      isAutoplayEnabled: _isAutoplayEnabled,
      onToggleAutoplay: () =>
          setState(() => _isAutoplayEnabled = !_isAutoplayEnabled),
      sponsorSegments: _sponsorSegments,
      onNext: _nextTrack,
      onPrevious: _prevTrack,
      onTogglePlay: _togglePlay,
      isAudioPlaying: _isAudioPlaying,
      onToggleDrivingMode: () => setState(() => _isDrivingMode = true),
      onToggleFullscreen: () {
        if (!_isAudioOnly) setState(() => _isFullscreen = true);
      },
    );
  }

  @override
  void dispose() {
    _audioStateSub?.cancel();
    _videoController?.dispose();
    _ytService.dispose();
    _audioHandler.dispose();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: _pipService.isInPip,
      builder: (context, isInPip, child) {
        if (isInPip) {
          return Scaffold(
            backgroundColor: Colors.black,
            body: Center(
              child: _videoController != null &&
                      _videoController!.value.isInitialized
                  ? AspectRatio(
                      aspectRatio: _videoController!.value.aspectRatio > 0
                          ? _videoController!.value.aspectRatio
                          : 16 / 9,
                      child: VideoPlayer(_videoController!),
                    )
                  : Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(
                          LucideIcons.music,
                          color: Colors.white70,
                          size: 40,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _currentVideo?.title ?? 'Audio',
                          maxLines: 2,
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            color: Colors.white70,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
            ),
          );
        }

        if (_isDrivingMode && _currentVideo != null) {
          return DrivingModeScreen(
            video: _currentVideo!,
            isPlaying: _isPlayingNow,
            isAudioOnly: _isAudioOnly,
            videoController: _videoController,
            audioPlayer: _audioHandler.player,
            onNext: _nextTrack,
            onPrevious: _prevTrack,
            onTogglePlay: _togglePlay,
            onSeek: _seek,
            onClose: () => setState(() => _isDrivingMode = false),
          );
        }

        return Scaffold(
          resizeToAvoidBottomInset: false,
          backgroundColor: CarTheme.bgPrimary,
          body: SafeArea(
            child: Stack(
              children: [
                Row(
                  children: [
                    Container(
                      width: 72,
                      color: CarTheme.bgSurface,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      child: Column(
                        children: [
                          Container(
                            width: 44,
                            height: 44,
                            decoration: BoxDecoration(
                              color: CarTheme.accentRed,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Icon(
                              LucideIcons.car,
                              color: Colors.white,
                              size: 24,
                            ),
                          ),
                          const Spacer(),
                          _buildSidebarButton(
                            icon: LucideIcons.home,
                            isActive: !_isDrivingMode && !_isFullscreen,
                            color: CarTheme.accentRed,
                            tooltip: 'Beranda',
                            onTap: () => _loadTrending(),
                            onLongPress: () => _loadTrending(force: true),
                          ),
                          const SizedBox(height: 8),
                          _buildSidebarButton(
                            icon: LucideIcons.tv,
                            isActive: _isDrivingMode,
                            color: Colors.orangeAccent,
                            tooltip: 'Mode Setir',
                            onTap: () {
                              if (_currentVideo != null) {
                                setState(() => _isDrivingMode = true);
                              }
                            },
                          ),
                          const SizedBox(height: 8),
                          _buildSidebarButton(
                            icon: LucideIcons.maximize,
                            isActive: _isFullscreen,
                            color: Colors.lightBlueAccent,
                            tooltip: 'Fullscreen',
                            onTap: () {
                              if (_currentVideo != null && !_isAudioOnly) {
                                setState(() => _isFullscreen = true);
                              }
                            },
                          ),
                          const SizedBox(height: 8),
                          _buildSidebarButton(
                            icon: LucideIcons.minimize2,
                            isActive: false,
                            color: Colors.purpleAccent,
                            tooltip: 'Picture-in-Picture',
                            onTap: () async {
                              final messenger = ScaffoldMessenger.of(context);
                              if (_videoController == null ||
                                  !(_videoController!.value.isInitialized)) {
                                messenger.showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      'PiP butuh video aktif (bukan Audio Only)',
                                    ),
                                  ),
                                );
                                return;
                              }
                              final success = await _pipService.enterPip();
                              if (!success) {
                                messenger.showSnackBar(
                                  const SnackBar(
                                    content: Text(
                                      'PiP didukung di Android 8.0+',
                                    ),
                                  ),
                                );
                              }
                            },
                          ),
                          const Spacer(),
                        ],
                      ),
                    ),
                    Expanded(
                      child: Column(
                        children: [
                          Container(
                            color: CarTheme.bgSurface,
                            padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Row(
                                  children: [
                                    const Text(
                                      'YOUTUBE CAR',
                                      style: TextStyle(
                                        fontSize: 15,
                                        fontWeight: FontWeight.bold,
                                        letterSpacing: 0.5,
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 6,
                                        vertical: 2,
                                      ),
                                      decoration: BoxDecoration(
                                        color: CarTheme.accentRed,
                                        borderRadius: BorderRadius.circular(8),
                                      ),
                                      child: Text(
                                        _selectedQuality,
                                        style: const TextStyle(
                                          fontSize: 10,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: SizedBox(
                                        height: 42,
                                        child: TextField(
                                          controller: _searchController,
                                          style: const TextStyle(fontSize: 13),
                                          onSubmitted: _search,
                                          textInputAction:
                                              TextInputAction.search,
                                          decoration: InputDecoration(
                                            hintText:
                                                'Cari musik, lagu anak, kajian...',
                                            prefixIcon: const Icon(
                                              LucideIcons.search,
                                              size: 18,
                                              color: CarTheme.textMuted,
                                            ),
                                            suffixIcon: _searchHasText
                                                ? IconButton(
                                                    icon: const Icon(
                                                      LucideIcons.x,
                                                      size: 16,
                                                    ),
                                                    onPressed: () {
                                                      _searchController.clear();
                                                      setState(() =>
                                                          _searchHasText =
                                                              false);
                                                      _loadTrending(
                                                        force: true,
                                                      );
                                                    },
                                                  )
                                                : null,
                                            filled: true,
                                            fillColor: CarTheme.bgPrimary,
                                            contentPadding:
                                                const EdgeInsets.symmetric(
                                              horizontal: 12,
                                              vertical: 8,
                                            ),
                                            border: OutlineInputBorder(
                                              borderRadius:
                                                  BorderRadius.circular(12),
                                              borderSide: BorderSide.none,
                                            ),
                                          ),
                                          onChanged: (v) {
                                            final has = v.isNotEmpty;
                                            if (has != _searchHasText) {
                                              setState(
                                                  () => _searchHasText = has);
                                            }
                                          },
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 6),
                                SizedBox(
                                  height: 36,
                                  child: ListView(
                                    scrollDirection: Axis.horizontal,
                                    children: [
                                      _buildCategoryChip(
                                        'Trending',
                                        () => _loadTrending(force: true),
                                      ),
                                      _buildCategoryChip(
                                        'Live',
                                        () => _search('Live Stream Indonesia'),
                                      ),
                                      _buildCategoryChip(
                                        'Musik',
                                        () => _search('Musik Indonesia'),
                                      ),
                                      _buildCategoryChip(
                                        'Lagu Anak',
                                        () => _search('Lagu Anak Indonesia'),
                                      ),
                                      _buildCategoryChip(
                                        'Islami',
                                        () => _search('Lagu Anak Islami'),
                                      ),
                                      _buildCategoryChip(
                                        'Kajian',
                                        () => _search('Kajian Islam Ceramah'),
                                      ),
                                      _buildCategoryChip(
                                        'Berita',
                                        () => _search('Berita Hari Ini'),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Expanded(
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (_currentVideo != null)
                                  Expanded(
                                    flex: 4,
                                    child: Padding(
                                      padding: const EdgeInsets.fromLTRB(
                                        10,
                                        6,
                                        4,
                                        6,
                                      ),
                                      child: _buildPlayer(),
                                    ),
                                  ),
                                Expanded(
                                  flex: 6,
                                  child: _isLoading
                                      ? const Center(
                                          child: SpinKitPulse(
                                            color: CarTheme.accentRed,
                                            size: 40,
                                          ),
                                        )
                                      : _videos.isEmpty
                                          ? Center(
                                              child: Column(
                                                mainAxisSize: MainAxisSize.min,
                                                children: [
                                                  const Icon(
                                                    LucideIcons.searchX,
                                                    color: CarTheme.textMuted,
                                                    size: 36,
                                                  ),
                                                  const SizedBox(height: 10),
                                                  const Text(
                                                    'Tidak ada hasil',
                                                    style: TextStyle(
                                                      color: CarTheme.textMuted,
                                                      fontWeight:
                                                          FontWeight.bold,
                                                    ),
                                                  ),
                                                  const SizedBox(height: 8),
                                                  TextButton(
                                                    onPressed: () =>
                                                        _loadTrending(
                                                      force: true,
                                                    ),
                                                    child: const Text(
                                                      'Muat Trending',
                                                    ),
                                                  ),
                                                ],
                                              ),
                                            )
                                          : GridView.builder(
                                              padding: const EdgeInsets.all(8),
                                              cacheExtent: 200,
                                              addAutomaticKeepAlives: false,
                                              addRepaintBoundaries: true,
                                              gridDelegate:
                                                  const SliverGridDelegateWithMaxCrossAxisExtent(
                                                maxCrossAxisExtent: 175,
                                                childAspectRatio: 0.92,
                                                crossAxisSpacing: 8,
                                                mainAxisSpacing: 8,
                                              ),
                                              itemCount: _videos.length,
                                              itemBuilder: (context, index) {
                                                final video = _videos[index];
                                                return VideoCard(
                                                  key: ValueKey(video.id),
                                                  video: video,
                                                  isSelected:
                                                      _currentVideo?.id ==
                                                          video.id,
                                                  onTap: () =>
                                                      _selectVideo(video),
                                                );
                                              },
                                            ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                if (_isFullscreen && _currentVideo != null && !_isAudioOnly)
                  Positioned.fill(
                    child: FullscreenPlayer(
                      video: _currentVideo!,
                      controller: _videoController,
                      isVideoLoading: _isVideoLoading,
                      onNext: _nextTrack,
                      onPrevious: _prevTrack,
                      onCloseFullscreen: () =>
                          setState(() => _isFullscreen = false),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}
