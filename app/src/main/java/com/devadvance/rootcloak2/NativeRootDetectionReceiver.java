package com.devadvance.rootcloak2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import eu.chainfire.libsuperuser.Shell;

public class NativeRootDetectionReceiver extends BroadcastReceiver {
    private static RootUtil mRootShell;
    @Override
    public void onReceive(Context context, Intent intent) {
    	if (intent == null) {
            return;
        }

        mRootShell = new RootUtil();
        if (!mRootShell.isSU()) {
            return;
        }

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            upgradeLibrary(context);
            return;
        }
        
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Common.REFRESH_APPS_INTENT.equals(intent.getAction())) {
            return;
        }

        if (Common.REFRESH_APPS_INTENT.equals(intent.getAction())) {
            resetNativeHooks(context);
        }

        applyNativeHooks(context);
    }

    private void applyNativeHooks(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> nativeHookingApps = prefs.getStringSet("remove_native_root_detection_apps",
                new HashSet<String>());
        boolean libraryInstalled = prefs.getBoolean("native_library_installed", false);

        for (String app : nativeHookingApps) {
            String property = packageNameToProperty(app);
            String command = "setprop " + property + " 'logwrapper /data/local/rootcloak-wrapper.sh'";
            mRootShell.runCommand(command);
            mRootShell.runCommand("am force-stop " + app);
        }

        if (libraryInstalled && !nativeHookingApps.isEmpty()) {
            mRootShell.runCommand("chmod 755 /data/local/");
            mRootShell.runCommand("chmod 755 /data/local/librootcloak.so");
            mRootShell.runCommand("chmod 755 /data/local/rootcloak-wrapper.sh");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Allowing wrapping on Lollipop and newer
                WrappingSELinuxPolicy policy = new WrappingSELinuxPolicy();
                policy.inject();
                if (!policy.haveInjected()) {
                    // try to use alternative tool
                    mRootShell.runCommand("sepolicy-inject -s untrusted_app -t zygote -c fifo_file -p write -l");
                }
            }
        }
    }

    private void resetNativeHooks(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> nativeHookingApps = prefs.getStringSet("reset_native_root_detection_apps",
                new HashSet<String>());

        for (String app : nativeHookingApps) {
            String property = packageNameToProperty(app);
            String command = "setprop " + property + " ''";

            mRootShell.runCommand(command);
            mRootShell.runCommand("am force-stop " + app);
        }
    }

    private String packageNameToProperty(String packageName) {
        String property = "wrap." + packageName;
        if (property.length() > 31) {
            // Avoid creating an illegal property name when truncating.
            if (property.charAt(30) != '.') {
                property = property.substring(0, 31);
            } else {
                property = property.substring(0, 30);
            }
        }

        return property;
    }

    private void upgradeLibrary(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean libraryInstalled = prefs.getBoolean("native_library_installed", false);
        String library = context.getApplicationInfo().nativeLibraryDir + File.separator + "librootcloak.so";

        if (!libraryInstalled && !new File(library).exists()) {
            return;
        }

        mRootShell.runCommand("cp '" + library + "' /data/local/");
        mRootShell.runCommand("chmod 755 /data/local/librootcloak.so");
    }
    
    private class WrappingSELinuxPolicy extends eu.chainfire.libsuperuser.Policy {
        @Override
        protected String[] getPolicies() {
            return new String[]{
                    "allow untrusted_app zygote fifo_file { write }"
            };
        }
    }
}
