package com.android.contacts.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.QuickContact;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.model.mem_adapter;
import com.android.contacts.quickcontact.QuickContactActivity;
import com.android.contacts.util.SimUtil_HQ;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.ContactsCommonListUtils;

public class ContactsSettingActivity extends Activity implements
		OnClickListener {

	private RelativeLayout MergerContacts;
	private RelativeLayout SetMyProfile;
	private ListView mem_status_lv;
	private ListAdapter mem_status_adapter;
	private static String PROFILE_DB = "/data/data/com.android.providers.contacts/databases/profile.db";
	private static SQLiteDatabase db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		int themeId = getResources().getIdentifier(
				"androidhwext:style/Theme.Emui.WithActionBar", null, null);
		if (themeId > 0) {
			setTheme(themeId);
		}
		super.onCreate(savedInstanceState);

		setContentView(R.layout.contact_setting_activity);
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(getResources().getString(R.string.menu_settings));

		init();

	}

	private void init() {
		// TODO Auto-generated method stub
		MergerContacts = (RelativeLayout) findViewById(R.id.MergerContacts);
		SetMyProfile = (RelativeLayout) findViewById(R.id.SetMyProfile);
		mem_status_lv = (ListView) findViewById(R.id.mem_status_lv);
		MergerContacts.setOnClickListener(this);
		SetMyProfile.setOnClickListener(this);
		List<HashMap<String, String>> accountMapList = countAccountAndContactsNum();
		if(accountMapList==null){
			return;
		}
		mem_status_adapter = new mem_adapter(accountMapList,
				ContactsSettingActivity.this);
		mem_status_lv.setAdapter(mem_status_adapter);
	}

	private List<HashMap<String, String>> countAccountAndContactsNum() {
		// TODO Auto-generated method stub
		
		final AccountTypeManager accountTypes = AccountTypeManager
				.getInstance(ContactsSettingActivity.this);
		List<AccountWithDataSet> accounts = accountTypes.getAccounts(false);

		List<HashMap<String, String>> accountMapList = new ArrayList<HashMap<String, String>>();
		for (AccountWithDataSet account : accounts) {
			String accountNameDisplay="Phone";
			HashMap<String, String> accountMap = new HashMap<String, String>();
			AccountType accountType = accountTypes.getAccountType(account.type,
					account.dataSet);
//			if (accountType != null && accountType.isExtension()
//					&& !account.hasData(ContactsSettingActivity.this)) {
//				// Hide extensions with no raw_contacts.
//				continue;
//			}
			if (accountType.isIccCardAccount()) {
				int subID=((AccountWithDataSetEx) account).mSubId;
                SubscriptionInfo sfr = SubInfoUtils.getSubInfoUsingSubId(subID);
				int slotId=-1;
                if (sfr != null) {
                    slotId = sfr.getSimSlotIndex();
                }
				
					if (slotId==0) {
						accountNameDisplay = getResources()
								.getString(R.string.card_1);
					} else if (slotId == 1) {
						accountNameDisplay = getResources()
								.getString(R.string.card_2);
					}else {
						accountNameDisplay="未知卡";
					}
			}else if (AccountWithDataSetEx.isLocalPhone(accountType.accountType)) {
				accountNameDisplay=getResources().getString(R.string.Local_phone);
			}else {
				accountNameDisplay=account.name;
			}

			String account_name = account.name;
			accountMap.put("account_name", account_name);
			accountMap.put("account_name_display", accountNameDisplay);
			accountMapList.add(accountMap);

		}

		return accountMapList;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.MergerContacts:
			Intent intent1 = new Intent();
			intent1.setClass(ContactsSettingActivity.this,
					SameNameJoinActivity.class);
			startActivity(intent1);
			break;

		case R.id.SetMyProfile:

			if (db == null || !db.isOpen()) {
				db = SQLiteDatabase.openDatabase(PROFILE_DB, null, 0);
			}
			Cursor cursor = db.rawQuery("select _id from view_contacts", null);
			Intent intent = new Intent();
			if (cursor.getCount() == 0) {
				intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
				intent.putExtra("newLocalProfile", true);
			} else {
				// intent.setAction(Intent.ACTION_VIEW);
				// intent.setData(ContactsContract.Profile.CONTENT_URI);
				intent = QuickContact.composeQuickContactsIntent(
						ContactsSettingActivity.this, (Rect) null,
						ContactsContract.Profile.CONTENT_URI,
						QuickContactActivity.MODE_FULLY_EXPANDED, null);
			}
			if (db != null && db.isOpen()) {
				db.close();
			}
			cursor.close();
			startActivity(intent);
			break;

		default:
			break;
		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		ContactsApplication.closeContactDb();
		super.onDestroy();
	}

}
