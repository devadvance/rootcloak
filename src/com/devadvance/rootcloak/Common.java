package com.devadvance.rootcloak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Common {
	public static final String PREFS_APPS = "CustomizeApps";
	public static final String PREFS_KEYWORDS = "CustomizeKeywords";
	public static final String PREFS_COMMANDS = "CustomizeCommands";
	public static final String PACKAGE_NAME = "com.devadvance.rootcloak";
	public static final String FIRST_RUN_KEY = "IS_FIRST_RUN";
	public static final String TAG = "RootCloak";
	public static final String[] DEFAULT_APPS_LIST = {"com.fde.DomesticDigitalCopy",
		"com.directv.application.android.go.production",
		"com.res.bby",
		"dk.excitor.dmemail",
		"com.BHTV", 
		"com.bradfordnetworks.bma", 
		"com.apriva.mobile.bams", 
		"com.apriva.mobile.aprivapay", 
		"pl.pkobp.iko", 
		"au.com.auspost", 
		"com.rogers.citytv.phone", 
		"com.zenprise", 
		"net.flixster.android", 
		"com.starfinanz.smob.android.sfinanzstatus", 
		"com.ovidos.yuppi", 
		"klb.android.lovelive", 
		"com.incube.epub", 
		"com.airwatch.androidagent", 
		"com.isis.mclient.atnt.activity",
		"com.zappware.twintv.d3"};
	public static final Set<String> DEFAULT_APPS_SET = new HashSet<String>(Arrays.asList(DEFAULT_APPS_LIST));
	public static final String DEBUG_KEY = "DEBUGGERPREF";
	
	public static final String APP_LIST_KEY = "APPS_LIST"; // Uses the name LIST for legacy purposes
	
	public static final String KEYWORD_SET_KEY = "KEYWORD_SET";
	public static final String COMMAND_SET_KEY = "APPS_SET";
	
	public static final String[] DEFAULT_KEYWORD_LIST = new String[] { "supersu", "superuser", "Superuser", "noshufou", "xposed", "rootcloak", "chainfire", "titanium"};
	public static final Set<String> DEFAULT_KEYWORD_SET = new HashSet<String>(Arrays.asList(DEFAULT_KEYWORD_LIST));
	public static final String[] DEFAULT_COMMAND_LIST = new String[] { "su", "which", "busybox", "pm", "am", "sh", "ps"};
	public static final Set<String> DEFAULT_COMMAND_SET = new HashSet<String>(Arrays.asList(DEFAULT_COMMAND_LIST));
}
