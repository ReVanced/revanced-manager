import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/installerView/custom_material_button.dart';
import 'package:stacked/stacked.dart';

class InstallerView extends StatelessWidget {
  const InstallerView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<InstallerViewModel>.reactive(
      onModelReady: (model) => model.initialize(context),
      viewModelBuilder: () => InstallerViewModel(),
      builder: (context, model, child) => WillPopScope(
        child: Scaffold(
          body: CustomScrollView(
            controller: model.scrollController,
            slivers: <Widget>[
              SliverAppBar(
                pinned: true,
                snap: false,
                floating: false,
                expandedHeight: 100.0,
                automaticallyImplyLeading: false,
                backgroundColor: MaterialStateColor.resolveWith(
                  (states) => states.contains(MaterialState.scrolledUnder)
                      ? isDark
                          ? Theme.of(context).colorScheme.primary
                          : Theme.of(context)
                              .navigationBarTheme
                              .backgroundColor!
                      : Theme.of(context).scaffoldBackgroundColor,
                ),
                flexibleSpace: FlexibleSpaceBar(
                  titlePadding: const EdgeInsets.symmetric(
                    vertical: 23.0,
                    horizontal: 20.0,
                  ),
                  title: Text(
                    model.headerLogs,
                    style: GoogleFonts.inter(
                      color: Theme.of(context).textTheme.headline5!.color,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
                bottom: PreferredSize(
                  preferredSize: const Size(double.infinity, 1.0),
                  child: LinearProgressIndicator(
                    color: Theme.of(context).colorScheme.secondary,
                    backgroundColor: Colors.white,
                    value: model.progress,
                  ),
                ),
              ),
              SliverPadding(
                padding: const EdgeInsets.all(20.0),
                sliver: SliverList(
                  delegate: SliverChildListDelegate.fixed(
                    <Widget>[
                      Container(
                        padding: const EdgeInsets.all(12.0),
                        width: double.infinity,
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.primary,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          model.logs,
                          style: GoogleFonts.jetBrainsMono(
                            fontSize: 13,
                            height: 1.5,
                          ),
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.symmetric(
                          vertical: 16,
                          horizontal: 0,
                        ),
                        child: Visibility(
                          visible: !model.isPatching,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              CustomMaterialButton(
                                text: 'installerView.shareButton',
                                isFilled: false,
                                onPressed: () => model.shareResult(),
                              ),
                              const SizedBox(width: 16),
                              CustomMaterialButton(
                                text: model.isInstalled
                                    ? 'installerView.openButton'
                                    : 'installerView.installButton',
                                isFilled: true,
                                isExpanded: true,
                                onPressed: () {
                                  if (model.isInstalled) {
                                    model.openApp();
                                    model.cleanPatcher();
                                    Navigator.of(context).pop();
                                  } else {
                                    model.installResult();
                                  }
                                },
                              ),
                            ],
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
        onWillPop: () async {
          if (!model.isPatching) {
            model.cleanPatcher();
            Navigator.of(context).pop();
          }
          return false;
        },
      ),
    );
  }
}
