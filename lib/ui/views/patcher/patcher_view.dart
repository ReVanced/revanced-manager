import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager_flutter/ui/views/app_selector/app_selector_view.dart';
import 'package:revanced_manager_flutter/ui/widgets/app_selector_card.dart';
import 'package:revanced_manager_flutter/ui/widgets/patch_selector_card.dart';
import 'package:stacked/stacked.dart';

import 'patcher_viewmodel.dart';

class PatcherView extends StatelessWidget {
  const PatcherView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder.reactive(
      builder: (context, PatcherViewModel model, child) => Scaffold(
        floatingActionButton: FloatingActionButton(
          onPressed: () {},
          child: const Icon(
            Icons.build,
            color: Colors.white,
          ),
        ),
        body: SafeArea(
          child: Padding(
            padding:
                const EdgeInsets.symmetric(vertical: 12.0, horizontal: 12.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 12),
                Text(
                  "Patcher",
                  style: GoogleFonts.inter(
                    fontSize: 28,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 23),
                AppSelectorCard(
                  onPressed: model.navigateToAppSelector,
                ),
                const SizedBox(height: 16),
                const PatchSelectorCard(),
              ],
            ),
          ),
        ),
      ),
      viewModelBuilder: () => PatcherViewModel(),
    );
  }
}
