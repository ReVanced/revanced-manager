import 'package:flutter/material.dart';
import 'package:skeletons/skeletons.dart';

class AppSkeletonLoader extends StatelessWidget {
  const AppSkeletonLoader({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    return Skeleton(
      isLoading: true,
      skeleton: ListView.builder(
        itemCount: 7,
        itemBuilder: (context, index) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8.0),
          child: SkeletonItem(
            child: Row(
              children: [
                SkeletonAvatar(
                  style: SkeletonAvatarStyle(
                    width: screenWidth * 0.15,
                    height: screenWidth * 0.15,
                    shape: BoxShape.rectangle,
                    borderRadius: const BorderRadius.all(Radius.circular(12)),
                  ),
                ),
                const SizedBox(width: 16),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      color: Colors.white,
                      height: 34,
                      width: screenWidth * 0.4,
                      child: SkeletonParagraph(
                        style: const SkeletonParagraphStyle(
                          lines: 1,
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    Container(
                      margin: const EdgeInsets.only(bottom: 4),
                      color: Colors.white,
                      height: 34,
                      width: screenWidth * 0.6,
                      child: SkeletonParagraph(
                        style: const SkeletonParagraphStyle(
                          lines: 1,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
      child: const Center(
        child: Text("Content"),
      ),
    );
  }
}
