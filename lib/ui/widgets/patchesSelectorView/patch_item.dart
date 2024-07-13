import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/services/toast.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_checkbox.dart';
import 'package:revanced_manager/ui/widgets/shared/haptics/haptic_custom_card.dart';

// ignore: must_be_immutable
class PatchItem extends StatefulWidget {
  PatchItem({
    super.key,
    required this.name,
    required this.simpleName,
    required this.description,
    required this.packageVersion,
    required this.supportedPackageVersions,
    required this.isUnsupported,
    required this.hasUnsupportedPatchOption,
    required this.options,
    required this.isSelected,
    required this.onChanged,
    required this.navigateToOptions,
    required this.isChangeEnabled,
  });
  final String name;
  final String simpleName;
  final String description;
  final String packageVersion;
  final List<String> supportedPackageVersions;
  final bool isUnsupported;
  final bool hasUnsupportedPatchOption;
  final List<Option> options;
  bool isSelected;
  final Function(bool) onChanged;
  final void Function(List<Option>) navigateToOptions;
  final bool isChangeEnabled;
  final toast = locator<Toast>();
  final _managerAPI = locator<ManagerAPI>();

  @override
  State<PatchItem> createState() => _PatchItemState();
}

class _PatchItemState extends State<PatchItem> {
  @override
  Widget build(BuildContext context) {
    widget.isSelected = widget.isSelected &&
        (!widget.isUnsupported ||
            !widget._managerAPI.isVersionCompatibilityCheckEnabled()) &&
        !widget.hasUnsupportedPatchOption;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Opacity(
        opacity: widget.isUnsupported &&
                widget._managerAPI.isVersionCompatibilityCheckEnabled() == true
            ? 0.5
            : 1,
        child: HapticCustomCard(
          padding: EdgeInsets.only(
            top: 12,
            bottom: 16,
            left: 8.0,
            right: widget.options.isNotEmpty ? 4.0 : 8.0,
          ),
          onTap: () {
            if (widget.isUnsupported &&
                widget._managerAPI.isVersionCompatibilityCheckEnabled()) {
              widget.isSelected = false;
              widget.toast.showBottom(t.patchItem.unsupportedPatchVersion);
            } else if (widget.isChangeEnabled) {
              if (!widget.isSelected) {
                if (widget.hasUnsupportedPatchOption) {
                  _showUnsupportedRequiredOptionDialog();
                  return;
                }
              }
              widget.isSelected = !widget.isSelected;
              setState(() {});
            }
            if (!widget.isUnsupported ||
                !widget._managerAPI.isVersionCompatibilityCheckEnabled()) {
              widget.onChanged(widget.isSelected);
            }
          },
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Transform.scale(
                scale: 1.2,
                child: HapticCheckbox(
                  value: widget.isSelected,
                  activeColor: Theme.of(context).colorScheme.primary,
                  checkColor: Theme.of(context).colorScheme.secondaryContainer,
                  side: BorderSide(
                    width: 2.0,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                  onChanged: (newValue) {
                    if (widget.isUnsupported &&
                        widget._managerAPI
                            .isVersionCompatibilityCheckEnabled()) {
                      widget.isSelected = false;
                      widget.toast.showBottom(
                        t.patchItem.unsupportedPatchVersion,
                      );
                    } else if (widget.isChangeEnabled) {
                      if (!widget.isSelected) {
                        if (widget.hasUnsupportedPatchOption) {
                          _showUnsupportedRequiredOptionDialog();
                          return;
                        }
                      }
                      widget.isSelected = newValue!;
                      setState(() {});
                    }
                    if (!widget.isUnsupported ||
                        !widget._managerAPI
                            .isVersionCompatibilityCheckEnabled()) {
                      widget.onChanged(widget.isSelected);
                    }
                  },
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(top: 10),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.simpleName,
                        maxLines: 2,
                        overflow: TextOverflow.visible,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      Text(
                        widget.description,
                        softWrap: true,
                        overflow: TextOverflow.visible,
                        style: TextStyle(
                          fontSize: 14,
                          color: Theme.of(context)
                              .colorScheme
                              .onSecondaryContainer,
                        ),
                      ),
                      if (widget.description.isNotEmpty)
                        Align(
                          alignment: Alignment.topLeft,
                          child: Wrap(
                            spacing: 8,
                            runSpacing: 4,
                            children: [
                              if (widget.isUnsupported &&
                                  !widget._managerAPI
                                      .isVersionCompatibilityCheckEnabled())
                                Padding(
                                  padding: const EdgeInsets.only(top: 8),
                                  child: TextButton.icon(
                                    label: Text(t.warning),
                                    icon: const Icon(
                                      Icons.warning_amber_outlined,
                                      size: 20.0,
                                    ),
                                    onPressed: () =>
                                        _showUnsupportedWarningDialog(),
                                    style: ButtonStyle(
                                      shape: WidgetStateProperty.all(
                                        RoundedRectangleBorder(
                                          borderRadius:
                                              BorderRadius.circular(8),
                                          side: BorderSide(
                                            color: Theme.of(context)
                                                .colorScheme
                                                .secondary,
                                          ),
                                        ),
                                      ),
                                      backgroundColor: WidgetStateProperty.all(
                                        Colors.transparent,
                                      ),
                                      foregroundColor: WidgetStateProperty.all(
                                        Theme.of(context).colorScheme.secondary,
                                      ),
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                    ],
                  ),
                ),
              ),
              if (widget.options.isNotEmpty)
                IconButton(
                  icon: const Icon(Icons.settings_outlined),
                  onPressed: () => widget.navigateToOptions(widget.options),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showUnsupportedWarningDialog() {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.warning),
        content: Text(
          t.patchItem.unsupportedDialogText(
            packageVersion: widget.packageVersion,
            supportedVersions:
                '• ${widget.supportedPackageVersions.reversed.join('\n• ')}',
          ),
        ),
        actions: <Widget>[
          FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }

  Future<void> _showUnsupportedRequiredOptionDialog() {
    return showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(t.notice),
        content: Text(
          t.patchItem.unsupportedRequiredOption,
        ),
        actions: <Widget>[
          FilledButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(t.okButton),
          ),
        ],
      ),
    );
  }
}
