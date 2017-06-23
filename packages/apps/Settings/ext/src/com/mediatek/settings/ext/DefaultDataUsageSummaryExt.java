package com.mediatek.settings.ext;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;
import android.widget.TabWidget;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class DefaultDataUsageSummaryExt implements IDataUsageSummaryExt {

    public DefaultDataUsageSummaryExt(Context context) {
    }

    public String customizeBackgroundString(String defStr, String tag) {
        return defStr;
    }

    public void customizeTextViewBackgroundResource(int simColor,
        TextView title) {
        return;
    }

    public TabSpec customizeTabInfo(Activity activity, String tag,
        TabSpec tab, TabWidget tabWidget, String title) {
        return tab;
    }


    @Override
    public void customizeMobileDataSummary(View container, View titleView,
        int slotId) {
    }

    @Override
    public void customizeDataConnectionObserver(Activity activity,
            ContentObserver mDataConnectionObserver) {
    }

    @Override
    public void customizeUnregisterDataConnectionObserver(Activity activity,
            ContentObserver mDataConnectionObserver) {
    }

    @Override
    public boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
            Switch dataEnabled, DialogInterface.OnClickListener dataEnabledDialogListerner) {
            return false;
    }

    @Override
    public boolean needToShowDialog() {
            return true;
    }

    @Override
    public boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
            Switch dataEnabled, OnClickListener dataEnabledDialogListerner) {
        return false;
    }

    @Override
    public void resume(Context context, IDataUsage datausage, Map<String, Boolean> mMobileDataEnabled) {
    
	}
	
    @Override
    public void pause(Context context) {
    }

    /**
     * Customize when OP18
     * Modify popup message if LTE services are active
     * @param builder : Dialog builder object to set popup title.
     * @param currentSir : subcription info of the selected SIM.
     */
    
    @Override
    public void warnIfLteServicesStop(AlertDialog.Builder builder, SubscriptionInfo currentSir) {
	}

    /**
     * Called when DataUsageSummary need set data switch state such as clickable.
     * @param view View
     * @param subId current tab's SIM subId
     * @return true if allow data switch.
     */
    public boolean isAllowDataEnable(View view, int subId) {
        return true;
    }
}
