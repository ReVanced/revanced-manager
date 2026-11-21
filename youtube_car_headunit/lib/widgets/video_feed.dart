import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../controllers/feed_controller.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';
import 'video_card.dart';

/// The video grid with its loading, error and empty states. Scrolling near the end loads
/// the next page.
class VideoFeed extends StatelessWidget {
  const VideoFeed({
    super.key,
    required this.feed,
    required this.selectedId,
    required this.onSelect,
  });

  final FeedController feed;
  final String? selectedId;
  final ValueChanged<CarVideo> onSelect;

  @override
  Widget build(BuildContext context) {
    if (feed.isLoading) {
      return const Center(
        child: SpinKitThreeBounce(color: CarTheme.accent, size: 22),
      );
    }
    if (feed.error != null) {
      return _Message(
        icon: LucideIcons.alertTriangle,
        title: feed.error!,
        actionLabel: 'Coba lagi',
        onAction: feed.retry,
      );
    }
    if (feed.videos.isEmpty) {
      return _Message(
        icon: LucideIcons.searchX,
        title: 'Tidak ada hasil',
        actionLabel: 'Muat Trending',
        onAction: feed.loadTrending,
      );
    }
    final videos = feed.videos;
    final showFooter =
        feed.hasMore || feed.isLoadingMore || feed.loadMoreError != null;
    return NotificationListener<ScrollNotification>(
      onNotification: (notification) {
        // Fetch the next page well before the end, so it is usually there in time.
        final metrics = notification.metrics;
        if (metrics.extentAfter < metrics.viewportDimension * 1.5) feed.loadMore();
        return false;
      },
      child: CustomScrollView(
        cacheExtent: 200,
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(8, 4, 8, 8),
            sliver: SliverLayoutBuilder(
              builder: (context, constraints) => SliverGrid(
                gridDelegate:
                    _gridDelegate(context, constraints.crossAxisExtent),
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    final video = videos[index];
                    return VideoCard(
                      key: ValueKey(video.id),
                      video: video,
                      isSelected: selectedId == video.id,
                      onTap: () => onSelect(video),
                    );
                  },
                  childCount: videos.length,
                  addAutomaticKeepAlives: false,
                ),
              ),
            ),
          ),
          if (showFooter)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(8, 0, 8, 16),
                child: Center(child: _footer()),
              ),
            ),
        ],
      ),
    );
  }

  /// Columns of at most ~230 dp. Each tile is exactly as tall as its content: a 16:9
  /// thumbnail plus two title lines and the channel line (scaled with the system font),
  /// so cards never overflow or leave gaps whatever the column width.
  static SliverGridDelegate _gridDelegate(BuildContext context, double width) {
    const spacing = 8.0;
    final columns = ((width + spacing) / (230 + spacing)).ceil().clamp(1, 6);
    final tileWidth = (width - spacing * (columns - 1)) / columns;
    final text = MediaQuery.textScalerOf(context);
    final textHeight = text.scale(14) * 1.25 * 2 + text.scale(12) * 1.3 + 4;
    return SliverGridDelegateWithFixedCrossAxisCount(
      crossAxisCount: columns,
      crossAxisSpacing: spacing,
      mainAxisSpacing: 6,
      // Card padding (4 + 4), thumbnail, gap under it, then the text.
      mainAxisExtent: 8 + (tileWidth - 8) * 9 / 16 + 8 + textHeight + 2,
    );
  }

  Widget _footer() {
    final error = feed.loadMoreError;
    if (error != null) {
      return TextButton.icon(
        onPressed: feed.retryLoadMore,
        icon: const Icon(LucideIcons.rotateCcw, size: 16),
        label: Text('$error Coba lagi'),
      );
    }
    if (feed.isLoadingMore) {
      return const SpinKitThreeBounce(color: CarTheme.accent, size: 18);
    }
    // A short first page may not scroll at all; this keeps paging reachable.
    return TextButton(
      onPressed: feed.loadMore,
      child: const Text('Muat lebih banyak'),
    );
  }
}

class _Message extends StatelessWidget {
  const _Message({
    required this.icon,
    required this.title,
    required this.actionLabel,
    required this.onAction,
  });

  final IconData icon;
  final String title;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: CarTheme.textFaint, size: 34),
            const SizedBox(height: 12),
            Text(title, textAlign: TextAlign.center, style: CarTheme.body),
            const SizedBox(height: 14),
            FilledButton(onPressed: onAction, child: Text(actionLabel)),
          ],
        ),
      ),
    );
  }
}
