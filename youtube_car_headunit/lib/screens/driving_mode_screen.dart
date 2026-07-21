import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';
import '../models/car_video.dart';
import '../theme/car_theme.dart';

class DrivingModeScreen extends StatelessWidget {
  final CarVideo video;
  final VoidCallback onNext;
  final VoidCallback onPrevious;
  final VoidCallback onClose;

  const DrivingModeScreen({
    super.key,
    required this.video,
    required this.onNext,
    required this.onPrevious,
    required this.onClose,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: CarTheme.bgPrimary,
      body: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onHorizontalDragEnd: (details) {
          if (details.primaryVelocity != null) {
            if (details.primaryVelocity! < -200) {
              // Swiped Left -> Next Track
              onNext();
              ScaffoldMessenger.of(context).hideCurrentSnackBar();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('▶️ Next Track'), duration: Duration(milliseconds: 800)),
              );
            } else if (details.primaryVelocity! > 200) {
              // Swiped Right -> Previous Track
              onPrevious();
              ScaffoldMessenger.of(context).hideCurrentSnackBar();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('◀️ Previous Track'), duration: Duration(milliseconds: 800)),
              );
            }
          }
        },
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          decoration: const BoxDecoration(
            gradient: RadialGradient(
              colors: [Color(0xFF182030), Color(0xFF07090E)],
              radius: 1.2,
            ),
          ),
          child: Column(
            children: [
              // Top Bar
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                        decoration: BoxDecoration(
                          color: CarTheme.accentRed,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: const Text('MODE BERKENDARA', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.white)),
                      ),
                      const SizedBox(width: 12),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: CarTheme.accentGreen.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: CarTheme.accentGreen),
                        ),
                        child: const Row(
                          children: [
                            Icon(LucideIcons.shieldCheck, color: CarTheme.accentGreen, size: 14),
                            SizedBox(width: 6),
                            Text('ADBLOCK ACTIVE', style: TextStyle(color: CarTheme.accentGreen, fontWeight: FontWeight.bold, fontSize: 12)),
                          ],
                        ),
                      ),
                    ],
                  ),
                  ElevatedButton.icon(
                    onPressed: onClose,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: CarTheme.bgCard,
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    icon: const Icon(LucideIcons.minimize2, size: 18),
                    label: const Text('Keluar Mode Setir', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                  ),
                ],
              ),

              // Gesture Hint Indicator for Driver
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(LucideIcons.hand, size: 14, color: CarTheme.textMuted),
                      SizedBox(width: 6),
                      Text(
                        'Geser Layar 👈 Kiri / Kanan 👉 untuk Skip Lagu Tanpa Melihat Layar',
                        style: TextStyle(fontSize: 11, color: CarTheme.textMuted, fontWeight: FontWeight.w500),
                      ),
                    ],
                  ),
                ),
              ),

              // Center Content (Responsive Artwork & Title)
              Expanded(
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // Artwork (Responsive Aspect Ratio Box)
                    Flexible(
                      flex: 4,
                      child: AspectRatio(
                        aspectRatio: 1,
                        child: Container(
                          constraints: const BoxConstraints(maxHeight: 180, maxWidth: 180),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(24),
                            border: Border.all(color: Colors.white24, width: 2),
                            boxShadow: const [
                              BoxShadow(color: Colors.black54, blurRadius: 30, spreadRadius: 5),
                            ],
                          ),
                          clipBehavior: Clip.antiAlias,
                          child: Image.network(
                            video.thumbnail,
                            fit: BoxFit.cover,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 24),
                    // Title Details
                    Expanded(
                      flex: 6,
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            video.title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                              height: 1.2,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            video.uploader,
                            style: const TextStyle(
                              fontSize: 16,
                              color: CarTheme.textMuted,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),

              // Giant Bottom Controls for Steering / Driver
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    iconSize: 52,
                    icon: const Icon(LucideIcons.skipBack, color: Colors.white),
                    onPressed: onPrevious,
                  ),
                  const SizedBox(width: 36),
                  CircleAvatar(
                    radius: 38,
                    backgroundColor: CarTheme.accentRed,
                    child: IconButton(
                      iconSize: 44,
                      icon: const Icon(LucideIcons.pause, color: Colors.white),
                      onPressed: () {},
                    ),
                  ),
                  const SizedBox(width: 36),
                  IconButton(
                    iconSize: 52,
                    icon: const Icon(LucideIcons.skipForward, color: Colors.white),
                    onPressed: onNext,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
