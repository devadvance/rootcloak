package com.devadvance.rootcloak2;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CustomizeKeywords extends PreferenceActivity {

    SharedPreferences sharedPref;
    Set<String> keywordSet = new HashSet<>();
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

        sharedPref = Common.KEYWORDS.getSharedPreferences(this);

        loadList();

        if (sharedPref.getBoolean(Common.SHOW_WARNING, true)) {
            Resources res = getResources();
            new AlertDialog.Builder(this)
                    .setMessage(res.getString(R.string.command_instructions) + "\n\n" + res.getString(R.string.both_instructions2))
                    .setTitle(res.getString(R.string.important_title))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            sharedPref.edit().putBoolean(Common.SHOW_WARNING, false).apply();
                        }
                    })
                    .show();
        }
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        final int positionFinal = position;
        new AlertDialog.Builder(CustomizeKeywords.this)
                .setTitle(R.string.remove_keyword_title)
                .setMessage(R.string.remove_keyword_message)
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
     * Set up the {@link ActionBar}.
     */
    private void setupActionBar() {

        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

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
            case R.id.action_new:
                final EditText input = new EditText(this);
                new AlertDialog.Builder(CustomizeKeywords.this)
                        .setTitle(R.string.add_keyword)
                        .setMessage(R.string.input_keyword)
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

    private void loadDefaults() {
        keywordSet.clear();
        keywordSet.addAll(Common.KEYWORDS.getDefaultSet());
        sharedPref.edit()
                .putStringSet(Common.KEYWORDS.getSetKey(), keywordSet)
                .putBoolean(Common.FIRST_RUN_KEY, false)
                .apply();
        loadList();
    }

    private void loadDefaultsWithConfirm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeKeywords.this)
                .setTitle(R.string.reset)
                .setMessage(getString(R.string.reset_commands))
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
        keywordSet.clear();
        keywordSet.addAll(sharedPref.getStringSet(Common.KEYWORDS.getSetKey(), new HashSet<String>()));
        isFirstRunKeywords = sharedPref.getBoolean(Common.FIRST_RUN_KEY, true);
        if (isFirstRunKeywords) {
            if (keywordSet.isEmpty()) {
                loadDefaults();
            } else {
                sharedPref.edit()
                    .putBoolean(Common.FIRST_RUN_KEY, false)
                    .apply();
            }
        }
        keywordList = keywordSet.toArray(new String[keywordSet.size()]);
        Arrays.sort(keywordList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, keywordList);
        // Bind to our new adapter.
        setListAdapter(adapter);
    }

    private void clearList() {
        final Editor editor = sharedPref.edit();
        AlertDialog.Builder builder = new AlertDialog.Builder(CustomizeKeywords.this)
                .setTitle(R.string.clear)
                .setMessage(R.string.clear_app_keywords)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        editor.remove(Common.KEYWORDS.getSetKey())
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

    private void savePref(String keyword) {
        if (!(keywordSet.contains(keyword))) {
            keywordSet.add(keyword);
            sharedPref.edit()
                .putStringSet(Common.KEYWORDS.getSetKey(), keywordSet)
                .putBoolean(Common.FIRST_RUN_KEY, false)
                .apply();
        }
    }

    private void removeKeyword(int position) {
        String tempName = keywordList[position];
        keywordSet.remove(tempName);
        sharedPref.edit()
            .putStringSet(Common.KEYWORDS.getSetKey(), keywordSet)
            .apply();
    }
}
