import 'package:envied/envied.dart';

part 'env_class.g.dart';

@Envied()
abstract class Env {
  @EnviedField(varName: 'sentryDSN')
  static const sentryDSN = _Env.sentryDSN;

  @EnviedField(varName: 'apiKey')
  static const apiKey = _Env.apiKey;

  @EnviedField(varName: 'appId')
  static const appId = _Env.appId;

  @EnviedField(varName: 'messagingSenderI')
  static const messagingSenderId = _Env.messagingSenderId;

  @EnviedField(varName: 'projectId')
  static const projectId = _Env.projectId;

  @EnviedField(varName: 'storageBucket')
  static const storageBucket = _Env.storageBucket;
}
