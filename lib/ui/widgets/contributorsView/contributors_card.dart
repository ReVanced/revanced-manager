import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/file.dart';
import 'package:revanced_manager/services/download_manager.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:url_launcher/url_launcher.dart';

class ContributorsCard extends StatefulWidget {
  const ContributorsCard({
    super.key,
    required this.title,
    required this.contributors,
  });
  final String title;
  final List<dynamic> contributors;

  @override
  State<ContributorsCard> createState() => _ContributorsCardState();
}

class _ContributorsCardState extends State<ContributorsCard> {
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(bottom: 8.0),
          child: Text(
            widget.title,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        CustomCard(
          child: GridView.builder(
            shrinkWrap: true,
            padding: EdgeInsets.zero,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 6,
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
            ),
            itemCount: widget.contributors.length,
            itemBuilder: (context, index) => ClipRRect(
              borderRadius: BorderRadius.circular(100),
              child: GestureDetector(
                onTap: () => launchUrl(
                  Uri.parse(
                    widget.contributors[index]['url'],
                  ),
                  mode: LaunchMode.externalApplication,
                ),
                child: FutureBuilder<File?>(
                  future: DownloadManager().getSingleFile(
                    widget.contributors[index]['avatar_url'],
                  ),
                  builder: (context, snapshot) => snapshot.hasData
                      ? Image.file(snapshot.data!)
                      : Image.network(
                          widget.contributors[index]['avatar_url'],
                        ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
