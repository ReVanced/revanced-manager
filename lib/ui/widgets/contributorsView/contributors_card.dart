import 'package:flutter/material.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:url_launcher/url_launcher.dart';

class ContributorsCard extends StatefulWidget {
  final String title;
  final List<dynamic> contributors;
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
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.only(bottom: 8.0),
            child: Text(
              widget.title,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          CustomCard(
            child: GridView.builder(
              shrinkWrap: true,
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
                    onTap: () => launchUrl(
                        Uri.parse(widget.contributors[index]['html_url'])),
                    child: Image.network(
                      widget.contributors[index]['avatar_url'],
                      height: 40,
                      width: 40,
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
