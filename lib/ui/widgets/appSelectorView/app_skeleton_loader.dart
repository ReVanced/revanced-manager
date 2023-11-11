import 'package:flutter/material.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:skeletons/skeletons.dart';

class AppSkeletonLoader extends StatelessWidget {
  const AppSkeletonLoader({super.key});

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.sizeOf(context).width;
    return ListView.builder(
      shrinkWrap: true,
      itemCount: 7,
      padding: EdgeInsets.zero,
      itemBuilder: (context, index) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12.0),
        child: CustomCard(
          child: Row(
            children: [
              SkeletonAvatar(
                style: SkeletonAvatarStyle(
                  width: screenWidth * 0.10,
                  height: screenWidth * 0.10,
                  borderRadius: const BorderRadius.all(Radius.circular(12)),
                ),
              ),
              const SizedBox(width: 16),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  SizedBox(
                    width: screenWidth * 0.4,
                    child: SkeletonLine(
                      style: SkeletonLineStyle(
                        height: 20,
                        width: screenWidth * 0.4,
                        borderRadius:
                            const BorderRadius.all(Radius.circular(10)),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: screenWidth * 0.6,
                    child: SkeletonLine(
                      style: SkeletonLineStyle(
                        height: 15,
                        width: screenWidth * 0.6,
                        borderRadius:
                            const BorderRadius.all(Radius.circular(10)),
                      ),
                    ),
                  ),
                  const SizedBox(height: 5),
                  SizedBox(
                    width: screenWidth * 0.5,
                    child: SkeletonLine(
                      style: SkeletonLineStyle(
                        height: 15,
                        width: screenWidth * 0.5,
                        borderRadius:
                            const BorderRadius.all(Radius.circular(10)),
                      ),
                    ),
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
