package com.android.settings.fingerprint;

import android.app.Activity;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.text.TextWatcher;
import android.text.Editable;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView; 
import android.widget.Toast;
import android.view.LayoutInflater;
import com.android.settings.R;
import com.fingerprints.service.FingerprintManager;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentResolver;

public class FingerprintDeleteActivity extends Activity{
	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private final String TAG = "DeleteActivity";
	private final boolean bDebug = true;
	FingerprintManager fpm = null;
    private Intent mTrustAgentClickIntent;
	private TextView mResultText;
	private EditText mNewName;
	AlertDialog.Builder renameBuilder;
	AlertDialog renameDialog;
	private Button mRename;
	private Button mDelete;
	private String fingerprintname;
	private int fingerprintid;
	private int fpcid;
	private int fingerprintmode;
	private String newname;
	private boolean bToOtherFragment = false;
	Uri uri = Uri.parse("content://com.huawei.fingerprintname.FingerprintNameProvider");
	Uri uri2 = Uri.parse("content://com.huawei.fingerprintname.FingerprintNameProvider2");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (bDebug){
			Log.i(TAG, "onCreate");
		}

		if (!getIntent().hasExtra("safe_enter")) {//add protection for security by liuchaochao
			Log.i("lcc", "forbid to enter FingerprintDeleteActivity due to no hasExtra ");
			finish();
			return;
		}

		setContentView(R.layout.fingerprint_delete);
		if (savedInstanceState != null
		        && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
		    mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}

		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			fingerprintname = bundle.getString("fingerprintname");
			fingerprintid = bundle.getInt("fingerprintid");
			fingerprintmode = bundle.getInt("fingerprintmode");
			fpcid = bundle.getInt("fpcid");
		}

		this.getActionBar().setTitle(R.string.fingerprint_detail);
		mResultText = (TextView) findViewById(R.id.fingerprint_id_state);
		mResultText.setText(fingerprintname);

		//mChildMode = (TextView) findViewById(R.id.fingerprint_child_state);

		mRename = (Button) findViewById(R.id.rename);
		mRename.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				renameBuilder = new AlertDialog.Builder(FingerprintDeleteActivity.this);
				LayoutInflater factory = LayoutInflater.from(FingerprintDeleteActivity.this);
				final View textEntryView = factory.inflate(R.layout.dialog_normal_layout, null);
				renameBuilder.setView(textEntryView);
				mNewName = (EditText) textEntryView.findViewById(R.id.newname);
				mNewName.setText(fingerprintname);
				mNewName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
				mNewName.selectAll();
				renameBuilder.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						newname = mNewName.getText().toString();
						Log.d(TAG, "rename newname ==" + newname);
						if ((newname == null) || (newname.length() == 0)) {
							newname = fingerprintname;
						} else if (isHasName(newname)) {
							newname = fingerprintname;
							Toast.makeText(FingerprintDeleteActivity.this,
									getString(R.string.fingerprint_enrol_rename_hasname_hint), 0).show();
						} else {
							Log.d(TAG, "save name to db");
							ContentValues values = new ContentValues();
							values.put("name", newname);
							FingerprintDeleteActivity.this.getContentResolver().update(uri, values, "id=?",
									new String[]{String.valueOf(fingerprintid)});
							bToOtherFragment = true;
							finish();
						}
						mResultText.setText(newname);

					}
				});
				renameBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
				renameDialog = renameBuilder.create();
				addRenameEdittextTextListener(mNewName);
				renameDialog.show();
			}
		});

		mDelete = (Button) findViewById(R.id.delete);
		mDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder normalDia=new AlertDialog.Builder(FingerprintDeleteActivity.this);
				normalDia.setMessage(getString(R.string.fingerprint_whether_delete)+fingerprintname+"?");
				normalDia.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						Log.i(TAG, "delete fingerpirnt from fpc fpcid = " + fpcid);
						fpm = FingerprintManager.open();
						fpm.deleteFingerData(fpcid);
						killfpm();

						Log.i(TAG, "delete fingerpirnt from db id = " + fingerprintid);
						ContentResolver mResolver = FingerprintDeleteActivity.this.getContentResolver();
						mResolver.delete(uri, "id=?" ,new String[]{String.valueOf(fingerprintid)});
						ContentValues cvReset = new ContentValues();

						//修改数据库id为0
						Cursor cursor = mResolver.query(uri, null, null, null, null);
						if(cursor.getCount() == 0){
							cvReset.put("seq", 0);
						}else{
							cursor.moveToLast();
							cvReset.put("seq", cursor.getInt(0));
						}
						FingerprintDeleteActivity.this.getContentResolver().update(uri, cvReset, "##", null);
						bToOtherFragment = true;
						finish();
					}
				});
				normalDia.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});
				normalDia.create().show();
			}
		});
	}

	@Override
	public void onResume() {
        super.onResume();
	}

	private void addRenameEdittextTextListener(EditText et){
		if(et != null){
			et.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
					if(renameDialog != null){
						int count = arg0.toString().trim().length();
						if((arg0 == null) || (arg0.length() == 0 ) || (count == 0)){
							renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
						}else{
							renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
						}					
					}
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
						int arg3) {}

				@Override
				public void afterTextChanged(Editable arg0) {}
			});
		}
	}

	private boolean isHasName(String newname){
		boolean ret = false;
		newname = newname.trim();
		Cursor nameCursor = FingerprintDeleteActivity.this.getContentResolver()
		            		.query(uri, new String[]{"id","name"}, "name=?", new String[]{newname}, null);

		if (nameCursor != null){
			Log.i(TAG,"rename cursor count:"+nameCursor.getCount());
			if (nameCursor.getCount() > 0){
				ret = true;
				Log.d(TAG, "newname already in use");
			}
			nameCursor.close();

			// return true;
		}

		Log.d(TAG, "newname does not in use");

//		while(nameCursor.moveToNext()){
//			String name = nameCursor.getString(1);
//			if(newname.equals(name)){
//				ret = true;
//				break;
//			}
//		}
		return ret;
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i(TAG, "onKeyDown keyCode= " + keyCode);
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            Intent intent = new Intent(FingerprintDeleteActivity.this, FingerprintManageActivity.class);
//            intent.putExtra("safe_enter","fingerprint_manager");//add protection for security by liuchaochao
//            startActivity(intent);
//            return true;
//        }
        return super.onKeyDown(keyCode, event);
    }

	@Override
	public void onPause() {
		super.onPause();
		if (!bToOtherFragment) {
			finish();
		}
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

	private void killfpm() {
		if(fpm!= null) {
			fpm.abort();
			fpm.release();
			fpm = null;
		}
	}
}
