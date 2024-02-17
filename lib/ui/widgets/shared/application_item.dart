import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:timeago/timeago.dart';

class ApplicationItem extends StatefulWidget {
  const ApplicationItem({
    super.key,
    required this.icon,
    required this.name,
    required this.patchDate,
    required this.onPressed,
  });
  final Uint8List icon;
  final String name;
  final DateTime patchDate;
  final Function() onPressed;

  @override
  State<ApplicationItem> createState() => _ApplicationItemState();
}

class _ApplicationItemState extends State<ApplicationItem> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16.0),
      child: CustomCard(
        onTap: widget.onPressed,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(
              child: Row(
                children: [
                  SizedBox(
                    width: 40,
                    child: Image.memory(widget.icon, height: 40, width: 40),
                  ),
                  const SizedBox(width: 19),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          widget.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        Text(
                          format(widget.patchDate),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Row(
              children: [
                const SizedBox(width: 8),
                Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: <Widget>[
                    FilledButton(
                      onPressed: widget.onPressed,
                      child: Text(t.applicationItem.infoButton),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
