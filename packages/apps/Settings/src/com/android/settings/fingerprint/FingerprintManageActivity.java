package com.android.settings.fingerprint;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import android.view.Display;
import com.fingerprints.service.FingerprintManager;
import android.widget.Toast;

public class FingerprintManageActivity extends Activity{

	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
 	private static final String TAG = "FingerprintManageActivity";
	private static final boolean bDebug = true;
	static Activity ActivityFingerprintManage;
    private Intent mTrustAgentClickIntent;
    //FingerprintManager fpm = null;
	FingerprintManage fingerprintmanage =  new FingerprintManage();
	//FingerprintNewGuide fingerprintnewguide = new FingerprintNewGuide();
	//FingerprintEnter fingerprintenter = new FingerprintEnter();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (bDebug){
			Log.i(TAG, "onCreate");
		}

		if (!getIntent().hasExtra("safe_enter")) {//add protection for security by liuchaochao
			Log.i(TAG, "forbid to enter fingerprintmanageractivity due to no hasExtra ");
			finish();
			return;
		}

		if (savedInstanceState != null &&
				savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
		    mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}


	}

	@Override
	protected void onStart() {
		super.onStart();

		ActivityFingerprintManage = this;
		setTitle(getString(R.string.fingerprint_settings_title));
		FragmentManager fragment = getFragmentManager();
		FragmentTransaction transacction = fragment.beginTransaction();
		transacction.replace(android.R.id.content, fingerprintmanage, "fingerprintmanage");
		transacction.commit();
		
		if (getIntent().getBooleanExtra("associate_fp", false)) {//add by liuchaochao for HQ01401891
            Toast.makeText(getApplicationContext(), getString(R.string.associate_fingerprint_hint),
            Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onResume() {
		super.onResume();

		if (bDebug){
			Log.i(TAG, "onResume");
		}


	}

	@Override
	public void onPause() {
		super.onPause();

		if (bDebug){
			Log.i(TAG, "onPause");
		}
	}
  
	@Override
	public void onDestroy() {
		if (bDebug){
			Log.i(TAG, "onPause");
		}

		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTrustAgentClickIntent != null) {
		    outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
		}
	}
	
//	private void killfpm() {
//		if(fpm!= null) {
//			fpm.release();
//			fpm = null;
//		}
//	}
}
