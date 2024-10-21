import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:url_launcher/url_launcher.dart';

class UpdateConfirmationSheet extends StatelessWidget {
  const UpdateConfirmationSheet({
    super.key,
    required this.isPatches,
    this.changelog = false,
  });

  final bool isPatches;
  final bool changelog;

  @override
  Widget build(BuildContext context) {
    final HomeViewModel model = locator<HomeViewModel>();

    return DraggableScrollableSheet(
      expand: false,
      snap: true,
      snapSizes: const [0.5],
      builder: (_, scrollController) => SingleChildScrollView(
        controller: scrollController,
        child: SafeArea(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (!changelog)
                Padding(
                  padding: const EdgeInsets.only(
                    top: 40.0,
                    left: 24.0,
                    right: 24.0,
                    bottom: 20.0,
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              isPatches
                                  ? t.homeView.updatePatchesSheetTitle
                                  : t.homeView.updateSheetTitle,
                              style: const TextStyle(
                                fontSize: 24,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 4.0),
                            Row(
                              children: [
                                Icon(
                                  Icons.new_releases_outlined,
                                  color:
                                      Theme.of(context).colorScheme.secondary,
                                ),
                                const SizedBox(width: 8.0),
                                Text(
                                  isPatches
                                      ? model.latestPatchesVersion ?? 'Unknown'
                                      : model.latestManagerVersion ?? 'Unknown',
                                  style: TextStyle(
                                    fontSize: 20,
                                    fontWeight: FontWeight.w500,
                                    color:
                                        Theme.of(context).colorScheme.secondary,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      FilledButton(
                        onPressed: () {
                          Navigator.of(context).pop();
                          isPatches
                              ? model.updatePatches(context)
                              : model.updateManager(context);
                        },
                        child: Text(t.updateButton),
                      ),
                    ],
                  ),
                ),
              Padding(
                padding: const EdgeInsets.only(
                  top: 12.0,
                  left: 24.0,
                  bottom: 12.0,
                ),
                child: Text(
                  t.homeView.updateChangelogTitle,
                  style: TextStyle(
                    fontSize: changelog ? 24 : 20,
                    fontWeight: FontWeight.w500,
                    color: Theme.of(context).colorScheme.onSecondaryContainer,
                  ),
                ),
              ),
              FutureBuilder<String?>(
                future: model.getChangelogs(isPatches),
                builder: (_, snapshot) {
                  if (!snapshot.hasData) {
                    return Padding(
                      padding: EdgeInsets.only(top: changelog ? 96 : 24),
                      child: const Center(
                        child: CircularProgressIndicator(),
                      ),
                    );
                  }
                  return Container(
                    margin: const EdgeInsets.symmetric(horizontal: 24.0),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.secondaryContainer,
                      borderRadius: BorderRadius.circular(12.0),
                    ),
                    child: Markdown(
                      styleSheet: MarkdownStyleSheet(
                        a: TextStyle(
                          color: Theme.of(context).colorScheme.primary,
                        ),
                      ),
                      onTapLink: (text, href, title) => href != null
                          ? launchUrl(
                              Uri.parse(href),
                              mode: LaunchMode.externalApplication,
                            )
                          : null,
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      padding: const EdgeInsets.all(20.0),
                      data: snapshot.data ?? '',
                    ),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
