package com.devadvance.rootcloak;

import static de.robv.android.xposed.XposedHelpers.*;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.pm.ApplicationInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;

public class RootCloak implements IXposedHookLoadPackage {
	private static XSharedPreferences pref;
	private Set<String> appSet;
	private boolean debugPref;
	private static final String FAKE_COMMAND = "FAKEJUNKCOMMAND";
	private static final String FAKE_FILE = "FAKEJUNKFILE";
	
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		loadPrefs(); // Load prefs for any app. This way we can determine if it matches the list of apps to hide root from.

		if (!(appSet.contains(lpparam.packageName))) { // If the app doesn't match, don't hook into anything, and just return.
			return;
		}
		
		if (debugPref) {
			XposedBridge.log("Loaded app: " + lpparam.packageName);
		}
		
		findAndHookMethod("android.app.ApplicationPackageManager", lpparam.classLoader, "getInstalledApplications", int.class, new XC_MethodHook() {
			@SuppressWarnings("unchecked")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// this will be called after the clock was updated by the original method
				if (debugPref) {
					XposedBridge.log("Hooked getInstalledApplications");
				}

				List<ApplicationInfo> packages = (List<ApplicationInfo>) param.getResult();
				Iterator<ApplicationInfo> iter = packages.iterator();
				ApplicationInfo tempAppInfo;
				String tempPackageName;
				while(iter.hasNext()) {
					tempAppInfo = iter.next();
					tempPackageName = tempAppInfo.packageName;
					if(tempPackageName != null && (tempPackageName.contains("supersu") || tempPackageName.contains("superuser"))) {
						iter.remove();
						break;
					}
				}
				param.setResult(packages);
			}
		});
		
		findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningServices", int.class, new XC_MethodHook() {
			@SuppressWarnings("unchecked")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// this will be called after the clock was updated by the original method
				if (debugPref) {
					XposedBridge.log("Hooked getRunningServices");
				}

				List<ActivityManager.RunningServiceInfo> services = (List<RunningServiceInfo>) param.getResult();
				
				Iterator<RunningServiceInfo> iter = services.iterator();
				RunningServiceInfo tempService;
				String tempProcessName;
				while(iter.hasNext()) {
					tempService = iter.next();
					tempProcessName = tempService.process;
					if(tempProcessName != null && (tempProcessName.contains("supersu") || tempProcessName.contains("superuser"))) {
						iter.remove();
					}
				}
				param.setResult(services);
			}
		});
		
		findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningTasks", int.class, new XC_MethodHook() {
			@SuppressWarnings("unchecked")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// this will be called after the clock was updated by the original method
				if (debugPref) {
					XposedBridge.log("Hooked getRunningTasks");
				}

				List<ActivityManager.RunningTaskInfo> services = (List<RunningTaskInfo>) param.getResult();
				
				Iterator<RunningTaskInfo> iter = services.iterator();
				RunningTaskInfo tempTask;
				String tempBaseActivity;
				while(iter.hasNext()) {
					tempTask = iter.next();
					tempBaseActivity = tempTask.baseActivity.flattenToString();
					if(tempBaseActivity != null && (tempBaseActivity.contains("supersu") || tempBaseActivity.contains("superuser"))) {
						iter.remove();
					}
				}
				param.setResult(services);
			}
		});
		
		findAndHookMethod("android.app.ActivityManager", lpparam.classLoader, "getRunningAppProcesses", new XC_MethodHook() {
			@SuppressWarnings("unchecked")
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// this will be called after the clock was updated by the original method
				if (debugPref) {
					XposedBridge.log("Hooked getRunningAppProcesses");
				}

				List<ActivityManager.RunningAppProcessInfo> processes = (List<ActivityManager.RunningAppProcessInfo>) param.getResult();
				
				Iterator<RunningAppProcessInfo> iter = processes.iterator();
				RunningAppProcessInfo tempProcess;
				String tempProcessName;
				while(iter.hasNext()) {
					tempProcess = iter.next();
					tempProcessName = tempProcess.processName;
					if(tempProcessName != null && (tempProcessName.contains("supersu") || tempProcessName.contains("superuser"))) {
						iter.remove();
					}
				}
				param.setResult(processes);
			}
		});
		
		findAndHookMethod("java.lang.Runtime", lpparam.classLoader, "exec", String[].class, String[].class, File.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (debugPref) {
					XposedBridge.log("Hooked Runtime.exec");
				}
				String[] execArray = (String[]) param.args[0];
				if ((execArray != null)  && (execArray.length >= 1)) {
					String firstParam = execArray[0];
					if (debugPref) {
						String tempString = "Exec Command:";
						for (String temp : execArray) {
							tempString = tempString + " " + temp;
						}
						XposedBridge.log(tempString);
					}
					if (firstParam.endsWith("su")) {
						execArray[0] = FAKE_COMMAND;
						param.args[0] = execArray;
					} else if (firstParam.contains("ps")) { // This is a process list command
						//String[] newExecArray = new String[] {"ps", "|", "grep", "-v", "su"};
						param.args[0] = new String[] {"ps", "|", "grep", "-v", "\"su\""};
					} else if (firstParam.contains("which")) { // This is a busybox which command
						if ((execArray.length >= 2)) {
							execArray[0] = "/system/xbin/" + FAKE_COMMAND;
							param.args[0] = execArray;
						}
					}
					
					if (debugPref) {
						String tempString = "New Exec Command:";
						for (String temp : (String[])param.args[0]) {
							tempString = tempString + " " + temp;
						}
						XposedBridge.log(tempString);
					}
				} else {
					if (debugPref) {
						XposedBridge.log("Null or empty array on exec");
					}
				}
			}
		});

		Constructor<?> constructLayoutParams = findConstructorExact(java.io.File.class,
				String.class);
		XposedBridge.hookMethod(constructLayoutParams, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (((String)param.args[0]).equalsIgnoreCase("/system/xbin/su")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with xbin su");
					}
					param.args[0] = "/system/xbin/" + FAKE_FILE;
				} else if (((String)param.args[0]).equalsIgnoreCase("/system/app/Superuser.apk")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with app superuser.apk");
					}
					param.args[0] = "/system/app/" + FAKE_FILE + ".apk";
				} else if (((String)param.args[0]).contains("super")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with word super");
					}
					param.args[0] = "/system/app/" + FAKE_FILE + ".apk";
				}
			}
		});
		
		Constructor<?> extendedFileConstructor = findConstructorExact(java.io.File.class,
				String.class, String.class);
		XposedBridge.hookMethod(extendedFileConstructor, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (((String)param.args[1]).equalsIgnoreCase("su")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with filename su");
					}
					param.args[1] = FAKE_FILE;
				} else if (((String)param.args[1]).contains("super")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with word super in filename");
					}
					param.args[1] = FAKE_FILE + ".apk";
				}
			}
		});
	}

	public void loadPrefs() {
        pref = new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS);
        pref.makeWorldReadable();
        appSet = pref.getStringSet(Common.PACKAGE_NAME + Common.APP_LIST_KEY, new HashSet<String>());
        debugPref = pref.getBoolean(Common.PACKAGE_NAME + Common.DEBUG_KEY, false);
	}

}
