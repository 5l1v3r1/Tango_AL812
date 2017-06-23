package com.android.settings.fingerprint;

import android.app.Fragment;
import android.app.Activity;
import android.view.View;
import android.text.InputFilter;
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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.settings.R;
import android.content.ContentValues;
import android.net.Uri;

public class FingerprintEnrol extends Fragment{
	private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
	private final String TAG = "FingerprintEnrol";
    private Intent mTrustAgentClickIntent;
    private ImageView imageview;
	private TextView mResultText, mChildmode;
	private EditText mNewName;
	private int resume = 0;
	//private int pause = 0;
	private int fingerprintmode = 0;
	private Button mRename;
	private Button mOkay;
	private String  fingerprintname,result;
	private String newname;
	private Activity context;
	Uri uri = Uri.parse("content://com.huawei.fingerprintname.FingerprintNameProvider");
	AlertDialog renameDialog;
	private boolean bToOtherFragment = false;
	private int fpcIndex = 0;
	private boolean bDebug = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (bDebug) {
			Log.i(TAG, "onCreate");
		}

		super.onCreate(savedInstanceState);

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
			mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
		}
		context = getActivity();
	}



	@Override
	public void onResume() {
        super.onResume();
		if (bDebug) {
			Log.i(TAG, "onResume");
		}

		int fingerprintid = getArguments().getInt("fingerprintname");
		fpcIndex = getArguments().getInt("fingerprintid");
		fingerprintname = Integer.toString(fingerprintid);
		result = getString(R.string.fingerprint_settings_title) + fingerprintname + getString(R.string.is_enrolled);
		mResultText.setText(result);
	}

	private void addRenameEdittextTextListener(EditText et){
		if(et != null){
			et.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
					if(renameDialog != null){
						int count = arg0.toString().trim().length();
						if((arg0 == null) || (arg0.length() == 0) || (count == 0)){
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
	
	@Override
	public void onPause() {
		super.onPause();

		if (bDebug) {
			Log.i(TAG, "onPause");
		}
		if (!bToOtherFragment) {
			getActivity().finish();
		}
	}

	private void updateName(int fpcIndex){
		ContentValues values = new ContentValues();

		Log.d(TAG, "upate db newname" + newname);
		values.put("name", newname);
		context.getContentResolver().update(uri, values, "fpcid=?",
				new String[]{String.valueOf(fpcIndex)});
	}
  
	@Override
	public void onDestroyView() {
        super.onDestroyView();
	}

	@Override
	public void onDestroy() {
        super.onDestroy();
		//Log.d(TAG,"fpm.getIds = " +fpm.getIds());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	     Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView =  inflater.inflate(R.layout.fingerprint_enrol, container,false);
		mResultText = (TextView)rootView.findViewById(R.id.fingerprint_id_state);
		mOkay = (Button) rootView.findViewById(R.id.nextstep);
		mOkay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				bToOtherFragment = true;
				getActivity().finish();
				Intent intent = new Intent(getActivity() , FingerprintManageActivity.class);
				intent.putExtra("safe_enter","fingerprint_manager");//add protection for security by liuchaochao
				startActivity(intent);
			}
		});

		mRename = (Button) rootView.findViewById(R.id.rename);
		mRename.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				LayoutInflater factory = LayoutInflater.from(getActivity());
				final View textEntryView = factory.inflate(R.layout.dialog_normal_layout, null);
				builder.setView(textEntryView);
				mNewName = (EditText) textEntryView.findViewById(R.id.newname);
				mNewName.setText(getString(R.string.fingerprint_settings_title) + fingerprintname);
				mNewName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
				mNewName.selectAll();

				builder.setPositiveButton(getString(R.string.okay), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						newname = mNewName.getText().toString();
						if (newname.equals("")) {
							newname = getString(R.string.fingerprint_settings_title) + fingerprintname;
						}
						Log.d(TAG, "newname ==" + newname);

						updateName(fpcIndex);
						bToOtherFragment = true;
						getActivity().finish();
						Intent intent = new Intent(getActivity(), FingerprintManageActivity.class);
						intent.putExtra("safe_enter", "fingerprint_manager");//add protection for security by liuchaochao
						startActivity(intent);
					}
				});

				builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});

				renameDialog = builder.create();
				addRenameEdittextTextListener(mNewName);
				renameDialog.show();
			}
		});

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
