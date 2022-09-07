import 'package:flutter/material.dart';

class CustomTextField extends StatelessWidget {
  final TextEditingController inputController;
  final Widget label;
  final String hint;
  final Widget? leadingIcon;
  final Function(String)? onChanged;

  const CustomTextField({
    Key? key,
    required this.inputController,
    required this.label,
    required this.hint,
    this.leadingIcon,
    required this.onChanged,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 4.0),
      child: TextField(
        controller: inputController,
        onChanged: onChanged,
        keyboardType: TextInputType.text,
        decoration: InputDecoration(
          icon: leadingIcon,
          label: label,
          filled: true,
          fillColor: Theme.of(context).colorScheme.secondaryContainer,
          hintText: hint,
          hintStyle: TextStyle(
            color: Theme.of(context).colorScheme.secondary,
          ),
          floatingLabelStyle: MaterialStateTextStyle.resolveWith(
            (states) => states.contains(MaterialState.focused)
                ? TextStyle(color: Theme.of(context).colorScheme.primary)
                : TextStyle(color: Theme.of(context).colorScheme.secondary),
          ),
          contentPadding: const EdgeInsets.symmetric(
            vertical: 8.0,
            horizontal: 16.0,
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
            borderSide: BorderSide(
              color: Theme.of(context).colorScheme.error,
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
    );
  }
}
