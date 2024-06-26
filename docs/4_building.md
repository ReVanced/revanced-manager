# 🛠️ Building from source

Learn how to build ReVanced Manager from source.

1. Setup the Flutter environment for your [platform](https://docs.flutter.dev/get-started/install)

2. Clone the repository

   ```sh
   git clone https://github.com/revanced/revanced-manager.git && cd revanced-manager
   ```

3. Get dependencies

   ```sh
   flutter pub get
   ```

4. Generate temporary files

   ```sh
   dart run slang
   dart run build_runner build -d
   ```

5. Build the APK

   ```sh
   flutter build apk
   ```

> [!NOTE]
> If the build fails due to authentication, you may need to authenticate to GitHub Packages.
> Create a PAT with the scope `read:packages` [here](https://github.com/settings/tokens/new?scopes=read:packages&description=ReVanced) and add your token to ~/.gradle/gradle.properties.
>
> Example `gradle.properties` file:
>
> ```properties
> gpr.user = user
> gpr.key = key
> ```
