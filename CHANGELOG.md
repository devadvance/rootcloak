# Changelog
v2.1.1 - Adds an easier-to-see icon for adding apps, etc. (pylerSM)

v2.1.0 - Fixes a crash when an app attempts to read from Settings, adds translation strings and UI updates (pylerSM)

v2.0.1 - Prep for release to Xposed. No changes other than to increment version information.

v2.0 - Many fixes from hikaritenchi (mostly build-related), hide ADB (pylerSM).

v1.5 - Updated to Android Studio using Grade build system. Merged in changed from abdulmoeedammar. Updated package name due to various changes.

v1.4 - Added Slovak (thanks pylerSM) and Traditional Chinese(Taiwan) (thanks Eric850130) translations. Reorganized the code so its more legible. Added a lot of default apps and keywords, fixed the broken activities, and fixed how exec() was being handled (sh -c works a differently than I expected).

v1.3 - Added reset/clear confirmation dialogs. Added hooks for getInstalledPackages (thanks sirdigitalpython) and getPackageInfo. Added the ability to customize which commands and keywords RootCloak hides from the selected apps.

v1.2 - Cleaned up code. Added sh blocking. Added additional package names. Added ProcessBuilder blocking.

v1.1 - Added additional methods for hiding root. Refined existing methods to be more consistent.

v1.0 - Initital release.
