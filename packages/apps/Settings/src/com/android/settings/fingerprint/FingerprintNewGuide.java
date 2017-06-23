package com.android.settings.fingerprint;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.settings.R;

import java.util.Timer;
import java.util.TimerTask;

public class FingerprintNewGuide extends Fragment {
	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private Intent mTrustAgentClickIntent;

	private ImageView imageview;
	private Vibrator vibrator;
	private final String TAG = "FingerprintNewGuide";
	private int i = 0;

	//private int resume = 0;
	Timer timer = new Timer();
	private boolean bToOtherFragment = false;

	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			if (i > 3) {
				i = 0;
			}
			else {
				switch (i)
				{
					case 1:
						imageview.setImageResource(R.drawable.fingerprint_control_up);
						break;
					case 2:
						imageview.setImageResource(R.drawable.fingerprint_control_press);
						break;
					case 3:
						imageview.setImageResource(R.drawable.fingerprint_control_vibrate);
						break;
					default:
						break;
				}
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
			mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}
	}

	TimerTask timerTask = null;
	void createTimer() {
		timerTask = new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				i++;
				Message mesasge = new Message();
				mesasge.what = i;
				handler.sendMessage(mesasge);
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();
		if (timerTask == null ) {
			createTimer();
		}
		timer.scheduleAtFixedRate(timerTask, 0, 400);
	}

	@Override
	public void onPause() {
		Log.d(TAG, "Fragment onPause");
		super.onPause();
		timer.cancel();

		if (!bToOtherFragment) {
			getActivity().finish();
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView =  inflater.inflate(R.layout.fingerprint_new_guide, container,false);
		imageview = (ImageView)rootView.findViewById(R.id.imageview);
		return rootView;

	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTrustAgentClickIntent != null) {
			outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
		}
	}

	private void dovibrate(){
		vibrator = (Vibrator)getActivity().getSystemService(Service.VIBRATOR_SERVICE);
		long [] pattern = {50,100,0,0};
		vibrator.vibrate(pattern, -1);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == event.KEYCODE_FPC) {
			dovibrate();

			bToOtherFragment = true;
			FragmentManager fragment=getFragmentManager();
			FragmentTransaction transacction=fragment.beginTransaction();
			FingerprintEnter fingerprintenter =  new FingerprintEnter();

			FingerprintEnterActivity mainActivity = (FingerprintEnterActivity)getActivity();
			mainActivity.setCurrentFragment(fingerprintenter);

			transacction.replace(android.R.id.content, fingerprintenter,"fingerprintenter");
			transacction.commit();
		}
		return true;
	}

}
