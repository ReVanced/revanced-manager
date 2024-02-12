import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart' as t;

class Toast {
  final t.FToast _fToast = t.FToast();
  late BuildContext buildContext;

  void initialize(BuildContext context) {
    _fToast.init(context);
  }

  void show(String text) {
    t.Fluttertoast.showToast(
      msg: text,
      toastLength: t.Toast.LENGTH_LONG,
      gravity: t.ToastGravity.CENTER,
    );
  }

  void showBottom(String text) {
    t.Fluttertoast.showToast(
      msg: text,
      toastLength: t.Toast.LENGTH_LONG,
      gravity: t.ToastGravity.BOTTOM,
    );
  }
}
