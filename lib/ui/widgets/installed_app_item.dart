import 'package:flutter/material.dart';

class InstalledAppItem extends StatelessWidget {
  final String name;
  final String pkgName;

  const InstalledAppItem({
    Key? key,
    required this.name,
    required this.pkgName,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0, horizontal: 8.0),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12.0, horizontal: 12.0),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(12),
          color: const Color(0xff1B222B),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name),
                Text(pkgName),
              ],
            ),
            Checkbox(
              value: false,
              onChanged: (val) {},
            ),
          ],
        ),
      ),
    );
  }
}
