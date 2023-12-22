# ❔ Troubleshooting

In case you encounter any issues while using ReVanced Manager, please refer to this page for possible solutions.

- 💉 Patching fails with an error

  Make sure ReVanced Manager is up to date by following [🔄 Updating ReVanced Manager](2_3_updating.md) and select the **Default** button when choosing patches.

- 🚫 App not installed as package conflicts with an existing package

  An existing installation of the app you're trying to patch is conflicting with the patched app (i.e., the existing version of the app doesn't allow upgrading to the patched version, or a different copy of the app causes a conflict). Uninstall the existing app before installing the patched app.

- ❗️ Error code `135`, `139` or `1` when patching the app

  You may be trying to patch a split APK[^1], which is sometimes not supported by ReVanced. Sometimes apps are available in both split and non-split forms. If your existing installation of the app is a split APK, try obtaining a non-split version and see if that can be patched correctly.

  Your device may otherwise be unsupported. Please look at the [Prerequisites](0_prerequisites.md) page for supported devices.

  Alternatively, you can use [ReVanced CLI](https://github.com/revanced/revanced-cli) to patch the app.

- 🚨 Patched app crashes on launch

  Select the **Default** button when choosing patches.

## ⏭️ What's next

The next page will teach you how to build ReVanced Manager from source.

Continue: [🔨 Building from source](4_building.md)

[^1]: https://old.reddit.com/r/revancedapp/comments/xna2gn/youtube_unsupported_version_and_split_apk/
