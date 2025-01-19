# ❔ Troubleshooting

In case you encounter any issues while using ReVanced Manager, please refer to this page for possible solutions.

- 💉 Patching fails with an error

  Make sure ReVanced Manager is up to date by following [🔄 Updating ReVanced Manager](2_3_updating.md) and select the **Default** button when choosing patches.
  
- 💥 App not installed as package conflicts with an existing package

  An existing installation of the app you're trying to patch is conflicting with the patched app. Uninstall the existing app before installing the patched app.

- 🥛 Out Of Memory error

  This indicate that your device doesn't have enough memory to patch the app. Try patching on another device that's have more memory.

  In the meantime there's a remedy fix by going to ⚙️ Settings -> Advanced -> toggle "Run Patcher in another process (experimental)" on

- ❗️ Error code `135`, `139` or `1` when patching the app

  Your device is not supported. Refer to the [Prerequisites](0_prerequisites.md) page for supported devices.

  Alternatively, you can use [ReVanced CLI](https://github.com/revanced/revanced-cli) to patch the app.

- 🚫 Non-root install is not possible with the current patches selection

  Select the **Default** button when choosing patches.

- 🚨 Patched app crashes on launch

  Select the **Default** button when choosing patches.
