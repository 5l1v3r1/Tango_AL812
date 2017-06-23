package com.android.settings.fingerprint;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.security.KeyStore;
import android.service.trust.TrustAgentService;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import com.mediatek.settings.ext.IPermissionControlExt;
import com.mediatek.settings.ext.IPplSettingsEntryExt;
import com.mediatek.settings.ext.IMdmPermissionControlExt;
import com.mediatek.settings.ext.IDataProtectionExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;

import java.util.ArrayList;
import java.util.List;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class FingerprintSettings extends SettingsPreferenceFragment implements
		OnPreferenceChangeListener, Indexable {
    private final String TAG = "FingerprintSettings";
	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private static final String KEY_FINGERPRINT_MANAGE = "fingerprint_manage";
	private static final String KEY_TOGGLE_RECIEVE_PHONECALLS = "toggle_recieve_phonecalls";
	private static final String KEY_TOGGLE_TAKE_PICTURES = "toggle_take_pictures";
	private static final String KEY_TOGGLE_OFF_ALARMCLOCK = "toggle_off_alarmclock";

    private Intent mTrustAgentClickIntent;
	private Preference mFingerprintManage;
 	private SwitchPreference mTogglePhonecall;
	private SwitchPreference mToggleTakepicture;
	private SwitchPreference mToggleOffAlarmclock;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null &&
				savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
		    mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}
		addPreferencesFromResource(R.xml.fingerprint_settings);
		PreferenceScreen root = getPreferenceScreen();
		mFingerprintManage = root.findPreference(KEY_FINGERPRINT_MANAGE);
	}

	@Override
	public void onResume() {

		super.onResume();
		mTogglePhonecall = (SwitchPreference) findPreference(KEY_TOGGLE_RECIEVE_PHONECALLS);
		if(mTogglePhonecall != null){
  			mTogglePhonecall.setChecked(isFingerprintRecieveCallAllowed());
  			mTogglePhonecall.setOnPreferenceChangeListener(this);
		}


		mToggleTakepicture = (SwitchPreference) findPreference(KEY_TOGGLE_TAKE_PICTURES);
		if(mToggleTakepicture != null){
  			mToggleTakepicture.setChecked(isFingerprintTakePictureAllowed());
  			mToggleTakepicture.setOnPreferenceChangeListener(this);
		}


		mToggleOffAlarmclock = (SwitchPreference) findPreference(KEY_TOGGLE_OFF_ALARMCLOCK);
		if(mToggleOffAlarmclock != null){
  			mToggleOffAlarmclock.setChecked(isFingerprintOffAlarmclockAllowed());
  			mToggleOffAlarmclock.setOnPreferenceChangeListener(this);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
	}
  
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mTrustAgentClickIntent != null) {
		    outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

		if(preference == mFingerprintManage){
			Log.d(TAG, "mFingerprintManage is clicked");
			Intent intent = new Intent(getActivity(), FingerprintPassword.class);
			getActivity().startActivity(intent);
		}
		return false;
	}

	@Override	
    public boolean onPreferenceChange(Preference preference, Object value) {

    	final String key = preference.getKey();
		if (KEY_TOGGLE_RECIEVE_PHONECALLS.equals(key)) {
			Log.e(TAG, "@@value111 = " + value);
			if ((Boolean) value) {
				setFingerprintPhonecallAllowed(true);
			}else {
				setFingerprintPhonecallAllowed(false);
	    		}
			mTogglePhonecall.setChecked((Boolean) value);
		}
		else if(KEY_TOGGLE_TAKE_PICTURES.equals(key)){
			Log.e(TAG, "@@value222 = " + value);
			if ((Boolean) value) {
				setFingerprintTakepictureAllowed(true);
			}else {
				setFingerprintTakepictureAllowed(false);
	    		}
	    	mToggleTakepicture.setChecked((Boolean) value);
		}
		else if(KEY_TOGGLE_OFF_ALARMCLOCK.equals(key)){
			Log.e(TAG, "@@value = " + value);
			if ((Boolean) value) {
				setFingerprintOffAlarmclockAllowed(true);
			}else {
				setFingerprintOffAlarmclockAllowed(false);
	    		}
	    	mToggleOffAlarmclock.setChecked((Boolean) value);
		}
		return false;
    }

	private boolean isFingerprintRecieveCallAllowed(){
		ContentResolver resolver = getActivity().getContentResolver();
		try{
			Log.d(TAG, "Settings.System.FINGERPRINT_PHONE_SWITCH = " +
					Settings.System.getInt(resolver, Settings.System.FINGERPRINT_PHONE_SWITCH));

			return Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_PHONE_SWITCH) > 0;
		} catch(SettingNotFoundException snfe) {
			Log.d(TAG, " Settings.System.FINGERPRINT_PHONE_SWITCH not found");
			return false;
		}
	}

	private boolean isFingerprintTakePictureAllowed(){
		ContentResolver resolver = getActivity().getContentResolver();
		try{
			Log.d(TAG, "Settings.System.FINGERPRINT_CAMERA_SWITCH = " +
					Settings.System.getInt(resolver, Settings.System.FINGERPRINT_CAMERA_SWITCH));

			return Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_CAMERA_SWITCH) > 0;
		} catch(SettingNotFoundException snfe){
			Log.d(TAG, " Settings.System.FINGERPRINT_CAMERA_SWITCH not found");
			return false;
		}
	}

	private boolean isFingerprintOffAlarmclockAllowed(){
		ContentResolver resolver = getActivity().getContentResolver();
		try{
			Log.d(TAG, "Settings.System.FINGERPRINT_ALARMCLOCK_SWITCH = " +
					Settings.System.getInt(resolver, Settings.System.FINGERPRINT_ALARMCLOCK_SWITCH));

			return Settings.System.getInt(resolver,
		                              Settings.System.FINGERPRINT_ALARMCLOCK_SWITCH) > 0;
		} catch(SettingNotFoundException snfe) {
			Log.d(TAG, " Settings.System.FINGERPRINT_ALARMCLOCK_SWITCH not found");
			return false;
		}
	}

	private void setFingerprintPhonecallAllowed(boolean enabled){
		ContentResolver resolver = getActivity().getContentResolver();
 		final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);

		Settings.System.putInt(getContentResolver(), Settings.System.FINGERPRINT_PHONE_SWITCH,
				enabled ? 1 : 0);
		Log.e(TAG, "setFingerprintPhonecallAllowed enabled = " + enabled);
		try{
			Log.e(TAG, "Settings.System.FINGERPRINT_KEYGUARD_SWITCH = " +
					Settings.System.getInt(resolver, Settings.System.FINGERPRINT_PHONE_SWITCH));
		} catch(SettingNotFoundException snfe) {
			Log.e(TAG, "Settings.System.FINGERPRINT_PHONE_SWITCH not found");
		}
			
	}

	private void setFingerprintTakepictureAllowed(boolean enabled){
		ContentResolver resolver = getActivity().getContentResolver();
 		final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);

		Settings.System.putInt(getContentResolver(), Settings.System.FINGERPRINT_CAMERA_SWITCH,
				enabled ? 1 : 0);
		Log.e(TAG, "setFingerprintTakepictureAllowed enabled = " + enabled);
		try{
			Log.e(TAG, "Settings.System.FINGERPRINT_KEYGUARD_SWITCH = " +
					Settings.System.getInt(resolver, Settings.System.FINGERPRINT_CAMERA_SWITCH));
		} catch(SettingNotFoundException snfe) {
			Log.e(TAG, " Settings.System.FINGERPRINT_PHONE_SWITCH not found");
		}
			
	}

	private void setFingerprintOffAlarmclockAllowed(boolean enabled){
		ContentResolver resolver = getActivity().getContentResolver();
 		final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);

		Settings.System.putInt(getContentResolver(), Settings.System.FINGERPRINT_ALARMCLOCK_SWITCH,
				enabled ? 1 : 0);
		Log.e(TAG, "setFingerprintOffAlarmclockAllowed enabled = " + enabled);
		try{
			Log.e(TAG, "Settings.System.FINGERPRINT_KEYGUARD_SWITCH = " +
					Settings.System.getInt(resolver, Settings.System.FINGERPRINT_ALARMCLOCK_SWITCH));
		} catch(SettingNotFoundException snfe) {
			Log.e(TAG, " Settings.System.FINGERPRINT_ALARMCLOCK_SWITCH not found");
		}
	}

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
    new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            // Add fragment title
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.fingerprint_settings_title);
            data.screenTitle = res.getString(R.string.fingerprint_settings_title);
            data.keywords = res.getString(R.string.fingerprint_settings_title);
            result.add(data);

            return result;
        }
    };

}
