import 'package:flutter/widgets.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

extension ContextX on BuildContext {
  AppLocalizations l10n() => AppLocalizations.of(this);
}
