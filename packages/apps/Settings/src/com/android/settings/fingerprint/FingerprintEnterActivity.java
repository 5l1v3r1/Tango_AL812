package com.android.settings.fingerprint;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.android.settings.R;
public class FingerprintEnterActivity extends Activity{

	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";

    private Intent mTrustAgentClickIntent;
	private static Fragment fgCurrent = null;
	//FingerprintManage fingerprintmanage =  new FingerprintManage();
	FingerprintNewGuide fingerprintnewguide = new FingerprintNewGuide();
//	FingerprintEnter fingerprintenter = new FingerprintEnter();

	//private PowerManager pm;
	//PowerManager.WakeLock mWakeLock;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!getIntent().hasExtra("safe_enter")) {//add protection for security by liuchaochao
			Log.i("lcc", "forbid to enter FingerprintEnterActivity due to no hasExtra ");
			finish();
			return;
		}
		if (savedInstanceState != null
		        && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
		    mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}
		setTitle(getString(R.string.new_fingerprint));

		setCurrentFragment(fingerprintnewguide);

		FragmentManager fragment = getFragmentManager();
		FragmentTransaction transacction = fragment.beginTransaction();
		transacction.replace(android.R.id.content, fingerprintnewguide,"fingerprintnewguide");
		Log.d("Fingerprint", "R.id.content" + R.id.content);
		transacction.commit();

		//pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		//mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "FingerprintEnterActivity");
	}
	@Override
	public void onResume() {

		super.onResume();
		//mWakeLock.acquire();
	}

	@Override
	public void onPause() {
		super.onPause();
		//mWakeLock.release();
	}
  
	@Override
	public void onDestroy() {
        	super.onDestroy();

	}

	@Override  
    public boolean onKeyDown(int keyCode, KeyEvent event)  
    {  
        if (keyCode == KeyEvent.KEYCODE_BACK )  
        {  
			Intent intent = new Intent(FingerprintEnterActivity.this , FingerprintManageActivity.class);
			intent.putExtra("safe_enter","fingerprint_manager");//add protection for security by liuchaochao
			startActivity(intent);
			finish();
        } else {
			if (fgCurrent instanceof FingerprintNewGuide){
				fingerprintnewguide.onKeyDown(keyCode, event);
			}
		}
          
        return false;  
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTrustAgentClickIntent != null) {
		    outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
		}
	}

	public boolean setCurrentFragment(Fragment fragment){
		if (fragment != null) {
			fgCurrent = fragment;
			return true;
		} else {
			return false;
		}
	}
}
