package com.android.settings.fingerprint;

import android.app.Fragment;
import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener; 
import android.text.TextWatcher;
import android.text.Editable;
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
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.mediatek.settings.ext.IPermissionControlExt;
import com.mediatek.settings.ext.IPplSettingsEntryExt;
import com.mediatek.settings.ext.IMdmPermissionControlExt;
import com.mediatek.settings.ext.IDataProtectionExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import android.text.TextWatcher;  
import java.util.ArrayList;
import java.util.List;
import com.android.settings.R;
import android.graphics.Color;  
import android.text.method.DialerKeyListener;
import com.android.internal.widget.LockPatternUtils;
import android.view.inputmethod.EditorInfo;
import android.app.admin.DevicePolicyManager;
import android.text.InputType;
public class FingerprintCheckPassword extends Fragment{
	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";

	private LockPatternUtils mLockPatternUtils;
    private Intent mTrustAgentClickIntent;
	private Button mCancle;
	private Button mNextstep;
	private EditText mPassword;
	private TextView mGuideText;
	private boolean isPasswordExist = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null
		        && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
		    mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	     Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView =  inflater.inflate(R.layout.fingerprint_check_password, container,false);
		return rootView;
	}

	@Override  
	public void onActivityCreated(Bundle savedInstanceState) {  
		super.onActivityCreated(savedInstanceState);
		mLockPatternUtils = new LockPatternUtils(getActivity());
		mGuideText = (TextView) getActivity().findViewById(R.id.fingerprint_password_guide);
		mPassword = (EditText) getActivity().findViewById(R.id.fingerprint_password);
		mCancle = (Button) getActivity().findViewById(R.id.cancle);
		mNextstep = (Button) getActivity().findViewById(R.id.nextstep);
		mNextstep.setClickable(false);
		mNextstep.setTextColor(Color.GRAY);
		int passwordtype = mLockPatternUtils.getKeyguardStoredPasswordQuality();
		if (passwordtype == 131072 ||passwordtype == 196608) {
			mPassword.setKeyListener(DialerKeyListener.getInstance());
		}
		TextWatcher textWatcher = new TextWatcher() {
			@Override    
			public void afterTextChanged(Editable s) {
				if(s.toString().equals("")){
					mNextstep.setClickable(false);
					mNextstep.setTextColor(Color.GRAY);
					mNextstep.setBackgroundResource(R.drawable.button_border_unclickable);
				}
			}   
			@Override 
			public void beforeTextChanged(CharSequence s, int start, int count,  
				int after) {  
			}  
			@Override
			public void onTextChanged(CharSequence s, int start, int before,     
			int count) {
				mNextstep.setClickable(true);
				mNextstep.setTextColor(Color.BLACK);
				mNextstep.setBackgroundResource(R.drawable.button_border_clickable);

			}                    
		};  
		mPassword.addTextChangedListener(textWatcher);
		mCancle.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				Log.d("Fingerprint","mCancle is clicked");
				getActivity().finish();
			}
		});

		mNextstep.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v)
			{
				String password = mPassword.getText().toString();
				String notetext = getString(R.string.enter_unlock_password_wrong);
				if(mLockPatternUtils.checkPassword(password)){
					Intent intent = new Intent(getActivity(), FingerprintManageActivity.class);
					intent.putExtra("safe_enter","fingerprint_manager");//add protection for security by liuchaochao
					getActivity().startActivity(intent);
					getActivity().finish();
				}
				else{
					mGuideText.setText(notetext);
					mPassword.setText(""); 
				}
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTrustAgentClickIntent != null) {
		    outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
		}
	}

}
