package com.android.settings.fingerprint;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.search.Indexable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.fingerprints.service.FingerprintManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import java.util.Date;
//import android.content.ContentValues;

public class FingerprintManage extends SettingsPreferenceFragment implements
		OnPreferenceChangeListener, Indexable {
	private final String TAG = "FingerprintManage";
	private final boolean bDebug = true;

	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private static final String KEY_FINGERPRINT_LIST = "fingerprint_use_category";
	private static final String KEY_NEW_FINGERPRINT = "new_fingerprint";

	private static final String KEY_TOGGLE_UNLOCK = "toggle_unlock";
	private static final String KEY_TOGGLE_SECURITY = "toggle_security";
	private static final String KEY_TOGGLE_APPLOCK = "toggle_applock";
	private static final String KEY_TOGGLE_CHECK_HUAWEI = "toggle_check_huawei";

	private static final int REQUESTCODE_BIND_APPLOCK = 302;
	private static final int REQUESTCODE_BIND_SECUIRITY = 303;
	private static final int REQUESTCODE_FIRST_BIND_APPLOCK = 305;
	private static final int REQUESTCODE_FIRST_BIND_SECUIRITY = 304;
	/* HQ_yulisuo 2015-07-30 modified for bind app lock and fingerprint */
	private boolean unfinishActivityFlag = false;
	private static final String TELEPHONY_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private SmsReceiver receiver = null;
	private Activity context;
	private Intent mTrustAgentClickIntent;
	private Preference mNewFingerprint;
	private Preference[] mFingerprintList = new Preference[5];
	//private Preference mFingerprintList;
	private SwitchPreference mToggleLock;
	private SwitchPreference mToggleSecurity;
	private SwitchPreference mToggleApplock;
	FingerprintManager fpm = null;
	private int switchtype = 0;
	private static boolean bFirstEnterManage = false;
	private int[] idFingerprintList = new int[5];
	private InitStatus initStatus = null;

	private int[] fpcid = new int[5];

	private int[] fingerprint_contain = new int[] {
			R.xml.fingerprint_contain_one,
			R.xml.fingerprint_contain_two,
			R.xml.fingerprint_contain_three,
			R.xml.fingerprint_contain_four,
			R.xml.fingerprint_contain_five
	};

	private CharSequence[] fingerprintPreName = new CharSequence[] {
			"fingerprint_one",
			"fingerprint_two",
			"fingerprint_three",
			"fingerprint_four",
			"fingerprint_five"
	};

	int []ids = null;

	Uri uri = Uri.parse("content://com.huawei.fingerprintname.FingerprintNameProvider");
	/*HQ_yulisuo modify for HQ01401840 1027*/
	Date bindBeginTime,bindEndTime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (bDebug){
			Log.i(TAG, "onCreate");
		}
		//第一次进入FingerManager
		bFirstEnterManage = true;

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
			mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}
		addPreferencesFromResource(R.xml.fingerprint_manage);
		context = getActivity();

		Cursor cursor = null;

		//add by HQ_yulisuo for HQ01377948 2015-9-11
		initStatus = new InitStatus();
		initStatus.start();
		//add by HQ_yulisuo for HQ01377948 2015-9-11 end
		addPreferencesFromResource(R.xml.fingerprint_contain_five);

		receiver = new SmsReceiver();
		IntentFilter filter = new IntentFilter(TELEPHONY_SMS_RECEIVED);
		context.registerReceiver(receiver, filter);
	}

	class InitStatus extends Thread{
		
		@Override
		public void run() {
			super.run();
			int appLockStatus = getAppLockBindStatus();
			int securityStatus = getSecurityBindStatus();
			Message msg = mHandler.obtainMessage();
			msg.what = 0x01;
			msg.obj = new int[]{appLockStatus,securityStatus};
			mHandler.sendMessage(msg);
		}
	}

	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0x01:
				int [] status = (int [])msg.obj;
				Log.i(TAG, "InitStatus s1:"+status[0]+",s2:"+status[1]);
				setFingerprintApplockAllowed(status[0] == 1);
				setFingerprintSecurityAllowed(status[1] == 1);
				break;

			case 0x02:
				initUIData((Cursor)msg.obj);
				break;
			default:
				break;
			}
		};
	};


	@Override
	public void onStart() {
		super.onStart();
		if (bDebug){
			Log.i(TAG, "onStart");
		}
		//一般情况下都退出
		unfinishActivityFlag = false;
		new GetFingerprintNames().start();
	}	


	class GetFingerprintNames extends Thread{	
		@Override
		public void run() {
			super.run();
			Cursor nameCursor = context.getContentResolver().query(uri, null, null, null, null);
			Message msg = mHandler.obtainMessage();
			msg.what = 0x02;
			msg.obj = nameCursor;
			mHandler.sendMessage(msg);
		}
	}

	private void initUIData(Cursor nameCursor){
		PreferenceScreen root = getPreferenceScreen();
		root.removePreference(root.findPreference("fingerprint_list_category"));
		try {
			int fpCount = nameCursor.getCount();

			//只有第一次进入且没有指纹是提示输入指纹
			if (fpCount == 0){
				Log.d(TAG, "add new fingerprint view");
				setFingerprintLockAllowed(false);
				addPreferencesFromResource(R.xml.fingerprint_contain_zero);
				if (bFirstEnterManage == true) {
					//每次进入manager，dialogInvitetoEnrol只启动一次
					bFirstEnterManage = false;

					//dialogInvitetoEnrol关了之后不退出FingerManager
					unfinishActivityFlag = true;
					dialogInvitetoEnrol();
				}
			} else {
				Log.d(TAG, "add fingerprint view");

				//根据指纹个数加载布局文件
				addPreferencesFromResource(fingerprint_contain[fpCount - 1]);
			}
   
			if(bDebug){
				Log.i(TAG, "set ui data");
			}

			mNewFingerprint = root.findPreference(KEY_NEW_FINGERPRINT);
			for (int i = 0; i < 5; i++){
				mFingerprintList[i] = root.findPreference(fingerprintPreName[i]);
			}

			nameCursor.moveToFirst();
			for (int i = 0; i < fpCount; i++){
				idFingerprintList[i] = nameCursor.getInt(0);

				fpcid[i] = nameCursor.getInt(3);
				Log.d(TAG, "fpcid[" + i +"] = " + fpcid[i]);

				if(nameCursor.getInt(2) == 0){
					mFingerprintList[i].setTitle(nameCursor.getString(1));
				}
				else{
					mFingerprintList[i].setTitle(nameCursor.getString(1) + getString(R.string.fingerprint_child));
				}

				nameCursor.moveToNext();
			}
			nameCursor.close();
		} catch (Exception e) {
			if (nameCursor != null)	nameCursor.close();
		}

		mToggleLock = (SwitchPreference) findPreference(KEY_TOGGLE_UNLOCK);
		if(mToggleLock != null){
			if(ids == null){
				mToggleLock.setChecked(false);
			}
			else{
				mToggleLock.setChecked(isFingerprintLockAllowed());

			}
			mToggleLock.setOnPreferenceChangeListener(this);
		}


		mToggleSecurity = (SwitchPreference) findPreference(KEY_TOGGLE_SECURITY);
		if(mToggleSecurity != null){
			if(ids == null){
				mToggleSecurity.setChecked(false);
				/* add by HQ_yulisuo for HQ01378093 at 2015-9-15 */
				unbindSecurity(false);
			}
			else{
				mToggleSecurity.setChecked(isFingerprintSecurityAllowed());
			}
			mToggleSecurity.setOnPreferenceChangeListener(this);
		}


		mToggleApplock = (SwitchPreference) findPreference(KEY_TOGGLE_APPLOCK);
		if(mToggleApplock != null){
			if(ids == null){
				mToggleApplock.setChecked(false);
				/* add by HQ_yulisuo for HQ01378093 at 2015-9-15 */
				unbindAppLock(false);
			}
			else{
				mToggleApplock.setChecked(isFingerprintApplockAllowed());
			}
			mToggleApplock.setOnPreferenceChangeListener(this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (bDebug){
			Log.i(TAG, "onResume");
		}

		//获取FPC指纹数
		fpm = FingerprintManager.open();
		ids = fpm.getIds();
		killfpm();
		Log.i(TAG, "ids = " + ids);
		/*if (ids == null) {
			ContentResolver mResolver = getActivity().getContentResolver();
			mResolver.delete(uri, null ,null);
			ContentValues cvReset = new ContentValues();
			//修改数据库id为0
			cvReset.put("seq", 0);
			getActivity().getContentResolver().update(uri, cvReset, "##", null);
			Log.i(TAG, "For ids == null,Deleted the table = " + uri);
		}*/
	}

	@Override
	public void onPause() {
		super.onPause();

		if (bDebug){
			Log.i(TAG, "onPause unfinishActivityFlag = " + unfinishActivityFlag);
		}
		/* HQ_yulisuo 2015-07-30 modified for bind app lock and fingerprint */

		if(! unfinishActivityFlag ){
			if (initStatus != null) {
				initStatus.interrupt();
			}
			getActivity().finish();
		}
	}
	@Override
	public void onStop() {
		super.onStop();
		if (bDebug){
			Log.i(TAG, "onStop");
		}
		
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, " onDestroy,fpm = " + fpm);
		if (receiver != null){
			context.unregisterReceiver(receiver);
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == REQUESTCODE_BIND_APPLOCK){			//app lock
			//query if bound
			Uri uri = Uri.parse("content://com.huawei.systemmanager.applockprovider/fingerprintstatus");
			Cursor c = getActivity().getContentResolver().query(uri, null, null, null, null);
			if(c != null){
				Log.i(TAG, "onActivityResult cursor count : " + c.getCount());
			}else{
				Log.i(TAG, "onActivityResult cursor is null");
				return;
			}

			for (c.moveToFirst(); ! c.isAfterLast(); c.moveToNext()) {
				int res = c.getInt(c.getColumnIndex("fingerprintBindType"));
				Log.i(TAG,"onActivityResult res:"+res);
				if(res == 1){
					setFingerprintApplockAllowed(true);
				}
			}
		}else if(requestCode == REQUESTCODE_BIND_SECUIRITY){
			//query if bound
			// Uri uri = Uri.parse("content://com.huawei.hidisk.fingerprint");
			// Bundle resBundle = getActivity().getContentResolver().call(uri, "query_is_box_bindstat", null, null);
			// int res = resBundle.getInt("fingerprintBindType", -2);
			if(data != null){
				boolean ret = data.getBooleanExtra("isSuccess", false);
				if(ret){
					setFingerprintSecurityAllowed(true);
				}
				Log.d(TAG,"resultCode:"+resultCode+",if bind res:"+ret);
			}else{
				Log.d(TAG,"the return intent is null");
			}

		}else if(requestCode == REQUESTCODE_FIRST_BIND_SECUIRITY){
			int status = getSecurityBindStatus();
			Log.d(TAG,"requestCode == 304 status:"+status);
			if(status == 1){
				setFingerprintSecurityAllowed(true);
			}
		}else if(requestCode == REQUESTCODE_FIRST_BIND_APPLOCK){
			// Toast.makeText(getActivity(), "#305#" ,Toast.LENGTH_LONG).show();
			int status = getAppLockBindStatus();
			Log.d(TAG,"requestCode == 305 status:"+status);
			if(status == 1){
				setFingerprintApplockAllowed(true);
			}
		}
		//以上设置后都不退出
		unfinishActivityFlag = false;
		ifBindTimeOut();
	}

	/*HQ_yulisuo modify for HQ01401840 1027*/
	private void ifBindTimeOut(){
		bindEndTime = new Date();
		if((bindEndTime.getTime() - bindBeginTime.getTime()) > 120000){
			Log.d(TAG,"bind timeout,finish activity.");
			getActivity().finish();
		}
		bindBeginTime = null;
		bindEndTime = null;
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

		if(preference == mNewFingerprint){
			if (ids  == null) {
				Intent intent1 = new Intent(getActivity(), FingerprintEnterActivity.class);
				intent1.putExtra("safe_enter","fingerprint_enter");//add protection for security by liuchaochao
				getActivity().startActivity(intent1);
				getActivity().finish();
			}
			else {
				if(ids.length == 5){
					Toast.makeText(getActivity().getApplicationContext(), getString(R.string.fingerprint_max_number),
							Toast.LENGTH_SHORT).show();
				}else{
					Intent intent1 = new Intent(getActivity(), FingerprintEnterActivity.class);
					intent1.putExtra("safe_enter","fingerprint_enter");//add protection for security by liuchaochao
					getActivity().startActivity(intent1);
					getActivity().finish();
				}
			}
		}
		else {
			int prefIndex = 0;
			boolean bClicked = false;

			if (preference == mFingerprintList[0]) {
				bClicked = true;
				prefIndex = 0;
			} else if (preference == mFingerprintList[1]) {
				bClicked = true;
				prefIndex = 1;
			} else if (preference == mFingerprintList[2]) {
				bClicked = true;
				prefIndex = 2;
			} else if (preference == mFingerprintList[3]) {
				bClicked = true;
				prefIndex = 3;
			} else if (preference == mFingerprintList[4]) {
				bClicked = true;
				prefIndex = 4;
			}

			if (bClicked) {
				Log.i(TAG, "delete fpcid = " + fpcid[prefIndex]);
				String fingerprintlist = mFingerprintList[prefIndex].getTitle().toString();
				Intent intent = new Intent(getActivity(), FingerprintDeleteActivity.class);
				intent.putExtra("safe_enter", "fingerprint_delete");//add protection for security by liuchaochao
				Bundle bundle = new Bundle();
				bundle.putInt("fingerprintid", idFingerprintList[prefIndex]);
				bundle.putInt("fpcid", fpcid[prefIndex]);
				bundle.putString("fingerprintname", fingerprintlist);
				intent.putExtras(bundle);

				//录完指纹还要会到fingermanager，不退出压入堆栈
				unfinishActivityFlag = true;
				getActivity().startActivity(intent);
			}
		}
		return false;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object value) {
		final String key = preference.getKey();
		if (KEY_TOGGLE_UNLOCK.equals(key)) {
			Log.e(TAG, "@@value = " + value);
			if ((Boolean) value) {
				switchtype  = 1;
				setFingerprintLockAllowed(true);
				mToggleLock.setChecked((Boolean) value);
				if(ids == null){
					dialogWarntoEnrol();
					//setFingerprintLockAllowed(false);
					mToggleLock.setChecked(false);
				}
				Log.i(TAG,"mToggleLock.isChecked START RecieveFingerprintService");

			}else {
				setFingerprintLockAllowed(false);
				mToggleLock.setChecked((Boolean) value);
				Log.i(TAG,"mToggleLock.isChecked stop RecieveFingerprintService");
			}
		}
		else if(KEY_TOGGLE_SECURITY.equals(key)){
			Log.e(TAG, "@@value = " + value);
			if ((Boolean) value) {
				switchtype  = 2;
				// setFingerprintSecurityAllowed(true);
				mToggleSecurity.setChecked((Boolean) value);
				/* add by yulisuo for HQ01293949 */
				if(ids == null){
					dialogWarntoEnrol();
					setFingerprintSecurityAllowed(false);
					mToggleSecurity.setChecked(false);
				}else{
					int status = getSecurityBindStatus();
					if(status == -1){
						AlertDialog.Builder builder;
						Context mcon = getActivity();
						builder = new AlertDialog.Builder(mcon);
						String title  = mcon.getResources().getString(R.string.notify_security_title);
						String msg = mcon.getResources().getString(R.string.notify_security_msg);
						String enable  = mcon.getResources().getString(R.string.enable);
						String cancel = mcon.getResources().getString(R.string.cancel);
						builder.setTitle(title).setMessage(msg).setPositiveButton(enable, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								unfinishActivityFlag = true;
								Intent intent = new Intent();
								intent.setAction("huawei.intent.action.STRONGBOX_FINGERPRINT_MANAGER");
								intent.setPackage("com.huawei.hidisk");
								intent.putExtra("fingerprintAuthSwitchType", 2); //输入密码绑定
								startActivityForResult(intent, REQUESTCODE_FIRST_BIND_SECUIRITY);
								bindBeginTime = new Date();
							}
						}).setNegativeButton(cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								setFingerprintSecurityAllowed(false);
								mToggleSecurity.setChecked(false);
							}
						}).create().show();

					}else if(status == 0){
						unfinishActivityFlag = true;
						Intent intent = new Intent();
						intent.setAction("huawei.intent.action.STRONGBOX_FINGERPRINT_MANAGER");
						intent.setPackage("com.huawei.hidisk");
						intent.putExtra("fingerprintAuthSwitchType", 1);
						startActivityForResult(intent, REQUESTCODE_BIND_SECUIRITY);
						bindBeginTime = new Date();
					}

					Log.d(TAG,"mToggleSecurity.isChecked");
				}
				/* add by yulisuo end */
				Log.d("Fingerprint","isFingerprintSecurityAllowed == true");
			}else {
				unbindSecurity((Boolean)value);
			}
		}
		else if(KEY_TOGGLE_APPLOCK.equals(key)){
			Log.e(TAG, "@@value = " + value);
			if ((Boolean) value) {
				switchtype  = 3;
				// setFingerprintApplockAllowed(true);
				mToggleApplock.setChecked((Boolean) value);			//??? ask xuweijie
				if(ids == null){
					dialogWarntoEnrol();
					setFingerprintApplockAllowed(false);
					mToggleApplock.setChecked(false);
				}else{
					int status = getAppLockBindStatus();
					if(status == -1){		//app lock has not been open
						setFingerprintApplockAllowed(false);
						mToggleApplock.setChecked(false);
						AlertDialog.Builder builder;
						Context mcon = getActivity();
						builder = new AlertDialog.Builder(mcon);
						String title  = mcon.getResources().getString(R.string.notify_applock_title);
						String msg = mcon.getResources().getString(R.string.notify_applock_msg);
						String enable  = mcon.getResources().getString(R.string.enable);
						String cancel = mcon.getResources().getString(R.string.cancel);
						builder.setTitle(title).setMessage(msg).setPositiveButton(enable, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								unfinishActivityFlag = true;
								Intent intent = new Intent();
								intent.setAction("huawei.intent.action.APPLOCK_FINGERPRINT_INIT");
								intent.setPackage("com.huawei.systemmanager");
								startActivityForResult(intent, REQUESTCODE_FIRST_BIND_APPLOCK);
								bindBeginTime = new Date();
							}
						}).setNegativeButton(cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
							}
						}).create().show();
					}else if(status == 0){
						unfinishActivityFlag = true;
						Intent intent = new Intent();
						intent.setAction("huawei.intent.action.APPLOCK_FINGERPRINT_MANAGER");
						intent.setPackage("com.huawei.systemmanager");
						intent.putExtra("fingerprintAuthSwitchType", 1);
						startActivityForResult(intent, REQUESTCODE_BIND_APPLOCK);
						bindBeginTime = new Date();
					}
					Log.d(TAG,"isFingerprintApplockAllowed == true");
				}
			}else {
				unbindAppLock((Boolean)value);
			}
		}
		return false;
	}
	/* add by HQ_yulisuo for HQ01378093 at 2015-9-15 */
	private void unbindSecurity(boolean value){
		Uri uri = Uri.parse("content://com.huawei.hidisk.fingerprint");
		Bundle resBundle = getActivity().getContentResolver().call(uri, "unbind_fingerprint", null, null);
		int res = resBundle.getInt("fingerprintBindType", -2);
		Log.d(TAG,"Security unbind res:"+res);
		Log.d(TAG,"isFingerprintSecurityAllowed == false");
		setFingerprintSecurityAllowed(false);
		mToggleSecurity.setChecked((Boolean) value);
	}

	private void unbindAppLock(boolean value){
		Uri uri = Uri.parse("content://com.huawei.systemmanager.applockprovider");
		Bundle resBundle = getActivity().getContentResolver().call(uri, "unbind_fingerprint", null, null);
		int res = resBundle.getInt("fingerprintBindType", -2);
		Log.d(TAG,"applock unbind res:"+res);
		Log.d(TAG,"isFingerprintApplockAllowed == false");
		/* HQ_yulisuo 2015-07-30 modified end */
		if(res == 0){
			setFingerprintApplockAllowed(false);
		}
		mToggleApplock.setChecked((Boolean) value);
	}
    /* add by HQ_yulisuo for HQ01378093 at 2015-9-15 end */

	/**
	 * return -1:never used,0:not bind,1:bind
	 */
	private int getAppLockBindStatus(){
		if (bDebug){
			Log.i(TAG, "getAppLockBindStatus");
		}
		int bindRet = -1;
		Uri uri = Uri.parse("content://com.huawei.systemmanager.applockprovider/fingerprintstatus");
		Cursor c = getActivity().getContentResolver().query(uri, null, null, null, null);
		for (c.moveToFirst(); ! c.isAfterLast(); c.moveToNext()) {
			bindRet = c.getInt(c.getColumnIndex("fingerprintBindType"));
		}
		return bindRet;
	}
	/**
	 * return -1:never used,0:not bind,1:bind
	 */
	private int getSecurityBindStatus(){
		if (bDebug){
			Log.i(TAG, "getSecurityBindStatus");
		}
		Uri uri = Uri.parse("content://com.huawei.hidisk.fingerprint");
		Bundle resBundle = getActivity().getContentResolver().call(uri, "query_is_box_bindstat", null, null);
		int res = resBundle.getInt("fingerprintBindType", -2);
		return res;
	}

	private boolean isFingerprintLockAllowed(){
		ContentResolver resolver = getActivity().getContentResolver();
		try{
			Log.d(TAG, "Settings.System.FINGERPRINT_KEYGUARD_SWITCH = " +Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_KEYGUARD_SWITCH));
			return Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_KEYGUARD_SWITCH) > 0;

		}
		catch(SettingNotFoundException snfe)
		{
			Log.d(TAG, " Settings.System.FINGERPRINT_KEYGUARD_SWITCH not found");
			return false;
		}
	}

	private boolean isFingerprintSecurityAllowed(){
		ContentResolver resolver = getActivity().getContentResolver();
		try{
			Log.d(TAG, "Settings.System.FINGERPRINT_SECURITY_SWITCH = " +Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_SECURITY_SWITCH));
			return Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_SECURITY_SWITCH) > 0;

		}
		catch(SettingNotFoundException snfe)
		{
			Log.d(TAG, " Settings.System.FINGERPRINT_SECURITY_SWITCH not found");
			return false;
		}
	}
	private boolean isFingerprintApplockAllowed(){
		ContentResolver resolver = getActivity().getContentResolver();
		try{
			Log.d(TAG, "Settings.System.FINGERPRINT_APPLOCK_SWITCH = " +Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_APPLOCK_SWITCH));
			return Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_APPLOCK_SWITCH) > 0;

		}
		catch(SettingNotFoundException snfe)
		{
			Log.d(TAG, " Settings.System.FINGERPRINT_APPLOCK_SWITCH not found");
			return false;
		}
	}
	private boolean isFingerprintCheckhuaweiAllowed(){
		return false;
	}

	private void setFingerprintLockAllowed(boolean enabled){
		ContentResolver resolver = getActivity().getContentResolver();
		final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
		Settings.System.putInt(getContentResolver(), Settings.System.FINGERPRINT_KEYGUARD_SWITCH,
				enabled ? 1 : 0);
		try{
			Log.e(TAG, "Settings.System.FINGERPRINT_KEYGUARD_SWITCH = " +Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_KEYGUARD_SWITCH));
		}
		catch(SettingNotFoundException snfe)
		{
			Log.e(TAG, " Settings.System.FINGERPRINT_KEYGUARD_SWITCH not found");
		}

	}
	private void setFingerprintSecurityAllowed(boolean enabled){
		ContentResolver resolver = getActivity().getContentResolver();
		final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
		Settings.System.putInt(getContentResolver(), Settings.System.FINGERPRINT_SECURITY_SWITCH,
				enabled ? 1 : 0);
		try{
			Log.e(TAG, "Settings.System.FINGERPRINT_SECURITY_SWITCH = " +Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_SECURITY_SWITCH));
		}
		catch(SettingNotFoundException snfe)
		{
			Log.e(TAG, " Settings.System.FINGERPRINT_SECURITY_SWITCH not found");
		}

	}
	private void setFingerprintApplockAllowed(boolean enabled){
		ContentResolver resolver = getActivity().getContentResolver();
		final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
		Settings.System.putInt(getContentResolver(), Settings.System.FINGERPRINT_APPLOCK_SWITCH,
				enabled ? 1 : 0);
		try{
			Log.e(TAG, "Settings.System.FINGERPRINT_APPLOCK_SWITCH = " +Settings.System.getInt(resolver,
					Settings.System.FINGERPRINT_APPLOCK_SWITCH));
		}
		catch(SettingNotFoundException snfe)
		{
			Log.e(TAG, " Settings.System.FINGERPRINT_APPLOCK_SWITCH not found");
		}

	}
	private void setFingerprintCheckhuaweiAllowed(boolean enabled){
	}
	protected void dialogInvitetoEnrol() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.fingerprint_enrol_invitation);
		builder.setTitle(R.string.enter_fingerprint);
		builder.setPositiveButton((R.string.fingerprint_enrol_later),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.i(TAG, "dialogInvitetoEnrol setPositiveButton");
						unfinishActivityFlag = false;
						dialog.dismiss();
					}
				});

		builder.setNegativeButton((R.string.fingerprint_enrol_now),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.i(TAG, "dialogInvitetoEnrol setNegativeButton");
						unfinishActivityFlag = false;
						dialog.dismiss();
						Intent intent1 = new Intent(getActivity(), FingerprintEnterActivity.class);
						intent1.putExtra("safe_enter", "fingerprint_enter");//add protection for security by liuchaochao
						getActivity().startActivity(intent1);
						//getActivity().finish();
					}
				});

		builder.setOnCancelListener(new DialogInterface.OnCancelListener(){
			@Override
			public void onCancel(DialogInterface dialogInterface) {
				Log.i(TAG, "dialogInvitetoEnrol setNegativeButton");
				unfinishActivityFlag = false;
			}
		});

		builder.create().show();
	}

	protected void dialogWarntoEnrol() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		switch (switchtype){
			case 1:
				builder.setMessage(R.string.fingerprint_enrol_keyguard);
				break;
			case 2:
				builder.setMessage(R.string.fingerprint_enrol_security);
				break;
			case 3:
				builder.setMessage(R.string.fingerprint_enrol_applock);
				break;
			case 4:
				builder.setMessage(R.string.fingerprint_enrol_huaweiid);
				break;
			default:
		}

		builder.setTitle(R.string.enter_fingerprint);

		builder.setPositiveButton((R.string.cancel), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.setNegativeButton((R.string.fingerprint_enrol_now), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				Intent intent1 = new Intent(getActivity(), FingerprintEnterActivity.class);
				intent1.putExtra("safe_enter", "fingerprint_enter");//add protection for security by liuchaochao
				getActivity().startActivity(intent1);
				//getActivity().finish();
			}
		});

		builder.create().show();
	}
	private void killfpm() {
		if(fpm!= null) {
			fpm.abort();
			fpm.release();
			fpm = null;
		}
	}

	public class SmsReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (TELEPHONY_SMS_RECEIVED.equals(intent.getAction())) {
				//来短消息不退出FingerManager
				unfinishActivityFlag = true;
			}
		}
	}
}
