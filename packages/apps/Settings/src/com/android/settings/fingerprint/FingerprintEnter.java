package com.android.settings.fingerprint;

import android.app.Fragment;
import android.app.Activity;
import android.view.View;
import java.util.Timer;
import java.util.TimerTask;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.settings.R;
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import android.os.Vibrator;
import android.app.Service;
import com.fingerprints.service.FingerprintManager;
import com.fingerprints.service.FingerprintManager.CaptureCallback;
import com.fingerprints.service.FingerprintManager.EnrolCallback;
import com.fingerprints.service.FingerprintManager.IdentifyCallback;
import com.fingerprints.service.FingerprintManager.GuidedData;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.util.Properties;  
import java.util.Random;

public class FingerprintEnter extends Fragment{
	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private final String TAG = "FingerprintEnter";
	private Intent mTrustAgentClickIntent;
	private ImageView imageview;
	private Vibrator vibrator;
	private TextView mGuideText,mGuideNotes1,mGuideNotes2,mGuideNotes3;
	private Handler mStateHandler = null;
	FingerprintManager fpm = null;
	private int i = 0;
	int randomnumber = 0;
	//private int resume = 0;
	private int progress = 0;
	private int identify = 0;
	private int enroltype = 0;
	private int enrolid = 0;
	private String deletename;
	private boolean whetherdelete = false;
	Timer timer = new Timer();

	private final static int IDENTIFY_AGAIN = 1;
	private final static int IDENTIFY_ENROLL = 2;
	private final static int CAPTURE_FAILED = 3;
	private final static int CAPTURE_ONINPUT = 4;
	private final static int ENROLL_FINISH = 5;
	private final static int ENROLL_START = 6;

	AlertDialog.Builder normalDia = null;
	FingerprintEnrol fingerprintenrol =  new FingerprintEnrol();
	AlertDialog dialog = null;
	Uri uri = Uri.parse("content://com.huawei.fingerprintname.FingerprintNameProvider");
	Uri uri2 = Uri.parse("content://com.huawei.fingerprintname.FingerprintNameProvider2");
	private Activity context;
	private boolean bToOtherFragment = false;

	private boolean bDebug = true;
	private boolean bEnrollFinished = true;
	private boolean bOninputType = true;

	ContentResolver resolver = null;
    Toast mtoast = null;
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			if(progress < 15){
				imageview.setImageResource(R.drawable.fingerprint_one);
			}
			else if(progress >= 15&&progress < 30){
				String noteText = getString(R.string.enter_fingerprint_border);
				mGuideText.setText(noteText);
				imageview.setImageResource(R.drawable.fingerprint_two);
			}
			else if(progress >= 30&&progress < 45){
				imageview.setImageResource(R.drawable.fingerprint_three);
			}
			else if(progress >= 45&&progress < 65){
				imageview.setImageResource(R.drawable.fingerprint_four);
			}
			else if(progress >= 65&&progress < 85){
				imageview.setImageResource(R.drawable.fingerprint_five);
			}
			else if(progress >= 85&&progress < 100){
				imageview.setImageResource(R.drawable.fingerprint_six);
				String noteText = getString(R.string.enter_fingerprint_border);
				mGuideText.setText(noteText);
				mGuideNotes1.setText(getString(R.string.enter_fingerprint_guide_five));
				mGuideNotes2.setText(getString(R.string.enter_fingerprint_guide_six));

				Random ran=new Random();
				randomnumber = ran.nextInt(3);
				if (timerTask == null ) {
					createTimer();
				}
				timer.scheduleAtFixedRate(timerTask, 0, 400);
			}
			super.handleMessage(msg);
		}

	};

private Handler timerhandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what){
				case 0:
				switch(randomnumber){
					case 0:
					imageview.setImageResource(R.drawable.fingerprint_up);
					mGuideNotes3.setText(getString(R.string.fingerprint_perfect_up));
					break;
					case 1:
					imageview.setImageResource(R.drawable.fingerprint_down);
					mGuideNotes3.setText(getString(R.string.fingerprint_perfect_down));
					break;
					case 2:
					imageview.setImageResource(R.drawable.fingerprint_left);
					mGuideNotes3.setText(getString(R.string.fingerprint_perfect_left));
					break;
					case 3:
					imageview.setImageResource(R.drawable.fingerprint_right);
					mGuideNotes3.setText(getString(R.string.fingerprint_perfect_right));
					break;
					default:
					break;
				}
				break;
				case 1:
				imageview.setImageResource(R.drawable.fingerprint_six);
				break;
				default:
				break;
			}
			super.handleMessage(msg);
		}

	};
	private Handler reIdentifyHandle = new Handler(){
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what){
				case IDENTIFY_AGAIN:
					if (fpm != null) {
						Log.e(TAG, "reIdentifyHandle, startIdentify");
						fpm.startIdentify(mIdentifyCallback, fpm.getIds());
					} else {
						Log.e(TAG, "fpm == null reIdentify fail ");
					}
					break;

				case IDENTIFY_ENROLL:
					if (fpm != null) {
						Log.e(TAG, "reIdentifyHandle, startEnrol");
						bOninputType = true;
						fpm.startEnrol(mEnrolCallback, getFPCId());
					} else {
						Log.e(TAG, "fpm == null startEnrol fail ");
					}
					break;

				case CAPTURE_FAILED:
					if (normalDia == null){
						normalDia = new AlertDialog.Builder(getActivity());
						normalDia.setTitle(getString(R.string.fingerprint_press));
						normalDia.setMessage(getString(R.string.fingerprint_lost_warning));
						normalDia.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
						dialog = normalDia.create();
					}

					if (dialog != null){
						if (!dialog.isShowing()){
							dialog.show();
						}
					}
					//dialog = normalDia.show();
					break;

				case CAPTURE_ONINPUT:
					if (bOninputType) {
						dovibrate();
					}

					if (dialog != null) {
						Log.d(TAG,"dialog.isShowing() = " + dialog.isShowing());
						if (dialog.isShowing()) {
							dialog.dismiss();
						}
					}
					break;

				case ENROLL_START:
					if (bDebug){
						Log.d(TAG, "ENROLL_START bEnrollFinished = " + bEnrollFinished);
					}
					if (bEnrollFinished) {
						bEnrollFinished = false;
						startSingleCapture();
					}
					break;

				case ENROLL_FINISH:
					if (bDebug){
						Log.d(TAG, "ENROLL_FINISH ----");
					}
					bEnrollFinished = true;
					killfpm();

					Log.d(TAG, "move to fingerprintenrol");
					bToOtherFragment = true;
					Bundle arguments  = new Bundle();
					arguments.putInt("fingerprintname", msg.arg2);
					arguments.putInt("fingerprintid", msg.arg1);
					FragmentManager fragment = getFragmentManager();
					fingerprintenrol.setArguments(arguments);

					FingerprintEnterActivity mainActivity = (FingerprintEnterActivity)getActivity();
					mainActivity.setCurrentFragment(fingerprintenrol);

					FragmentTransaction transacction = fragment.beginTransaction();
					transacction.replace(android.R.id.content, fingerprintenrol, "FingerprintEnrol");
					transacction.commit();
					break;

				default:
					break;
			}

			super.handleMessage(msg);
		}
	};

	CaptureCallback mCaptureCallback = new CaptureCallback() {
		int input = 0;
		/**
		 * Called when the sensor is waiting for the user to touch the sensor with the finger.
		 */
		@Override
		public void onWaitingForInput(){
			if (bDebug){
				Log.d(TAG,"onWaitingForInput");
			}
		}

		/**
		 * Called when the user has put down the finger and the image capture procedure has started.
		 */
		@Override
		public void onInput(){
			if (bDebug){
				Log.d(TAG, "onInput");
			}

			input++;
			Log.d(TAG,"onInput = " + input);
			Log.d(TAG,"dialog = " + dialog);
			Message msg = Message.obtain();
			msg.what = CAPTURE_ONINPUT;
			reIdentifyHandle.sendMessage(msg);
		}

		/**
		 * Called when the image capture procedure has completed.
		 */
		@Override
		public void onCaptureCompleted(){
			if (bDebug){
				Log.d(TAG, "onCaptureCompleted");
			}
		}

		@Override
		public void onCaptureFailed(int reason){
			if (bDebug){
				Log.d(TAG,"onCaptureFailed" + reason);
			}

			Message msg = Message.obtain();
			msg.what = CAPTURE_FAILED;
			reIdentifyHandle.sendMessage(msg);
		}
	};

	EnrolCallback mEnrolCallback = new EnrolCallback(){

		@Override
		public void onProgress(GuidedData arg0) {
			if (bDebug){
				Log.d(TAG, "onProgress");
			}
			// TODO Auto-generated method stub
			Log.d(TAG,"arg0="+arg0.guidedProgress);

			if (progress < arg0.guidedProgress) {
				progress = arg0.guidedProgress;
				Message mesasge = new Message();
				handler.sendMessage(mesasge);
			}
		}

		@Override
		public void onEnrolled(int who) {
			if (bDebug){
				Log.d(TAG, "onEnrolled");
			}

			if (fpm == null){
				Log.e(TAG, "fpm is null return from onEnrolled");
				return;
			}

			Log.d(TAG, "who=" + who);
			int total = 0;
			if (fpm.getIds() != null){
				Cursor cursor = resolver.query(uri, null, null, null, null);
					// total = fpm.getIds().length;
				if(cursor == null){
            	Log.i(TAG, "insertNameDB cursor == null");
            		return;
        		}else{
            		total = getName(resolver, cursor);
        		}
				cursor.close();
				Log.d(TAG, "getName(resolver, cursor) total = " + total);
			}

			//只有一个指纹时设置指纹解锁
			if (total == 1) {
				final UserManager um = (UserManager)getActivity().getSystemService(Context.USER_SERVICE);
				Settings.System.putInt(resolver, Settings.System.FINGERPRINT_KEYGUARD_SWITCH, 1);
				System.setProperty("sys.resettype", "1"); 
			}
			
			Message msg = Message.obtain();
			msg.what = ENROLL_FINISH;
			msg.arg1 = who;
			msg.arg2 = total;
			reIdentifyHandle.sendMessage(msg) ;

			//录入完成写数据库
//			SaveFingerprintToDB(who);
		}

		@Override
		public void onEnrollmentFailed() {
			Log.d(TAG, "onEnrollmentFailed");
		}
	};

	private int getName(ContentResolver resolver, Cursor cursor){
		if (bDebug){
			Log.d(TAG, "getName");
		}

		int ret = 1;
		ContentValues values = new ContentValues();
		if(cursor.getCount() == 0){
			Log.i(TAG,"1---query fp count is 0.");
		}else{
			cursor.moveToLast();
			ret = cursor.getInt(0);
			Log.i(TAG, "2 return moveToLast ret:" + ret);
		}
		return ret;
	}

	private void SaveFingerprintToDB(int fpcIndex){

		if (bDebug){
			Log.i(TAG, "SaveFingerprintToDB");
		}

		final int fpcIdx = fpcIndex;

		new Thread(new Runnable() {
			@Override
			public void run() {
				int total = 0;
				if(fpm.getIds() == null){
					total = 1;
					Log.d(TAG,"total = 1;;;;;");
					ContentValues cvReset = new ContentValues();

					//回复ID值为0
					cvReset.put("seq", 0);
					resolver.update(uri, cvReset, "##", null);
				}
				else{
					Cursor cursor = resolver.query(uri, null, null, null, null);
					// total = fpm.getIds().length;
					total = getName(resolver, cursor);
					cursor.close();
				}

				String name = getString(R.string.fingerprint_settings_title) + total;
				ContentValues values = new ContentValues();
				values.put("name", name);
				values.put("mode", 0);
				values.put("fpcid", fpcIdx);
				Log.i(TAG, "Save Fingerprint to DB name is " + name + " fpcid = " + fpcIdx);
				resolver.insert(uri, values);

				Message message = new Message();
				message.what = ENROLL_FINISH;
				message.arg1 = fpcIdx;
				message.arg2 = total;

				reIdentifyHandle.sendMessage(message);
			}
		}).start();
	}

	IdentifyCallback mIdentifyCallback = new  IdentifyCallback(){
		@Override
		public void onIdentified(int fingerId, boolean updated){
			if (bDebug){
				Log.d(TAG, "onIdentified");
			}

			if (mtoast != null){
				mtoast.setText(getString(R.string.fingerprint_enrol_another));
				mtoast.setDuration(Toast.LENGTH_SHORT);
				mtoast.show();
			} else{
				mtoast = Toast.makeText(getActivity().getApplicationContext(),
						getString(R.string.fingerprint_enrol_another), Toast.LENGTH_SHORT);
				mtoast.show();
			}
			//指纹相同换另外的手指
			/*mtoast = Toast.makeText(getActivity().getApplicationContext(),
					getString(R.string.fingerprint_enrol_another), Toast.LENGTH_SHORT);
			mtoast.show();*/

			//fpm.startIdentify(mIdentifyCallback, fpm.getIds());
			//重新开始录指纹
			Message msg = Message.obtain();
			msg.what = IDENTIFY_AGAIN;
			reIdentifyHandle.sendMessage(msg);
		}

		@Override
		public  void onNoMatch(int nomatchReason){
			if (bDebug) {
				Log.d(TAG, "onNoMatch");
			}
			//fpm.startEnrol(mEnrolCallback, getFPCId());
			Message msg = Message.obtain();
			msg.what = IDENTIFY_ENROLL;
			reIdentifyHandle.sendMessage(msg);
		}
	};

	private int getFPCId(){
		if (bDebug){
			Log.d(TAG, "getFPCId");
		}

		int ret = 0;
		Cursor c = getActivity().getContentResolver().query(uri, null, null, null, null);
		while(c.moveToNext()){
			int oldId = c.getInt(3);
			if(oldId >= ret){
				ret = oldId + 1;
			}
		}
		return ret;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
			mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}

		context = getActivity();
		resolver = getActivity().getContentResolver();
	}

	private void startSingleCapture() {
		if (bDebug){
			Log.d(TAG, "startSingleCapture");
		}

		if (fpm!= null) {
			fpm.abort();
			fpm.release();
			fpm = null;
		}

		fpm = FingerprintManager.open();
		if (fpm != null) {
			Log.d(TAG, "FingerprintManager open");
			Log.d(TAG, "fpm.getIds() = " + fpm.getIds());
			fpm.startUseCase();
			fpm.setCaptureCallback(mCaptureCallback);
			if (fpm.getIds() == null) {
				Log.d(TAG, "fpm.startEnrol");
				fpm.startEnrol(mEnrolCallback, 0);
			} else {
				Log.d(TAG, "fpm.startIdentify");
				bOninputType = false;
				fpm.startIdentify(mIdentifyCallback, fpm.getIds());

			}
		} else {
			Log.e(TAG, "FingerprintManager.open return null");
		}
	}

	private void killfpm() {
		if (bDebug){
			Log.d(TAG, "killfpm");
		}

		if(fpm!= null) {
			//fpm.deleteFingerData(0);
			Log.d(TAG, "stopUseCase is called");
			fpm.stopUseCase();
			fpm.abort();
			fpm.release();
			Log.d(TAG, "release is called");
			fpm = null;
		}
	}

	private void dovibrate(){
		vibrator = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);
		long [] pattern = {50,100,0,0};
		vibrator.vibrate(pattern,-1);
	}

	TimerTask timerTask = null;
	void createTimer() {
		timerTask = new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				i++;
				Message mesasge = new Message();
				mesasge.what = i%2;
				timerhandler.sendMessage(mesasge);
			}
		};
	}

	@Override
	public void onResume() {
		super.onResume();

		if (bDebug){
			Log.d(TAG, "onResume");
		}

		Message msg = Message.obtain();
		msg.what = ENROLL_START;
		reIdentifyHandle.sendMessage(msg);
	}

	@Override
	public void onPause() {
		super.onPause();

		if (bDebug){
			Log.d(TAG, "onPause");
		}
		if (mtoast != null) {
			mtoast.cancel();
		}
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
		killfpm();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView =  inflater.inflate(R.layout.fingerprint_enter, container,false);

		mGuideText =(TextView) rootView.findViewById(R.id.fingerprint_enter_guide);
		mGuideNotes1 =(TextView) rootView.findViewById(R.id.fingerprint_enter_note_one);
		mGuideNotes2 =(TextView) rootView.findViewById(R.id.fingerprint_enter_note_two);
		mGuideNotes3 =(TextView) rootView.findViewById(R.id.fingerprint_enter_Perfect);

		imageview =(ImageView) rootView.findViewById(R.id.imageview);

		return rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTrustAgentClickIntent != null) {
			outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
		}
	}
}
