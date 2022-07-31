import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(
            vertical: 0.0,
            horizontal: 24.0,
          ),
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    "ReVanced Manager",
                    style: GoogleFonts.manrope(
                      fontSize: 24,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  IconButton(
                    onPressed: () {},
                    icon: const Icon(
                      Icons.more_vert,
                    ),
                  )
                ],
              ),
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.topLeft,
                child: Text(
                  "Dashboard",
                  style: GoogleFonts.lato(
                    fontSize: 32,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.topLeft,
                child: Text(
                  "ReVanced Updates",
                  style: GoogleFonts.lato(
                    fontSize: 22,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
