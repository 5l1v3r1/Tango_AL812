/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.mediatek.settings.cdma;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.Utils;
import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.internal.telephony.uicc.SvlteUiccUtils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;

public class CdmaUtils {

    private static final String TAG = "CdmaUtils";

    public static final int CT_4G_SIM = 0;
    public static final int CT_3G_SIM = 1;
    public static final int NOT_CT_SIM = 2;

    private static final String TWO_CDMA_INTENT = "com.mediatek.settings.cdma.TWO_CDMA_POPUP";

    private static final String[]  PROPERTY_RIL_FULL_UICC_TYPE = {
        "gsm.ril.fulluicctype",
        "gsm.ril.fulluicctype.2",
        "gsm.ril.fulluicctype.3",
        "gsm.ril.fulluicctype.4",
    };
    /**
     * Get whether a CT cdam card inserted refer to {@link SvlteUiccUtils}
     * @return
     */
    public static boolean isCdmaCardInsert() {
        int simCount = TelephonyManager.getDefault().getSimCount();
        boolean hasCdmaCards =false;
        Log.d(TAG,"simCount = " + simCount);
        for (int slotId = 0 ; slotId < simCount; slotId ++) {
            if (isCdmaCardType(slotId)) {
                hasCdmaCards = true;
                break;
            }
        }
        return hasCdmaCards;
    }
    
    public static int getSIMCardType(int slotId) {
        SvlteUiccUtils utils = SvlteUiccUtils.getInstance();
        boolean isCtCdmaCard = utils.isRuimCsim(slotId);
        int type = NOT_CT_SIM;
        if (isCtCdmaCard) {
            type = utils.isUsimWithCsim(slotId) ? CT_4G_SIM : CT_3G_SIM;
        }
        Log.d(TAG,"type = " + type);
        return type;
    }

    private static boolean isCdmaCardType(int slotId) {
        SvlteUiccUtils util = SvlteUiccUtils.getInstance();
        boolean isCdmaCard= util.isRuimCsim(slotId);
        Log.d(TAG,"slotId = " + slotId + " isCdmaCard = " + isCdmaCard);
        return isCdmaCard;
    }

    /**
     * get the c2k Modem Slot.
     * @return 1 means sim1,2 means sim2
     */
    public static int getExternalModemSlot() {
        return CdmaFeatureOptionUtils.getExternalModemSlot();
    }

    public static boolean phoneConstantsIsSimOne(SubscriptionInfo subscriptionInfo) {
        if (subscriptionInfo != null) {
            int slotId = SubscriptionManager.getSlotId(subscriptionInfo.getSubscriptionId());
            Log.i(TAG, "phoneConstantsIsSimOne slotIsSimOne = "
                    + (PhoneConstants.SIM_ID_1 == slotId));
            return PhoneConstants.SIM_ID_1 == slotId;
        }
        return false;
    }

    /**
     * add for c2k. get data usage for CDMA LTE.
     * @param template template.
     * @param subId subId
     */
    public static void fillTemplateForCdmaLte(NetworkTemplate template, int subId) {
        if (CdmaFeatureOptionUtils.isCdmaLteDcSupport()) {
            final TelephonyManagerEx teleEx = TelephonyManagerEx.getDefault();
            final String svlteSubscriberId = teleEx.getSubscriberIdForLteDcPhone(subId);
            if (!(TextUtils.isEmpty(svlteSubscriberId)) && svlteSubscriberId.length() > 0){
                Log.d(TAG, "bf:" + template);
                template.addMatchSubscriberIds(svlteSubscriberId);
                Log.d(TAG, "af:" + template);
            }
        }
    }
    /**
     * Launch the dialog activity if under SVLTE and two new SIM detect.
     * @param context Context
     * @param simDetectNum New SIM number detected
     */
    public static void startCdmaWaringDialog(Context context, int simDetectNum) {
        Log.d(TAG, "startCdmaWaringDialog," + " simDetectNum = " + simDetectNum);
        boolean twoCdmaInsert = true;
        if (simDetectNum > 1) {
            for (int i = 0; i < simDetectNum; i++) {
                if (SvlteUiccUtils.getInstance().getSimType(i) != SvlteUiccUtils.SIM_TYPE_CDMA) {
                    Log.d(TAG, "not CDMA SIM type, slot = " + i);
                    twoCdmaInsert = false;
                }
            }
        } else {
            twoCdmaInsert = false;
        }

        Log.d(TAG, "twoCdmaInsert = " + twoCdmaInsert);
        if (twoCdmaInsert) {
            Intent intent = new Intent(TWO_CDMA_INTENT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(CdmaSimDialogActivity.DIALOG_TYPE_KEY,
                    CdmaSimDialogActivity.TWO_CDMA_CARD);
            context.startActivity(intent);
        }
    }

    public static void startAlertCdmaDialog(Context context, int targetSubId, int actionType) {
        Intent intent = new Intent(TWO_CDMA_INTENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(CdmaSimDialogActivity.DIALOG_TYPE_KEY, CdmaSimDialogActivity.ALERT_CDMA_CARD);
        intent.putExtra(CdmaSimDialogActivity.TARGET_SUBID_KEY, targetSubId);
        intent.putExtra(CdmaSimDialogActivity.ACTION_TYPE_KEY, actionType);
        context.startActivity(intent);
    }

    /**
     * For C2K C+C case, only one SIM card register network, other card can recognition.
     * and can not register the network
     * 1. two CDMA cards.
     * 2. two cards is competitive. only one modem can register CDMA network. 
     * @param context
     * @return
     */
    public static boolean isCdmaCardCompetion(Context context) {
        boolean isCdmaCard = true;
        boolean isCompetition = true;
        int simCount = 0;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        }
        if (simCount == 2) {
            for (int i = 0; i < simCount ; i++) {
                isCdmaCard = isCdmaCard
                        && (SvlteUiccUtils.getInstance().getSimType(i) == SvlteUiccUtils.SIM_TYPE_CDMA);
                SubscriptionInfo subscriptionInfo = Utils.findRecordBySlotId(context, i);
                if (subscriptionInfo != null) {
                    isCompetition = isCompetition &&
                            TelephonyManagerEx.getDefault().isInHomeNetwork(subscriptionInfo.getSubscriptionId());
                } else {
                    isCompetition = false;
                    break;
                }
            }
        } else {
            isCdmaCard = false;
            isCompetition = false;
        }
        Log.d(TAG, "isCdmaCard: " + isCdmaCard + " isCompletition: " + isCompetition);
        return isCdmaCard && isCompetition;
    }

    /**
     * 1. two CDMA cards.
     * 2. two cards is competitive. only one modem can register CDMA network. 
     * @param context
     * @return
     */
    public static boolean isCdmaCardCompetionForData(Context context) {
        return isCdmaCardCompetion(context);
    }

    /*
     * 1. two CDMA cards.
     * 2. two cards is competitive. only one modem can register CDMA network.
     * 3. valid subId
     * 4 .capability switch
     */
    public static boolean isCdmaCardCompetionForSms(Context context,int targetItem) {
        Log.d(TAG, "targetItem: " + targetItem);
        return SubscriptionManager.isValidSubscriptionId(targetItem)
                && isCdmaCardCompetion(context)
                && (TelephonyUtils.getMainCapabilitySlotId() != SubscriptionManager.getSlotId(targetItem));
    }

    /**
     * 1. user click SIM card
     * 2. two CDMA cards.
     * 3. two cards is competitive. only one modem can register CDMA network.
     * 4. capability switch
     * @param context
     * @param targetItem
     * @return
     */
    public static boolean isCdmaCardCompetionForCalls(Context context,int targetItem) {
        boolean shouldDisplay = false;
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (context != null) {
            final TelecomManager telecomManager = TelecomManager.from(context);
            final List<PhoneAccountHandle> phoneAccountsList = telecomManager.getCallCapablePhoneAccounts();
            PhoneAccountHandle handle = targetItem < 1 ? null : phoneAccountsList.get(targetItem - 1);
            subId = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, handle);
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            shouldDisplay = TelephonyUtils.getMainCapabilitySlotId() != SubscriptionManager.getSlotId(subId);
        }
        Log.d(TAG, "shouldDisplay: " + shouldDisplay + " targetItem: " + targetItem);
        return shouldDisplay && isCdmaCardCompetion(context);
    }

    /**
     * For C2K signal disturb, When CDMA can not coexist with LTE and WCDMA,
     * should caution user the result than change default data.
     * 1. two SIM cards. 
     * 2. one SIM Card is CDMA card, other is GSM card
     * 3. change default  card to G card
     * 4. the main capability is CDMA card
     * @param context context
     * @param targetSubId targetSubId
     * @return true can not coexist
     */
    public static boolean isSwitchCdmaCardToGsmCard(Context context, int targetSubId) {
        boolean isGsmCardForTarget = false;
        boolean isCdmaCardForMainCapability = false;
        if (!FeatureOption.MTK_C2K_SLOT2_SUPPORT
                && SubscriptionManager.isValidSubscriptionId(targetSubId)) {
           int mainCapabilitySlotId = TelephonyUtils.getMainCapabilitySlotId();
           isCdmaCardForMainCapability = SvlteUiccUtils.getInstance().getSimType(mainCapabilitySlotId)
                   == SvlteUiccUtils.SIM_TYPE_CDMA;
            int targetDataSlotId = SubscriptionManager.getSlotId(targetSubId);
            isGsmCardForTarget = SvlteUiccUtils.getInstance().getSimType(targetDataSlotId)
                    == SvlteUiccUtils.SIM_TYPE_GSM;
        }
        Log.d(TAG, "isGsmCardForTarget: " + isGsmCardForTarget
                + " isCdmaCardForMainCapability: " + isCdmaCardForMainCapability);
        return isGsmCardForTarget && isCdmaCardForMainCapability;
    }

    /**
     * For C2K signal disturb, and the Main Capability is GSM Card 
     * and change default from default card to CDMA Card. should switch
     * main capability to target subId by reason of C Card can not
     * register network now
     * 1. two SIM cards. 
     * 2. one SIM Card is CDMA card, other is GSM card
     * 3. change default card to C card
     * 4. the main capability is GSM card
     * @param context context
     * @param targetSubId targetSubId
     * @return true switch
     */
    public static boolean shouldSwitchCapability(Context context, int targetSubId) {
        boolean isCdmaCardForTarget = false;
        boolean isGsmCardForMainCapability = false;
        if (!FeatureOption.MTK_C2K_SLOT2_SUPPORT
                && SubscriptionManager.isValidSubscriptionId(targetSubId)) {
           int mainCapabilitySlotId = TelephonyUtils.getMainCapabilitySlotId();
           isGsmCardForMainCapability = SvlteUiccUtils.getInstance().getSimType(mainCapabilitySlotId)
                   == SvlteUiccUtils.SIM_TYPE_GSM;
            int targetSmsSlotId = SubscriptionManager.getSlotId(targetSubId);
            isCdmaCardForTarget = SvlteUiccUtils.getInstance().getSimType(targetSmsSlotId)
                    == SvlteUiccUtils.SIM_TYPE_CDMA;
        }
        Log.d(TAG, "isCdmaCardForTarget: " + isCdmaCardForTarget
                + " isGsmCardForMainCapability: " + isGsmCardForMainCapability);
        return isCdmaCardForTarget && isGsmCardForMainCapability;
    }

    /**
     * For C2K signal disturb, and the Main Capability is GSM Card 
     * and change default from default card to CDMA Card.should switch
     * main capability to target subId by reason of C Card can not
     * register network now.
     * 1. two SIM cards. 
     * 2. one SIM Card is CDMA card, other is GSM card
     * 3. change default card to C card
     * 4. the main capability is GSM card
     * @param context context
     * @param phoneAccount phoneAccount
     * @return true switch
     */
    public static boolean shouldSwichCapabilityForCalls(Context context, PhoneAccountHandle handle) {
        boolean isCdmaCardForTarget = false;
        boolean isGsmCardForMainCapability = false;
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (context != null) {
            subId = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, handle);
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)
                && !FeatureOption.MTK_C2K_SLOT2_SUPPORT) {
            int mainCapabilitySlotId = TelephonyUtils.getMainCapabilitySlotId();
            isGsmCardForMainCapability = SvlteUiccUtils.getInstance().getSimType(mainCapabilitySlotId)
                    == SvlteUiccUtils.SIM_TYPE_GSM;
            int targetCallsSlotId = SubscriptionManager.getSlotId(subId);
            isCdmaCardForTarget = SvlteUiccUtils.getInstance().getSimType(targetCallsSlotId)
                    == SvlteUiccUtils.SIM_TYPE_CDMA;
        }
        Log.d(TAG, "isCdmaCardForTarget: " + isCdmaCardForTarget
                + " isGsmCardForMainCapability: " + isGsmCardForMainCapability);
        return isCdmaCardForTarget && isGsmCardForMainCapability;
    }

    public static boolean isCdamCardAndGsmCard(Context context) {
        boolean isCdmaCard = false;
        boolean isGsmCard = false;
        int simCount = 0;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        }
        for (int i = 0; i < simCount; i++) {
            if (SvlteUiccUtils.getInstance().getSimType(i) == SvlteUiccUtils.SIM_TYPE_CDMA) {
                isCdmaCard = true;
            } else if (SvlteUiccUtils.getInstance().getSimType(i) == SvlteUiccUtils.SIM_TYPE_GSM) {
                isGsmCard = true;
            }
        }
        Log.d(TAG, "isCdmaCard: " + isCdmaCard + " isGsmCard: " + isGsmCard
                + " solution2 support: " + FeatureOption.MTK_C2K_SLOT2_SUPPORT);
        return isCdmaCard && isGsmCard & (!FeatureOption.MTK_C2K_SLOT2_SUPPORT);
    }
}
