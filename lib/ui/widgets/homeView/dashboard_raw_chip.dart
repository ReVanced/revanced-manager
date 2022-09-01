import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/theme.dart';

class DashboardChip extends StatelessWidget {
  final String label;
  final bool isSelected;
  final Function(bool)? onSelected;

  const DashboardChip({
    Key? key,
    required this.label,
    required this.isSelected,
    this.onSelected,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return RawChip(
      showCheckmark: false,
      label: I18nText(label),
      selected: isSelected,
      labelStyle: GoogleFonts.inter(
        color: isSelected
            ? isDark
                ? const Color(0xff95C0FE)
                : Theme.of(context).colorScheme.secondary
            : isDark
                ? Colors.grey
                : Colors.grey[700],
        fontWeight: FontWeight.w500,
      ),
      backgroundColor:
          isDark ? Theme.of(context).colorScheme.background : Colors.white,
      selectedColor: const Color.fromRGBO(118, 155, 209, 0.42),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          width: 1,
          color: isDark
              ? isSelected
                  ? const Color.fromRGBO(118, 155, 209, 0.42)
                  : Colors.grey
              : isSelected
                  ? const Color.fromRGBO(118, 155, 209, 0.42)
                  : Colors.grey,
        ),
      ),
      onSelected: onSelected,
    );
  }
}
