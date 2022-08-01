import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager_flutter/ui/widgets/app_details.dart';

class InstalledAppsWidget extends StatelessWidget {
  const InstalledAppsWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: const Color(0xff1B222B),
      ),
      padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Text(
            "Total Installed(3)",
            style: GoogleFonts.inter(
              fontSize: 16,
              color: const Color(0xff7792BA),
              fontWeight: FontWeight.w500,
            ),
          ),
          AppDetails(
            asset: "lib/assets/images/revanced.svg",
            name: "ReVanced",
            releaseDate: "2 days ago",
            onPressed: () {},
          ),
          Text(
            "Changelog",
            style: GoogleFonts.roboto(
              color: const Color(0xff8691A0),
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            "fix: we made the player even worse (you love)",
            style: GoogleFonts.roboto(
              color: const Color(0xff8691A0),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            "chore: guhhughghu",
            style: GoogleFonts.roboto(
              color: const Color(0xff8691A0),
            ),
          ),
        ],
      ),
    );
  }
}
