# 🛟 Troubleshooting

### 💥 App not installed as package conflicts with an existing package

It could mean that the app you're trying to patch is already up to date or has been updated since the patch was released. If you think this is incorrect and you still want to patch the app, you can try uninstalling it and then installing the version you want to patch.

### 🚫 Non-root install is not possible with the current patches selection

Just open up Patcher, select the "Recommended" chip in "Select patches", and you should be good to go!

### 🪢 Signature mismatch when updating the application

You can try uninstalling the app that you're trying to update and then reinstalling it from ReVanced Manager. This should help ensure that the correct verification key is used when verifying the app *assuming if you don't accidentally delete keystore, I hate when that happens :)*.

### ❗️Error code `135` or `139` when patching the application

ReVanced Manager doesn't fully support patching with device that reported as ARM32, sorry for any inconvenience!

<details>
<summary><h4>⚙️ Alternative solution</h4></summary>

You can try using [ReVanced CLI](https://github.com/revanced/revanced-cli) to patch the application, consider reading the first [documentation](https://github.com/revanced/revanced-cli/blob/main/docs/README.md) or ask some questions in our official [Discord server](https://discord.gg/revanced)!

> **Warning**: We don't recommend that you download prebuilt APK from internet as they can be malicious.

</details>

### 🚨 Patched application from ReVanced Manager crashing

Just follow these steps in ReVanced Manager:

1. Select the app you want to patch
2. Check if your current version matches the recommended version
   1. If not, download the recommended APK version and save it to your device
   2. In select application, press the Floating Action Button called `🗂️ Storage` and locate your downloaded APK
3. Go to "Select patches" and press the `Recommended` or `Default` chip

That's it! You should now be good to go.

## ⏭️ What's next
The next section will teach you how to build Manager from source.

Continue: [🛠 Building from source](5_building-from-source.md)
