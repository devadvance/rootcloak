package com.devadvance.rootcloak2;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.StrictMode;
import android.provider.Settings;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;

public class RootCloak implements IXposedHookLoadPackage {
    private static final String FAKE_COMMAND = "FAKEJUNKCOMMAND";
    private static final String FAKE_FILE = "FAKEJUNKFILE";
    private static final String FAKE_PACKAGE = "FAKE.JUNK.PACKAGE";
    private static final String FAKE_APPLICATION = "FAKE.JUNK.APPLICATION";
    private Set<String> appSet;
    private Set<String> keywordSet;
    private Set<String> commandSet;
    private Set<String> libnameSet;
    private boolean debugPref;
    private boolean isRootCloakLoadingPref = false;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        isRootCloakLoadingPref = true;
        Set<String> tmpAppSet = loadSetFromPrefs(Common.APPS); // Load prefs for any app. This way we can determine if it matches the list of apps to hide root from.
        if (!(tmpAppSet.contains(lpparam.packageName))) { // If the app doesn't match, don't hook into anything, and just return.
            isRootCloakLoadingPref = false;
        } else {
            appSet = tmpAppSet;
            keywordSet = loadSetFromPrefs(Common.KEYWORDS);
            commandSet = loadSetFromPrefs(Common.COMMANDS);
            libnameSet = loadSetFromPrefs(Common.LIBRARIES);
            initSettings();
            isRootCloakLoadingPref = false;

            if (debugPref) {
                XposedBridge.log("Loaded app: " + lpparam.packageName);
            }

            // Do all of the hooks
            initOther(lpparam);
            initFile(lpparam);
            initPackageManager(lpparam);
            initActivityManager(lpparam);
            initRuntime(lpparam);
            initProcessBuilder(lpparam);
            initSettingsGlobal(lpparam);
        }
    }

    /**
     * Handles a bunch of miscellaneous hooks.
     * @param lpparam Wraps information about the app being loaded.
     */
    private void initOther(final LoadPackageParam lpparam) {
        // Always return false when checking if debug is on
        XposedHelpers.findAndHookMethod("android.os.Debug", lpparam.classLoader, "isDebuggerConnected", XC_MethodReplacement.returnConstant(false));

        // If test-keys, change to release-keys
        if (!Build.TAGS.equals("release-keys")) {
            if (debugPref) {
                XposedBridge.log("Original build tags: " + Build.TAGS);
            }
            XposedHelpers.setStaticObjectField(android.os.Build.class, "TAGS", "release-keys");
            if (debugPref) {
                XposedBridge.log("New build tags: " + Build.TAGS);
            }
        } else {
            if (debugPref) {
                XposedBridge.log("No need to change build tags: " + Build.TAGS);
            }
        }

        // Tell the app that SELinux is enforcing, even if it is not.
        findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "get", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (((String) param.args[0]).equals("ro.build.selinux")) {
                    param.setResult("1");
                    if (debugPref) {
                        XposedBridge.log("SELinux is enforced.");
                    }
                }
            }
        });

        // Hide the Xposed classes from the app
        findAndHookMethod("java.lang.Class", lpparam.classLoader, "forName", String.class, boolean.class, ClassLoader.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String classname = (String) param.args[0];

                if (classname != null && (classname.equals("de.robv.android.xposed.XposedBridge") || classname.equals("de.robv.android.xposed.XC_MethodReplacement"))) {
                    param.setThrowable(new ClassNotFoundException());
                    if (debugPref) {
                        XposedBridge.log("Found and hid Xposed class name: " + classname);
                    }
                }
            }
        });
    }

    /**
     * Handles all of the hooking related to java.io.File.
     * @param lpparam Wraps information about the app being loaded.
     */
    private void initFile(final LoadPackageParam lpparam) {
        /**
         * Hooks a version of the File constructor.
         * An app may use File to check for the existence of files like su, busybox, or others.
         */
        Constructor<?> constructLayoutParams = findConstructorExact(java.io.File.class, String.class);
        XposedBridge.hookMethod(constructLayoutParams, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor: " + ((String) param.args[0]));
                    }
                }

                if (isRootCloakLoadingPref) {
                    // RootCloak is trying to load it's preferences, we shouldn't block this.
                    return;
                }

                if (((String) param.args[0]).endsWith("su")) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor ending with su");
                    }
                    param.args[0] = "/system/xbin/" + FAKE_FILE;
                } else if (((String) param.args[0]).endsWith("busybox")) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor ending with busybox");
                    }
                    param.args[0] = "/system/xbin/" + FAKE_FILE;
                } else if (stringContainsFromSet(((String) param.args[0]), keywordSet)) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor with word super, noshufou, or chainfire");
                    }
                    param.args[0] = "/system/app/" + FAKE_FILE + ".apk";
                }
            }
        });

        /**
         * Hooks a version of the File constructor.
         * An app may use File to check for the existence of files like su, busybox, or others.
         */
        Constructor<?> extendedFileConstructor = findConstructorExact(java.io.File.class, String.class, String.class);
        XposedBridge.hookMethod(extendedFileConstructor, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null && param.args[1] != null) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor: " + ((String) param.args[0]) + ", with: " + ((String) param.args[1]));
                    }
                }

                if (isRootCloakLoadingPref) {
                    // RootCloak is trying to load it's preferences, we shouldn't block this.
                    return;
                }

                if (((String) param.args[1]).equalsIgnoreCase("su")) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor with filename su");
                    }
                    param.args[1] = FAKE_FILE;
                } else if (((String) param.args[1]).contains("busybox")) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor ending with busybox");
                    }
                    param.args[1] = FAKE_FILE;
                } else if (stringContainsFromSet(((String) param.args[1]), keywordSet)) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a File constructor with word super, noshufou, or chainfire");
                    }
                    param.args[1] = FAKE_FILE + ".apk";
                }
            }
        });

        /**
         * Hooks a version of the File constructor that uses a URI.
         * An app may use File to check for the existence of files like su, busybox, or others.
         * NOTE: Currently just for debugging purposes, not normally used.
         */
        Constructor<?> uriFileConstructor = findConstructorExact(java.io.File.class, URI.class);
        XposedBridge.hookMethod(uriFileConstructor, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] != null) {
                    if (debugPref) {
                        XposedBridge.log("File: Found a URI File constructor: " + ((URI) param.args[0]).toString());
                    }
                }
            }
        });
    }

    /**
     * Handles all of the hooking related to the PackageManager.
     * @param lpparam Wraps information about the app being loaded.
     */
    private void initPackageManager(final LoadPackageParam lpparam) {
        /**
         * Hooks getInstalledApplications within the PackageManager.
         * An app can check for other apps this way. In the context of a rooted device, an app may look for SuperSU, Xposed, Superuser, or others.
         * Results that match entries in the keywordSet are hidden.
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getIntalledApplications is called
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledApplications");
                }

                List<ApplicationInfo> packages = (List<ApplicationInfo>) param.getResult(); // Get the results from the method call
                Iterator<ApplicationInfo> iter = packages.iterator();
                ApplicationInfo tempAppInfo;
                String tempPackageName;

                // Iterate through the list of ApplicationInfo and remove any mentions that match a keyword in the keywordSet
                while (iter.hasNext()) {
                    tempAppInfo = iter.next();
                    tempPackageName = tempAppInfo.packageName;
                    if (tempPackageName != null && stringContainsFromSet(tempPackageName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid package: " + tempPackageName);
                        }
                    }
                }

                param.setResult(packages); // Set the return value to the clean list
            }
        });

        /**
         * Hooks getInstalledPackages within the PackageManager.
         * An app can check for other apps this way. In the context of a rooted device, an app may look for SuperSU, Xposed, Superuser, or others.
         * Results that match entries in the keywordSet are hidden.
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getInstalledPackages is called
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledPackages");
                }

                List<PackageInfo> packages = (List<PackageInfo>) param.getResult(); // Get the results from the method call
                Iterator<PackageInfo> iter = packages.iterator();
                PackageInfo tempPackageInfo;
                String tempPackageName;

                // Iterate through the list of PackageInfo and remove any mentions that match a keyword in the keywordSet
                while (iter.hasNext()) {
                    tempPackageInfo = iter.next();
                    tempPackageName = tempPackageInfo.packageName;
                    if (tempPackageName != null && stringContainsFromSet(tempPackageName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid package: " + tempPackageName);
                        }
                    }
                }

                param.setResult(packages); // Set the return value to the clean list
            }
        });

        /**
         * Hooks getPackageInfo within the PackageManager.
         * An app can check for other packages this way. We hook before getPackageInfo is called.
         * If the package being looked at matches an entry in the keywordSet, then substitute a fake package name.
         * This will ultimately throw a PackageManager.NameNotFoundException.
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getPackageInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getPackageInfo");
                }
                String name = (String) param.args[0];

                if (name != null && stringContainsFromSet(name, keywordSet)) {
                    param.args[0] = FAKE_PACKAGE; // Set a fake package name
                    if (debugPref) {
                        XposedBridge.log("Found and hid package: " + name);
                    }
                }
            }
        });

        /**
         * Hooks getApplicationInfo within the PackageManager.
         * An app can check for other applications this way. We hook before getApplicationInfo is called.
         * If the application being looked at matches an entry in the keywordSet, then substitute a fake application name.
         * This will ultimately throw a PackageManager.NameNotFoundException.
         */
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getApplicationInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                String name = (String) param.args[0];
                if (debugPref) {
                    XposedBridge.log("Hooked getApplicationInfo : " + name);
                }

                if (name != null && stringContainsFromSet(name, keywordSet)) {
                    param.args[0] = FAKE_APPLICATION; // Set a fake application name
                    if (debugPref) {
                        XposedBridge.log("Found and hid application: " + name);
                    }
                }
            }
        });
    }

    /**
     * Handles all of the hooking related to the ActivityManager.
     * @param lpparam Wraps information about the app being loaded.
     */
    private void initActivityManager(final LoadPackageParam lpparam) {
        /**
         * Hooks getRunningServices within the ActivityManager.
         * An app can check for other apps this way. In the context of a rooted device, an app may look for SuperSU, Xposed, Superuser, or others.
         * Results that match entries in the keywordSet are hidden.
         */
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningServices", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getRunningServices is called
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningServices");
                }

                List<ActivityManager.RunningServiceInfo> services = (List<RunningServiceInfo>) param.getResult(); // Get the results from the method call
                Iterator<RunningServiceInfo> iter = services.iterator();
                RunningServiceInfo tempService;
                String tempProcessName;

                // Iterate through the list of RunningServiceInfo and remove any mentions that match a keyword in the keywordSet
                while (iter.hasNext()) {
                    tempService = iter.next();
                    tempProcessName = tempService.process;
                    if (tempProcessName != null && stringContainsFromSet(tempProcessName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid service: " + tempProcessName);
                        }
                    }
                }

                param.setResult(services); // Set the return value to the clean list
            }
        });

        /**
         * Hooks getRunningTasks within the ActivityManager.
         * An app can check for other apps this way. In the context of a rooted device, an app may look for SuperSU, Xposed, Superuser, or others.
         * Results that match entries in the keywordSet are hidden.
         */
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningTasks", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getRunningTasks is called
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningTasks");
                }

                List<ActivityManager.RunningTaskInfo> services = (List<RunningTaskInfo>) param.getResult(); // Get the results from the method call
                Iterator<RunningTaskInfo> iter = services.iterator();
                RunningTaskInfo tempTask;
                String tempBaseActivity;

                // Iterate through the list of RunningTaskInfo and remove any mentions that match a keyword in the keywordSet
                while (iter.hasNext()) {
                    tempTask = iter.next();
                    tempBaseActivity = tempTask.baseActivity.flattenToString(); // Need to make it a string for comparison
                    if (tempBaseActivity != null && stringContainsFromSet(tempBaseActivity, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid BaseActivity: " + tempBaseActivity);
                        }
                    }
                }

                param.setResult(services); // Set the return value to the clean list
            }
        });

        /**
         * Hooks getRunningAppProcesses within the ActivityManager.
         * An app can check for other apps this way. In the context of a rooted device, an app may look for SuperSU, Xposed, Superuser, or others.
         * Results that match entries in the keywordSet are hidden.
         */
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningAppProcesses", new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable { // Hook after getRunningAppProcesses is called
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningAppProcesses");
                }

                List<ActivityManager.RunningAppProcessInfo> processes = (List<ActivityManager.RunningAppProcessInfo>) param.getResult(); // Get the results from the method call
                Iterator<RunningAppProcessInfo> iter = processes.iterator();
                RunningAppProcessInfo tempProcess;
                String tempProcessName;

                // Iterate through the list of RunningAppProcessInfo and remove any mentions that match a keyword in the keywordSet
                while (iter.hasNext()) {
                    tempProcess = iter.next();
                    tempProcessName = tempProcess.processName;
                    if (tempProcessName != null && stringContainsFromSet(tempProcessName, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid process: " + tempProcessName);
                        }
                    }
                }

                param.setResult(processes); // Set the return value to the clean list
            }
        });
    }

    /**
     * Handles all of the hooking related to java.lang.Runtime, which is used for executing other programs or shell commands.
     * @param lpparam Wraps information about the app being loaded.
     */
    private void initRuntime(final LoadPackageParam lpparam) {
        /**
         * Hooks exec() within java.lang.Runtime.
         * This is the only version that needs to be hooked, since all of the others are "convenience" variations.
         * This takes the form: exec(String[] cmdarray, String[] envp, File dir).
         * There are a lot of different ways that exec can be used to check for a rooted device. See the comments within this section for more details.
         */
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked Runtime.exec");
                }

                String[] execArray = (String[]) param.args[0]; // Grab the tokenized array of commands
                if ((execArray != null) && (execArray.length >= 1)) { // Do some checking so we don't break anything
                    String firstParam = execArray[0]; // firstParam is going to be the main command/program being run
                    if (debugPref) { // If debugging is on, print out what is being called
                        String tempString = "Exec Command:";
                        for (String temp : execArray) {
                            tempString = tempString + " " + temp;
                        }
                        XposedBridge.log(tempString);
                    }

                    if (stringEndsWithFromSet(firstParam, commandSet)) { // Check if the firstParam is one of the keywords we want to filter
                        if (debugPref) {
                            XposedBridge.log("Found blacklisted command at the end of the string: " + firstParam);
                        }

                        // A bunch of logic follows since the solution depends on which command is being called
                        // TODO: ***Clean up this logic***
                        if (firstParam.equals("su") || firstParam.endsWith("/su")) { // If its su or ends with su (/bin/su, /xbin/su, etc)
                            param.setThrowable(new IOException()); // Throw an exception to imply the command was not found
                        } else if (commandSet.contains("pm") && (firstParam.equals("pm") || firstParam.endsWith("/pm"))) {
                            // Trying to run the pm (package manager) using exec. Now let's deal with the subcases
                            if (execArray.length >= 3 && execArray[1].equalsIgnoreCase("list") && execArray[2].equalsIgnoreCase("packages")) {
                                // Trying to list out all of the packages, so we will filter out anything that matches the keywords
                                //param.args[0] = new String[] {"pm", "list", "packages", "-v", "grep", "-v", "\"su\""};
                                param.args[0] = buildGrepArraySingle(execArray, true);
                            } else if (execArray.length >= 3 && (execArray[1].equalsIgnoreCase("dump") || execArray[1].equalsIgnoreCase("path"))) {
                                // Trying to either dump package info or list the path to the APK (both will tell the app that the package exists)
                                // If it matches anything in the keywordSet, stop it from working by using a fake package name
                                if (stringContainsFromSet(execArray[2], keywordSet)) {
                                    param.args[0] = new String[]{execArray[0], execArray[1], FAKE_PACKAGE};
                                }
                            }
                        } else if (commandSet.contains("ps") && (firstParam.equals("ps") || firstParam.endsWith("/ps"))) { // This is a process list command
                            // Trying to run the ps command to see running processes (e.g. looking for things running as su or daemonsu). Filter this out.
                            param.args[0] = buildGrepArraySingle(execArray, true);
                        } else if (commandSet.contains("which") && (firstParam.equals("which") || firstParam.endsWith("/which"))) {
                            // Busybox "which" command. Thrown an excepton
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("busybox") && anyWordEndingWithKeyword("busybox", execArray)) {
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("sh") && (firstParam.equals("sh") || firstParam.endsWith("/sh"))) {
                            param.setThrowable(new IOException());
                        } else {
                            param.setThrowable(new IOException());
                        }

                        if (debugPref && param.getThrowable() == null) { // Print out the new command if debugging is on
                            String tempString = "New Exec Command:";
                            for (String temp : (String[]) param.args[0]) {
                                tempString = tempString + " " + temp;
                            }
                            XposedBridge.log(tempString);
                        }
                    }


                } else {
                    if (debugPref) {
                        XposedBridge.log("Null or empty array on exec");
                    }
                }
            }
        });

        /**
         * Hooks loadLibrary() within java.lang.Runtime.
         * There are libraries specifically built to check for root. This helps us block those and others.
         */
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "loadLibrary", String.class, ClassLoader.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked loadLibrary");
                }
                String libname = (String) param.args[0];

                if (libname != null && stringContainsFromSet(libname, libnameSet)) { // If we found one of the libraries we block, let's prevent it from being loaded
                    param.setResult(null);
                    if (debugPref) {
                        XposedBridge.log("Loading of library " + libname + " disabled.");
                    }
                }
            }
        });
    }

    private void initProcessBuilder(final LoadPackageParam lpparam) {
        // Hook ProcessBuilder and prevent running of certain commands
        Constructor<?> processBuilderConstructor2 = findConstructorExact(java.lang.ProcessBuilder.class, String[].class);
        XposedBridge.hookMethod(processBuilderConstructor2, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Hooked ProcessBuilder");
                if (param.args[0] != null) {
                    String[] cmdArray = (String[]) param.args[0];
                    if (debugPref) {
                        String tempString = "ProcessBuilder Command:";
                        for (String temp : cmdArray) {
                            tempString = tempString + " " + temp;
                        }
                        XposedBridge.log(tempString);
                    }
                    if (stringEndsWithFromSet(cmdArray[0], commandSet)) {
                        cmdArray[0] = FAKE_COMMAND;
                        param.args[0] = cmdArray;
                    }

                    if (debugPref) {
                        String tempString = "New ProcessBuilder Command:";
                        for (String temp : (String[]) param.args[0]) {
                            tempString = tempString + " " + temp;
                        }
                        XposedBridge.log(tempString);
                    }
                }
            }
        });
    }

    /**
     * Hooks for the device settings.
     */
    private void initSettingsGlobal(final LoadPackageParam lpparam) {
        // Hooks Settings.Global.getInt. For this method we will prevent the package info from being obtained for any app in the list
        findAndHookMethod(Settings.Global.class, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                String setting = (String) param.args[1];
                if (setting == null) return;
                if (Settings.Global.ADB_ENABLED.equals(setting)) { // Hide ADB being on from an app
                    param.setResult(0);
                    if (debugPref) {
                        XposedBridge.log("Hooked ADB debugging info, adb status is off");
                    }
                }

                if (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED.equals(setting)) { // Hide development options being on from an app
                    param.setResult(0);
                    if (debugPref) {
                        XposedBridge.log("Hooked development options info, development options status is off");
                    }
                }
            }
        });
    }

    private void initSettings() {
        final XSharedPreferences prefSettings = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.PREFS_SETTINGS);
        prefSettings.makeWorldReadable();
        debugPref = prefSettings.getBoolean(Common.DEBUG_KEY, false);
    }

    /**
     * Load all preferences, such as keywords, commands, etc.
     */
    private static Set<String> loadSetFromPrefs(Common.PrefSet type) {
        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                .permitDiskReads()
                .permitDiskWrites()
                .build());

        final Set<String> newSet = new HashSet<>();
        try {
            final XSharedPreferences loadedPrefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, type.getPrefKey());
            loadedPrefs.makeWorldReadable();

            final boolean isFirstRun = loadedPrefs.getBoolean(Common.FIRST_RUN_KEY, true); // Load boolean that determines if this is the first run since being installed.

            // Loaded set is IMMUTABLE. We need to copy the values out of it.
            final Set<String> loadedSet = loadedPrefs.getStringSet(type.getSetKey(), null);
            if (loadedSet != null) {
                newSet.addAll(loadedSet);
            } else if (isFirstRun) {
                newSet.addAll(type.getDefaultSet());
            }
        } finally {
            StrictMode.setThreadPolicy(old);
        }

        return newSet;
    }

    /* ********************
     * Helper method section
     * ********************/

    // TODO: Clean up these helper methods?

    /**
     * Takes a keyword string and an array of strings, and checks to see if any values in the array end with the keyword
     * @param keyword
     * @param wordArray
     * @return boolean indicating if any value from the array ends in the keyword
     */
    private Boolean anyWordEndingWithKeyword(String keyword, String[] wordArray) {
        for (String tempString : wordArray) {
            if (tempString.endsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes a string and a set of strings, and checks to see if the base string contains any of the values from the set.
     * @param base a string that we want to check the contents of
     * @param values a set of strings to check the base for
     * @return boolean indicating if the string contains any value from the set of strings
     */
    public boolean stringContainsFromSet(String base, Set<String> values) {
        if (base != null && values != null) {
            for (String tempString : values) {
                if (base.matches(".*(\\W|^)" + tempString + "(\\W|$).*")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Takes a string and a set of strings, and checks to see if the base string ends with any of the values from the set.
     * @param base a string that we check the end of
     * @param values a set of strings to check the end of the base for
     * @return boolean indicating if the string ends with any value from the set of strings
     */
    public boolean stringEndsWithFromSet(String base, Set<String> values) {
        if (base != null && values != null) {
            for (String tempString : values) {
                if (base.endsWith(tempString)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This helper takes a command and appends a lot of greps to it. The idea is to filter out anything that matches the keywordSet.
     * @param original the original command array
     * @param addSH whether or not to add sh -c to the command array
     * @return a new String array that has the grep filtering added
     */
    private String[] buildGrepArraySingle(String[] original, boolean addSH) {
        StringBuilder builder = new StringBuilder();
        ArrayList<String> originalList = new ArrayList<String>();
        if (addSH) {
            originalList.add("sh");
            originalList.add("-c");
        }
        for (String temp : original) {
            builder.append(" ");
            builder.append(temp);
        }
        //originalList.addAll(Arrays.asList(original));
        // ***TODO: Switch to using -e with alternation***
        for (String temp : keywordSet) {
            builder.append(" | grep -v ");
            builder.append(temp);
        }
        //originalList.addAll(Common.DEFAULT_GREP_ENTRIES);
        originalList.add(builder.toString());
        return originalList.toArray(new String[0]);
    }

}
