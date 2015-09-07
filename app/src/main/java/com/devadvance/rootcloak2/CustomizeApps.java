package com.devadvance.rootcloak2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.support.v4.app.NavUtils;
import android.preference.PreferenceActivity;

public class CustomizeApps extends PreferenceActivity {

    SharedPreferences sharedPref;
    Set<String> appSet;
    String[] appList;
    boolean isFirstRun;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_apps);
        // Show the Up button in the action bar.
        setupActionBar();


        getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        sharedPref = getSharedPreferences(Common.PREFS_APPS, MODE_WORLD_READABLE);

        loadList();

    }

    public void onListItemClick( ListView parent, View v, int position, long id) {
        final int positionFinal = position;
        new AlertDialog.Builder(CustomizeApps.this)
        .setTitle("Remove App")
        .setMessage("Are you sure you want to remove this app?")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                removeApp(positionFinal);
                loadList();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        }).show();

    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {

        getActionBar().setDisplayHomeAsUpEnabled(true);

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
        case android.R.id.home:
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;

        case R.id.action_new:
            final PackageManager pm = getPackageManager();
            //get a list of installed apps.
            final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            final String[] names = new String[packages.size()];
            final HashMap<String, String> nameMap = new HashMap<String,String>();
            int i = 0;
            for (ApplicationInfo info : packages) {
                //names[i] = info.packageName;
                names[i] = (String)info.loadLabel(pm) + "\n(" + info.packageName + ")";
                nameMap.put(names[i], info.packageName);
                i++;
            }
            Arrays.sort(names);

            new AlertDialog.Builder(this).setTitle("Add an App")
            .setItems((CharSequence[]) names, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       savePref(nameMap.get(names[which]));
                        loadList();
               }
        }).show();

            return true;
        case R.id.action_new_custom:
            final EditText input = new EditText(this);
            new AlertDialog.Builder(CustomizeApps.this)
            .setTitle("Add App")
            .setMessage("Input the app package name:")
            .setView(input)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    savePref(input.getText().toString());
                    loadList();
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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

    private void loadDefaults() {
        appSet = Common.DEFAULT_APPS_SET;
        Editor editor = sharedPref.edit();
        editor.remove(Common.PACKAGE_NAME + Common.APP_LIST_KEY);
        editor.commit();
        editor.putStringSet(Common.PACKAGE_NAME + Common.APP_LIST_KEY, appSet);
        editor.commit();
        editor.putBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, false);
        editor.commit();
        loadList();
    }

    private void loadDefaultsWithConfirm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeApps.this)
        .setTitle("Reset apps to default?")
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                loadDefaults();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing on cancel
            }
        }).show();
    }

    private void loadList() {
        appSet =  sharedPref.getStringSet(Common.PACKAGE_NAME + Common.APP_LIST_KEY, new HashSet<String>());
        isFirstRun = sharedPref.getBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, true);
        if (isFirstRun) {
            if (appSet.isEmpty()) {
                loadDefaults();
            }
            else {
                Editor editor = sharedPref.edit();
                editor.putBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, false);
                editor.commit();
            }
        }
        appList = appSet.toArray(new String[0]);
        Arrays.sort(appList);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, appList);
        // Bind to our new adapter.
        setListAdapter(adapter);
    }

    private void clearList() {
        final Editor editor = sharedPref.edit();
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeApps.this)
        .setTitle("Proceed to clear all apps?")
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                editor.remove(Common.PACKAGE_NAME + Common.APP_LIST_KEY);
                editor.commit();
                loadList();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing on cancel
            }
        }).show();
    }

    private void savePref(String appName) {
        if (!(appSet.contains(appName))) {
            appSet.add(appName);
            Editor editor = sharedPref.edit();
            editor.remove(Common.PACKAGE_NAME + Common.APP_LIST_KEY);
            editor.commit();
            editor.putStringSet(Common.PACKAGE_NAME + Common.APP_LIST_KEY, appSet);
            editor.commit();
            editor.putBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, false);
            editor.commit();
        }
    }

    private void removeApp(int position) {
        String tempName = appList[position];
        appSet.remove(tempName);
        Editor editor = sharedPref.edit();
        editor.remove(Common.PACKAGE_NAME + Common.APP_LIST_KEY);
        editor.commit();
        editor.putStringSet(Common.PACKAGE_NAME + Common.APP_LIST_KEY, appSet);
        editor.commit();
    }

}
