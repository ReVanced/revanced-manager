import 'package:flutter/material.dart';

class CustomSwitch extends StatelessWidget {
  final ValueChanged<bool> onChanged;
  final bool value;

  const CustomSwitch({
    Key? key,
    required this.onChanged,
    required this.value,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => onChanged(!value),
      child: SizedBox(
        height: 25,
        width: 50,
        child: Stack(
          children: <Widget>[
            AnimatedContainer(
              height: 25,
              width: 50,
              curve: Curves.ease,
              duration: const Duration(milliseconds: 400),
              decoration: BoxDecoration(
                borderRadius: const BorderRadius.all(
                  Radius.circular(25.0),
                ),
                color: value
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(context).colorScheme.secondary,
              ),
            ),
            AnimatedAlign(
              curve: Curves.ease,
              duration: const Duration(milliseconds: 400),
              alignment: !value ? Alignment.centerLeft : Alignment.centerRight,
              child: Container(
                height: 20,
                width: 20,
                margin: const EdgeInsets.symmetric(horizontal: 3),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: value
                      ? Theme.of(context).colorScheme.primaryContainer
                      : Colors.white,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black12.withOpacity(0.1),
                      spreadRadius: 0.5,
                      blurRadius: 1,
                    )
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
