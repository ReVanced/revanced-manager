import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

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
          ListTile(
            horizontalTitleGap: 12.0,
            leading: const Image(
              image: AssetImage("lib/assets/images/reddit.png"),
              height: 39,
              width: 39,
            ),
            title: Text(
              "ReVanced",
              style: GoogleFonts.roboto(
                color: const Color(0xff7792BA),
              ),
            ),
            subtitle: const Text("Released 2 days ago"),
            trailing: TextButton(
              onPressed: () {},
              style: TextButton.styleFrom(
                side: const BorderSide(
                  color: Color(0xff7792BA),
                  width: 1,
                ),
                primary: Colors.white,
                padding: const EdgeInsets.symmetric(
                  vertical: 10,
                  horizontal: 24,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(24),
                ),
              ),
              child: const Text("Patch"),
            ),
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
