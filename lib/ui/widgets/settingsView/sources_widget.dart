import 'package:expandable/expandable.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/ui/widgets/settingsView/custom_text_field.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class SourcesWidget extends StatelessWidget {
  final String title;
  final TextEditingController organizationController;
  final TextEditingController patchesSourceController;
  final TextEditingController integrationsSourceController;

  const SourcesWidget({
    Key? key,
    required this.title,
    required this.organizationController,
    required this.patchesSourceController,
    required this.integrationsSourceController,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ExpandablePanel(
      theme: ExpandableThemeData(
        hasIcon: true,
        iconColor: Theme.of(context).iconTheme.color,
        iconPadding: const EdgeInsets.symmetric(vertical: 16.0),
        animationDuration: const Duration(milliseconds: 400),
      ),
      header: ListTile(
        contentPadding: EdgeInsets.zero,
        title: I18nText(
          'sourcesCard.widgetTitle',
          child: const Text(
            '',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        subtitle: I18nText('sourcesCard.widgetSubtitle'),
      ),
      expanded: CustomCard(
        child: Column(
          children: <Widget>[
            CustomTextField(
              inputController: organizationController,
              label: I18nText('sourcesCard.organizationLabel'),
              hint: ghOrg,
              onChanged: (value) => ghOrg = value,
            ),
            const SizedBox(height: 8),
            CustomTextField(
              inputController: patchesSourceController,
              label: I18nText('sourcesCard.patchesSourceLabel'),
              hint: patchesRepo,
              onChanged: (value) => patchesRepo = value,
            ),
            const SizedBox(height: 8),
            CustomTextField(
              inputController: integrationsSourceController,
              label: I18nText('sourcesCard.integrationsSourceLabel'),
              hint: integrationsRepo,
              onChanged: (value) => integrationsRepo = value,
            ),
          ],
        ),
      ),
      collapsed: Container(),
    );
  }
}
