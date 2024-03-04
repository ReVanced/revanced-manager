# ❔ Troubleshooting

In case you encounter any issues while using ReVanced Manager, this page will help you troubleshoot them.

- 💉 Common issues during or after patching

  - Make sure ReVanced Manager is up to date by following [🔄 Updating ReVanced Manager](2_3_updating.md)
  - You may have changed settings in ReVanced Manager that are not recommended to change. Please review the warnings that appear when adjusting these settings and reset them to their default configuration as explained in [⚙️ Configuring ReVanced Manager](2_4_settings.md)

- 🚫 App not installed as package conflicts with an existing package

  An existing installation of the app you're trying to patch conflicts with the patched app (i.e., signature mismatch or downgrade). Uninstall the existing app before installing the patched app.

  > 💡 Tip  
  > This may also be caused by changing settings in ReVanced Manager that are not recommended to change. Please review the warnings that appear when adjusting these settings and reset them to their default configuration as explained in [⚙️ Configuring ReVanced Manager](2_4_settings.md)

- ❗️ Error code `135`, `139` or `1` when patching the app

  - You may be trying to patch a split APK[^1]. This can fail under certain circumstances. If that is the case, patch a full APK
  - Your device may otherwise be unsupported. Please look at the [Prerequisites](0_prerequisites.md) page to see if your device is supported. Alternatively, you can use [ReVanced CLI](https://github.com/revanced/revanced-cli) to patch the app.

- 🚨 Patched app crashes on launch

  This may also be caused by changing settings in ReVanced Manager that are not recommended to change. Please review the warnings that appear when adjusting these settings and reset them to their default configuration as explained in [⚙️ Configuring ReVanced Manager](2_4_settings.md)

## ⏭️ What's next

The next page will teach you how to build ReVanced Manager from source.

Continue: [🔨 Building from source](4_building.md)

[^1]: https://developer.android.com/guide/app-bundle/app-bundle-format
