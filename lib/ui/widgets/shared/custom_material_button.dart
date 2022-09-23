import 'package:flutter/material.dart';

class CustomMaterialButton extends StatelessWidget {
  final Widget label;
  final bool isFilled;
  final bool isExpanded;
  final Function()? onPressed;

  const CustomMaterialButton({
    Key? key,
    required this.label,
    this.isFilled = true,
    this.isExpanded = false,
    required this.onPressed,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton(
      style: ButtonStyle(
        padding: MaterialStateProperty.all(
          isExpanded
              ? const EdgeInsets.symmetric(horizontal: 24, vertical: 12)
              : const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        ),
        shape: MaterialStateProperty.all(
          StadiumBorder(
            side: isFilled
                ? BorderSide.none
                : BorderSide(
                    width: 1,
                    color: Theme.of(context).colorScheme.primary,
                  ),
          ),
        ),
        backgroundColor: MaterialStateProperty.all(
          isFilled ? Theme.of(context).colorScheme.primary : Colors.transparent,
        ),
        foregroundColor: MaterialStateProperty.all(
          isFilled
              ? Theme.of(context).colorScheme.surface
              : Theme.of(context).colorScheme.primary,
        ),
      ),
      onPressed: onPressed,
      child: label,
    );
  }
}

// ignore: must_be_immutable
class TimerButton extends StatefulWidget {
  Widget label;
  bool isFilled;
  int seconds;
  final bool isRunning;
  final Function()? onTimerEnd;

  TimerButton({
    Key? key,
    required this.seconds,
    required this.isRunning,
    required this.onTimerEnd,
    this.label = const Text(''),
    this.isFilled = true,
  }) : super(key: key);

  @override
  State<TimerButton> createState() => _TimerButtonState();
}

class _TimerButtonState extends State<TimerButton> {
  void timer(int seconds) {
    Future.delayed(const Duration(seconds: 1), () {
      if (seconds > 0) {
        setState(() {
          seconds--;
        });
        timer(seconds);
      } else {
        widget.onTimerEnd!();
      }
    });
  }

  @override
  void initState() {
    //decrement seconds
    if (widget.isRunning) {
      timer(widget.seconds);
    }
    super.initState();
  }

  @override
  Widget build(BuildContext build) {
    return TextButton(
      style: ButtonStyle(
        shape: MaterialStateProperty.all(
          StadiumBorder(
            side: widget.isFilled
                ? BorderSide.none
                : BorderSide(
                    width: 1,
                    color: Theme.of(context).colorScheme.primary,
                  ),
          ),
        ),
        backgroundColor: MaterialStateProperty.all(
          widget.isFilled
              ? Theme.of(context).colorScheme.primary
              : Colors.transparent,
        ),
        foregroundColor: MaterialStateProperty.all(
          widget.isFilled
              ? Theme.of(context).colorScheme.surface
              : Theme.of(context).colorScheme.primary,
        ),
      ),
      onPressed: widget.isRunning ? null : widget.onTimerEnd,
      child: Text(
        widget.isRunning ? '${widget.seconds}' : 'Install',
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
