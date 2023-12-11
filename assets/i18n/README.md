# Nuke:tm:
Postprocessor solution to a very Crowdin problem.

Crowdin doesn't let us remove JSON key that have empty values (untranslated strings) in them, so we have to manually remove them. This is a problem because we have to do this every time we update the translations which is not ideal; so this tool was born.

> ![CAUTION]
> Some of the code are licensed under BSD 3-Clause License, please check the code for more information.

## Usage
Move to your desire directory and run

```bash
dart nuke.dart
```

and it will remove all the empty keys from the JSON files in the current folder.

> ![CAUTION]
> Some of the code are licensed under BSD 3-Clause License, please check the code for more information.
