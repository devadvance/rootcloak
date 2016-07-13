package com.devadvance.rootcloak2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class SettingsActivity extends ListActivity {
    public static final String LOG_TAG = "Settings";
    SharedPreferences sharedPref;
    String[] menuItems;
    String instructionsString;
    String instructionsTitle;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Resources res = getResources();
        menuItems = res.getStringArray(R.array.menu_array);
        instructionsString = res.getString(R.string.instructions1) + "\n\n"
                + res.getString(R.string.instructions2) + "\n\n"
                + res.getString(R.string.instructions3) + "\n\n"
                + res.getString(R.string.instructions4);

        instructionsTitle = res.getString(R.string.instructions_title);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, menuItems);
        setListAdapter(adapter);

        sharedPref = getSharedPreferences(Common.PREFS_SETTINGS, MODE_WORLD_READABLE);
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        Intent intent;
        switch (position) {
            case 0:
                intent = new Intent(this, CustomizeApps.class);
                startActivity(intent);
                break;
            case 1:
                intent = new Intent(this, CustomizeKeywords.class);
                startActivity(intent);
                break;
            case 2:
                intent = new Intent(this, CustomizeCommands.class);
                startActivity(intent);
                break;
            case 3:
                new AlertDialog.Builder(this)
                        .setMessage(instructionsString)
                        .setTitle(instructionsTitle)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
                break;
            case 4:
                boolean debugPref = sharedPref.getBoolean(Common.DEBUG_KEY, false);
                debugPref = !debugPref;
                Editor editor = sharedPref.edit();
                editor.remove(Common.DEBUG_KEY);
                editor.commit();
                editor.putBoolean(Common.DEBUG_KEY, debugPref);
                editor.commit();
                String debugStatus = getString(debugPref ? R.string.debug_on : R.string.debug_off);
                Log.d(LOG_TAG, debugStatus);
                Toast.makeText(getApplicationContext(), debugStatus, Toast.LENGTH_LONG).show();
                break;
            case 5:
                String aboutMsg = getString(R.string.app_name) + ": " + BuildConfig.VERSION_NAME; //TODO!
                new AlertDialog.Builder(this)
                        .setMessage(aboutMsg)
                        .setTitle(R.string.about)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
                break;
            default:
                break;
        }
    }
}
