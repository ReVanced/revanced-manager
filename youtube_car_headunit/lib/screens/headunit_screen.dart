import 'dart:async';

import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:video_player/video_player.dart';

import '../controllers/feed_controller.dart';
import '../controllers/location_controller.dart';
import '../controllers/navigation_controller.dart';
import '../controllers/playback_controller.dart';
import '../models/car_video.dart';
import '../models/navigation.dart';
import '../services/platform_bridge.dart';
import '../services/settings_store.dart';
import '../services/voice_guidance.dart';
import '../theme/car_theme.dart';
import '../widgets/fullscreen_player.dart';
import '../widgets/navigation/maneuver_banner.dart';
import '../widgets/nav_rail.dart';
import '../widgets/now_playing.dart';
import 'home_section.dart';
import 'maps_section.dart';
import 'music_section.dart';
import 'youtube_section.dart';

/// The head-unit shell: menu rail, the four sections, Fullscreen and PiP.
///
/// Sections stay mounted after their first visit, so switching keeps their state (grid
/// scroll, search, map camera) and is instant. Back closes Fullscreen, the destination
/// search or the route preview, then returns to Home, and on Home sends the app to the
/// background with playback (and guidance) running.
class HeadunitScreen extends StatefulWidget {
  const HeadunitScreen({
    super.key,
    required this.playback,
    required this.feed,
    required this.settings,
    required this.location,
    required this.navigation,
    this.tilesEnabled = true,
    this.speech,
    PlatformBridge? bridge,
  }) : _bridge = bridge;

  final PlaybackController playback;
  final FeedController feed;
  final SettingsStore settings;
  final LocationController location;
  final NavigationController navigation;

  /// False in widget tests, which have no network for map tiles.
  final bool tilesEnabled;

  /// Speaks the voice prompts; the device's engine when null.
  final SpeechEngine? speech;
  final PlatformBridge? _bridge;

  @override
  State<HeadunitScreen> createState() => _HeadunitScreenState();
}

class _HeadunitScreenState extends State<HeadunitScreen>
    with WidgetsBindingObserver {
  late final PlatformBridge _bridge = widget._bridge ?? PlatformBridge();
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _searchFocus = FocusNode();
  final Set<HeadunitSection> _visited = {HeadunitSection.home};
  final ValueNotifier<bool> _navSearchOpen = ValueNotifier(false);
  late final VoiceGuidance _voice;
  StreamSubscription<String>? _messagesSub;
  StreamSubscription<NavEvent>? _navEventsSub;
  Timer? _prebuildTimer;
  Timer? _systemUiTimer;
  bool _keyboardVisible = false;
  HeadunitSection _section = HeadunitSection.home;
  CarVideo? _resumeVideo;
  bool _fullscreen = false;

  PlaybackController get _playback => widget.playback;

  FeedController get _feed => widget.feed;

  NavigationController get _navigation => widget.navigation;

  @override
  void initState() {
    super.initState();
    _resumeVideo = widget.settings.lastVideo;
    _messagesSub = _playback.messages.listen(_showMessage);
    _navEventsSub = _navigation.events.listen((event) => _showNotice(event.message));
    _voice = VoiceGuidance(
      cues: _navigation.cues,
      engine: widget.speech ?? createSpeechEngine(),
    );
    _playback.addListener(_onPlayback);
    _feed.addListener(_syncQueue);
    _feed.loadTrending();
    widget.location.init();
    WidgetsBinding.instance.addObserver(this);
    // Build Maps, then YouTube, offstage shortly after startup so their first visits are
    // instant instead of waiting for the map to initialise and the grid to build (a
    // ~60 ms stall on the A6+).
    _prebuildTimer = Timer(const Duration(seconds: 2), () {
      if (!mounted) return;
      setState(() => _visited.add(HeadunitSection.maps));
      _prebuildTimer = Timer(const Duration(milliseconds: 1500), () {
        if (mounted) setState(() => _visited.add(HeadunitSection.youtube));
      });
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // The engine is cached by audio_service, so main() does not run again when Android
    // recreates the activity; reapply the head-unit system bars on every return.
    if (state == AppLifecycleState.resumed) applyCarSystemUi();
  }

  /// Android brings the phone status bar back on its own: when the keyboard opens, and
  /// sometimes on a window-focus or notification change. It is hidden again when the
  /// keyboard closes, and otherwise after a pause, so a driver pulling down the shade
  /// is not fought instantly.
  @override
  void didChangeMetrics() {
    final view = WidgetsBinding.instance.platformDispatcher.implicitView;
    if (view == null) return;
    final keyboard = view.viewInsets.bottom > 0;
    final closed = _keyboardVisible && !keyboard;
    _keyboardVisible = keyboard;
    final ownBars = !_fullscreen && !_bridge.isInPip.value;
    if (closed && ownBars) applyCarSystemUi();
    if (view.padding.top > 0 && !keyboard && ownBars) {
      _systemUiTimer ??= Timer(const Duration(seconds: 2), () {
        _systemUiTimer = null;
        final view = WidgetsBinding.instance.platformDispatcher.implicitView;
        if (!mounted || view == null || view.padding.top == 0) return;
        if (_fullscreen || _bridge.isInPip.value || view.viewInsets.bottom > 0) return;
        applyCarSystemUi();
      });
    } else if (view.padding.top == 0) {
      _systemUiTimer?.cancel();
      _systemUiTimer = null;
    }
  }

  /// Lets autoplay run into pages loaded after the video was picked.
  void _syncQueue() => _playback.updateQueue(_feed.videos);

  void _onPlayback() {
    // Fullscreen only makes sense while there is a picture.
    if (_fullscreen && !_playback.hasVideo && !_playback.isLoading) {
      setState(() => _fullscreen = false);
    }
  }

  void _go(HeadunitSection section) {
    if (section != HeadunitSection.youtube) _searchFocus.unfocus();
    // The destination search stays open in Maps, but its keyboard must not follow the
    // driver into another menu.
    if (section != HeadunitSection.maps && _navSearchOpen.value) {
      FocusManager.instance.primaryFocus?.unfocus();
    }
    setState(() {
      _section = section;
      _visited.add(section);
    });
  }

  void _handleBack() {
    final onMaps = _section == HeadunitSection.maps;
    if (_fullscreen) {
      setState(() => _fullscreen = false);
    } else if (onMaps && _navSearchOpen.value) {
      _navSearchOpen.value = false;
    } else if (onMaps && _navigation.state == NavState.preview) {
      _navigation.stop();
    } else if (_section != HeadunitSection.home) {
      _go(HeadunitSection.home);
    } else {
      _bridge.minimize();
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    final canRetry = _playback.error != null;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          duration: const Duration(seconds: 3),
          action: canRetry
              ? SnackBarAction(label: 'Ulangi', onPressed: _playback.retry)
              : null,
        ),
      );
  }

  /// Short notice without a retry action (navigation events).
  void _showNotice(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(content: Text(message), duration: const Duration(seconds: 3)),
      );
  }

  void _select(CarVideo video, {Duration? startAt}) {
    setState(() => _resumeVideo = null);
    _playback.select(video, queue: _feed.videos, startAt: startAt);
  }

  void _resume() {
    final video = _resumeVideo;
    if (video == null) return;
    _select(video, startAt: widget.settings.lastPosition);
    _go(HeadunitSection.music);
  }

  void _openFeed(String? query) {
    _searchController.clear();
    if (query == null) {
      _feed.loadTrending();
    } else {
      _feed.search(query);
    }
    _go(HeadunitSection.youtube);
  }

  void _openSearch() {
    _go(HeadunitSection.youtube);
    _searchFocus.requestFocus();
  }

  /// Home's Navigasi: the Maps menu with the destination search open, or the guidance
  /// already under way.
  void _openNavigation() {
    _go(HeadunitSection.maps);
    if (_navigation.state == NavState.idle) _navSearchOpen.value = true;
  }

  /// Hands navigation to Google Maps, to [destination] when set. Video goes to PiP
  /// first, because launching another app does not trigger the automatic PiP (that only
  /// follows Home or Recents).
  Future<void> _openGoogleMaps(Place? destination) async {
    final messenger = ScaffoldMessenger.of(context);
    if (_playback.hasVideo && await _bridge.enterPip()) {
      await Future<void>.delayed(const Duration(milliseconds: 400));
    }
    final target = destination?.location;
    final opened = await launchUrl(
      Uri.https('www.google.com', '/maps/dir/', {
        'api': '1',
        'travelmode': 'driving',
        if (target != null)
          'destination': '${target.latitude},${target.longitude}',
      }),
      mode: LaunchMode.externalApplication,
    ).catchError((Object _) => false);
    if (!opened) {
      messenger.showSnackBar(
        const SnackBar(content: Text('Google Maps tidak bisa dibuka.')),
      );
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _prebuildTimer?.cancel();
    _systemUiTimer?.cancel();
    _messagesSub?.cancel();
    _navEventsSub?.cancel();
    _voice.dispose();
    _navSearchOpen.dispose();
    _playback.removeListener(_onPlayback);
    _feed.removeListener(_syncQueue);
    _searchController.dispose();
    _searchFocus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<bool>(
      valueListenable: _bridge.isInPip,
      builder: (context, isInPip, _) {
        if (isInPip) return _PipView(playback: _playback);
        return PopScope(
          canPop: false,
          onPopInvokedWithResult: (didPop, _) {
            if (!didPop) _handleBack();
          },
          child: Scaffold(
            resizeToAvoidBottomInset: false,
            body: SafeArea(
              child: Stack(
                children: [
                  Row(
                    children: [
                      ListenableBuilder(
                        listenable: Listenable.merge([_playback, _navigation]),
                        builder: (context, _) => NavRail(
                          section: _section,
                          onSelect: _go,
                          isPlaying: _playback.isPlaying,
                          turnIndicator: _guiding
                              ? NextTurnTile(
                                  navigation: _navigation,
                                  onTap: () => _go(HeadunitSection.maps),
                                )
                              : null,
                        ),
                      ),
                      Expanded(child: _sections()),
                    ],
                  ),
                  if (_fullscreen)
                    Positioned.fill(
                      child: FullscreenPlayer(
                        playback: _playback,
                        onClose: () => setState(() => _fullscreen = false),
                      ),
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  bool get _guiding =>
      _navigation.state == NavState.navigating ||
      _navigation.state == NavState.arrived;

  Widget _sections() {
    return Stack(
      children: [
        for (final section in HeadunitSection.values)
          if (_visited.contains(section)) _slot(section, _build(section)),
      ],
    );
  }

  Widget _slot(HeadunitSection section, Widget child) =>
      _SectionSlot(active: section == _section, child: child);

  Widget _build(HeadunitSection section) {
    final active = section == _section;
    return switch (section) {
      HeadunitSection.home => HomeSection(
          playback: _playback,
          location: widget.location,
          navigation: _navigation,
          active: active,
          tilesEnabled: widget.tilesEnabled,
          resumeVideo: _resumeVideo,
          onResume: _resume,
          onOpenMaps: () => _go(HeadunitSection.maps),
          onOpenMusic: () => _go(HeadunitSection.music),
          onOpenYouTube: () => _go(HeadunitSection.youtube),
          onTrending: () => _openFeed(null),
          onLive: () => _openFeed(YouTubeSection.categories[1].query),
          onSearch: _openSearch,
          onNavigate: _openNavigation,
        ),
      HeadunitSection.maps => MapsSection(
          playback: _playback,
          location: widget.location,
          navigation: _navigation,
          searchOpen: _navSearchOpen,
          active: active,
          tilesEnabled: widget.tilesEnabled,
          voice: _voice,
          onOpenMusic: () => _go(HeadunitSection.music),
          onGoogleMaps: _openGoogleMaps,
        ),
      HeadunitSection.youtube => YouTubeSection(
          playback: _playback,
          feed: _feed,
          searchController: _searchController,
          searchFocus: _searchFocus,
          onSelect: _select,
          onOpenMusic: () => _go(HeadunitSection.music),
          onOpenFullscreen: () => setState(() => _fullscreen = true),
        ),
      HeadunitSection.music => MusicSection(
          playback: _playback,
          onOpenYouTube: () => _go(HeadunitSection.youtube),
        ),
    };
  }
}

/// Keeps a visited section mounted (state survives switching) while only the active one
/// is laid out on screen, painted, hit-tested and animated; it fades in on activation.
class _SectionSlot extends StatefulWidget {
  const _SectionSlot({required this.active, required this.child});

  final bool active;
  final Widget child;

  @override
  State<_SectionSlot> createState() => _SectionSlotState();
}

class _SectionSlotState extends State<_SectionSlot>
    with SingleTickerProviderStateMixin {
  late final AnimationController _fade = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 160),
    value: widget.active ? 1 : 0,
  );

  @override
  void didUpdateWidget(_SectionSlot oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active && !oldWidget.active) _fade.forward(from: 0);
  }

  @override
  void dispose() {
    _fade.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: Offstage(
        offstage: !widget.active,
        child: TickerMode(
          enabled: widget.active,
          child: FadeTransition(opacity: _fade, child: widget.child),
        ),
      ),
    );
  }
}

class _PipView extends StatelessWidget {
  const _PipView({required this.playback});

  final PlaybackController playback;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: playback,
      builder: (context, _) {
        final controller = playback.videoController;
        final video = playback.current;
        return Scaffold(
          backgroundColor: Colors.black,
          body: Center(
            child: playback.hasVideo && controller != null
                ? AspectRatio(
                    aspectRatio: controller.value.aspectRatio > 0
                        ? controller.value.aspectRatio
                        : 16 / 9,
                    child: VideoPlayer(controller),
                  )
                : Padding(
                    padding: const EdgeInsets.all(8),
                    child: Row(
                      children: [
                        if (video != null)
                          SizedBox.square(
                            dimension: 56,
                            child: Artwork(url: video.thumbnail, radius: 8),
                          )
                        else
                          const Icon(LucideIcons.music2, color: CarTheme.text),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            video?.title ?? 'Audio',
                            maxLines: 3,
                            overflow: TextOverflow.ellipsis,
                            style: CarTheme.caption.copyWith(
                              color: CarTheme.text,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
          ),
        );
      },
    );
  }
}
