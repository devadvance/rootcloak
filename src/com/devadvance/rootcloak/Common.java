package com.devadvance.rootcloak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Common {
	public static final String PREFS = "CustomizeApps";
	public static final String PACKAGE_NAME = "com.devadvance.rootcloak";
	public static final String TAG = "RootCloak";
	public static final String[] DEFAULT_APPS = {"com.fde.DomesticDigitalCopy",
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
		"com.incube.epub"};
	public static final String DEBUG_KEY = "DEBUGGERPREF";
	public static final String APP_LIST_KEY = "APPS_LIST";
	public static final String[] KEYWORD_LIST = new String[] { "supersu", "superuser", "Superuser", "noshufou", "xposed", "rootcloak", "chainfire", "titanium"};
	public static final Set<String> KEYWORD_SET = new HashSet<String>(Arrays.asList(KEYWORD_LIST));
	public static final String[] COMMAND_LIST = new String[] { "su", "which", "busybox", "pm", "am", "sh"};
	public static final Set<String> COMMAND_SET = new HashSet<String>(Arrays.asList(COMMAND_LIST));
}
