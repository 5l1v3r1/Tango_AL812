package com.android.settings.brightness;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.R;

public class BrightnessPreference extends DialogPreference {
    private static final String TAG = "BrightnessPreference";

    protected BrightnessController mBrightnessController;
    protected boolean mBrightnessControllerFlag = false;
    private CheckBox mAutoBrightnessCB = null;
    private boolean CheckBoxDefault = false;
    private int valueDefault = -1;

    public BrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.display_brightness_setting_dialog);
        createActionButtons();

        // Steal the XML dialogIcon attribute's value
        setDialogIcon(null);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mAutoBrightnessCB = (CheckBox) view.findViewById(R.id.brightness_auto_checkbox);
        mAutoBrightnessCB.setOnCheckedChangeListener(new AutoBrightnessOnCheckedChangeListener(getContext()));

        final ToggleSlider slider = (ToggleSlider) view.findViewById(R.id.brightness_slider);
        mBrightnessController = new BrightnessController(getContext(), null, slider);
        mBrightnessController.registerCallbacks();
        mBrightnessControllerFlag = true;
        int brightnessMode = Settings.System.getInt(getContext().getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);

        CheckBoxDefault = (brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        mAutoBrightnessCB.setChecked(CheckBoxDefault);

        valueDefault = mBrightnessController.getUpdateSliderValue();
    }

    // Allow subclasses to override the action buttons
    public void createActionButtons() {
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        final ContentResolver resolver = getContext().getContentResolver();

        if (!positiveResult) {
            mAutoBrightnessCB.setChecked(CheckBoxDefault);
            Settings.System.putInt(resolver, SCREEN_BRIGHTNESS_MODE,
                    CheckBoxDefault ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
            if (valueDefault != -1) {
                mBrightnessController.setSliderValue(valueDefault);
            }
            mBrightnessControllerFlag = false;
        }
         mBrightnessController.unregisterCallbacks();
    }

    class AutoBrightnessOnCheckedChangeListener implements OnCheckedChangeListener {
        Context context = null;

        public AutoBrightnessOnCheckedChangeListener(Context context){
            this.context = context;
        }

        @Override
        public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
           Settings.System.putInt(context.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                 isChecked ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
    }
}
