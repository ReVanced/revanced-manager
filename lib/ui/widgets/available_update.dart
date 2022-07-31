import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:google_fonts/google_fonts.dart';

class AvailableUpdatesWidget extends StatelessWidget {
  const AvailableUpdatesWidget({Key? key}) : super(key: key);

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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                "Updates Available(2)",
                style: GoogleFonts.inter(
                  fontSize: 16,
                  color: const Color(0xff7792BA),
                  fontWeight: FontWeight.w500,
                ),
              ),
              TextButton(
                onPressed: () {},
                style: TextButton.styleFrom(
                  primary: Colors.white,
                  backgroundColor: const Color(0xff7792BA),
                  padding: const EdgeInsets.symmetric(
                    vertical: 8,
                    horizontal: 18,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                ),
                child: const Text("Patch all"),
              )
            ],
          ),
          ListTile(
            horizontalTitleGap: 12.0,
            leading: SvgPicture.asset(
              "lib/assets/images/revanced.svg",
              height: 26,
              width: 26,
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
          ListTile(
            horizontalTitleGap: 12.0,
            leading: const Image(
              image: AssetImage("lib/assets/images/reddit.png"),
              height: 39,
              width: 39,
            ),
            title: Text(
              "ReReddit",
              style: GoogleFonts.roboto(
                color: const Color(0xff7792BA),
              ),
            ),
            subtitle: const Text("Released 1 month ago"),
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
          const SizedBox(height: 4),
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
