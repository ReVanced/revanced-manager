# 🛠️ Build from source

This page will guide you through building ReVanced Manager from source.

1. Install Java Development Kit 17 (e.g. [Temurin JDK](https://adoptium.net/temurin/releases/?os=any&arch=any&version=17))

   Using [WinGet](https://learn.microsoft.com/en-us/windows/package-manager/winget):
   ``sh
   winget install EclipseAdoptium.Temurin.17.JDK
   ``
   
   Using [SDKMAN!](https://sdkman.io/):
   ```sh
   sdk install java 17.0.15-tem
   ```

2. Clone the repository

   ```sh
   git clone https://github.com/revanced/revanced-manager.git && cd revanced-manager
   ```

3. Build the APK

   ```sh
   ./gradlew assembleRelease
   ```

> [!NOTE]
> If the build fails due to authentication, you may need to authenticate to GitHub Packages.
> Create a personal access tokens with the scope `read:packages` [here](https://github.com/settings/tokens/new?scopes=read:packages&description=ReVanced) and add your token to ~/.gradle/gradle.properties. Create the file if it does not exist.
>
> Example `gradle.properties` file:
>
> ```properties
> gpr.user = <GitHub username>
> gpr.key = <Personal access token>
> ```
