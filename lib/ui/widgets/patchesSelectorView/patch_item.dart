import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/theme.dart';

// ignore: must_be_immutable
class PatchItem extends StatefulWidget {
  final String name;
  final String simpleName;
  final String description;
  final String version;
  final String packageVersion;
  final List<String> supportedPackageVersions;
  final bool isUnsupported;
  bool isSelected;
  final Function(bool) onChanged;

  PatchItem({
    Key? key,
    required this.name,
    required this.simpleName,
    required this.description,
    required this.version,
    required this.packageVersion,
    required this.supportedPackageVersions,
    required this.isUnsupported,
    required this.isSelected,
    required this.onChanged,
  }) : super(key: key);

  @override
  State<PatchItem> createState() => _PatchItemState();
}

class _PatchItemState extends State<PatchItem> {
  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        setState(() => widget.isSelected = !widget.isSelected);
        widget.onChanged(widget.isSelected);
      },
      child: Container(
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.primary,
          borderRadius: BorderRadius.circular(12),
        ),
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
        margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Flexible(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(
                            widget.simpleName,
                            style: GoogleFonts.inter(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(width: 4),
                          Text(widget.version)
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        widget.description,
                        softWrap: true,
                        maxLines: 3,
                        overflow: TextOverflow.visible,
                        style: GoogleFonts.roboto(
                          fontSize: 14,
                        ),
                      ),
                    ],
                  ),
                ),
                Transform.scale(
                  scale: 1.2,
                  child: Checkbox(
                    value: widget.isSelected,
                    activeColor: Colors.blueGrey[500],
                    onChanged: (newValue) {
                      setState(() => widget.isSelected = newValue!);
                      widget.onChanged(widget.isSelected);
                    },
                  ),
                )
              ],
            ),
            widget.isUnsupported
                ? Row(
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: TextButton.icon(
                          label: I18nText('patchItem.unsupportedWarningButton'),
                          icon: const Icon(Icons.warning),
                          onPressed: () => _showUnsupportedWarningDialog(),
                          style: ButtonStyle(
                            shape: MaterialStateProperty.all(
                              RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                                side: BorderSide(
                                  width: 1,
                                  color:
                                      Theme.of(context).colorScheme.secondary,
                                ),
                              ),
                            ),
                            backgroundColor: MaterialStateProperty.all(
                              isDark
                                  ? Theme.of(context).colorScheme.background
                                  : Colors.white,
                            ),
                            foregroundColor: MaterialStateProperty.all(
                              Theme.of(context).colorScheme.secondary,
                            ),
                          ),
                        ),
                      ),
                    ],
                  )
                : Container(),
          ],
        ),
      ),
    );
  }

  Future<void> _showUnsupportedWarningDialog() {
    return showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: I18nText('patchItem.alertDialogTitle'),
          content: I18nText(
            'patchItem.alertDialogText',
            translationParams: {
              'packageVersion': widget.packageVersion,
              'supportedVersions':
                  '\u2022 ${widget.supportedPackageVersions.join('\n\u2022 ')}',
            },
          ),
          actions: [
            TextButton(
              child: I18nText('okButton'),
              onPressed: () => Navigator.of(context).pop(),
            )
          ],
        );
      },
    );
  }
}
