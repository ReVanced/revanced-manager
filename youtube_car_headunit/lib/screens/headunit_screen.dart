import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:video_player/video_player.dart';
import '../models/car_video.dart';
import '../services/youtube_service.dart';
import '../services/car_audio_handler.dart';
import '../theme/car_theme.dart';
import '../widgets/video_card.dart';
import '../widgets/player_widget.dart';
import '../widgets/fullscreen_player.dart';
import 'driving_mode_screen.dart';

class HeadunitScreen extends StatefulWidget {
  const HeadunitScreen({super.key});

  @override
  State<HeadunitScreen> createState() => _HeadunitScreenState();
}

class _HeadunitScreenState extends State<HeadunitScreen> {
  final YouTubeService _ytService = YouTubeService();
  final CarAudioHandler _audioHandler = CarAudioHandler();
  final TextEditingController _searchController = TextEditingController();

  List<CarVideo> _videos = [];
  CarVideo? _currentVideo;
  List<SponsorSegment> _sponsorSegments = [];
  VideoPlayerController? _videoController;

  bool _isLoading = true;
  bool _isVideoLoading = false;
  bool _isDrivingMode = false;
  bool _isFullscreen = false;
  String _selectedQuality = '360p';
  bool _isAutoplayEnabled = true;

  static const Map<String, String> defaultHeaders = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  };

  @override
  void initState() {
    super.initState();
    _loadTrending();
  }

  Future<void> _loadTrending() async {
    setState(() => _isLoading = true);
    final list = await _ytService.getCarTrendingVideos();
    setState(() {
      _videos = list;
      _isLoading = false;
    });
    if (list.isNotEmpty && _currentVideo == null) {
      _selectVideo(list[0]);
    }
  }

  Future<void> _search(String query) async {
    if (query.trim().isEmpty) return _loadTrending();
    setState(() => _isLoading = true);
    final list = await _ytService.searchVideos(query);
    setState(() {
      _videos = list;
      _isLoading = false;
    });
    if (list.isNotEmpty && _currentVideo == null) {
      _selectVideo(list[0]);
    }
  }

  Future<void> _selectVideo(CarVideo video, {Duration? seekToPosition}) async {
    // 1. Instant Audio Stop & Instant UI State Update (0ms delay)
    _audioHandler.stop();

    final oldController = _videoController;
    _videoController = null;

    setState(() {
      _currentVideo = video;
      _isVideoLoading = true;
      _sponsorSegments = [];
    });

    await oldController?.dispose();

    try {
      final videoWithStreams = await _ytService.getAdFreeStreams(video, preferredQuality: _selectedQuality);
      final segments = await _ytService.getSponsorSegments(video.id);

      // Race Condition Guard: If user selected another video while fetching, cancel!
      if (_currentVideo?.id != video.id) return;

      if (videoWithStreams.videoStreamUrl != null) {
        final newController = VideoPlayerController.networkUrl(
          Uri.parse(videoWithStreams.videoStreamUrl!),
          httpHeaders: defaultHeaders,
        );

        await newController.initialize();

        // Second Guard Check after async initialize
        if (_currentVideo?.id != video.id) {
          await newController.dispose();
          return;
        }

        // Autoplay Completion Listener
        newController.addListener(() {
          if (!mounted || _currentVideo?.id != video.id) return;
          final val = newController.value;
          if (val.isInitialized && !val.isPlaying && val.position >= val.duration && val.duration > Duration.zero) {
            if (!_isVideoLoading && _isAutoplayEnabled) {
              _nextTrack();
            }
          }
        });

        if (seekToPosition != null) {
          await newController.seekTo(seekToPosition);
        }
        newController.play();

        if (mounted && _currentVideo?.id == video.id) {
          setState(() {
            _videoController = newController;
            _currentVideo = videoWithStreams;
            _sponsorSegments = segments;
            _isVideoLoading = false; // Loading finished!
          });
          _audioHandler.playCarVideo(videoWithStreams);
        } else {
          await newController.dispose();
        }
      } else {
        if (mounted && _currentVideo?.id == video.id) {
          setState(() => _isVideoLoading = false);
        }
      }
    } catch (e) {
      if (mounted && _currentVideo?.id == video.id) {
        setState(() => _isVideoLoading = false);
      }
    }
  }

  void _onQualityChanged(String newQuality) {
    if (_selectedQuality == newQuality || _currentVideo == null) return;
    final currentPos = _videoController?.value.position;
    setState(() {
      _selectedQuality = newQuality;
    });
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Mengubah Kualitas Video ke $newQuality...'),
        duration: const Duration(seconds: 2),
      ),
    );
    _selectVideo(_currentVideo!, seekToPosition: currentPos);
  }

  void _nextTrack() {
    if (_videos.isEmpty || _currentVideo == null) return;
    final index = _videos.indexWhere((v) => v.id == _currentVideo!.id);
    final nextIndex = (index + 1) % _videos.length;
    _selectVideo(_videos[nextIndex]);
  }

  void _prevTrack() {
    if (_videos.isEmpty || _currentVideo == null) return;
    final index = _videos.indexWhere((v) => v.id == _currentVideo!.id);
    final prevIndex = (index - 1 + _videos.length) % _videos.length;
    _selectVideo(_videos[prevIndex]);
  }

  Widget _buildSidebarButton({
    required IconData icon,
    required bool isActive,
    required Color color,
    required VoidCallback onTap,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: isActive ? color.withValues(alpha: 0.2) : Colors.transparent,
        borderRadius: BorderRadius.circular(16),
        border: isActive ? Border.all(color: color.withValues(alpha: 0.6), width: 1.5) : null,
      ),
      child: IconButton(
        iconSize: 26,
        padding: const EdgeInsets.all(10),
        constraints: const BoxConstraints(minWidth: 46, minHeight: 46),
        icon: Icon(icon, color: isActive ? color : CarTheme.textMuted),
        onPressed: onTap,
      ),
    );
  }

  Widget _buildCategoryChip(String label, VoidCallback onTap) {
    return Padding(
      padding: const EdgeInsets.only(right: 6),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration: BoxDecoration(
            color: CarTheme.bgCard,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: Colors.white12),
          ),
          child: Text(
            label,
            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: CarTheme.textMain),
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _videoController?.dispose();
    _ytService.dispose();
    _audioHandler.dispose();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_isDrivingMode && _currentVideo != null) {
      return DrivingModeScreen(
        video: _currentVideo!,
        onNext: _nextTrack,
        onPrevious: _prevTrack,
        onClose: () => setState(() => _isDrivingMode = false),
      );
    }

    return Scaffold(
      backgroundColor: CarTheme.bgPrimary,
      body: SafeArea(
        child: Stack(
          children: [
            // Main Dashboard View
            Row(
              children: [
                // Left Sidebar Navigation Bar (Car Launcher Style)
                Container(
                  width: 72,
                  color: CarTheme.bgSurface,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  child: Column(
                    children: [
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: CarTheme.accentRed,
                          borderRadius: BorderRadius.circular(14),
                          boxShadow: const [BoxShadow(color: CarTheme.accentRedGlow, blurRadius: 12)],
                        ),
                        child: const Icon(LucideIcons.car, color: Colors.white, size: 24),
                      ),
                      const Spacer(),
                      _buildSidebarButton(
                        icon: LucideIcons.home,
                        isActive: !_isDrivingMode && !_isFullscreen,
                        color: CarTheme.accentRed,
                        onTap: _loadTrending,
                      ),
                      const SizedBox(height: 12),
                      _buildSidebarButton(
                        icon: LucideIcons.tv,
                        isActive: _isDrivingMode,
                        color: Colors.orangeAccent,
                        onTap: () {
                          if (_currentVideo != null) {
                            setState(() => _isDrivingMode = true);
                          }
                        },
                      ),
                      const SizedBox(height: 12),
                      _buildSidebarButton(
                        icon: LucideIcons.maximize,
                        isActive: _isFullscreen,
                        color: Colors.lightBlueAccent,
                        onTap: () {
                          if (_currentVideo != null) {
                            setState(() => _isFullscreen = true);
                          }
                        },
                      ),
                      const Spacer(),
                      _buildSidebarButton(
                        icon: LucideIcons.mapPin,
                        isActive: false,
                        color: CarTheme.accentGreen,
                        onTap: () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Membuka Google Maps / Waze...')),
                          );
                        },
                      ),
                    ],
                  ),
                ),

                // Main Content Area
                Expanded(
                  child: Column(
                    children: [
                      // Top Search Header with Category Chips & Voice Search
                      Container(
                        color: CarTheme.bgSurface,
                        padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Row(
                              children: [
                                const Text(
                                  'YOUTUBE CAR',
                                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: CarTheme.accentRed,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: const Text('PRO', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                                ),
                                const SizedBox(width: 16),
                                Expanded(
                                  child: SizedBox(
                                    height: 42,
                                    child: TextField(
                                      controller: _searchController,
                                      style: const TextStyle(fontSize: 13),
                                      onSubmitted: _search,
                                      decoration: InputDecoration(
                                        hintText: 'Cari musik, lagu anak, kajian, atau video...',
                                        prefixIcon: const Icon(LucideIcons.search, size: 18, color: CarTheme.textMuted),
                                        suffixIcon: IconButton(
                                          icon: const Icon(LucideIcons.mic, size: 18, color: CarTheme.accentRed),
                                          onPressed: () {
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(content: Text('Pencarian Suara: Katakan yang ingin Anda cari...')),
                                            );
                                          },
                                        ),
                                        filled: true,
                                        fillColor: CarTheme.bgPrimary,
                                        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                        border: OutlineInputBorder(
                                          borderRadius: BorderRadius.circular(12),
                                          borderSide: BorderSide.none,
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            // Category Chips (1-tap quick search for drivers)
                            SizedBox(
                              height: 28,
                              child: ListView(
                                scrollDirection: Axis.horizontal,
                                children: [
                                  _buildCategoryChip('🔥 Trending', () => _loadTrending()),
                                  _buildCategoryChip('🔴 Live', () => _search('Live Stream Indonesia')),
                                  _buildCategoryChip('🎵 Musik', () => _search('Musik Indonesia')),
                                  _buildCategoryChip('👶 Lagu Anak', () => _search('Lagu Anak Indonesia')),
                                  _buildCategoryChip('🌙 Lagu Anak Islami', () => _search('Lagu Anak Islami')),
                                  _buildCategoryChip('🕌 Kajian Islam', () => _search('Kajian Islam Ceramah')),
                                  _buildCategoryChip('📰 Berita', () => _search('Berita Hari Ini')),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),

                      // Feed + Player Section (Responsive Split)
                      Expanded(
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Video Player Panel (Left Column) - Responsive Flexible Width
                            if (_currentVideo != null)
                              Expanded(
                                flex: 4,
                                child: Padding(
                                  padding: const EdgeInsets.fromLTRB(10, 6, 4, 6),
                                  child: PlayerWidget(
                                    video: _currentVideo!,
                                    controller: _videoController,
                                    isVideoLoading: _isVideoLoading,
                                    selectedQuality: _selectedQuality,
                                    onQualityChanged: _onQualityChanged,
                                    isAutoplayEnabled: _isAutoplayEnabled,
                                    onToggleAutoplay: () => setState(() => _isAutoplayEnabled = !_isAutoplayEnabled),
                                    sponsorSegments: _sponsorSegments,
                                    onNext: _nextTrack,
                                    onPrevious: _prevTrack,
                                    onToggleDrivingMode: () => setState(() => _isDrivingMode = true),
                                    onToggleFullscreen: () => setState(() => _isFullscreen = true),
                                  ),
                                ),
                              ),

                            // Video Grid Feed (Right Column) - Compact & High Capacity
                            Expanded(
                              flex: 6,
                              child: _isLoading
                                  ? const Center(
                                      child: SpinKitPulse(color: CarTheme.accentRed, size: 50),
                                    )
                                  : GridView.builder(
                                      padding: const EdgeInsets.all(8),
                                      gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                                        maxCrossAxisExtent: 175,
                                        childAspectRatio: 0.92,
                                        crossAxisSpacing: 8,
                                        mainAxisSpacing: 8,
                                      ),
                                      itemCount: _videos.length,
                                      itemBuilder: (context, index) {
                                        final video = _videos[index];
                                        return VideoCard(
                                          video: video,
                                          isSelected: _currentVideo?.id == video.id,
                                          onTap: () => _selectVideo(video),
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

            // Fullscreen Overlay Stack
            if (_isFullscreen && _currentVideo != null)
              Positioned.fill(
                child: FullscreenPlayer(
                  video: _currentVideo!,
                  controller: _videoController,
                  isVideoLoading: _isVideoLoading,
                  onNext: _nextTrack,
                  onPrevious: _prevTrack,
                  onCloseFullscreen: () => setState(() => _isFullscreen = false),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
