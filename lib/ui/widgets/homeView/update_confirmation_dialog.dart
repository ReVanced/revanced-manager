import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_material_button.dart';

class UpdateConfirmationDialog extends StatelessWidget {
  const UpdateConfirmationDialog({super.key, required this.isPatches});

  final bool isPatches;
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
          child: FutureBuilder<Map<String, dynamic>?>(
            future: !isPatches
                ? model.getLatestManagerRelease()
                : model.getLatestPatchesRelease(),
            builder: (_, snapshot) {
              if (!snapshot.hasData) {
                return const SizedBox(
                  height: 300,
                  child: Center(
                    child: CircularProgressIndicator(),
                  ),
                );
              }

              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(
                      top: 40.0,
                      left: 24.0,
                      right: 24.0,
                      bottom: 32.0,
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                isPatches
                                    ? t.homeView.updatePatchesDialogTitle
                                    : t.homeView.updateDialogTitle,
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
                                    snapshot.data!['tag_name'] ?? 'Unknown',
                                    style: TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.w500,
                                      color: Theme.of(context)
                                          .colorScheme
                                          .secondary,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        CustomMaterialButton(
                          isExpanded: true,
                          label: Text(t.updateButton),
                          onPressed: () {
                            Navigator.of(context).pop();
                            isPatches
                                ? model.updatePatches(context)
                                : model.updateManager(context);
                          },
                        ),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(left: 24.0, bottom: 12.0),
                    child: Text(
                      t.homeView.updateChangelogTitle,
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w500,
                        color:
                            Theme.of(context).colorScheme.onSecondaryContainer,
                      ),
                    ),
                  ),
                  Container(
                    margin: const EdgeInsets.symmetric(horizontal: 24.0),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.secondaryContainer,
                      borderRadius: BorderRadius.circular(12.0),
                    ),
                    child: Markdown(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      padding: const EdgeInsets.all(20.0),
                      data: snapshot.data!['body'] ?? '',
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}
