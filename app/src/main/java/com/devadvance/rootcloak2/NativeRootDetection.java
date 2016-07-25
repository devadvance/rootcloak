package com.devadvance.rootcloak2;

import android.app.ActionBar;
import android.app.AlertDialog;
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
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

@SuppressWarnings("deprecation")
public class NativeRootDetection extends PreferenceActivity {
    private static SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        getPreferenceManager()
                .setSharedPreferencesMode(MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.native_root_detection);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Preference uninstallLibrary = findPreference("uninstall_library");
        uninstallLibrary.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                uninstallLibrary();
                finish();
                return false;
            }
        });

        if (!mPrefs.getBoolean("native_library_installed", false)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.library_installation_info)
                    .setTitle(R.string.library_installation)
                    .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            installLibrary();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            finish();

                        }
                    })
                    .show();
        }

        reloadAppsList();
    }


    public void installLibrary() {
        String library = getApplicationInfo().nativeLibraryDir + File.separator + "librootcloak.so";

        if (!Shell.SU.available() || !new File(library).exists()) {
            Toast.makeText(this, R.string.library_installation_failed, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Shell.SU.run("mkdir /data/local/");
        Shell.SU.run("chmod 755 /data/local/");
        Shell.SU.run("cp '" + library + "' /data/local/");

        String wrapper = "#!/system/bin/sh\n" +
                "export LD_PRELOAD=/data/local/librootcloak.so\n" +
                "exec $*\n";
        Shell.SU.run("echo '" + wrapper + "' > /data/local/rootcloak-wrapper.sh");

        Shell.SU.run("chmod 755 /data/local/librootcloak.so");
        Shell.SU.run("chmod 755 /data/local/rootcloak-wrapper.sh");

        Toast.makeText(this, R.string.successfully_installed, Toast.LENGTH_LONG).show();

        mPrefs.edit().putBoolean("native_library_installed", true).apply();
    }

    public void uninstallLibrary() {
        if (!Shell.SU.available()) {
            Toast.makeText(this, R.string.library_uninstallation_failed, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Shell.SU.run("rm /data/local/librootcloak.so");
        Shell.SU.run("rm /data/local/rootcloak-wrapper.sh");

        Toast.makeText(this, R.string.successfully_uninstalled, Toast.LENGTH_LONG).show();

        mPrefs.edit().putStringSet("remove_native_root_detection_apps", new HashSet<String>()).apply();
        mPrefs.edit().putBoolean("native_library_installed", false).apply();
        Intent refreshApps = new Intent(Common.REFRESH_APPS_INTENT);
        sendBroadcast(refreshApps);
    }


    public void reloadAppsList() {
        new LoadApps().execute();
    }


    public class LoadApps extends AsyncTask<Void, Void, Void> {
        MultiSelectListPreference removeNativeRootDetectionApps = (MultiSelectListPreference) findPreference("remove_native_root_detection_apps");
        List<CharSequence> appNames = new ArrayList<>();
        List<CharSequence> packageNames = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm
                .getInstalledApplications(PackageManager.GET_META_DATA);

        @Override
        protected void onPreExecute() {
            removeNativeRootDetectionApps.setEnabled(false);
            removeNativeRootDetectionApps.setSummary(R.string.loading_apps);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            List<String[]> sortedApps = new ArrayList<>();

            for (ApplicationInfo app : packages) {
                if (Common.isUserApp(app)) {
                    sortedApps.add(new String[]{
                            app.packageName,
                            app.loadLabel(pm)
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

            removeNativeRootDetectionApps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                        Preference preference, Object newValue) {
                    Intent refreshApps = new Intent(Common.REFRESH_APPS_INTENT);
                    sendBroadcast(refreshApps);
                    return true;
                }
            });

            removeNativeRootDetectionApps.setEntries(appNamesList);
            removeNativeRootDetectionApps.setEntryValues(packageNamesList);

            removeNativeRootDetectionApps.setEnabled(true);
            removeNativeRootDetectionApps.setSummary(null);
        }
    }
}
