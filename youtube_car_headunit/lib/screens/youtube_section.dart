import 'package:flutter/material.dart';

import '../controllers/feed_controller.dart';
import '../controllers/playback_controller.dart';
import '../models/car_video.dart';
import '../widgets/player_widget.dart';
import '../widgets/search_header.dart';
import '../widgets/video_feed.dart';

/// Search, categories, the inline player and the video grid.
class YouTubeSection extends StatelessWidget {
  const YouTubeSection({
    super.key,
    required this.playback,
    required this.feed,
    required this.searchController,
    required this.searchFocus,
    required this.onSelect,
    required this.onOpenMusic,
    required this.onOpenFullscreen,
  });

  static const List<FeedCategory> categories = [
    (label: 'Trending', query: null),
    (label: 'Live', query: 'Live Stream Indonesia'),
    (label: 'Musik', query: 'Musik Indonesia'),
    (label: 'Lagu Anak', query: 'Lagu Anak Indonesia'),
    (label: 'Islami', query: 'Lagu Anak Islami'),
    (label: 'Kajian', query: 'Kajian Islam Ceramah'),
    (label: 'Berita', query: 'Berita Hari Ini'),
  ];

  final PlaybackController playback;
  final FeedController feed;
  final TextEditingController searchController;
  final FocusNode searchFocus;
  final ValueChanged<CarVideo> onSelect;
  final VoidCallback onOpenMusic;
  final VoidCallback onOpenFullscreen;

  void _openCategory(FeedCategory category) {
    searchController.clear();
    searchFocus.unfocus();
    final query = category.query;
    if (query == null) {
      feed.loadTrending();
    } else {
      feed.search(query);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([playback, feed]),
      builder: (context, _) {
        final current = playback.current;
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 6),
              child: SearchHeader(
                controller: searchController,
                focusNode: searchFocus,
                categories: categories,
                activeQuery: feed.isTrending ? '' : feed.query,
                onSearch: (query) {
                  searchFocus.unfocus();
                  feed.search(query);
                },
                onCategory: _openCategory,
                onClear: () {
                  searchController.clear();
                  feed.loadTrending();
                },
              ),
            ),
            Expanded(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (current != null)
                    SizedBox(
                      width: 300,
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.fromLTRB(16, 4, 8, 12),
                        child: PlayerWidget(
                          playback: playback,
                          onOpenMusic: onOpenMusic,
                          onOpenFullscreen: onOpenFullscreen,
                        ),
                      ),
                    ),
                  Expanded(
                    child: VideoFeed(
                      feed: feed,
                      selectedId: current?.id,
                      onSelect: onSelect,
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}
