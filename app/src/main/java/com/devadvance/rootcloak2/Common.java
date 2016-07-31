package com.devadvance.rootcloak2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.preference.PreferenceActivity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Common {
    public static final String PREFS_SETTINGS = "CustomizeSettings";
    public static final String PACKAGE_NAME = "com.devadvance.rootcloak2";
    public static final String FIRST_RUN_KEY = PACKAGE_NAME + "IS_FIRST_RUN";
    public static final String DEBUG_KEY = "debug_logs";
    public static final String SHOW_WARNING = "SHOW_WARNING";

    public static final PrefSet APPS = new AppsSet();
    public static final PrefSet KEYWORDS = new KeywordSet();
    public static final PrefSet COMMANDS = new CommandSet();
    public static final PrefSet LIBRARIES = new LibrarySet();

    public static final String REFRESH_APPS_INTENT = "com.devadvance.rootcloak2.REFRESH_APPS";

    public static boolean isUserApp(ApplicationInfo appInfo) {
        /* Hide RootCloak */
        if (BuildConfig.APPLICATION_ID.equals(appInfo.packageName)) return false;

        if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return false;
        }
        return true;
    }

    public static abstract class PrefSet {
        abstract String getPrefKey();

        abstract String getSetKey();

        abstract Set<String> getDefaultSet();

        @SuppressLint("WorldReadableFiles")
        public SharedPreferences getSharedPreferences(PreferenceActivity activity) {
            activity.getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            return activity.getSharedPreferences(getPrefKey(), Context.MODE_WORLD_READABLE);
        }
    }

    public static class AppsSet extends PrefSet {
        public static final String PREFS_APPS = "CustomizeApps";
        public static final String APP_SET_KEY = PACKAGE_NAME + "APPS_LIST"; // Uses the name LIST for legacy purposes
        public static final Set<String> DEFAULT_APPS_SET = new HashSet<>(Arrays.asList(DefaultLists.DEFAULT_APPS_LIST));

        @Override
        public String getPrefKey() {
            return PREFS_APPS;
        }

        @Override
        public String getSetKey() {
            return APP_SET_KEY;
        }

        @Override
        public Set<String> getDefaultSet() {
            return DEFAULT_APPS_SET;
        }
    }

    public static class KeywordSet extends PrefSet {
        public static final String PREFS_KEYWORDS = "CustomizeKeywords";
        public static final String KEYWORD_SET_KEY = PACKAGE_NAME + "KEYWORD_SET";
        public static final Set<String> DEFAULT_KEYWORD_SET = new HashSet<>(Arrays.asList(DefaultLists.DEFAULT_KEYWORD_LIST));

        @Override
        public String getPrefKey() {
            return PREFS_KEYWORDS;
        }

        @Override
        public String getSetKey() {
            return KEYWORD_SET_KEY;
        }

        @Override
        public Set<String> getDefaultSet() {
            return DEFAULT_KEYWORD_SET;
        }
    }

    public static class CommandSet extends PrefSet {
        public static final String PREFS_COMMANDS = "CustomizeCommands";
        public static final String COMMAND_SET_KEY = PACKAGE_NAME + "APPS_SET";
        public static final Set<String> DEFAULT_COMMAND_SET = new HashSet<>(Arrays.asList(DefaultLists.DEFAULT_COMMAND_LIST));

        @Override
        public String getPrefKey() {
            return PREFS_COMMANDS;
        }

        @Override
        public String getSetKey() {
            return COMMAND_SET_KEY;
        }

        @Override
        public Set<String> getDefaultSet() {
            return DEFAULT_COMMAND_SET;
        }
    }

    public static class LibrarySet extends PrefSet {
        public static final String PREFS_LIBNAMES = "CustomizeLibnames";
        public static final String LIBRARY_SET_KEY = "LIBNAMES_SET";
        public static final Set<String> DEFAULT_LIBNAME_SET = new HashSet<>(Arrays.asList(DefaultLists.DEFAULT_LIBNAME_LIST));

        @Override
        public String getPrefKey() {
            return PREFS_LIBNAMES;
        }

        @Override
        public String getSetKey() {
            return LIBRARY_SET_KEY;
        }

        @Override
        public Set<String> getDefaultSet() {
            return DEFAULT_LIBNAME_SET;
        }
    }
}
