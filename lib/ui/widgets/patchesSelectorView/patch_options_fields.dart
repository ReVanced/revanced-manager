import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:google_fonts/google_fonts.dart';

class OptionsTextField extends StatelessWidget {
  final String hint;
  const OptionsTextField({Key? key, required this.hint}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final sHeight = MediaQuery.of(context).size.height;
    final sWidth = MediaQuery.of(context).size.width;
    return Container(
      margin: const EdgeInsets.only(top: 12, bottom: 6),
      padding: const EdgeInsets.all(0),
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
  final String optionName;
  const OptionsFilePicker({Key? key, required this.optionName})
      : super(key: key);

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
                color: Theme.of(context).textTheme.bodyText1?.color,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
