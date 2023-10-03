import 'package:flutter/material.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class BooleanPatchOption extends StatelessWidget {
  const BooleanPatchOption({super.key, required this.patchOptions});
  final Option patchOptions;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: CustomCard(
          onTap: () {},
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(patchOptions.title),
              const SizedBox(height: 8),
              Text(patchOptions.description),
            ],
          ),

      ),
    );
  }
}

class StringPatchOption extends StatelessWidget {
  const StringPatchOption({super.key, required this.patchOptions});
  final Option patchOptions;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: CustomCard(
        onTap: () {},
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(patchOptions.title),
            const SizedBox(height: 8),
            Text(patchOptions.description),
          ],
        ),
      ),
    );
  }
}

class ListPatchOption extends StatelessWidget {
  const ListPatchOption({super.key, required this.patchOptions});
  final Option patchOptions;

  @override
  Widget build(BuildContext context) {
    return const Placeholder();
  }
}