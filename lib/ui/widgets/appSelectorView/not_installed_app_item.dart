import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class NotInstalledAppItem extends StatefulWidget {
  const NotInstalledAppItem({
    super.key,
    required this.name,
    required this.patchesCount,
    required this.suggestedVersion,
    this.onTap,
    this.onLinkTap,
  });

  final String name;
  final int patchesCount;
  final String suggestedVersion;
  final Function()? onTap;
  final Function()? onLinkTap;

  @override
  State<NotInstalledAppItem> createState() => _NotInstalledAppItem();
}

class _NotInstalledAppItem extends State<NotInstalledAppItem> {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: CustomCard(
        onTap: widget.onTap,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Container(
              height: 48,
              padding: const EdgeInsets.symmetric(vertical: 4.0),
              alignment: Alignment.center,
              child: const CircleAvatar(
                backgroundColor: Colors.transparent,
                child: Icon(
                  Icons.square_rounded,
                  color: Colors.grey,
                  size: 44,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    widget.name,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    t.appSelectorCard.notInstalled,
                    style: TextStyle(
                      color: Theme.of(context).textTheme.titleLarge!.color,
                    ),
                  ),
                  Wrap(
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      Material(
                        color: Theme.of(context).colorScheme.secondaryContainer,
                        borderRadius:
                            const BorderRadius.all(Radius.circular(8)),
                        child: InkWell(
                          onTap: widget.onLinkTap,
                          borderRadius:
                              const BorderRadius.all(Radius.circular(8)),
                          child: Container(
                            padding: const EdgeInsets.all(4),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  t.suggested(
                                    version: widget.suggestedVersion.isEmpty
                                        ? t.appSelectorCard.allVersions
                                        : 'v${widget.suggestedVersion}',
                                  ),
                                ),
                                const SizedBox(width: 4),
                                Icon(
                                  Icons.search,
                                  size: 16,
                                  color: Theme.of(context)
                                      .colorScheme
                                      .onSecondaryContainer,
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 4),
                      Text(
                        widget.patchesCount == 1
                            ? '• ${widget.patchesCount} patch'
                            : '• ${widget.patchesCount} patches',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.secondary,
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
  }
}
