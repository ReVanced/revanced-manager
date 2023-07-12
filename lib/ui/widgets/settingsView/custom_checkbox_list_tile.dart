import 'package:flutter/material.dart';

class CustomCheckboxListTile extends StatelessWidget {
  const CustomCheckboxListTile({
    Key? key,
    required this.selectedSources,
    required this.sources,
    required this.value,
  }) : super(key: key);
  final ValueNotifier<List<String>> selectedSources;
  final List<String> sources;
  final String value;

  @override
  Widget build(BuildContext context) {
    return CheckboxListTile(
      value: sources.contains(value),
      title: Text(value),
      onChanged: (selected) {
        if (selected!) {
          sources.add(value);
        } else {
          sources.remove(value);
        }
        selectedSources.value = [...sources];
      },
    );
  }
}
