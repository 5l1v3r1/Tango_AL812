package com.android.settings.fingerprint;


import android.app.Activity;
import android.app.ActivityManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;  
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
import android.os.Build;
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
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import android.view.Display;
import com.android.internal.widget.LockPatternUtils;
import android.app.admin.DevicePolicyManager;
import com.android.settings.ChooseLockSettingsHelper;
import android.app.admin.DevicePolicyManager;

public class FingerprintPassword extends Activity{

	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private static final String TAG = "FingerprintPassword";

    private Intent intent;
	private Intent mTrustAgentClickIntent;
	LockPatternUtils lockPatternUtils;
	private boolean isPasswordExist = false;

	private ChooseLockSettingsHelper mChooseLockSettingsHelper;
	private static final int CONFIRM_EXISTING_REQUEST = 100;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null &&
				savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
		    mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}

		intent = FingerprintPassword.this.getIntent();

		//add by HQ_zhouguo at 20150811 start
		mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this);
		int passType = mChooseLockSettingsHelper.utils().getKeyguardStoredPasswordQuality();
		if(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING == passType){
			mChooseLockSettingsHelper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST, null, null);
		}
		//add by HQ_zhouguo at 20150811 end

		FragmentManager fragment=getFragmentManager();
		FragmentTransaction transacction=fragment.beginTransaction();
		Log.d(TAG, "FingerprintPassword is start");

		CheckPassword();
		//check is password exist and get lockscreen_password here
		if(isPasswordExist){
			setTitle(getString(R.string.enter_password_title));
			FingerprintCheckPassword checkpassword =  new FingerprintCheckPassword();
			transacction.replace(android.R.id.content, checkpassword);
		}
		else{
			setTitle(getString(R.string.switch_unlock_style_title));
			FingerprintSelectPassword selectpassword =  new FingerprintSelectPassword();
			transacction.replace(android.R.id.content, selectpassword);
		}
		transacction.commit();
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

	private void CheckPassword(){
		lockPatternUtils = new LockPatternUtils(this);

		int passwordtype = lockPatternUtils.getKeyguardStoredPasswordQuality();
		if (Build.TYPE.equals("eng")) {
			Log.e(TAG, "passwordtype = "+passwordtype);
		}
		if (!lockPatternUtils.isSecure()){
			if(lockPatternUtils.isLockScreenDisabled()){
				isPasswordExist = false;
			}
		}
		else{
			if(passwordtype == 262144 || passwordtype == 327680 || passwordtype == 131072 ||
					passwordtype == 196608 || passwordtype == 196608){
				isPasswordExist = true;
			}
		}
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //press system back button
        if(0 == resultCode){
            finish();
        }
    }
}
