import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/github_api.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/patch_text_button.dart';

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
  final GithubAPI _githubAPI = GithubAPI();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: Theme.of(context).colorScheme.primary,
      ),
      padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 20),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Row(
                children: <Widget>[
                  I18nText(
                    'latestCommitCard.patcherLabel',
                    child: Text(
                      '',
                      style: GoogleFonts.roboto(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  FutureBuilder<String>(
                    future: _githubAPI.latestCommitTime(ghOrg, patcherRepo),
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
                      style: kRobotoTextStyle,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: <Widget>[
                  I18nText(
                    'latestCommitCard.managerLabel',
                    child: Text(
                      '',
                      style: GoogleFonts.roboto(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  FutureBuilder<String>(
                    future: _githubAPI.latestCommitTime(ghOrg, managerRepo),
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
                      style: kRobotoTextStyle,
                    ),
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
              child: PatchTextButton(
                text: FlutterI18n.translate(
                  context,
                  'latestCommitCard.updateButton',
                ),
                onPressed: snapshot.hasData && snapshot.data!
                    ? widget.onPressed
                    : () => {},
                backgroundColor: Theme.of(context).colorScheme.secondary,
                borderColor: Theme.of(context).colorScheme.secondary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
