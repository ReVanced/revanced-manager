import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';

class OptionsTextField extends StatelessWidget {
  const OptionsTextField({Key? key, required this.hint}) : super(key: key);
  final String hint;

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    final sHeight = size.height;
    final sWidth = size.width;
    return Container(
      margin: const EdgeInsets.only(top: 12, bottom: 6),
      padding: EdgeInsets.zero,
      child: TextField(
        decoration: InputDecoration(
          constraints: BoxConstraints(
            maxHeight: sHeight * 0.05,
            maxWidth: sWidth * 1,
          ),
          border: const OutlineInputBorder(),
          labelText: hint,
        ),
      ),
    );
  }
}

class OptionsFilePicker extends StatelessWidget {
  const OptionsFilePicker({Key? key, required this.optionName})
      : super(key: key);
  final String optionName;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          I18nText(
            optionName,
            child: Text(
              '',
              style: GoogleFonts.inter(
                fontSize: 16,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          ElevatedButton(
            style: ButtonStyle(
              backgroundColor: MaterialStateProperty.all(
                Theme.of(context).colorScheme.primary,
              ),
            ),
            onPressed: () {
              // pick files
            },
            child: Text(
              'Select File',
              style: TextStyle(
                color: Theme.of(context).textTheme.bodyLarge?.color,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
