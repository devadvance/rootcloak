package com.devadvance.rootcloak2;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomizeApps extends PreferenceActivity {

    SharedPreferences sharedPref;
    Set<String> appSet = new HashSet<>();
    String[] appList;
    boolean isFirstRun;

    ProgressDialog progressDialog;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_apps);
        // Show the Up button in the action bar.
        setupActionBar();

        sharedPref = Common.APPS.getSharedPreferences(this);

        loadList();

    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        final int positionFinal = position;
        new AlertDialog.Builder(CustomizeApps.this)
                .setTitle(R.string.remove_app_title)
                .setMessage(R.string.remove_app_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeApp(positionFinal);
                        loadList();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();

    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {

        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissProgressDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.customize_apps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new:
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage(getString(R.string.loading_apps));
                }
                progressDialog.show();

                // Load application list on a background thread while the ProgressDialog spins
                new LoadAppList(this).execute(getPackageManager());
                return true;
            case R.id.action_new_custom:
                final EditText input = new EditText(this);
                new AlertDialog.Builder(CustomizeApps.this)
                        .setTitle(R.string.add_app)
                        .setMessage(R.string.input_package_name)
                        .setView(input)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                savePref(input.getText().toString());
                                loadList();
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            case R.id.action_load_defaults:
                loadDefaultsWithConfirm();
                return true;
            case R.id.action_clear_list:
                clearList();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class LoadAppList extends AsyncTask<PackageManager, Void, Void> {
        WeakReference<CustomizeApps> callbackHolder;
        HashMap<String, String> nameMap;
        String[] names;

        public LoadAppList(CustomizeApps activity) {
            callbackHolder = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(PackageManager... params) {
            PackageManager pm = params[0];
            final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            names = new String[packages.size()];
            nameMap = new HashMap<>();
            int i = 0;
            for (ApplicationInfo info : packages) {
                //names[i] = info.packageName;
                names[i] = info.loadLabel(pm) + "\n(" + info.packageName + ")";
                nameMap.put(names[i], info.packageName);
                i++;
            }
            Arrays.sort(names);
            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            // Now that the list is loaded, show the dialog on the UI thread
            final CustomizeApps callbackReference;
            if ((callbackReference = callbackHolder.get()) != null) {
                callbackReference.onAppListLoaded(nameMap, names);
            }
        }
    }

    public void onAppListLoaded(final HashMap<String, String> nameMap, final String[] names) {
        dismissProgressDialog();
        new AlertDialog.Builder(CustomizeApps.this).setTitle(R.string.add_app)
                .setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        savePref(nameMap.get(names[which]));
                        loadList();
                    }
                }).show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadDefaults() {
        appSet.clear();
        appSet.addAll(Common.APPS.getDefaultSet());
        sharedPref.edit()
            .putStringSet(Common.APPS.getSetKey(), appSet)
            .putBoolean(Common.FIRST_RUN_KEY, false)
            .apply();
        loadList();
    }

    private void loadDefaultsWithConfirm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeApps.this)
                .setTitle(R.string.reset)
                .setMessage(getString(R.string.reset_apps))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        loadDefaults();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing on cancel
            }
        }).show();
    }

    private void loadList() {
        appSet.clear();
        appSet.addAll(sharedPref.getStringSet(Common.APPS.getSetKey(), new HashSet<String>()));
        isFirstRun = sharedPref.getBoolean(Common.FIRST_RUN_KEY, true);
        if (isFirstRun) {
            if (appSet.isEmpty()) {
                loadDefaults();
            } else {
                Editor editor = sharedPref.edit();
                editor.putBoolean(Common.FIRST_RUN_KEY, false);
                editor.apply();
            }
        }
        appList = appSet.toArray(new String[appSet.size()]);
        Arrays.sort(appList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, appList);
        // Bind to our new adapter.
        setListAdapter(adapter);
    }

    private void clearList() {
        final Editor editor = sharedPref.edit();
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeApps.this)
                .setTitle(R.string.clear)
                .setMessage(R.string.clear_all_apps)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        editor.remove(Common.APPS.getSetKey())
                            .apply();
                        loadList();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing on cancel
            }
        }).show();
    }

    private void savePref(String appName) {
        if (!(appSet.contains(appName))) {
            appSet.add(appName);
            sharedPref.edit()
                .putStringSet(Common.APPS.getSetKey(), appSet)
                .putBoolean(Common.FIRST_RUN_KEY, false)
                .apply();
        }
    }

    private void removeApp(int position) {
        String tempName = appList[position];
        appSet.remove(tempName);
        sharedPref.edit()
            .putStringSet(Common.APPS.getSetKey(), appSet)
            .apply();
    }

}
