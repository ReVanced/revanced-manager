# 💊 ReVanced Manager

The official ReVanced Manager based on Flutter.

## 🔽 Download
You can download the latest version of ReVanced Manager [here](https://github.com/revanced/revanced-manager/releases/latest).

## 📝 Prerequisites
- Android 8+
- Devices **not** ARMv7

## 🔴 Issues
For suggestions and bug reports, open an issue [here](https://github.com/revanced/revanced-manager/issues/new/choose).

## 💭 Discussion
If you wish to discuss the Manager, a thread has been made under the [#development](https://discord.com/channels/952946952348270622/1002922226443632761) channel in the Discord server, please note that this thread may be temporary and may be removed in the future.


## 🛠️ Building Manager from source
1. Setup flutter environment for your [platform](https://docs.flutter.dev/get-started/install)
2. Clone the repository locally
3. Add your github token in gradle.properties like [this](https://github.com/revanced/revanced-documentation/wiki/Building-from-source)
4. Open the project in terminal
5. Run `flutter pub get` in terminal
6. Then `flutter packages pub run build_runner build --delete-conflicting-outputs` (Must be done on each git pull)
7. To build release apk run `flutter build apk`
