/*
 * Copyright (C) 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.teamdouche.nexuskang.nexus;

import com.teamdouche.nexuskang.R;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.service.wallpaper.WallpaperSettingsActivity;
import android.util.Log;

import java.lang.Integer;

public class NexusSettings extends WallpaperSettingsActivity 
	implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String COLORSCHEME_PREF = "nexus_colorscheme";
    public static final String CUSTOM_COLORS1 = "custom_colors1";
	private static final String COLOR0_PREF = "color0";
	private static final String COLOR1_PREF = "color1";
	private static final String COLOR2_PREF = "color2";
	private static final String COLOR3_PREF = "color3";

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getPreferenceManager().setSharedPreferencesName(
				NexusWallpaper.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.nexus_prefs);
		final PreferenceGroup parentPreference = getPreferenceScreen();
		parentPreference.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	protected void onResume() {
		super.onResume();
	}

	protected void onDestroy() {
		super.onDestroy();
	}

	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		if (COLORSCHEME_PREF.equals(key)) {
			final Resources res = this.getResources();
			final String[] colorscheme = res.getStringArray(res.getIdentifier("nexus_colorscheme_" + 
				preferences.getString(key, "softblues"), "array", "com.teamdouche.nexuskang"));

			SharedPreferences.Editor editor = preferences.edit();
			editor.putString(COLOR0_PREF, colorscheme[0]);
			editor.putString(COLOR1_PREF, colorscheme[1]);
			editor.putString(COLOR2_PREF, colorscheme[2]);
			editor.putString(COLOR3_PREF, colorscheme[3]);
			editor.commit();

			Log.d("Nexus LWP", "colorScheme="+preferences.getString(key, "none"));

			Log.d("Nexus LWP", "color0="+colorscheme[0]);
			Log.d("Nexus LWP", "color1="+colorscheme[1]);
			Log.d("Nexus LWP", "color2="+colorscheme[2]);
			Log.d("Nexus LWP", "color3="+colorscheme[3]);
		}
    }
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("custom_colors0")) {
            int i = Color.parseColor(getPreferenceManager().getSharedPreferences().getString(COLOR0_PREF, "333333"));
            ColorPickerDialog cp = new ColorPickerDialog(this,mColor0Listener, i);
            cp.show();
        }
        else if (preference.getKey().equals("custom_colors1")) {
            int i = Color.parseColor(getPreferenceManager().getSharedPreferences().getString(COLOR1_PREF, "333333"));
            ColorPickerDialog cp = new ColorPickerDialog(this,mColor1Listener, i);
        	cp.show();
        }
        else if (preference.getKey().equals("custom_colors2")) {
            int i = Color.parseColor(getPreferenceManager().getSharedPreferences().getString(COLOR2_PREF, "333333"));
            ColorPickerDialog cp = new ColorPickerDialog(this,mColor2Listener, i);
        	cp.show();
        }
        else if (preference.getKey().equals("custom_colors3")) {
            int i = Color.parseColor(getPreferenceManager().getSharedPreferences().getString(COLOR3_PREF, "333333"));
            ColorPickerDialog cp = new ColorPickerDialog(this,mColor3Listener, i);
        	cp.show();
        }
        return false;
	}
    ColorPickerDialog.OnColorChangedListener mColor0Listener =
    	new ColorPickerDialog.OnColorChangedListener() {
    	public void colorChanged(int color) {
    		getPreferenceManager().getSharedPreferences().edit().putString(COLOR0_PREF, convertToRGB(color)).commit();
    	}
    };
    ColorPickerDialog.OnColorChangedListener mColor1Listener =
    	new ColorPickerDialog.OnColorChangedListener() {
    	public void colorChanged(int color) {
    		getPreferenceManager().getSharedPreferences().edit().putString(COLOR1_PREF, convertToRGB(color)).commit();
    	}
    };
    ColorPickerDialog.OnColorChangedListener mColor2Listener =
    	new ColorPickerDialog.OnColorChangedListener() {
    	public void colorChanged(int color) {
    		getPreferenceManager().getSharedPreferences().edit().putString(COLOR2_PREF, convertToRGB(color)).commit();
    	}
    };
    ColorPickerDialog.OnColorChangedListener mColor3Listener =
    	new ColorPickerDialog.OnColorChangedListener() {
    	public void colorChanged(int color) {
    		getPreferenceManager().getSharedPreferences().edit().putString(COLOR3_PREF, convertToRGB(color)).commit();
    	}
    };

    private String convertToRGB(int color) {
	    String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

	    if (red.length() == 1) {
            red = "0" + red;
        }
        if (green.length() == 1) {
            green = "0" + green;
        }
        if (blue.length() == 1) {
            blue = "0" + blue;
        }
        return "#" + red + green + blue;
    }
    private String removeHex(String string){
        String hexless = string.substring(1);
        Log.d("Hexless", hexless);
        return hexless;
    }

}
