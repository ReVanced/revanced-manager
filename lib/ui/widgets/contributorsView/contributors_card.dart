import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/file.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:url_launcher/url_launcher.dart';

class ContributorsCard extends StatefulWidget {
  final String title;
  final List<dynamic> contributors;

  const ContributorsCard({
    Key? key,
    required this.title,
    required this.contributors,
  }) : super(key: key);

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
          child: I18nText(
            widget.title,
            child: const Text(
              '',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w600,
              ),
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
                    widget.contributors[index]['html_url'],
                  ),
                ),
                child: FutureBuilder<File?>(
                  future: DefaultCacheManager().getSingleFile(
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
