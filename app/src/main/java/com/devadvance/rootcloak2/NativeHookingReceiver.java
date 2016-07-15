package com.devadvance.rootcloak2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import eu.chainfire.libsuperuser.Shell;

public class NativeHookingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Common.REFRESH_APPS_INTENT.equals(intent.getAction())) {
            return;
        }
        applyNativeHooks(context);
    }

    public void applyNativeHooks(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Common.PREFS_NAME, Context.MODE_WORLD_READABLE);
        Set<String> nativeHookingApps = prefs.getStringSet("native_hooking_apps",
                new HashSet<String>());
        for (String app : nativeHookingApps) {
            String property = packageNameToProperty(app);
            String command = "setprop " + property + " 'logwrapper /data/local/rootcloak-wrapper.sh'";
            Shell.SU.run(command);
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
