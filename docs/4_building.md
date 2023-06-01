# 🛠️ Building from source

This page will guide you through building ReVanced Manager from source.

1. Setup the Flutter environment for your [platform](https://docs.flutter.dev/get-started/install)

2. Clone the repository

   ```sh
   git clone https://github.com/revanced/revanced-manager.git && cd revanced-manager
   ```

3. Create a GitHub PAT with the `read:packages` scope [here](https://github.com/settings/tokens/new?scopes=read:packages&description=ReVanced) and add your token in `android/gradle.properties`

   ```properties
   gpr.user = YourUsername
   gpr.key = ghp_longrandomkey
   ```

4. Get dependencies

   ```sh
   flutter pub get
   ```

5. Delete conflicting outputs

   ```sh
   flutter packages pub run build_runner build --delete-conflicting-outputs
   ```

   > **Note**: Must be run every time you sync your local repository with the remote repository

6. Build the APK

   ```sh
   flutter build apk
   ```
