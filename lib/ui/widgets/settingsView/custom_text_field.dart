import 'package:flutter/material.dart';

class CustomTextField extends StatelessWidget {
  final TextEditingController inputController;
  final Widget label;
  final String hint;
  final Function(String)? onChanged;

  const CustomTextField({
    Key? key,
    required this.inputController,
    required this.label,
    required this.hint,
    required this.onChanged,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        const SizedBox(height: 8),
        TextField(
          controller: inputController,
          onChanged: onChanged,
          keyboardType: TextInputType.text,
          decoration: InputDecoration(
            label: label,
            filled: true,
            fillColor: Theme.of(context).colorScheme.secondaryContainer,
            hintText: hint,
            contentPadding: const EdgeInsets.symmetric(
              vertical: 0.0,
              horizontal: 20.0,
            ),
            border: OutlineInputBorder(
              borderSide: BorderSide(
                color: Theme.of(context).colorScheme.primary,
                width: 1.0,
              ),
              borderRadius: BorderRadius.circular(10),
              gapPadding: 4.0,
            ),
            focusedBorder: OutlineInputBorder(
              borderSide: BorderSide(
                color: Theme.of(context).colorScheme.primary,
                width: 2.0,
              ),
              borderRadius: BorderRadius.circular(10),
            ),
            errorBorder: OutlineInputBorder(
              borderSide: const BorderSide(
                color: Colors.red,
                width: 1.0,
              ),
              borderRadius: BorderRadius.circular(10),
            ),
            enabledBorder: OutlineInputBorder(
              borderSide: BorderSide(
                color: Theme.of(context).colorScheme.primary,
                width: 1.0,
              ),
              borderRadius: BorderRadius.circular(10),
            ),
          ),
        ),
      ],
    );
  }
}
