# 💼 Prerequisites

Here's everything you'll need to develop on ReVanced Manager

## 🤝 Recommended enviroment

- Any environment that capable of running the latest Android Studio, latest Android SDK and JDK 17 with at least 4 GB of memory for coding
- Any devices with preferably the latest Android version or at least higher than the [`minSdk`](/app/build.gradle.kts) for testing

### Additional Requirements

- Device with root capabilities with latest version of [KernelSU](https://github.com/tiann/KernelSU) or [Magisk](https://github.com/topjohnwu/Magisk)

## ⚙️ Setting up

### Authenticating to GitHub Registry

ReVanced Manager uses dependency from the GitHub Registry and so your build may fail without authenticating to the service, to authenticate you must add create a personal access token with the scope `read:packages` [here](https://github.com/settings/tokens/new?scopes=read:packages&description=ReVanced) and add your token to `~/.gradle/gradle.properties`, create the file if it does not exist.

```properties
gpr.user = username
gpr.key = ghp_******************************
```

## ⏭️ What's next

The next page will guide you through developing for ReVanced Manager.

Continue: [🧑‍💻 Developing for ReVanced Manager](1_develop.md)