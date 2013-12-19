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
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XCallback;

public class RootCloak implements IXposedHookLoadPackage {
	private static XSharedPreferences pref;
	private Set<String> appSet;
	private boolean debugPref;
	private static final String FAKE_COMMAND = "FAKEJUNKCOMMAND";
	private static final String FAKE_FILE = "FAKEJUNKFILE";
	private static final String FAKE_PACKAGE = "FAKE.JUNK.PACKAGE";
	
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		loadPrefs(); // Load prefs for any app. This way we can determine if it matches the list of apps to hide root from.

		if (!(appSet.contains(lpparam.packageName))) { // If the app doesn't match, don't hook into anything, and just return.
			return;
		}
		
		ClassLoader classLoader = lpparam.classLoader;
		
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
					if(tempProcessName != null && (tempProcessName.contains("supersu") 
							|| tempProcessName.contains("superuser") 
							|| tempProcessName.contains("noshufou") 
							|| tempProcessName.contains("chainfire"))) {
						iter.remove();
						if (debugPref) {
							XposedBridge.log("Found and hid service: " + tempProcessName);
						}
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
					if(tempBaseActivity != null && (tempBaseActivity.contains("supersu") || tempBaseActivity.contains("superuser") 
							|| tempBaseActivity.contains("chainfire") || tempBaseActivity.contains("noshufou"))) {
						iter.remove();
						if (debugPref) {
							XposedBridge.log("Found and hid BaseActivity: " + tempBaseActivity);
						}
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
					if(tempProcessName != null && (tempProcessName.contains("supersu") || tempProcessName.contains("superuser") 
							|| tempProcessName.contains("chainfire") || tempProcessName.contains("noshufou"))) {
						iter.remove();
						if (debugPref) {
							XposedBridge.log("Found and hid process: " + tempProcessName);
						}
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
					
					if (firstParam.equalsIgnoreCase("pm")) {
						if (execArray.length >= 3 && execArray[1].equalsIgnoreCase("list") && execArray[2].equalsIgnoreCase("packages")) {
							// If getting list of packages, exclude anything with su
							param.args[0] = new String[] {"pm", "list", "packages", "-v", "grep", "-v", "\"su\""};
						} else if (execArray.length >= 3 && (execArray[1].equalsIgnoreCase("dump") || execArray[1].equalsIgnoreCase("path"))) {
							// If getting dumping or getting the path, don't let it work if it contains super, noshufou, or chainfire
							if (execArray[2].contains("super") || execArray[2].contains("noshufou") || execArray[2].contains("chainfire")) {
								param.args[0] = new String[] {execArray[0], execArray[1], FAKE_PACKAGE};
							}
						}
					} else if (firstParam.contains("ps")) { // This is a process list command
						//String[] newExecArray = new String[] {"ps", "|", "grep", "-v", "su"};
						param.args[0] = new String[] {"ps", "|", "grep", "-v", "\"su\""};
					} else if (firstParam.endsWith("su")) {
						execArray[0] = FAKE_COMMAND;
						param.args[0] = execArray;
					} else if (firstParam.contains("which")) { // This is a busybox which command
						if ((execArray.length >= 2)) {
							execArray[0] = "/system/xbin/" + FAKE_COMMAND;
							param.args[0] = execArray;
						}
					} else {
						for (String tempString : execArray) {
							if (tempString.contains("busybox")) {
								param.args[0] = new String[] {FAKE_COMMAND, FAKE_COMMAND};
								break;
							}
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
//				if (param.args[0] != null){
//					if (debugPref) {
//						XposedBridge.log("File: Found a File constructor: " + ((String)param.args[0]));
//					}
//				}
				
				if (((String)param.args[0]).endsWith("su")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor ending with su");
					}
					param.args[0] = "/system/xbin/" + FAKE_FILE;
				} else if (((String)param.args[0]).endsWith("busybox")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor ending with busybox");
					}
					param.args[0] = "/system/xbin/" + FAKE_FILE;
				} else if (((String)param.args[0]).contains("super") 
						|| ((String)param.args[0]).contains("Super") 
						|| ((String)param.args[0]).contains("noshufou") 
						|| ((String)param.args[0]).contains("chainfire")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with word super, noshufou, or chainfire");
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
//				if (param.args[0] != null && param.args[1] != null){
//					if (debugPref) {
//						XposedBridge.log("File: Found a File constructor: " + ((String)param.args[0]) + ", with: "+ ((String)param.args[1]));
//					}
//				}
				
				if (((String)param.args[1]).equalsIgnoreCase("su")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with filename su");
					}
					param.args[1] = FAKE_FILE;
				} else if (((String)param.args[1]).contains("busybox")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor ending with busybox");
					}
					param.args[1] = FAKE_FILE;
				} else if (((String)param.args[1]).contains("super") 
						|| ((String)param.args[1]).contains("Super") 
						|| ((String)param.args[1]).contains("noshufou") 
						|| ((String)param.args[1]).contains("chainfire")) {
					if (debugPref) {
						XposedBridge.log("File: Found a File constructor with word super, noshufou, or chainfire");
					}
					param.args[1] = FAKE_FILE + ".apk";
				}
			}
		});
		
		
		// Always return false when checking if debug is on
        XposedHelpers.findAndHookMethod("android.os.Debug", lpparam.classLoader, 
        		"isDebuggerConnected", XC_MethodReplacement.returnConstant(false));

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
		
	}

	public void loadPrefs() {
        pref = new XSharedPreferences(Common.PACKAGE_NAME, Common.PREFS);
        pref.makeWorldReadable();
        appSet = pref.getStringSet(Common.PACKAGE_NAME + Common.APP_LIST_KEY, new HashSet<String>());
        debugPref = pref.getBoolean(Common.PACKAGE_NAME + Common.DEBUG_KEY, false);
	}

}
