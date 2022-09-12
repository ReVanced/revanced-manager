import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class LatestCommitCard extends StatefulWidget {
  final Function() onPressed;

  const LatestCommitCard({
    Key? key,
    required this.onPressed,
  }) : super(key: key);

  @override
  State<LatestCommitCard> createState() => _LatestCommitCardState();
}

class _LatestCommitCardState extends State<LatestCommitCard> {
  final HomeViewModel model = locator<HomeViewModel>();

  @override
  Widget build(BuildContext context) {
    return CustomCard(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                children: <Widget>[
                  I18nText('latestCommitCard.patcherLabel'),
                  FutureBuilder<String?>(
                    future: model.getLatestPatcherReleaseTime(),
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
              const SizedBox(height: 8),
              Row(
                children: <Widget>[
                  I18nText('latestCommitCard.managerLabel'),
                  FutureBuilder<String?>(
                    future: model.getLatestManagerReleaseTime(),
                    builder: (context, snapshot) =>
                        snapshot.hasData && snapshot.data!.isNotEmpty
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
          FutureBuilder<bool>(
            future: locator<HomeViewModel>().hasManagerUpdates(),
            initialData: false,
            builder: (context, snapshot) => Opacity(
              opacity: snapshot.hasData && snapshot.data! ? 1.0 : 0.5,
              child: CustomMaterialButton(
                isExpanded: false,
                label: I18nText('latestCommitCard.updateButton'),
                onPressed: snapshot.hasData && snapshot.data!
                    ? widget.onPressed
                    : () => {},
              ),
            ),
          ),
        ],
      ),
    );
  }
}
