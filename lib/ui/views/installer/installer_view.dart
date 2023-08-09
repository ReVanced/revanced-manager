import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/gradient_progress_indicator.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class InstallerView extends StatelessWidget {
  const InstallerView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<InstallerViewModel>.reactive(
      onViewModelReady: (model) => model.initialize(context),
      viewModelBuilder: () => InstallerViewModel(),
      builder: (context, model, child) => WillPopScope(
        child: SafeArea(
          top: false,
          bottom: false,
          child: Scaffold(
            floatingActionButton: Visibility(
              visible: !model.isPatching,
              child: FloatingActionButton.extended(
                label: I18nText('installerView.installButton'),
                icon: const Icon(Icons.file_download_outlined),
                onPressed: () => model.installTypeDialog(context),
              ),
            ),
            floatingActionButtonLocation:
                FloatingActionButtonLocation.endContained,
            bottomNavigationBar: Visibility(
              visible: !model.isPatching,
              child: BottomAppBar(
                child: Row(
                  children: <Widget>[
                    Visibility(
                      visible: !model.hasErrors,
                      child: IconButton.filledTonal(
                        tooltip: 'Save APK',
                        icon: const Icon(Icons.save),
                        onPressed: () => model.onButtonPressed(0),
                      ),
                    ),
                    IconButton.filledTonal(
                      tooltip: 'Copy logs',
                      icon: const Icon(Icons.content_copy),
                      onPressed: () => model.onButtonPressed(1),
                    ),
                  ],
                ),
              ),
            ),
            body: CustomScrollView(
              controller: model.scrollController,
              slivers: <Widget>[
                CustomSliverAppBar(
                  title: Text(
                    model.headerLogs,
                    style: GoogleFonts.inter(
                      color: Theme.of(context).textTheme.titleLarge!.color,
                    ),
                  ),
                  onBackButtonPressed: () => model.onWillPop(context),
                  bottom: PreferredSize(
                    preferredSize: const Size(double.infinity, 1.0),
                    child: GradientProgressIndicator(progress: model.progress),
                  ),
                ),
                SliverPadding(
                  padding: const EdgeInsets.all(20.0),
                  sliver: SliverList(
                    delegate: SliverChildListDelegate.fixed(
                      <Widget>[
                        CustomCard(
                          child: Text(
                            model.logs,
                            style: GoogleFonts.jetBrainsMono(
                              fontSize: 13,
                              height: 1.5,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        onWillPop: () => model.onWillPop(context),
      ),
    );
  }
}
