package com.speeddial;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * Created by guofeiyao on 15-9-29
 * For SpeedDial all version support
 */
public class SpeedDialPlugin implements View.OnLongClickListener {
    public static final String TAG = SpeedDialPlugin.class.getSimpleName();
    public static final String DZ = "duanze_";

    private Activity mHostActivity;
    private String mHostPackage;
    private Resources mHostResources;
    private EditText mEditText;

    private Context mContext;

    private String[] identifierNameArr;

    public SpeedDialPlugin(Context mContext) {
        this.mContext = mContext;
    }

    public void onViewCreated(Activity activity, View view, String[] idArr, EditText editText) {
        Log.i(TAG, "onViewCreated.");
        mHostActivity = activity;

        mHostPackage = activity.getPackageName();
        mHostResources = activity.getResources();

        identifierNameArr = idArr;
        mEditText = editText;

        if (null == identifierNameArr || 8 != identifierNameArr.length) {
            Log.e(DZ + TAG, "Error when test identifierNameArr!");
            return;
        }

        for (String name : identifierNameArr) {
            view.findViewById(mHostResources.getIdentifier(name, "id", mHostPackage)).setOnLongClickListener(this);
        }
    }


    @Override
    public boolean onLongClick(View v) {
// We have tested it in the above method.
//        if (null == identifierNameArr || 8 != identifierNameArr.length) {
//            Log.e(DZ + TAG, "Error when test identifierNameArr!");
//            return false;
//        }

        if (null == mEditText) {
            Log.e(DZ + TAG, "Error when test mEditText!");
            return false;
        }

        int id = v.getId();

        int key = 0;
        if (id == mHostResources.getIdentifier(identifierNameArr[0], "id", mHostPackage)) {
            key = 2;
        } else if (id == mHostResources.getIdentifier(identifierNameArr[1], "id", mHostPackage)) {
            key = 3;
        } else if (id == mHostResources.getIdentifier(identifierNameArr[2], "id", mHostPackage)) {
            key = 4;
        } else if (id == mHostResources.getIdentifier(identifierNameArr[3], "id", mHostPackage)) {
            key = 5;
        } else if (id == mHostResources.getIdentifier(identifierNameArr[4], "id", mHostPackage)) {
            key = 6;
        } else if (id == mHostResources.getIdentifier(identifierNameArr[5], "id", mHostPackage)) {
            key = 7;
        } else if (id == mHostResources.getIdentifier(identifierNameArr[6], "id", mHostPackage)) {
            key = 8;
        } else if (id == mHostResources.getIdentifier(identifierNameArr[7], "id", mHostPackage)) {
            key = 9;
        }

        if (key > 0 && mEditText.getText().length() <= 1) {
            SpeedDialController.getInstance().handleKeyLongProcess(mHostActivity, mContext, key);
            mEditText.getText().clear();
            return true;
        }
        return false;
    }

    public void onPause(){
        SpeedDialController.getInstance().onPause();
    }
}
