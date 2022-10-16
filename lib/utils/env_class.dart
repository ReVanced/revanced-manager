import 'package:envied/envied.dart';

part 'env_class.g.dart';

@Envied()
abstract class Env {
  @EnviedField(varName: 'sentryDSN')
  static const sentryDSN = _Env.sentryDSN;
}
