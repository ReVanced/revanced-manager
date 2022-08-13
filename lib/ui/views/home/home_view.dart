import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/theme.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/available_updates_card.dart';
import 'package:revanced_manager/ui/widgets/installed_apps_card.dart';
import 'package:revanced_manager/ui/widgets/latest_commit_card.dart';
import 'package:stacked/stacked.dart';

class HomeView extends StatelessWidget {
  const HomeView({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder.reactive(
      viewModelBuilder: () => HomeViewModel(),
      builder: (context, model, child) => Scaffold(
        body: SafeArea(
          child: SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Align(
                    alignment: Alignment.topRight,
                    child: IconButton(
                      onPressed: () => {},
                      icon: const Icon(
                        Icons.more_vert,
                      ),
                    ),
                  ),
                  const SizedBox(height: 60),
                  I18nText(
                    'homeView.widgetTitle',
                    child: Text(
                      '',
                      style: GoogleFonts.inter(
                        fontSize: 28,
                      ),
                    ),
                  ),
                  const SizedBox(height: 23),
                  I18nText(
                    'homeView.updatesSubtitle',
                    child: Text(
                      '',
                      style: GoogleFonts.inter(
                        fontSize: 20,
                        color: isDark
                            ? const Color(0xffD1E1FA)
                            : const Color(0xff384E6E),
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  LatestCommitCard(
                      color: Theme.of(context).colorScheme.primary),
                  const SizedBox(height: 14),
                  I18nText(
                    'homeView.patchedSubtitle',
                    child: Text(
                      '',
                      style: GoogleFonts.inter(
                        fontSize: 20,
                        color: isDark
                            ? const Color(0xffD1E1FA)
                            : const Color(0xff384E6E),
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                  AvailableUpdatesCard(
                    color: Theme.of(context).colorScheme.primary,
                  ),
                  const SizedBox(height: 15),
                  InstalledAppsCard(
                    color: Theme.of(context).colorScheme.primary,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
