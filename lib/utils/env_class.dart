import 'package:envied/envied.dart';

part 'env_class.g.dart';

@Envied()
abstract class Env {
  @EnviedField(varName: 'sentryDSN')
  static String sentryDSN = _Env.sentryDSN;

  @EnviedField(varName: 'apiKey')
  static String apiKey = _Env.apiKey;

  @EnviedField(varName: 'appId')
  static String appId = _Env.appId;
}
