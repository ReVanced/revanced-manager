import 'package:flutter/material.dart';

class PasswordTextField extends StatefulWidget {
  const PasswordTextField({
    Key? key,
    required this.inputController,
    required this.label,
    required this.hint,
    this.obscureText = false,
    this.leadingIcon,
    required this.onChanged,
  }) : super(key: key);
  final TextEditingController inputController;
  final Widget label;
  final String hint;
  final bool obscureText;
  final Widget? leadingIcon;
  final Function(String)? onChanged;

  @override
  State<PasswordTextField> createState() => _PasswordTextFieldState();
}

class _PasswordTextFieldState extends State<PasswordTextField> {
  Icon visibleIcon = const Icon(Icons.visibility_outlined);
  bool obscureText = false;

  @override
  void initState() {
    obscureText = widget.obscureText;
    super.initState();
  }

  void onVisilbe() {
    visibleIcon = obscureText
        ? const Icon(Icons.visibility_off_outlined)
        : const Icon(Icons.visibility_outlined);

    setState(() {
      obscureText = !obscureText;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 4.0),
      child: TextField(
        controller: widget.inputController,
        onChanged: widget.onChanged,
        obscureText: obscureText,
        keyboardType: TextInputType.text,
        decoration: InputDecoration(
          icon: widget.leadingIcon,
          label: widget.label,
          filled: true,
          fillColor: Theme.of(context).colorScheme.secondaryContainer,
          suffixIcon: IconButton(
            icon: visibleIcon,
            onPressed: onVisilbe,
          ),
          hintText: widget.hint,
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
            ),
            borderRadius: BorderRadius.circular(10),
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
            ),
            borderRadius: BorderRadius.circular(10),
          ),
          enabledBorder: OutlineInputBorder(
            borderSide: BorderSide(
              color: Theme.of(context).colorScheme.primary,
            ),
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      ),
    );
  }
}
