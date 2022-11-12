# 🛠 Building from source
1. Setup the Flutter environment for your [platform](https://docs.flutter.dev/get-started/install)
2. Clone the repository
```sh
git clone https://github.com/revanced/revanced-manager.git && cd revanced-manager
```

3. Create a GitHub personal access token with the `read:packages` scope [here](https://github.com/settings/tokens/new?scopes=read:packages&description=Revanced)


4. Add your token in `android/gradle.properties`
``` properties
gpr.user = YourUsername
gpr.key = ghp_longrandomkey
```
5. Download project packages
```sh
flutter pub get
```
6. Delete conflicting outputs (must be run everytime you sync your local repository with the remote's)
```sh
flutter packages pub run build_runner build --delete-conflicting-outputs
```
7. Build the APK
```sh
flutter build apk
```