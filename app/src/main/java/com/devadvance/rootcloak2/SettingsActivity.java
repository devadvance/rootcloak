package com.devadvance.rootcloak2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;


@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
    public static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        getPreferenceManager()
                .setSharedPreferencesMode(MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.preferences);

        Preference manageApps = findPreference("manage_apps");
        manageApps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(mContext, CustomizeApps.class);
                startActivity(intent);
                return false;
            }
        });

        Preference manageKeywords = findPreference("manage_keywords");
        manageKeywords.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(mContext, CustomizeKeywords.class);
                startActivity(intent);
                return false;
            }
        });

        Preference manageCommands = findPreference("manage_commands");
        manageCommands.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(mContext, CustomizeCommands.class);
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
                Intent intent = new Intent(mContext, NativeRootDetection.class);
                startActivity(intent);
                return false;
            }
        });

        Preference about = findPreference("about");
        about.setSummary("RootCloak v" + BuildConfig.VERSION_NAME);
    }


}
