RootCloak Prime
===============

This is a module for Xposed Framework. Many thanks to devadvance for his work on RootCloak and RootCloak Plus.

This allows you to run apps that detect root without disabling root. You select from a list of your installed apps (or add a custom entry), and using a variety of methods, it will completely hide root from that app. This includes hiding the su binary, superuser/supersu apks, processes run by root, adb, and more.

**Requires:**
- Xposed Framework
- Root (otherwise why use this?)

**Instructions:**

1. Have Xposed Framework installed.
2. Install RootCloak Prime.
3. Enable RootCloak Prime in the Xposed app, in the Modules section.
4. Reboot your phone.
5. Open RootCloak Prime settings, go to Add/Remove Apps.
6. If the app you want to hide root from is not in the list, you must then press + to add it.
7. Exit RootCloak Prime settings. If the app you just added to the list was already running, you need to FORCE CLOSE it for the settings to take effect.
8. Run the app to see if root was successfully hidden.

For support, visit the official XDA thread: http://forum.xda-developers.com/showthread.php?t=2574647

**Where is RootCloak?**
devadvance has gone MIA for a few months, so I've taken it over and will be maintaining it as RootCloak Prime.

Copyright (C) 2013-2016+ Matt Joseph (devadvance), Jingran Wang (hikaritenchi), and contributors.

Licensed under the Apache License, Version 2.0. See LICENSE file for license information.

[![Build Status](https://travis-ci.org/devadvance/rootcloak.svg?branch=master)](https://travis-ci.org/devadvance/rootcloak)

Attribution:

The icon used for this module is a creative interpretation of a photo of the sculpture "Cloak of Conscience".

Both the sculpture and photo are by Anna Chromý. The photo is licensed under under the Creative Commons Attribution 3.0 Unported license (license text available here: http://creativecommons.org/licenses/by/3.0/deed.en) The original photo can be located here: http://en.wikipedia.org/wiki/File:Anna_Chromy_Cloak_Of_Conscience.jpg
