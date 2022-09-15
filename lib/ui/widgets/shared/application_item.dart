import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:expandable/expandable.dart';
import 'package:timeago/timeago.dart';

class ApplicationItem extends StatefulWidget {
  final Uint8List icon;
  final String name;
  final DateTime patchDate;
  final List<String> changelog;
  final bool isUpdatableApp;
  final Function() onPressed;

  const ApplicationItem({
    Key? key,
    required this.icon,
    required this.name,
    required this.patchDate,
    required this.changelog,
    required this.isUpdatableApp,
    required this.onPressed,
  }) : super(key: key);

  @override
  State<ApplicationItem> createState() => _ApplicationItemState();
}

class _ApplicationItemState extends State<ApplicationItem>
    with TickerProviderStateMixin {
  late AnimationController _animationController;

  @override
  initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ExpandableController expController = ExpandableController();
    return ExpandablePanel(
      controller: expController,
      theme: const ExpandableThemeData(
        inkWellBorderRadius: BorderRadius.all(Radius.circular(16)),
        tapBodyToCollapse: false,
        tapBodyToExpand: false,
        tapHeaderToExpand: false,
        hasIcon: false,
        animationDuration: Duration(milliseconds: 450),
      ),
      header: CustomCard(
        child: Row(
          children: <Widget>[
            SizedBox(
              width: 60,
              child: Image.memory(widget.icon, height: 39, width: 39),
            ),
            const SizedBox(width: 4),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  widget.name,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Text(format(widget.patchDate)),
              ],
            ),
            const Spacer(),
            RotationTransition(
              turns: Tween(begin: 0.0, end: 0.50).animate(_animationController),
              child: IconButton(
                onPressed: () {
                  expController.toggle();
                  _animationController.isCompleted
                      ? _animationController.reverse()
                      : _animationController.forward();
                },
                icon: const Icon(Icons.arrow_drop_down),
              ),
            ),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.end,
              children: <Widget>[
                CustomMaterialButton(
                  label: widget.isUpdatableApp
                      ? I18nText('applicationItem.patchButton')
                      : I18nText('applicationItem.infoButton'),
                  onPressed: widget.onPressed,
                ),
              ],
            ),
          ],
        ),
      ),
      collapsed: const Text(''),
      expanded: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            I18nText(
              'applicationItem.changelogLabel',
              child: const Text(
                '',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
            ),
            const SizedBox(height: 4),
            Text('\u2022 ${widget.changelog.join('\n\u2022 ')}'),
          ],
        ),
      ),
    );
  }
}
