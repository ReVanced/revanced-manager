<p align="center">
  <img width="800" src="Logo (with text).png">
</p>


### <p align="center">  The official ReVanced Manager based on Flutter</p>

> **Warning**: This repository currently has no active maintainer. For this reason, development is stale. Active development continues for [revanced-cli](https://github.com/revanced/revanced-cli). If you are interested in maintaining this repository, please let us know at manager@revanced.app.

## 🔽 Download
ReVanced Manager is still in early development (ALPHA) . You can install the latest version [here](https://github.com/revanced/revanced-manager/releases/latest).

## 📝 Prerequisites
1. Android 8 or higher.
2. [Vanced MicroG](https://github.com/TeamVanced/VancedMicroG/releases) is required for YouTube and YouTube Music (only for non-root).

&nbsp; 🟥 Does not work on some ARMv7 devices

## ⚠️ Disclaimer
*Please note that even though we're releasing the Manager, it is an ALPHA version. There's a big chance that the Manager might not work at all for you.*

## 🔴 Issues
For suggestions and bug reports, open an issue [here](https://github.com/revanced/revanced-manager/issues/new/choose).

## 💭 Discussion
If you wish to discuss the Manager, a thread has been made under the [#development](https://discord.com/channels/952946952348270622/1002922226443632761) channel in the Discord server. *This thread may be removed in the future.*


## 🌐 Translation
[![Crowdin](https://badges.crowdin.net/revanced/localized.svg)](https://crowdin.com/project/revanced)

We are accepting translations on [Crowdin](https://translate.revanced.app)

## 🛠️ Building the Manager from the source code
1. Setup flutter environment for your [platform.](https://docs.flutter.dev/get-started/install)
2. Clone the repository locally.
3. Add your github token in gradle.properties [like this](https://github.com/revanced/revanced-manager/blob/docs/docs/5_building-from-source.md).
4. Open the project in the terminal.
5. Run `flutter pub get`.
6. Then `flutter packages pub run build_runner build --delete-conflicting-outputs` (Must be done on each git pull).
7. To build release apk run `flutter build apk`.
