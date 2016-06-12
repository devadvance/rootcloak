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
    private static XSharedPreferences prefApps;
    private static XSharedPreferences prefKeywords;
    private static XSharedPreferences prefCommands;
    private static XSharedPreferences prefLibnames;
    private Set<String> appSet;
    private Set<String> keywordSet;
    private Set<String> commandSet;
    private Set<String> libnameSet;
    private boolean debugPref;
    private boolean isFirstRunApps;
    private boolean isFirstRunKeywords;
    private boolean isFirstRunCommands;
    private boolean isFirstRunLibnames;
    private boolean isRootCloakLoadingPref = false;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        loadPrefs(); // Load prefs for any app. This way we can determine if it matches the list of apps to hide root from.
//		if (debugPref) {
//			XposedBridge.log("Found app: " + lpparam.packageName);
//		}

        if (!(appSet.contains(lpparam.packageName))) { // If the app doesn't match, don't hook into anything, and just return.
            return;
        }

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
        
        findAndHookMethod("android.os.SystemProperties", lpparam.classLoader, "get", String.class , new XC_MethodHook() {
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

    private void initFile(final LoadPackageParam lpparam) {
        // Handles a version of the File constructor.
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

        // Another version of the File constructor.
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

        // Currently just for debugging purposes, not normally used
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

    private void initPackageManager(final LoadPackageParam lpparam) {
        // Hooks getInstalledApplications. For this method we will remove any keywords, such as supersu and superuser, out of the result list.
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledApplications");
                }

                List<ApplicationInfo> packages = (List<ApplicationInfo>) param.getResult();
                Iterator<ApplicationInfo> iter = packages.iterator();
                ApplicationInfo tempAppInfo;
                String tempPackageName;
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
                param.setResult(packages);
            }
        });

        // Hooks getInstalledPackages. For this method we will remove any keywords, such as supersu and superuser, out of the result list.
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getInstalledPackages");
                }

                List<PackageInfo> packages = (List<PackageInfo>) param.getResult();
                Iterator<PackageInfo> iter = packages.iterator();
                PackageInfo tempPackageInfo;
                String tempPackageName;
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
                param.setResult(packages);
            }
        });
    }


    private void initActivityManager(final LoadPackageParam lpparam) {
        // Hooks getPackageInfo. For this method we will prevent the package info from being obtained for any app in the list
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getPackageInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getPackageInfo");
                }
                String name = (String) param.args[0];

                if (name != null && stringContainsFromSet(name, keywordSet)) {
                    param.args[0] = FAKE_PACKAGE;
                    if (debugPref) {
                        XposedBridge.log("Found and hid package: " + name);
                    }
                }
            }
        });

        // Hooks getApplicationInfo. For this method we will prevent the package info from being obtained for any app in the list
        findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getApplicationInfo", String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                String name = (String) param.args[0];
                if (debugPref) {
                    XposedBridge.log("Hooked getApplicationInfo : " + name);
                }

                if (name != null && stringContainsFromSet(name, keywordSet)) {
                    param.args[0] = FAKE_APPLICATION;
                    if (debugPref) {
                        XposedBridge.log("Found and hid application: " + name);
                    }
                }
            }
        });
        // Hooks getRunningServices. For this method we will remove any keywords, such as supersu and superuser, out of the result list.
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningServices", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningServices");
                }

                List<ActivityManager.RunningServiceInfo> services = (List<RunningServiceInfo>) param.getResult();
                Iterator<RunningServiceInfo> iter = services.iterator();
                RunningServiceInfo tempService;
                String tempProcessName;
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
                param.setResult(services);
            }
        });

        // Hooks getRunningTasks. For this method we will remove any keywords, such as supersu and superuser, out of the result list.
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningTasks", int.class, new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningTasks");
                }

                List<ActivityManager.RunningTaskInfo> services = (List<RunningTaskInfo>) param.getResult();
                Iterator<RunningTaskInfo> iter = services.iterator();
                RunningTaskInfo tempTask;
                String tempBaseActivity;
                while (iter.hasNext()) {
                    tempTask = iter.next();
                    tempBaseActivity = tempTask.baseActivity.flattenToString();
                    if (tempBaseActivity != null && stringContainsFromSet(tempBaseActivity, keywordSet)) {
                        iter.remove();
                        if (debugPref) {
                            XposedBridge.log("Found and hid BaseActivity: " + tempBaseActivity);
                        }
                    }
                }
                param.setResult(services);
            }
        });

        // Hooks getRunningAppProcesses. For this method we will remove any keywords, such as supersu and superuser, out of the result list.
        findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningAppProcesses", new XC_MethodHook() {
            @SuppressWarnings("unchecked")
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked getRunningAppProcesses");
                }

                List<ActivityManager.RunningAppProcessInfo> processes = (List<ActivityManager.RunningAppProcessInfo>) param.getResult();
                Iterator<RunningAppProcessInfo> iter = processes.iterator();
                RunningAppProcessInfo tempProcess;
                String tempProcessName;
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
                param.setResult(processes);
            }
        });
    }

    private void initRuntime(final LoadPackageParam lpparam) {
        // Hooks the Runtime.exec() method. This is the only one that needs to be hooked because the other two versions of exec() just end up calling this one.
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked Runtime.exec");
                }
                String[] execArray = (String[]) param.args[0];
                if ((execArray != null) && (execArray.length >= 1)) {
                    String firstParam = execArray[0];
                    if (debugPref) {
                        String tempString = "Exec Command:";
                        for (String temp : execArray) {
                            tempString = tempString + " " + temp;
                        }
                        XposedBridge.log(tempString);
                    }

                    if (stringEndsWithFromSet(firstParam, commandSet)) {
                        if (debugPref) {
                            XposedBridge.log("Found blacklisted command at the end of the string: " + firstParam);
                        }

                        if (firstParam.equals("su") || firstParam.endsWith("/su")) { // If its su or ends with su (/bin/su, /xbin/su, etc)
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("pm") && (firstParam.equals("pm") || firstParam.endsWith("/pm"))) {
                            if (execArray.length >= 3 && execArray[1].equalsIgnoreCase("list") && execArray[2].equalsIgnoreCase("packages")) {
                                // If getting list of packages, exclude anything with su
                                //param.args[0] = new String[] {"pm", "list", "packages", "-v", "grep", "-v", "\"su\""};
                                param.args[0] = buildGrepArraySingle(execArray, true);
                            } else if (execArray.length >= 3 && (execArray[1].equalsIgnoreCase("dump") || execArray[1].equalsIgnoreCase("path"))) {
                                // If getting dumping or getting the path, don't let it work if it contains any of the keywords
                                if (stringContainsFromSet(execArray[2], keywordSet)) {
                                    param.args[0] = new String[]{execArray[0], execArray[1], FAKE_PACKAGE};
                                }
                            }
                        } else if (commandSet.contains("ps") && (firstParam.equals("ps") || firstParam.endsWith("/ps"))) { // This is a process list command
                            //param.args[0] = new String[] {"ps", "|", "grep", "-v", "\"su\""};
                            param.args[0] = buildGrepArraySingle(execArray, true);
                        } else if (commandSet.contains("which") && (firstParam.equals("which") || firstParam.endsWith("/which"))) { // This is a busybox which command
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("busybox") && anyWordEndingWithKeyword("busybox", execArray)) {
                            param.setThrowable(new IOException());
                        } else if (commandSet.contains("sh") && (firstParam.equals("sh") || firstParam.endsWith("/sh"))) {
                            param.setThrowable(new IOException());
                        } else {
                            param.setThrowable(new IOException());
                        }

                        if (debugPref && param.getThrowable() == null) {
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
        
        findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "loadLibrary", String.class, ClassLoader.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (debugPref) {
                    XposedBridge.log("Hooked loadLibrary");
                }
                String libname = (String) param.args[0];

                if (libname != null && stringContainsFromSet(libname, libnameSet)) {
                    param.setResult(null);
                    if (debugPref) {
                        XposedBridge.log("Loading of library " + name + " disabled.";
                    }
                }
            }
        });
    }

    private Boolean anyWordEndingWithKeyword(String keyword, String[] wordArray) {
        for (String tempString : wordArray) {
            if (tempString.endsWith(keyword)) {
                return true;
            }
        }
        return false;
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

    private void initSettingsGlobal(final LoadPackageParam lpparam) {
        // Hooks Settings.Global.getInt. For this method we will prevent the package info from being obtained for any app in the list
        findAndHookMethod(Settings.Global.class, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                String setting = (String) param.args[0];
                if (setting != null && Settings.Global.ADB_ENABLED.equals(setting)) {
                    param.setResult(0);
                    if (debugPref) {
                        XposedBridge.log("Hooked ADB debugging info, adb status is off");
                    }
                }
            }
        });
    }

    public void loadPrefs() {

        StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(old)
                .permitDiskReads()
                .permitDiskWrites()
                .build());

        isRootCloakLoadingPref = true;

        try {
            prefApps = new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS_APPS);
            prefApps.makeWorldReadable();

            prefKeywords = new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS_KEYWORDS);
            prefKeywords.makeWorldReadable();

            prefCommands = new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS_COMMANDS);
            prefCommands.makeWorldReadable();
            
            prefLibnames = new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS_LIBNAMES);
            prefLibnames.makeWorldReadable();

            debugPref = prefApps.getBoolean(Common.PACKAGE_NAME + Common.DEBUG_KEY, false); // This enables/disables printing of debug messages

            isFirstRunApps = prefApps.getBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, true); // Load boolean that determines if this is the first run since being installed.
            isFirstRunKeywords = prefKeywords.getBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, true); // Load boolean that determines if this is the first run since being installed.
            isFirstRunCommands = prefCommands.getBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, true); // Load boolean that determines if this is the first run since being installed.
            isFirstRunLibnames = prefLibnames.getBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, true); // Load boolean that determines if this is the first run since being installed.

            appSet = prefApps.getStringSet(Common.PACKAGE_NAME + Common.APP_LIST_KEY, new HashSet<String>()); // Load appSet. This is the set of apps to hide root from.
            keywordSet = prefKeywords.getStringSet(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY, new HashSet<String>()); // Load keywordSet.
            commandSet = prefCommands.getStringSet(Common.PACKAGE_NAME + Common.COMMAND_SET_KEY, new HashSet<String>()); // Load commandSet.
            libnameSet = prefLibnames.getStringSet(Common.PACKAGE_NAME + Common.COMMAND_SET_KEY, new HashSet<String>()); // Load libnameSet.

            // If the settings for any of the sets have never been modified, possibly need to use default sets.
            if (isFirstRunApps && appSet.isEmpty()) {
                appSet = Common.DEFAULT_APPS_SET;
            }
            if (isFirstRunKeywords && keywordSet.isEmpty()) {
                keywordSet = Common.DEFAULT_KEYWORD_SET;
            }
            if (isFirstRunCommands && commandSet.isEmpty()) {
                commandSet = Common.DEFAULT_COMMAND_SET;
            }
            if (isFirstRunLibnames && libnameSet.isEmpty()) {
                libnameSet = Common.DEFAULT_LIBNAME_SET;
            }
        } finally {
            StrictMode.setThreadPolicy(old);

            isRootCloakLoadingPref = false;
        }

    }

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
        for (String temp : keywordSet) {
            builder.append(" | grep -v ");
            builder.append(temp);
        }
        //originalList.addAll(Common.DEFAULT_GREP_ENTRIES);
        originalList.add(builder.toString());
        return originalList.toArray(new String[0]);
    }

}
