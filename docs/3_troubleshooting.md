# ❔ Troubleshooting

In case you encounter any issues while using ReVanced Manager, please refer to this page for possible solutions.

- 💉 Patching fails with an error

  Make sure ReVanced Manager is up to date by following [🔄 Updating ReVanced Manager](2_3_updating.md) and select the **Default** button when choosing patches.

- 🚫 App not installed as package conflicts with an existing package

  An existing installation of the app you're trying to patch conflicts with the patched app (i.e., signature mismatch or downgrade). Uninstall the existing app before installing the patched app.

- ❗️ Error code `135`, `139` or `1` when patching the app

  You may be trying to patch a split APK[^1]. This may not work under certain circumstances. In such a case, patch a full APK.

  Your device may otherwise be unsupported. Please look at the [Prerequisites](0_prerequisites.md) page for supported devices.

  Alternatively, you can use [ReVanced CLI](https://github.com/revanced/revanced-cli) to patch the app.

- 🚨 Patched app crashes on launch

  Select the **Default** button when choosing patches.

## ⏭️ What's next

The next page will teach you how to build ReVanced Manager from source.

Continue: [🔨 Building from source](4_building.md)

[^1]: https://developer.android.com/guide/app-bundle/app-bundle-format
