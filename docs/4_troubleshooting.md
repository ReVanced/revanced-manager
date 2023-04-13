# 🛟 Troubleshooting

<!-- Note -->
<!--

The visual guide have a commentary or a note for people needed accessibility

-->

Got stuck? Let's fix it together!

**Applicable to the latest version of ReVanced Manager**

## 🚦 App not installed as package conflicts with an existing package
That means that you have installed a equivalent or newer version of the application that you're trying to patch, If that seems wrong, **you can uninstall the application to solve the issue!**

## 🚦 Non-root install is not possible with the current patches selection

> **Note** <br>
> This troubleshooting step is applicable to YouTube, and YouTube Music

**You need to go to "Patcher", "Select patches", search for "MicroG Support" and enable them to fix the issue**, *why would that cause issue?* that because YouTube rely on Google Play Service (open-source alternative: MicroG), without them YouTube can't functions properly.

<details>
<summary>✨ Visual guide</summary>

https://user-images.githubusercontent.com/93124920/231701309-a7c383c1-66db-4b9e-9e64-62549cbdff52.mp4

<br>

> **Note** <br>
> The video show that you need to go to "Select patches", search for "MicroG Support" and enable them to fix the issue.

You're good to go!

</details>

## 🚦 Signature mismatch when updating the application
**Uninstall the application that you're trying to update to solve it**, this problem is caused by incorrect verification key being used when verifying the application.

## 🌋 Error code `135` when patching the application
**ReVanced Manager doesn't fully support patching with device that reported as ARM32, sorry for any inconvenience!**

<details>
<summary>Alternative solution</summary>

You can try using unofficial tools to patch the application, but do note that program marked with "unofficial" means that **we do not provide any support for them**.

- Official: [ReVanced CLI](https://github.com/revanced/revanced-cli) - Any (x64)
- Unofficial: [ReVanced Builder](https://github.com/reisxd/revanced-builder) - Windows (x64), macOS (x64), Linux (x64)

</details>

## ⏭️ What's next
The next section will teach you how to build Manager from source.

Continue: [🛠 Building from source](5_building-from-source.md)
