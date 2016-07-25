package com.devadvance.rootcloak2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.List;
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
        
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Common.REFRESH_APPS_INTENT.equals(intent.getAction())) {
            return;
        }

        if (Common.REFRESH_APPS_INTENT.equals(intent.getAction())) {
            resetNativeHooks(context);
        }
        applyNativeHooks(context);
    }

    public void applyNativeHooks(Context context) {
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

        if (libraryInstalled) {
            mRootShell.runCommand("chmod 755 /data/local/");
            mRootShell.runCommand("chmod 755 /data/local/librootcloak.so");
            mRootShell.runCommand("chmod 755 /data/local/rootcloak-wrapper.sh");
        }
    }

    public void resetNativeHooks(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm
                .getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : packages) {
            if (!Common.isUserApp(app)) {
                continue;
            }
            String property = packageNameToProperty(app.packageName);
            String command = "setprop " + property + " ''";
            mRootShell.runCommand(command);
            mRootShell.runCommand("am force-stop " + app.packageName);
        }
    }

    String packageNameToProperty(String packageName) {
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
}
