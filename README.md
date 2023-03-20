# 💊 ReVanced Manager

The official ReVanced Manager based on Flutter.

## 🔽 Download
To download the Alpha version of Manager, go [here](https://github.com/revanced/revanced-manager/releases/latest) and install the provided APK file.

## 📝 Prerequisites
1. Android 8 or higher
2. Does not work on some armv7 devices
3. [Vanced MicroG](https://github.com/TeamVanced/VancedMicroG/releases) required for YouTube and YouTube Music (Only for non-root)

## ⚠️ Disclaimer
*Please note that even though we're releasing the Manager, it is an ALPHA version. There's a big chance that the Manager might not work at all for you.*

## 🔴 Issues
For suggestions and bug reports, open an issue [here](https://github.com/revanced/revanced-manager/issues/new/choose).

## 💭 Discussion
If you wish to discuss the Manager, a thread has been made under the [#development](https://discord.com/channels/952946952348270622/1002922226443632761) channel in the Discord server, please note that this thread may be temporary and may be removed in the future.


## 🌐 Translation
[![Crowdin](https://badges.crowdin.net/revanced/localized.svg)](https://crowdin.com/project/revanced)

If you wish to translate ReVanced Manager, we're accepting translations on [Crowdin](https://translate.revanced.app)

## 🛠️ Building Manager from source
1. Setup flutter environment for your [platform](https://docs.flutter.dev/get-started/install)
2. Clone the repository locally
3. Add your github token in gradle.properties like [this](https://github.com/revanced/revanced-manager/blob/docs/docs/5_building-from-source.md)
4. Open the project in terminal
5. Run `flutter pub get` in terminal
6. Then `flutter packages pub run build_runner build --delete-conflicting-outputs` (Must be done on each git pull)
7. To build release apk run `flutter build apk`
