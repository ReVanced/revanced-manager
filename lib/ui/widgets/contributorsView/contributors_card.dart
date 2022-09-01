import 'package:flutter/material.dart';
import 'package:github/github.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher.dart';

class ContributorsCard extends StatefulWidget {
  final String title;
  final List<Contributor> contributors;
  final double height;

  const ContributorsCard({
    Key? key,
    required this.title,
    required this.contributors,
    this.height = 200,
  }) : super(key: key);

  @override
  State<ContributorsCard> createState() => _ContributorsCardState();
}

class _ContributorsCardState extends State<ContributorsCard> {
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 4.0, horizontal: 4.0),
          child: Text(
            widget.title,
            style: GoogleFonts.poppins(
              fontSize: 20,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        Container(
          margin: const EdgeInsets.all(8.0),
          padding: const EdgeInsets.all(4.0),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.tertiary,
            borderRadius: BorderRadius.circular(12),
          ),
          height: widget.height,
          child: GridView.builder(
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 7,
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
            ),
            itemCount: widget.contributors.length,
            itemBuilder: (context, index) {
              return ClipRRect(
                borderRadius: BorderRadius.circular(100),
                child: GestureDetector(
                  onTap: () =>
                      launchUrl(Uri.parse(widget.contributors[index].htmlUrl!)),
                  child: Image.network(
                    widget.contributors[index].avatarUrl!,
                    height: 40,
                    width: 40,
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
