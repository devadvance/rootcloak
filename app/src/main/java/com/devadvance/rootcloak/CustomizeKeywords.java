package com.devadvance.rootcloak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.support.v4.app.NavUtils;
import android.preference.PreferenceActivity;

public class CustomizeKeywords extends PreferenceActivity {

    SharedPreferences sharedPref;
    Set<String> keywordSet;
    String[] keywordList;
    boolean isFirstRunKeywords;

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_keywords);
        // Show the Up button in the action bar.
        setupActionBar();


        getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        sharedPref = getSharedPreferences(Common.PREFS_KEYWORDS, MODE_WORLD_READABLE);

        loadList();

        Resources res = getResources();
        new AlertDialog.Builder(this)
        .setMessage(res.getString(R.string.keyword_instructions) + "\n\n" + res.getString(R.string.both_instructions2))
        .setTitle(res.getString(R.string.important_title)).show();

    }

    public void onListItemClick( ListView parent, View v, int position, long id) {
        final int positionFinal = position;
        new AlertDialog.Builder(CustomizeKeywords.this)
        .setTitle("Remove Keyword")
        .setMessage("Are you sure you want to remove this keyword?")
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                removeKeyword(positionFinal);
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
        getMenuInflater().inflate(R.menu.customize_keywords, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;

        case R.id.action_new:
            final EditText input = new EditText(this);
            new AlertDialog.Builder(CustomizeKeywords.this)
            .setTitle("Add Keyword")
            .setMessage("Input the keyword:")
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
        keywordSet = Common.DEFAULT_KEYWORD_SET;
        Editor editor = sharedPref.edit();
        editor.remove(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY);
        editor.commit();
        editor.putStringSet(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY, keywordSet);
        editor.commit();
        editor.putBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, false);
        editor.commit();
        loadList();
    }

    private void loadDefaultsWithConfirm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeKeywords.this)
        .setTitle("Reset keywords to default?")
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
        keywordSet =  sharedPref.getStringSet(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY, new HashSet<String>());
        isFirstRunKeywords = sharedPref.getBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, true);
        if (isFirstRunKeywords) {
            if (keywordSet.isEmpty()) {
                loadDefaults();
            }
            else {
                Editor editor = sharedPref.edit();
                editor.putBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, false);
                editor.commit();
            }
        }
        keywordList = keywordSet.toArray(new String[0]);
        Arrays.sort(keywordList);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, keywordList);
        // Bind to our new adapter.
        setListAdapter(adapter);
    }

    private void clearList() {
        final Editor editor = sharedPref.edit();
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeKeywords.this)
        .setTitle("Proceed to clear all keywords?")
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                editor.remove(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY);
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

    private void savePref(String keyword) {
        if (!(keywordSet.contains(keyword))) {
            keywordSet.add(keyword);
            Editor editor = sharedPref.edit();
            editor.remove(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY);
            editor.commit();
            editor.putStringSet(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY, keywordSet);
            editor.commit();
            editor.putBoolean(Common.PACKAGE_NAME + Common.FIRST_RUN_KEY, false);
            editor.commit();
        }
    }

    private void removeKeyword(int position) {
        String tempName = keywordList[position];
        keywordSet.remove(tempName);
        Editor editor = sharedPref.edit();
        editor.remove(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY);
        editor.commit();
        editor.putStringSet(Common.PACKAGE_NAME + Common.KEYWORD_SET_KEY, keywordSet);
        editor.commit();
    }

}
