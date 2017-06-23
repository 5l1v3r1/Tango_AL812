/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

// TODO: Needed for move to system service: import com.android.internal.R;

/**
 * Helper class to manage the "Respond via SMS Message" feature for incoming calls.
 */
public class RespondViaSmsSettings {
    // TODO: This class is newly copied into Telecom (com.android.server.telecom) from it previous
    // location in Telephony (com.android.phone). User's preferences stored in the old location
    // will be lost. We need code here to migrate KLP -> LMP settings values.

    /**
     * Settings activity under "Call settings" to let you manage the
     * canned responses; see respond_via_sms_settings.xml
     */
    public static class Settings extends PreferenceActivity
            implements Preference.OnPreferenceChangeListener {
    	// modify by wangmingyue for HQ01669685 begin
    	private boolean mIsRes1Changed;
    	private boolean mIsRes2Changed;
    	private boolean mIsRes3Changed;
    	private boolean mIsRes4Changed;
    	private SharedPreferences prefs ;
    	public static final String SHARED_ISCHANGED_NAME="name_ischanged";
    	// modify by wangmingyue for HQ01669685 end
        @Override
        protected void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            Log.d(this, "Settings: onCreate()...");

              // / Annotated by guofeiyao
            // This function guarantees that QuickResponses will be in our
            // SharedPreferences with the proper values considering there may be
            // old QuickResponses in Telephony pre L.
            //QuickResponseUtils.maybeMigrateLegacyQuickResponses(this);
              // / End
            // modify by wangmingyue for HQ01669685 begin
            getPreferenceManager().setSharedPreferencesName(
                    QuickResponseUtils.SHARED_PREFERENCES_NAME);
            prefs = this.getSharedPreferences(
            		SHARED_ISCHANGED_NAME, Context.MODE_PRIVATE);
            // This preference screen is ultra-simple; it's just 4 plain
            // <EditTextPreference>s, one for each of the 4 "canned responses".
            //
            // The only nontrivial thing we do here is copy the text value of
            // each of those EditTextPreferences and use it as the preference's
            // "title" as well, so that the user will immediately see all 4
            // strings when they arrive here.
            //
            // Also, listen for change events (since we'll need to update the
            // title any time the user edits one of the strings.)
            Resources res = this.getResources();
            addPreferencesFromResource(R.xml.respond_via_sms_settings);

            EditTextPreference pref;
            pref = (EditTextPreference) findPreference(
                    QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_1);
            if(prefs.getBoolean("values1_isChanged", false)) {
            	pref.setTitle(pref.getText());
            }
            else {
            	pref.setTitle(res.getString(R.string.respond_via_sms_canned_response_1));
            	pref.setText(res.getString(R.string.respond_via_sms_canned_response_1));
            }
            
            pref.setOnPreferenceChangeListener(respond1Listener);

            pref = (EditTextPreference) findPreference(
                    QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_2);
            if(prefs.getBoolean("values2_isChanged", false)) {
            	pref.setTitle(pref.getText());
            }
            else {
            	pref.setTitle(res.getString(R.string.respond_via_sms_canned_response_2));
            	pref.setText(res.getString(R.string.respond_via_sms_canned_response_2));

            }
            pref.setOnPreferenceChangeListener(respond2Listener);

            pref = (EditTextPreference) findPreference(
                    QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_3);
            if(prefs.getBoolean("values3_isChanged", false)) {
            	pref.setTitle(pref.getText());
            }
            else {
            	pref.setTitle(res.getString(R.string.respond_via_sms_canned_response_3));
            	pref.setText(res.getString(R.string.respond_via_sms_canned_response_3));

            }
            pref.setOnPreferenceChangeListener(respond3Listener);

            pref = (EditTextPreference) findPreference(
                    QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_4);
            if(prefs.getBoolean("values4_isChanged", false)) {
            	pref.setTitle(pref.getText());
            }
            else {
            	pref.setTitle(res.getString(R.string.respond_via_sms_canned_response_4));
            	pref.setText(res.getString(R.string.respond_via_sms_canned_response_4));

            }
            pref.setOnPreferenceChangeListener(respond4Listener);
         // modify by wangmingyue for HQ01669685 end
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                // android.R.id.home will be triggered in onOptionsItemSelected()
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        // Preference.OnPreferenceChangeListener implementation
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.d(this, "onPreferenceChange: key = %s", preference.getKey());
            Log.d(this, "  preference = '%s'", preference);
            Log.d(this, "  newValue = '%s'", newValue);

            EditTextPreference pref = (EditTextPreference) preference;

            // Copy the new text over to the title, just like in onCreate().
            // (Watch out: onPreferenceChange() is called *before* the
            // Preference itself gets updated, so we need to use newValue here
            // rather than pref.getText().)
            pref.setTitle((String) newValue);

            return true;  // means it's OK to update the state of the Preference with the new value
        }

     // add by wangmingyue for HQ01669685 begin
        Preference.OnPreferenceChangeListener respond1Listener = new Preference.OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// TODO Auto-generated method stub
				 EditTextPreference pref = (EditTextPreference) preference;
				 if(!pref.getText().equals((String) newValue)){
					 mIsRes1Changed = true;
				 }
				 else {
					 mIsRes1Changed = false;
				 }
				 pref.setTitle((String) newValue);
				 prefs.edit().putBoolean("values1_isChanged", mIsRes1Changed).commit();
				return true;
			}
		};
       Preference.OnPreferenceChangeListener respond2Listener = new Preference.OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				 EditTextPreference pref = (EditTextPreference) preference;
				 if(!pref.getText().equals((String) newValue)){
					 mIsRes2Changed = true;
				 }
				 else {
					 mIsRes2Changed = false;
				 }
				 pref.setTitle((String) newValue);
				 prefs.edit().putBoolean("values2_isChanged", mIsRes2Changed).commit();
				return true;
			}
		};
       Preference.OnPreferenceChangeListener respond3Listener = new Preference.OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// TODO Auto-generated method stub
				 EditTextPreference pref = (EditTextPreference) preference;
				 if(!pref.getText().equals((String) newValue)){
					 mIsRes3Changed = true;
				 }
				 else {
					 mIsRes3Changed = false;
				 }
				 pref.setTitle((String) newValue);
				 prefs.edit().putBoolean("values3_isChanged", mIsRes3Changed).commit();
				return true;
			}
		};
      Preference.OnPreferenceChangeListener respond4Listener = new Preference.OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// TODO Auto-generated method stub
				 EditTextPreference pref = (EditTextPreference) preference;
				 if(!pref.getText().equals((String) newValue)){
					 mIsRes4Changed = true;
				 }
				 else {
					 mIsRes4Changed = false;
				 }
				 pref.setTitle((String) newValue);
				 prefs.edit().putBoolean("values4_isChanged", mIsRes4Changed).commit();
				return true;
			}
		};
		 // add by wangmingyue for HQ01669685 end
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            final int itemId = item.getItemId();
            switch (itemId) {
                case android.R.id.home:
                    goUpToTopLevelSetting(this);
                    return true;
                default:
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Finish current Activity and go up to the top level Settings.
     */
    public static void goUpToTopLevelSetting(Activity activity) {
        activity.finish();
     }
}
