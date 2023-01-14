import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class VancedMicroGCard extends ViewModelWidget<HomeViewModel> {
  const VancedMicroGCard({
    super.key,
    required this.onPressed,
  });

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context, HomeViewModel viewModel) {
    return CustomCard(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              I18nText('vancedMicroGCard.name'),
              const SizedBox(height: 4),
              FutureBuilder<String?>(
                future: viewModel.getLatestMicroGVersion(),
                builder: (context, snapshot) =>
                    snapshot.hasData && snapshot.data!.isNotEmpty
                        ? I18nText(snapshot.data ?? '')
                        : I18nText('vancedMicroGCard.loadingLabel'),
              ),
            ],
          ),
          Opacity(
            opacity: viewModel.hasUpdateOrInstallMicroG ? 1.0 : 0.25,
            child: CustomMaterialButton(
              isExpanded: false,
              label: I18nText(viewModel.vancedMicroGButton),
              onPressed:
                  viewModel.hasUpdateOrInstallMicroG ? onPressed : () => {},
            ),
          ),
          // FutureBuilder<bool>(
          //   future: viewModel.hasUpdateOrInstallMicroG,
          //   initialData: false,
          //   builder: (context, snapshot) => Opacity(
          //     opacity: snapshot.hasData && snapshot.data! ? 1.0 : 0.25,
          //     child: CustomMaterialButton(
          //       isExpanded: false,
          //       label: I18nText(viewModel.vancedMicroGButton),
          //       onPressed:
          //           snapshot.hasData && snapshot.data! ? onPressed : () => {},
          //     ),
          //   ),
          // ),
        ],
      ),
    );
  }
}
