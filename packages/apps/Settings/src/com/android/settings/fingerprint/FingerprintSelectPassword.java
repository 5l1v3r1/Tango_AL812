package com.android.settings.fingerprint;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
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
import com.android.settings.R.*;
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import com.android.settings.SettingsPreferenceFragment;
public class FingerprintSelectPassword extends SettingsPreferenceFragment implements Indexable {
	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private static final String KEY_FINGERPRINT_NUMBER_PASSWORD = "fingerprint_number_password";
	private static final String KEY_FINGERPRINT_COMPLEX_PASSWORD = "fingerprint_complex_password";

    private Intent mTrustAgentClickIntent;
	private Preference mNumberPassword;
	private Preference mComplexPassword;

	private static final String TAG = "FingerprintSelectPassword";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null &&
				savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
		    mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}
		addPreferencesFromResource(R.xml.fingerprint_select_password);

		PreferenceScreen root = getPreferenceScreen();
		mNumberPassword = root.findPreference(KEY_FINGERPRINT_NUMBER_PASSWORD);
		mComplexPassword = root.findPreference(KEY_FINGERPRINT_COMPLEX_PASSWORD);
	}

	@Override
	public void onResume() {
		super.onResume();
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
		FragmentManager fragment=getFragmentManager();
		FragmentTransaction transacction=fragment.beginTransaction();
		if(preference == mNumberPassword){
			Log.d(TAG, "mNumberPassword is clicked");
			getActivity().setTitle(getString(R.string.enter_number_password));
			FingerprintNumberPassword numberpassword =  new FingerprintNumberPassword();
			transacction.replace(android.R.id.content, numberpassword);
		}
		else if(preference == mComplexPassword){
			Log.d(TAG, "mComplexPassword is clicked");
			getActivity().setTitle(getString(R.string.enter_complex_password));
			FingerprintComplexPassword complexpassword =  new FingerprintComplexPassword();
			transacction.replace(android.R.id.content, complexpassword);
		}
		//HQ_hushunli 2016-11-15 modify for HQ02059820 begin
		//transacction.commit();
		transacction.commitAllowingStateLoss();
		//HQ_hushunli 2016-11-15 modify for HQ02059820 end
		return false;
	}

}
