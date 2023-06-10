import 'package:flutter/material.dart';
import 'package:revanced_manager/services/google_play_api.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class NotInstalledAppItem extends StatefulWidget {
  const NotInstalledAppItem({
    Key? key,
    required this.name,
    required this.patchesCount,
    required this.suggestedVersion,
    this.onTap,
  }) : super(key: key);
  final String name;
  final int patchesCount;
  final String suggestedVersion;
  final Function()? onTap;

  @override
  State<NotInstalledAppItem> createState() => _NotInstalledAppItem();
}

class _NotInstalledAppItem extends State<NotInstalledAppItem> {
  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: getPackageInfo(widget.name),
      builder: (context, snapshot) {
        if(snapshot.hasData) {
          final Map<String, dynamic> data = snapshot.data!;
          final String pkgName = widget.name;
          final String iconUrl = data['image'];
          final String name = data['name'];
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 4.0),
            child: CustomCard(
              onTap: widget.onTap,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Container(
                    width: 48,
                    height: 48,
                    padding: const EdgeInsets.symmetric(vertical: 4.0),
                    alignment: Alignment.center,
                    child: ClipRRect(
                        borderRadius:BorderRadius.circular(20),
                        child: Image.network(iconUrl),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          name,
                          maxLines: 2,
                          overflow: TextOverflow.visible,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        const SizedBox(height: 4),
                        const Text('App not installed'),
                        Text(pkgName),
                        Row(
                          children: [
                            Text(
                              widget.suggestedVersion.isEmpty
                                  ? 'All versions'
                                  : widget.suggestedVersion,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              widget.patchesCount == 1
                                  ? '• ${widget.patchesCount} patch'
                                  : '• ${widget.patchesCount} patches',
                              style: TextStyle(
                                color: Theme
                                    .of(context)
                                    .colorScheme
                                    .secondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        }else{
          return const SizedBox();
        }},
    );
  }
}
