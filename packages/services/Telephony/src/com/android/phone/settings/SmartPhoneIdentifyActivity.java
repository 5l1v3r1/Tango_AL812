package com.android.phone.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import com.cootek.smartdialer_plugin_oem.CooTekSmartdialerOemModule;
import com.cootek.smartdialer_plugin_oem.IServiceStateCallback;
import com.android.phone.R;
import android.provider.Settings;
import android.widget.CheckBox;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.DialogInterface;

import android.preference.SwitchPreference;


public class SmartPhoneIdentifyActivity extends PreferenceActivity implements  IServiceStateCallback{
		private CooTekSmartdialerOemModule csom;
		private SwitchPreference identifySwitch;
		private PreferenceScreen offlineNumberDatabase;
		private boolean chubaoConnected=false;
		private CheckBox isNomore;
		
	 	@Override
	    public void onCreate(Bundle savedInstanceState) {
			int themeId = getResources().getIdentifier("androidhwext:style/Theme.Emui.WithActionBar", null, null);
			if (themeId > 0){
				setTheme(themeId);
			}
	 		super.onCreate(savedInstanceState);
	        
	        addPreferencesFromResource(R.xml.smart_number_identify);
	        csom= new CooTekSmartdialerOemModule(this,this);
	    }

	 
		//add by zhangjinqiang
			@Override
			public void onServiceConnected() {
		//		Toast.makeText(PeopleActivity.this, "号码助手Service通信成功！", Toast.LENGTH_SHORT).show();  
				chubaoConnected=true;
			}
			
			@Override
			public void onServiceDisconnected() {
		//		Toast.makeText(PeopleActivity.this, "号码助手Service通信连接失败！！", Toast.LENGTH_LONG).show(); 
				chubaoConnected=false;
			}

		//end

		@Override
	    public void onResume() {
	        super.onResume();
					
			identifySwitch = (SwitchPreference)findPreference("identify_switch");
			int b = Settings.System.getInt(getContentResolver(), "identify_switch", 0);					
			identifySwitch.setChecked(0 != b);
			identifySwitch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					// TODO Auto-generated method stub
					if (identifySwitch.isChecked()){
		                 Settings.System.putInt(getContentResolver(), "identify_switch", 0);
					} else {
					    confirmDialog();
					}
					return true;
				}
			});
			
			offlineNumberDatabase= (PreferenceScreen)findPreference("offline_number_database");
			offlineNumberDatabase.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference arg0) {
					// TODO Auto-generated method stub
					csom.launchOfflineData();
					return true;
				}
			});
		}

	@Override
	protected void onDestroy() {		
		// TODO Auto-generated method stub
		if(csom != null){
			try{
			csom.destroy();
			csom = null;
			}catch(Exception e){
			}
		}
		super.onDestroy();	
	}

	//add by zhangjinqiang for chubao -start
	private void confirmDialog(){
			if(chubaoConnected==false){
				//Toast.makeText(SmartPhoneIdentifyActivity.this, "触宝号码助手通讯失败，可能已经被卸载。", Toast.LENGTH_SHORT).show();
				return;
			}
			int isFirstClick = Settings.System.getInt(getContentResolver(), "chubaoWarn", 0);	
			if(isFirstClick==0){
				LayoutInflater inflater=LayoutInflater.from(SmartPhoneIdentifyActivity.this);
				View chubao_confirm_dialog  = inflater.inflate(R.layout.chubao_confirm_dialog,null);
	            	isNomore = (CheckBox)chubao_confirm_dialog.findViewById(R.id.nomore);
				Builder builder1 = new AlertDialog.Builder(SmartPhoneIdentifyActivity.this);
				builder1.setTitle(R.string.user_aggreement);
				builder1.setView(chubao_confirm_dialog);
                builder1.setCancelable(false); /* added by shanlan for HQ02049202 */
				builder1.setPositiveButton(R.string.confirm_hw, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						/* HQ_liujin modified for HQ01356021 to add Positioning function begin */
						csom.setNetworkAccessible(true);
						Settings.System.putInt(getContentResolver(),"chubaoWarn", isNomore.isChecked()?1:0);
						 Settings.System.putInt(getContentResolver(), "identify_switch", 1);
					}
				});
				
				builder1.setNegativeButton(R.string.cancel_hw, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						//add by zhangjinqiang for HQ01506350-start
						int checkId = Settings.System.getInt(getContentResolver(), "identify_switch", 0);	
						identifySwitch.setChecked(0 != checkId);
						//add by zjq end
					}
				});
				
				builder1.create();
				builder1.show();
				
			}else {
				csom.setNetworkAccessible(true);
				Settings.System.putInt(getContentResolver(), "identify_switch", 1);
			}
	}
	//add by zjq end
}
