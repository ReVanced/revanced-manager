# 💊 ReVanced Manager

The official ReVanced Manager based on Flutter.

## 🔽 Download
To download latest Manager, go [here](https://github.com/revanced/revanced-manager/releases/latest) and install the provided APK file.

## 📝 Prerequisites
1. Android 8 or higher
2. Does not work on some armv7 devices

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
3. Add your github token in gradle.properties like [this](/docs/4_building.md)
4. Open the project in terminal
5. Run `flutter pub get` in terminal
6. Then `flutter packages pub run build_runner build --delete-conflicting-outputs` (Must be done on each git pull)
7. To build release apk run `flutter build apk`

## ℹ️ Additional Info. 
If you wish to know more about any of the processes involved, you can head over to the [Secondary Readme](https://github.com/revanced/revanced-manager/blob/main/docs/README.md) file.

Or, directly jump to one of its sub-files:

0. [💼 Prerequisites](https://github.com/ReVanced/revanced-manager/blob/main/docs/0_prerequisites.md)
1. [⬇️ Installation](https://github.com/ReVanced/revanced-manager/blob/main/docs/1_installation.md)
2. [🛠️ Usage](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_usage.md)
   1. [🧩 Patching apps](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_1_patching.md)
   2. [🧰 Managing patched apps](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_2_managing.md)
   3. [🔄 Updating ReVanced Manager](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_3_updating.md)
   4. [⚙️ Configuring ReVanced Manager](https://github.com/ReVanced/revanced-manager/blob/main/docs/2_4_settings.md)
3. [🛟 Troubleshooting](https://github.com/ReVanced/revanced-manager/blob/main/docs/3_troubleshooting.md)
4. [🛠 Building from source](https://github.com/ReVanced/revanced-manager/blob/main/docs/4_building.md)
