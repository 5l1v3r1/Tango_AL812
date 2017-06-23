package com.android.settings;

import android.security.KeyStore;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Fragment;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.app.FragmentManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import android.util.MutableBoolean;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.widget.LockPatternUtils;

public class ScreenLockEnabled extends SettingsActivity {
	public static final String CONFIRM_CREDENTIALS = "confirm_credentials";
	public static final String TAG_FRP_WARNING_DIALOG = "frp_warning_dialog";
	///M: Add for voice unlock
    private static final String KEY_UNLOCK_SET_VOICE_WEAK = "unlock_set_voice_weak";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ScreenLockEnabledFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return ScreenLockEnabledFragment.class;
    }

    public static class InternalActivity extends ScreenLockEnabled {
    }
    
    public static class ScreenLockEnabledFragment extends SettingsPreferenceFragment {
    	
    	private static final String TAG = "ScreenLockEnabledFragment";
    	
    	public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";
    	private static final boolean ALWAY_SHOW_TUTORIAL = true;
    	
    	private static final int MIN_PASSWORD_LENGTH = 4;
        private static final String KEY_UNLOCK_BACKUP_INFO = "unlock_backup_info";
        private static final String KEY_UNLOCK_SET_OFF = "unlock_set_off";
        private static final String KEY_UNLOCK_SET_NONE = "unlock_set_none";
        private static final String KEY_UNLOCK_SET_BIOMETRIC_WEAK = "unlock_set_biometric_weak";
        private static final String KEY_UNLOCK_SET_PIN = "unlock_set_pin";
        private static final String KEY_UNLOCK_SET_PASSWORD = "unlock_set_password";
        private static final String KEY_UNLOCK_SET_PATTERN = "unlock_set_pattern";
    	
    	private static final String PASSWORD_CONFIRMED = "password_confirmed";
        private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
        private static final String FINISH_PENDING = "finish_pending";
        public static final String ENCRYPT_REQUESTED_QUALITY = "encrypt_requested_quality";
        public static final String ENCRYPT_REQUESTED_DISABLED = "encrypt_requested_disabled";
        public static final String MINIMUM_QUALITY_KEY = "minimum_quality";
        
        private static final int CONFIRM_EXISTING_REQUEST = 100;
        private static final int FALLBACK_REQUEST = 101;
        private static final int ENABLE_ENCRYPTION_REQUEST = 102;
    	
    	private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    	private DevicePolicyManager mDPM;
    	private KeyStore mKeyStore;
    	
    	private boolean mPasswordConfirmed = false;
    	private boolean mWaitingForConfirmation = false;
        private static boolean mFinishPending = false;
        private int mEncryptionRequestQuality;
        private boolean mEncryptionRequestDisabled;
        private boolean mRequirePassword;
        private LockPatternUtils mLockPatternUtils;
        private boolean mIsLockEnabledAllowed;
        private boolean mIsOrgSecure = false;
        private static boolean mWaitForDlg = false;
        private boolean updateNoneScreenLockFlag = false;
//        private static boolean allowRelieveFigureprintUnlockFlag = false;
//        private static boolean figureprintUnlockDlgFinish = false;
    	
    	@Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mKeyStore = KeyStore.getInstance();
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this.getActivity());
            mLockPatternUtils = new LockPatternUtils(getActivity());
            mIsOrgSecure = mLockPatternUtils.isSecure();
            
            mIsLockEnabledAllowed = isLockEnabledAllowed();

            // Defaults to needing to confirm credentials
            final boolean confirmCredentials = getActivity().getIntent()
                .getBooleanExtra(CONFIRM_CREDENTIALS, true);
            if (getActivity() instanceof ScreenLockEnabled.InternalActivity) {
                mPasswordConfirmed = !confirmCredentials;
            }

            if (savedInstanceState != null) {
                mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
                mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
                mFinishPending = savedInstanceState.getBoolean(FINISH_PENDING);
                mEncryptionRequestQuality = savedInstanceState.getInt(ENCRYPT_REQUESTED_QUALITY);
                mEncryptionRequestDisabled = savedInstanceState.getBoolean(
                        ENCRYPT_REQUESTED_DISABLED);
            }
            
            updateNoneScreenLockFlag = false;
//            allowRelieveFigureprintUnlockFlag = true;
//            figureprintUnlockDlgFinish = false;
            mWaitForDlg = false;
            
            if (mPasswordConfirmed) {
            	updateNoneScreenLock();
            } else if (!mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                if (!helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST, null, null)) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updateNoneScreenLock(false);
                } else {
                    mWaitingForConfirmation = true;
                }
            }
        }
    	
    	//add by HQ_caoxuhao at 20150918 HQ01369476 begin
    	@Override
        public void onResume() {
            super.onResume();
            Log.i("caoxuhao","onResume");
            
            //if user cancel the password input dialog 
            if (mWaitForDlg && !updateNoneScreenLockFlag) {
				finish();
			}
            
            //this flag = true means we entry the correct password and the origin password is Secure
            boolean flag = (!mIsLockEnabledAllowed) && mIsOrgSecure;
            
            //If the origin password is unsecure, we need not creat the figureprint unlock prompt dialog.
            //So, in this situation, the figureprintUnlockDlgFinish = true
//            if (flag) {
//            	figureprintUnlockDlgFinish = false;
//			}else {
//				figureprintUnlockDlgFinish = true;
//			}
            
            //show the figureprint unlock prompt dialog after entry the correct password and meet the flag
            if (mWaitForDlg && flag) {
            	Log.i("caoxuhao","onResume begin FigureprintUnlockFlag");
//            	allowRelieveFigureprintUnlockFlag = false;
            	showFactoryResetProtectionWarningDialog(KEY_UNLOCK_SET_OFF);
			}
            
            //Finish this activity if user click the cancel button in the figureprint unlock prompt dialog. 
//            if (figureprintUnlockDlgFinish && !allowRelieveFigureprintUnlockFlag) {
//            	Log.i("caoxuhao","Finish this activity if user click the cancel button in the figureprint unlock prompt dialog");
//            	finish();
//			}
            
            //update the screen lock when we entry the correct password and allow unuse figureprint unlock
//            if (figureprintUnlockDlgFinish && allowRelieveFigureprintUnlockFlag && updateNoneScreenLockFlag) {
//            	Log.i("caoxuhao","onResume begin change password");
//            	updateNoneScreenLock();
//			}
            
            //if the origin password is unsecure, this will finish the fragment
            if (mFinishPending){
            	Log.i("caoxuhao","mFinishPending finish");
                mFinishPending = false;
                finish();
  			}
        }
    	//add by HQ_caoxuhao at 20150918 HQ01369476 end
    	
    	private boolean isLockEnabledAllowed() {
            int speed = Settings.System.getInt(getContentResolver(), 
            		Settings.System.LOCKSCREEN_DISABLED, 0);
            if (speed == 1) {
                return true;
            } else {
                return false;
            }
        }
    	
    	@Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.i("caoxuhao","onActivityResult");
            mWaitingForConfirmation = false;
            if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
                mPasswordConfirmed = true;
//                updateNoneScreenLock();
                updateNoneScreenLockFlag = true;
            } else if (requestCode == FALLBACK_REQUEST) {
                mChooseLockSettingsHelper.utils().deleteTempGallery();
                getActivity().setResult(resultCode);
                finish();
            } else if (requestCode == ENABLE_ENCRYPTION_REQUEST
                    && resultCode == Activity.RESULT_OK) {
                mRequirePassword = data.getBooleanExtra(
                        EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true);
                //do something
            } else {
                getActivity().setResult(Activity.RESULT_CANCELED);
                finish();
            }
            
            mWaitForDlg = true;
        }
    	
    	
    	@Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // Saved so we don't force user to re-enter their password if configuration changes
            outState.putBoolean(PASSWORD_CONFIRMED, mPasswordConfirmed);
            outState.putBoolean(WAITING_FOR_CONFIRMATION, mWaitingForConfirmation);
            outState.putBoolean(FINISH_PENDING, mFinishPending);
            outState.putInt(ENCRYPT_REQUESTED_QUALITY, mEncryptionRequestQuality);
            outState.putBoolean(ENCRYPT_REQUESTED_DISABLED, mEncryptionRequestDisabled);
        }
    	
    	@Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = super.onCreateView(inflater, container, savedInstanceState);
            return v;
        }
    	
        private void updateNoneScreenLock() {
        	updateNoneScreenLock(true);
        }
        
        private void updateNoneScreenLock(boolean doFinish) {
        	
        	mFinishPending = true;
        	
        	final PreferenceScreen prefScreen = getPreferenceScreen();
            if (prefScreen != null) {
                prefScreen.removeAll();
            }
            
        	if (mIsLockEnabledAllowed) {
        		addPreferencesFromResource(R.xml.security_settings_chooser);
        		updateUnlockMethodAndFinish(
	                     DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, false, doFinish);       		
			}else{
				addPreferencesFromResource(R.xml.security_settings_lockscreen);
				updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, true, doFinish);
			}
        }
        
        void updateUnlockMethodAndFinish(int quality, boolean disabled, boolean doFinish) {
            // Sanity check. We should never get here without confirming user's existing password.
            if (!mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }

            final boolean isFallback = getActivity().getIntent()
                .getBooleanExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK, false);  //M: Modify for voice unlock

            quality = upgradeQuality(quality, null);

            final Context context = getActivity();
           
            if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
            	Log.i(TAG,"quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED do finish");
                mChooseLockSettingsHelper.utils().clearLock(false);
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled);
                getActivity().setResult(Activity.RESULT_OK);
                if (doFinish) finish();
                
            } else {
            	Log.i(TAG,"updateUnlockMethodAndFinish do finish");
            	if (doFinish) finish();
            }
        }
    	
        
        private int upgradeQuality(int quality, MutableBoolean allowBiometric) {
            quality = upgradeQualityForDPM(quality);
            quality = upgradeQualityForKeyStore(quality);
            return quality;
        }

        private int upgradeQualityForDPM(int quality) {
            // Compare min allowed password quality
            int minQuality = mDPM.getPasswordQuality(null);
            if (quality < minQuality) {
                quality = minQuality;
            }
            return quality;
        }

        private int upgradeQualityForKeyStore(int quality) {
            if (!mKeyStore.isEmpty()) {
                if (quality < CredentialStorage.MIN_PASSWORD_QUALITY) {
                    quality = CredentialStorage.MIN_PASSWORD_QUALITY;
                }
            }
            return quality;
        }
        
        
        
        void updateUnlockMethodAndFinish(int quality, boolean disabled) {
            // Sanity check. We should never get here without confirming user's existing password.
            if (!mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }

            final boolean isFallback = getActivity().getIntent()
                .getBooleanExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK, false);  //M: Modify for voice unlock

            quality = upgradeQuality(quality, null);

            final Context context = getActivity();
            if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                int minLength = mDPM.getPasswordMinimumLength(null);
                if (minLength < MIN_PASSWORD_LENGTH) {
                    minLength = MIN_PASSWORD_LENGTH;
                }
                final int maxLength = mDPM.getPasswordMaximumLength(quality);
                Intent intent = getLockPasswordIntent(context, quality, isFallback, minLength,
                        maxLength, mRequirePassword,  /* confirm credentials */false);
                if (isFallback) {
                    //M: Add for voice unlock @{
                    String isFallbackFor = getActivity().getIntent().getStringExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK_FOR);
                    String commandKey = getActivity().getIntent().
                        getStringExtra(LockPatternUtils.SETTINGS_COMMAND_KEY);
                    String commandValue = getActivity().getIntent().
                        getStringExtra(LockPatternUtils.SETTINGS_COMMAND_VALUE);
                    intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_KEY, commandKey);
                    intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_VALUE, commandValue);
                    intent.putExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK_FOR, isFallbackFor);
                    //@}
                    startActivityForResult(intent, FALLBACK_REQUEST);
                    return;
                } else {
                    mFinishPending = true;
                    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    startActivity(intent);
                }
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                Intent intent = getLockPatternIntent(context, isFallback, mRequirePassword,
                        /* confirm credentials */false);
                if (isFallback) {
                    //M: Add for voice unlock @{
                    String isFallbackFor = getActivity().getIntent().
                        getStringExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK_FOR);
                    String commandKey = getActivity().getIntent().
                        getStringExtra(LockPatternUtils.SETTINGS_COMMAND_KEY);
                    String commandValue = getActivity().getIntent().
                        getStringExtra(LockPatternUtils.SETTINGS_COMMAND_VALUE);
                    intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_KEY, commandKey);
                    intent.putExtra(LockPatternUtils.SETTINGS_COMMAND_VALUE, commandValue);
                    intent.putExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK_FOR, isFallbackFor);
                    //@}
                    startActivityForResult(intent, FALLBACK_REQUEST);
                    return;
                } else {
                    mFinishPending = true;
                    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    startActivity(intent);
                }
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK) {
                Intent intent = getBiometricSensorIntent();
                mFinishPending = true;
                startActivity(intent);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_VOICE_WEAK) {  ///M: Add for voice unlock
                Intent intent = getVoiceSensorIntent();
                mFinishPending = true;
                startActivity(intent);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                mChooseLockSettingsHelper.utils().clearLock(false);
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled);
                getActivity().setResult(Activity.RESULT_OK);
                finish();
            } else {
                finish();
            }
        }
        
        private void finishSelf(){
        	finish();
        }
        
        private boolean setUnlockMethod(String unlockMethod) {
            EventLog.writeEvent(EventLogTags.LOCK_SCREEN_TYPE, unlockMethod);

            if (KEY_UNLOCK_SET_OFF.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, true /* disabled */ );
            } else if (KEY_UNLOCK_SET_NONE.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, false /* disabled */ );
                //add by HQ_yulisuo for HQ01377948 2015-9-11
                // unBindAppLock();
                // unBindSecurity();
            } else if (KEY_UNLOCK_SET_BIOMETRIC_WEAK.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK, false);
            } else if (KEY_UNLOCK_SET_PATTERN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, false);
            } else if (KEY_UNLOCK_SET_PIN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC, false);
            } else if (KEY_UNLOCK_SET_PASSWORD.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC, false);
            } else if (KEY_UNLOCK_SET_VOICE_WEAK.equals(unlockMethod)) {  ///M: Add for voice unlock
                 updateUnlockMethodAndFinish(
                         DevicePolicyManager.PASSWORD_QUALITY_VOICE_WEAK, false);
            } else {
                Log.e(TAG, "Encountered unknown unlock method to set: " + unlockMethod);
                return false;
            }
            return true;
        }
        
        /**
         * If the device has encryption already enabled, then ask the user if they
         * also want to encrypt the phone with this password.
         *
         * @param quality
         * @param disabled
         */
        private void maybeEnableEncryption(int quality, boolean disabled) {
            if (Process.myUserHandle().isOwner() && LockPatternUtils.isDeviceEncryptionEnabled()) {
                mEncryptionRequestQuality = quality;
                mEncryptionRequestDisabled = disabled;
                final Context context = getActivity();
                // If accessibility is enabled and the user hasn't seen this dialog before, set the
                // default state to agree with that which is compatible with accessibility
                // (password not required).
                final boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                final boolean required = mLockPatternUtils.isCredentialRequiredToDecrypt(!accEn);
                Intent intent = getEncryptionInterstitialIntent(context, quality, required);
                startActivityForResult(intent, ENABLE_ENCRYPTION_REQUEST);
            } else {
                mRequirePassword = false; // device encryption not enabled or not device owner.
                updateUnlockMethodAndFinish(quality, disabled);
            }
        }
        
        
        
        
        private Intent getVoiceSensorIntent() {
            Intent fallBackIntent = new Intent().setClass(getActivity(), ChooseLockGeneric.class);
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK, true);
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK_FOR,
                    LockPatternUtils.TYPE_VOICE_UNLOCK);
            fallBackIntent.putExtra(CONFIRM_CREDENTIALS, false);
            fallBackIntent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE,
                    R.string.backup_lock_settings_picker_title);

            boolean showTutorial = ALWAY_SHOW_TUTORIAL ||
                    !mChooseLockSettingsHelper.utils().isBiometricWeakEverChosen();
            Intent intent = new Intent();
            intent.setClassName("com.mediatek.voiceunlock", "com.mediatek.voiceunlock.VoiceUnlock");
            return intent;
        }
        
        
        private Intent getBiometricSensorIntent() {
            Intent fallBackIntent = new Intent().setClass(getActivity(),
                    ChooseLockGeneric.InternalActivity.class);
            //M: Modify for voice unlock @{
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK, true);
            fallBackIntent.putExtra(LockPatternUtils.LOCKSCREEN_WEAK_FALLBACK_FOR,
                    LockPatternUtils.TYPE_FACE_UNLOCK);
            //@}
            fallBackIntent.putExtra(CONFIRM_CREDENTIALS, false);
            fallBackIntent.putExtra(EXTRA_SHOW_FRAGMENT_TITLE,
                    R.string.backup_lock_settings_picker_title);

            boolean showTutorial = ALWAY_SHOW_TUTORIAL ||
                    !mChooseLockSettingsHelper.utils().isBiometricWeakEverChosen();
            Intent intent = new Intent();
            intent.setClassName("com.android.facelock", "com.android.facelock.SetupIntro");
            intent.putExtra("showTutorial", showTutorial);
            PendingIntent pending = PendingIntent.getActivity(getActivity(), 0, fallBackIntent, 0);
            intent.putExtra("PendingIntent", pending);
            return intent;
        }
        
        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                boolean required) {
            return EncryptionInterstitial.createStartIntent(context, quality, required);
        }

        
        protected Intent getLockPasswordIntent(Context context, int quality,
                final boolean isFallback, int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, boolean confirmCredentials) {
            return ChooseLockPassword.createIntent(context, quality, isFallback, minLength,
                    maxLength, requirePasswordToDecrypt, confirmCredentials);
        }
        
        protected Intent getLockPatternIntent(Context context, final boolean isFallback,
                final boolean requirePassword, final boolean confirmCredentials) {
            return ChooseLockPattern.createIntent(context, isFallback, requirePassword,
                    confirmCredentials);
        }
        
        private int getResIdForFactoryResetProtectionWarningTitle() {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return R.string.unlock_disable_lock_pattern_summary;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return R.string.unlock_disable_lock_pin_summary;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return R.string.unlock_disable_lock_password_summary;
                default:
                    return R.string.unlock_disable_lock_unknown_summary;
            }
        }
        
        private void showFactoryResetProtectionWarningDialog(final String unlockMethodToSet) {
            int title = getResIdForFactoryResetProtectionWarningTitle();
            FactoryResetProtectionWarningDialog dialog =
                    FactoryResetProtectionWarningDialog.newInstance(title, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), TAG_FRP_WARNING_DIALOG);
            
        }
        
        public static class FactoryResetProtectionWarningDialog extends DialogFragment {

            private static final String ARG_TITLE_RES = "titleRes";
            private static final String ARG_UNLOCK_METHOD_TO_SET = "unlockMethodToSet";

            public static FactoryResetProtectionWarningDialog newInstance(int title,
                    String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag =
                        new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt(ARG_TITLE_RES, title);
                args.putString(ARG_UNLOCK_METHOD_TO_SET, unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            @Override
            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) == null) {
                    // Prevent opening multiple dialogs if tapped on button quickly
                    super.show(manager, tag);
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();
                return new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fingerprint_no_protection)
                        .setMessage(R.string.fingerprint_no_protection_note)
                        .setPositiveButton(R.string.continue_all_caps,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ((ScreenLockEnabledFragment) getParentFragment())
                                                .setUnlockMethod(
                                                        args.getString(ARG_UNLOCK_METHOD_TO_SET));
//                                        allowRelieveFigureprintUnlockFlag = true;
//                                        figureprintUnlockDlgFinish = true;
                                            unBindAppLock();
                                            unBindSecurity();
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
//                                    	Log.i("caoxuhao","click setNegativeButton to dismiss");
                                    	dismiss();
                                    	((ScreenLockEnabledFragment) getParentFragment()).finishSelf();
//                                        figureprintUnlockDlgFinish = true;
                                    }
                                }
                        )
                        .create();
            }

            //add by HQ_yulisuo for HQ01377948 2015-9-11
        private void unBindAppLock(){
            Uri uri = Uri.parse("content://com.huawei.systemmanager.applockprovider");
            Bundle resBundle = getActivity().getContentResolver().call(uri, "unbind_fingerprint", null, null);
        }

        private void unBindSecurity(){
            Uri uri = Uri.parse("content://com.huawei.hidisk.fingerprint");
            Bundle resBundle = getActivity().getContentResolver().call(uri, "unbind_fingerprint", null, null);
        }
        //add by HQ_yulisuo for HQ01377948 2015-9-11 end
        }
        
        
    }
}
