import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/installer/installer_viewmodel.dart';
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
              ),
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 20.0),
                sliver: SliverList(
                  delegate: SliverChildListDelegate.fixed(
                    <Widget>[
                      Padding(
                        padding: const EdgeInsets.only(
                          left: 4.0,
                          top: 0.0,
                          right: 4.0,
                          bottom: 16.0,
                        ),
                        child: LinearProgressIndicator(
                          color: Theme.of(context).colorScheme.secondary,
                          backgroundColor: Colors.white,
                          value: model.progress,
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.all(12.0),
                        width: double.infinity,
                        decoration: BoxDecoration(
                          color: Theme.of(context).colorScheme.primary,
                          borderRadius: BorderRadius.circular(8),
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
                            vertical: 16, horizontal: 0),
                        child: Visibility(
                          visible: !model.isPatching,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              //TODO: Move to separate file
                              TextButton(
                                style: ButtonStyle(
                                  padding: MaterialStateProperty.all(
                                    const EdgeInsets.symmetric(
                                      horizontal: 20,
                                      vertical: 12,
                                    ),
                                  ),
                                  shape: MaterialStateProperty.all(
                                    RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(100),
                                      side: BorderSide(
                                        width: 1,
                                        color: Theme.of(context)
                                            .colorScheme
                                            .secondary,
                                      ),
                                    ),
                                  ),
                                  side: MaterialStateProperty.all(
                                    BorderSide(
                                      color: Theme.of(context)
                                          .iconTheme
                                          .color!
                                          .withOpacity(0.4),
                                      width: 1,
                                    ),
                                  ),
                                  backgroundColor: MaterialStateProperty.all(
                                    isDark
                                        ? Theme.of(context)
                                            .colorScheme
                                            .background
                                        : Colors.white,
                                  ),
                                  foregroundColor: MaterialStateProperty.all(
                                    Theme.of(context).colorScheme.secondary,
                                  ),
                                ),
                                onPressed: () => model.shareResult(),
                                child: I18nText("Share file"),
                              ),
                              const SizedBox(width: 16),
                              TextButton(
                                onPressed: () {
                                  if (model.isInstalled) {
                                    model.openApp();
                                    Navigator.of(context).pop();
                                  } else {
                                    model.installResult();
                                  }
                                },
                                style: ButtonStyle(
                                  padding: MaterialStateProperty.all(
                                    const EdgeInsets.symmetric(
                                      horizontal: 24,
                                      vertical: 8,
                                    ),
                                  ),
                                  shape: MaterialStateProperty.all(
                                    RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(100),
                                      side: BorderSide(
                                        width: 1,
                                        color: Theme.of(context)
                                            .colorScheme
                                            .secondary,
                                      ),
                                    ),
                                  ),
                                  backgroundColor: MaterialStateProperty.all(
                                    Theme.of(context).colorScheme.secondary,
                                  ),
                                  foregroundColor: MaterialStateProperty.all(
                                    Theme.of(context).colorScheme.background,
                                  ),
                                ),
                                child: I18nText(model.isInstalled
                                    ? 'installerView.openButton'
                                    : 'installerView.installButton'),
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
