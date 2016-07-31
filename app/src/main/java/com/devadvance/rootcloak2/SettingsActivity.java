package com.devadvance.rootcloak2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {

    @SuppressLint("WorldReadableFiles")
    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return getApplicationContext().getSharedPreferences(Common.PREFS_SETTINGS, MODE_WORLD_READABLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager()
                .setSharedPreferencesMode(MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.preferences);

        Preference manageApps = findPreference("manage_apps");
        manageApps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, CustomizeApps.class);
                startActivity(intent);
                return false;
            }
        });

        Preference manageKeywords = findPreference("manage_keywords");
        manageKeywords.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, CustomizeKeywords.class);
                startActivity(intent);
                return false;
            }
        });

        Preference manageCommands = findPreference("manage_commands");
        manageCommands.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, CustomizeCommands.class);
                startActivity(intent);
                return false;
            }
        });

        Preference instructions = findPreference("instructions");
        instructions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String instructionsString = getString(R.string.instructions1) + "\n\n"
                        + getString(R.string.instructions2) + "\n\n"
                        + getString(R.string.instructions3) + "\n\n"
                        + getString(R.string.instructions4);

                String instructionsTitle = getString(R.string.instructions_title);
                new AlertDialog.Builder(SettingsActivity.this)
                        .setMessage(instructionsString)
                        .setTitle(instructionsTitle)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
                return false;
            }
        });

        Preference nativeRootDetection = findPreference("native_root_detection");
        nativeRootDetection.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, NativeRootDetection.class);
                startActivity(intent);
                return false;
            }
        });

        Preference appLauncherIcon = findPreference("app_launcher_icon");
        appLauncherIcon.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        int state = (boolean)newValue ?
                                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                        ComponentName alias = new ComponentName( SettingsActivity.this, "com.devadvance.rootcloak2.Settings" );
                        getPackageManager().setComponentEnabledSetting( alias, state, PackageManager.DONT_KILL_APP );
                        return true;
                    }
                });


        Preference about = findPreference("about");
        about.setSummary("RootCloak v" + BuildConfig.VERSION_NAME);
    }


}
