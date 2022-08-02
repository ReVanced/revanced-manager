import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager_flutter/ui/widgets/available_updates_card.dart';
import 'package:revanced_manager_flutter/ui/widgets/installed_apps_card.dart';
import 'package:revanced_manager_flutter/ui/widgets/latest_commit_card.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.symmetric(
              vertical: 0.0,
              horizontal: 20.0,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Align(
                  alignment: Alignment.topRight,
                  child: IconButton(
                    onPressed: () {},
                    icon: const Icon(
                      Icons.more_vert,
                    ),
                  ),
                ),
                const SizedBox(height: 60),
                Text(
                  "Dashboard",
                  style: GoogleFonts.inter(
                    fontSize: 28,
                  ),
                ),
                const SizedBox(height: 23),
                Text(
                  "ReVanced Updates",
                  style: GoogleFonts.inter(
                    fontSize: 18,
                  ),
                ),
                const SizedBox(height: 10),
                const LatestCommitCard(),
                const SizedBox(height: 14),
                Text(
                  "Patched Applications",
                  style: GoogleFonts.inter(
                    fontSize: 18,
                  ),
                ),
                const SizedBox(height: 14),
                const AvailableUpdatesCard(),
                const SizedBox(height: 15),
                const InstalledAppsCard(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
