package com.mediatek.settings.ext;

import java.util.Map;

import android.content.Context;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.Switch;
import android.widget.TabWidget;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public interface IDataUsageSummaryExt {

    /**
     * Customize data usage background data restrict string by tag.
     * @param: default string.
     * @param: tag string.
     * @return: customized summary string.
     */
    String customizeBackgroundString(String defStr, String tag);

    /**
     * Customize the background resource of Textview.
     * @param simColor: color id of current Sim card
     * @param textview: origin textview
     */
    void customizeTextViewBackgroundResource(int simColor, TextView title);

    /**
     * customize the tabspec. Change title, backgournd resource etc. It will be called
     * when rebuild all tabs.
     * @param activity: parent activity
     * @param tag: tag info
     * @param tab: tabspec to be customized
     * @param tabWidget as the parent
     * @param title: tab title
     * @return updated tabspec
     */
    TabSpec customizeTabInfo(Activity activity, String tag,
            TabSpec tab, TabWidget tabWidget, String title);

    /**
     * Customize the summary of mobile data.
     * Used in OverViewTabAdapter.java
     * @param container The view container to add SIM indicator
     * @param titleView We will add SIM indicator to left of the titleView
     * @param slotId Decide which SIM indicator drawable to add
     * @return The LayoutInflater
     */
    void customizeMobileDataSummary(View container, View titleView, int slotId);

    /**
     * Customize when CT Gemini
     * Register ContentObserver to observe data connection status change
     * @param activity : which activity will register ContentObserver
     * @param mDataConnectionObserver : which ContentObserver will be observed
     */
    void customizeDataConnectionObserver(Activity activity, ContentObserver mDataConnectionObserver);

    /**
     * Customize when CT Gemini
     * UnRegister ContentObserver to observe data connection status change
     * @param activity : which activity will unregister ContentObserver
     * @param mDataConnectionObserver : which ContentObserver will be observed
     */
    void customizeUnregisterDataConnectionObserver(Activity activity, ContentObserver mDataConnectionObserver);


    /**
     * Customize when Orange
     * Show popup informing user about data enable/disable
     * @param mDataEnabledView : data enabled view for which click listener will be set by plugin
     * @param mDataEnabledDialogListerner : click listener for dialog created by plugin
     * @param isChecked : whether data is enabled or not
     */
    boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
            Switch dataEnabled, DialogInterface.OnClickListener dataEnabledDialogListerner);

    /**
     * Customize when Orange
     * Show popup informing user about data enable/disable
     * @param mDataEnabledView : data enabled view for which click listener will be set by plugin
     * @param mDataEnabledDialogListerner : click listener for dialog created by plugin
     * @param isChecked : whether data is enabled or not
     */
    boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
            Switch dataEnabled, View.OnClickListener dataEnabledDialogListerner);
    /**
     * For different operator to show a host dialog
     *
     */
    boolean needToShowDialog();

    public void resume(Context context, IDataUsage datausage, Map<String, Boolean> mMobileDataEnabled);
    public void pause(Context context);	

    /**
     * Customize when OP18
     * Modify popup message if LTE services are active
     * @param builder : Dialog builder object to set popup title.
     * @param currentSir : subcription info of the selected SIM.
     */    
    void warnIfLteServicesStop(AlertDialog.Builder builder, SubscriptionInfo currentSir);

    /**
     * Called when DataUsageSummary need set data switch state such as clickable.
     * @param view View
     * @param subId current tab's SIM subId
     * @return true if allow data switch.
     */
    public boolean isAllowDataEnable(View view, int subId);
}

