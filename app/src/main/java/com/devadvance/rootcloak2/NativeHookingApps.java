package com.devadvance.rootcloak2;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class NativeHookingApps extends PreferenceActivity {
    public static Context mContext;
    public static SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new Settings()).commit();
    }

    @SuppressWarnings("deprecation")
    public static class Settings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager()
                    .setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.native_hooking_apps);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            final Preference uninstallLibrary = (Preference) findPreference("uninstall_library");
            uninstallLibrary.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    uninstallLibrary();
                    return false;
                }
            });

            if (!mPrefs.getBoolean("installed", false)) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.library_installation_info)
                        .setTitle(R.string.library_installation)
                        .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                installLibrary();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                Intent refreshApps = new Intent(mContext, SettingsActivity.class);
                                mContext.sendBroadcast(refreshApps);
                            }
                        })
                        .show();
            }

            reloadAppsList();

        }

        @Override
        public void onPause() {
            super.onPause();

            // Set preferences file permissions to be world readable
            File prefsDir = new File(getActivity().getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        }

        public void installLibrary() {
            if (!Shell.SU.available()) {
                Toast.makeText(mContext, R.string.library_installation_failed, Toast.LENGTH_LONG).show();
            }

            Shell.SU.run("mkdir /data/local/");
            Shell.SU.run("chmod 755 /data/local/");
            String library = mContext.getApplicationInfo().nativeLibraryDir + File.separator + "librootcloak.so";
            Shell.SU.run("cp '" + library + "' /data/local/");

            String wrapper = "#!/system/bin/sh\n" +
                    "export LD_PRELOAD=/data/local/librootcloak.so\n" +
                    "exec $*\n";
            Shell.SU.run("echo '" + wrapper + "' > /data/local/rootcloak-wrapper.sh");

            Shell.SU.run("chmod 755 /data/local/librootcloak.so");
            Shell.SU.run("chmod 755 /data/local/rootcloak-wrapper.sh");

            Toast.makeText(mContext, R.string.successfully_installed, Toast.LENGTH_LONG).show();

            mPrefs.edit().putBoolean("installed", true).apply();
        }

        public void uninstallLibrary() {
            if (!Shell.SU.available()) {
                Toast.makeText(mContext, R.string.library_uninstallation_failed, Toast.LENGTH_LONG).show();
            }

            Shell.SU.run("rm /data/local/librootcloak.so");
            Shell.SU.run("rm /data/local/rootcloak-wrapper.sh");

            Toast.makeText(mContext, R.string.successfully_uninstalled, Toast.LENGTH_LONG).show();

            mPrefs.edit().putStringSet("native_hooking_apps", new HashSet<String>()).apply();
            mPrefs.edit().putBoolean("installed", false).apply();
            Intent refreshApps = new Intent(Common.REFRESH_APPS_INTENT);
            mContext.sendBroadcast(refreshApps);
        }


        public void reloadAppsList() {
            new LoadApps().execute();
        }


        public boolean isUserApp(ApplicationInfo appInfo) {
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return false;
            }
            return true;
        }

        public class LoadApps extends AsyncTask<Void, Void, Void> {
            MultiSelectListPreference nativeHookingApps = (MultiSelectListPreference) findPreference("native_hooking_apps");
            List<CharSequence> appNames = new ArrayList<CharSequence>();
            List<CharSequence> packageNames = new ArrayList<CharSequence>();
            PackageManager pm = mContext.getPackageManager();
            List<ApplicationInfo> packages = pm
                    .getInstalledApplications(PackageManager.GET_META_DATA);

            @Override
            protected void onPreExecute() {
                nativeHookingApps.setEnabled(false);
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                List<String[]> sortedApps = new ArrayList<String[]>();

                for (ApplicationInfo app : packages) {
                    if (isUserApp(app)) {
                        sortedApps.add(new String[]{
                                app.packageName,
                                app.loadLabel(mContext.getPackageManager())
                                        .toString()});
                    }
                }

                Collections.sort(sortedApps, new Comparator<String[]>() {
                    @Override
                    public int compare(String[] entry1, String[] entry2) {
                        return entry1[1].compareToIgnoreCase(entry2[1]);
                    }
                });

                for (int i = 0; i < sortedApps.size(); i++) {
                    appNames.add(sortedApps.get(i)[1]);
                    packageNames.add(sortedApps.get(i)[0]);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                CharSequence[] appNamesList = appNames
                        .toArray(new CharSequence[appNames.size()]);
                CharSequence[] packageNamesList = packageNames
                        .toArray(new CharSequence[packageNames.size()]);

                nativeHookingApps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(
                            Preference preference, Object newValue) {
                        Intent refreshApps = new Intent(Common.REFRESH_APPS_INTENT);
                        mContext.sendBroadcast(refreshApps);
                        return true;
                    }
                });

                nativeHookingApps.setEntries(appNamesList);
                nativeHookingApps.setEntryValues(packageNamesList);

                nativeHookingApps.setEnabled(true);
            }
        }

    }
}
