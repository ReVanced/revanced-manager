import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';

class LatestCommitCard extends StatefulWidget {
  const LatestCommitCard({
    Key? key,
    required this.model,
    required this.parentContext,
  }) : super(key: key);
  final HomeViewModel model;
  final BuildContext parentContext;

  @override
  State<LatestCommitCard> createState() => _LatestCommitCardState();
}

class _LatestCommitCardState extends State<LatestCommitCard> {
  final HomeViewModel model = locator<HomeViewModel>();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // ReVanced Manager
        CustomCard(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    const Text('ReVanced Manager'),
                    const SizedBox(height: 4),
                    Row(
                      children: <Widget>[
                        FutureBuilder<String?>(
                          future: model.getLatestManagerReleaseTime(),
                          builder: (context, snapshot) => snapshot.hasData &&
                                  snapshot.data!.isNotEmpty
                              ? I18nText(
                                  'latestCommitCard.timeagoLabel',
                                  translationParams: {'time': snapshot.data!},
                                )
                              : I18nText('latestCommitCard.loadingLabel'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              FutureBuilder<bool>(
                future: model.hasManagerUpdates(),
                initialData: false,
                builder: (context, snapshot) => Opacity(
                  opacity: snapshot.hasData && snapshot.data! ? 1.0 : 0.25,
                  child: CustomMaterialButton(
                    label: I18nText('updateButton'),
                    onPressed: snapshot.hasData && snapshot.data!
                        ? () => widget.model.showUpdateConfirmationDialog(
                              widget.parentContext,
                              false,
                            )
                        : () => {},
                  ),
                ),
              ),
            ],
          ),
        ),

        const SizedBox(height: 16),

        // Patches
        CustomCard(
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    const Text('Patches'),
                    const SizedBox(height: 4),
                    Row(
                      children: <Widget>[
                        FutureBuilder<String?>(
                          future: model.getLatestPatchesReleaseTime(),
                          builder: (context, snapshot) => Text(
                            snapshot.hasData && snapshot.data!.isNotEmpty
                                ? FlutterI18n.translate(
                                    context,
                                    'latestCommitCard.timeagoLabel',
                                    translationParams: {'time': snapshot.data!},
                                  )
                                : FlutterI18n.translate(
                                    context,
                                    'latestCommitCard.loadingLabel',
                                  ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              FutureBuilder<bool>(
                future: locator<HomeViewModel>().hasPatchesUpdates(),
                initialData: false,
                builder: (context, snapshot) => Opacity(
                  opacity: snapshot.hasData && snapshot.data! ? 1.0 : 0.25,
                  child: CustomMaterialButton(
                    label: I18nText('updateButton'),
                    onPressed: snapshot.hasData && snapshot.data!
                        ? () => widget.model.showUpdateConfirmationDialog(
                              widget.parentContext,
                              true,
                            )
                        : () => {},
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
