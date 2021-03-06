/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2014. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.v2.mode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import junit.framework.Assert;

import com.mediatek.camera.v2.mode.facebeauty.CfbCaptureMode;
import com.mediatek.camera.v2.mode.normal.CaptureMode;
import com.mediatek.camera.v2.mode.pip.PipMode;
import com.mediatek.camera.v2.module.ModuleListener;
import com.mediatek.camera.v2.platform.ModeChangeListener;
import com.mediatek.camera.v2.platform.app.AppController;
import com.mediatek.camera.v2.platform.app.AppUi;
import com.mediatek.camera.v2.services.CameraServices;
import com.mediatek.camera.v2.setting.ISettingServant;
import com.mediatek.camera.v2.setting.ISettingServant.ISettingChangedListener;
import com.mediatek.camera.v2.setting.SettingCtrl;
import com.mediatek.camera.v2.stream.CaptureStreamController;
import com.mediatek.camera.v2.stream.PreviewStreamController;
import com.mediatek.camera.v2.stream.RecordStreamView;
import com.mediatek.camera.v2.stream.PreviewStreamController.PreviewStreamCallback;
import com.mediatek.camera.v2.stream.PreviewStreamController.PreviewSurfaceCallback;
import com.mediatek.camera.v2.stream.RecordStream.RecordStreamStatus;
import com.mediatek.camera.v2.stream.RecordStreamController;
import com.mediatek.camera.v2.stream.StreamManager;
import com.mediatek.camera.v2.stream.CaptureStreamController.CaptureStreamCallback;
import com.mediatek.camera.v2.util.SettingKeys;
import com.mediatek.camera.v2.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.RectF;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.media.Image;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.ViewGroup;

import com.android.camera.v2.app.location.LocationManager;


/**
 * A common abstract {@link ModeController} implementation that contains some utility
 * functions and plumbing we don't want every sub-class of {@link ModeController} to
 * duplicate. Hence all {@link ModeController} implementations should sub-class this
 * class instead.
 */
public abstract class AbstractCameraMode implements ModeController, ISettingChangedListener {
    protected final String               FEATURE_TAG;
    private   final String               TAG;
    protected static final Byte          JPEG_QUALITY = 90; // Default JPEG encoding quality.
    // module controller
    protected final  ModuleListener      mModuleListener;
    // app resources
    protected final  AppController       mAppController;
    protected final  CameraServices      mCameraServices;
    protected final  AppUi               mAppUi;
    protected ISettingServant            mSettingServant;
    protected SettingCtrl                mSettingCtroller;
    protected boolean                    mIsCaptureIntent;
    protected Intent                     mIntent;
    protected Activity                   mActivity;
    
    protected ArrayList<String>          mCaredSettingChangedKeys = new ArrayList<String>();
    
    // preview controller
    protected PreviewStreamController    mPreviewController;
    private   PreviewStreamCallback      mPreviewStreamCallback;
    // capture controller
    protected CaptureStreamController    mCaptureController;
    private   CaptureStreamCallback      mCaptureStreamCallback;
    // record controller
    protected RecordStreamController mRecordController;
    // location
    protected LocationManager mLocationManager;
    protected Location mLocation;

    private RecordStreamView             mRecordStreamView;
    private   RecordStreamStatus         mRecordStreamCallback;
    
    /**
     * App resources should be initialized in constructor.
     * @param app the controller at app level.
     * @param moduleListener the controller at module level for mode.
     */
    public AbstractCameraMode(AppController app, ModuleListener moduleListener) {
        Assert.assertNotNull(app);
        mAppController   = app;
        mAppUi           = app.getCameraAppUi();
        mActivity        = app.getActivity();
        mIntent          = app.getActivity().getIntent();
        mModuleListener  = moduleListener;
        mCameraServices  = app.getServices();
        mSettingCtroller = mCameraServices.getSettingController();
        mSettingServant  = mSettingCtroller.getSettingServant(null);
        FEATURE_TAG      = getTagByModeId(getModeId());
        mLocationManager = app.getLocationManager();

        TAG              = AbstractCameraMode.class.getSimpleName() + "(" + FEATURE_TAG + ")";
        updateCaredSettingChangedKeys();
    }

    @Override
    public void onSettingChanged(Map<String, String> result) {
        String pictureRatio = result.get(SettingKeys.KEY_PICTURE_RATIO);
        final String pictureSize = result.get(SettingKeys.KEY_PICTURE_SIZE);
        if (pictureRatio != null) {
            updatePreviewSize(new PreviewSurfaceCallback() {
                @Override
                public void onPreviewSufaceIsReady(boolean surfaceChanged) {
                    boolean isNeedChaningSessionOutputs = surfaceChanged;
                    if (pictureSize != null) {
                        isNeedChaningSessionOutputs = updatePictureSize() | isNeedChaningSessionOutputs;
                    }
                    if (isNeedChaningSessionOutputs) {
                        mModuleListener.requestChangeSessionOutputs(true);
                    }
                }
            });
        }
    }
    
    /**
     * Streams should be got at open, if stream not initialized, {@link StreamManager} will initialized it.
     * <p>
     * Initialize resources this mode needed, these resources will be destroyed in {@link #close()}
     */
    @Override
    public void open(StreamManager streamManager,
            ViewGroup parentView, boolean isCaptureIntent) {
        mIsCaptureIntent = isCaptureIntent;
        initializeStreamControllers(streamManager);
        mRecordStreamView = new RecordStreamView(mAppController.getActivity(), mRecordController, 
                mAppUi.getModuleLayoutRoot(), isCaptureIntent);
        mSettingServant.registerSettingChangedListener(this,mCaredSettingChangedKeys,
                ISettingChangedListener.MIDDLE_PRIORITY);
    }

    /**
     *  Exit this mode.
     *  <p>
     *  Destroy resources allocated in {@link #open(StreamManager, ViewGroup, boolean)}}
     */
    @Override
    public void close() {
        unInitializeStreamControllers();
        mRecordStreamView.close();
        mSettingServant.unRegisterSettingChangedListener(this);
    }

    /**
     *  Activity's onResume or switch mode(enter new mode) during onResume will call this.
     */
    @Override
    public void resume() {
        updatePreviewSize(new PreviewSurfaceCallback() {
            @Override
            public void onPreviewSufaceIsReady(boolean surfaceChanged) {
                boolean isNeedChaningSessionOutputs = surfaceChanged;
                isNeedChaningSessionOutputs = updatePictureSize() | isNeedChaningSessionOutputs;
                if (isNeedChaningSessionOutputs) {
                    mModuleListener.requestChangeSessionOutputs(true);
                }
            }
        });
    }

    /**
     * Activity's onPause or switch mode(exit old mode) during onResume will call this.
     * <p>
     * Note: When onPause arrives, any capturing or recording should be interrupted.
     */
    @Override
    public void pause() {
        // may be ui is disable when capture, while capture do not finish when pause camera
        // so can not enable ui, force to enable ui when pause camera.
        mAppUi.setAllCommonViewEnable(true);
        mAppUi.setSwipeEnabled(true);
        
        mCaptureController.releaseCaptureStream();
    }
    
    @Override
    public void onOrientationChanged(int orientation) {
        
    }
    
    @Override
    public void onPreviewVisibilityChanged(int visibility) {
        
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        
    }

    @Override
    public void onOkClick() {
        
    }
    
    @Override
    public void onCancelClick() {
        
    }

    @Override
    public ModeGestureListener getModeGestureListener() {
        return null;
    }
    
    @Override
    public void prepareSurfaceBeforeOpenCamera() {
        Log.i(TAG, "[prepareSurfaceBeforeOpenCamera]+");
        updatePreviewSize(new PreviewSurfaceCallback() {
            @Override
            public void onPreviewSufaceIsReady(boolean surfaceChanged) {
                boolean isNeedChaningSessionOutputs = surfaceChanged;
                isNeedChaningSessionOutputs = updatePictureSize() | isNeedChaningSessionOutputs;
                if (isNeedChaningSessionOutputs) {
                    mModuleListener.requestChangeSessionOutputs(true);
                }
            }
        });
        Log.i(TAG, "[prepareSurfaceBeforeOpenCamera]-");
    }

    @Override
    public void onFirstFrameAvailable() {
        if (mPreviewController != null) {
            mPreviewController.onFirstFrameAvailable();
        }
    }
    
    @Override
    public void onPreviewCaptureStarted(CaptureRequest request, long timestamp,
            long frameNumber) {
        
    }

    @Override
    public void onPreviewCaptureCompleted(CaptureRequest request,
            TotalCaptureResult result) {
    }

    @Override
    public CaptureCallback getCaptureCallback() {
        return null;
    }
    
    @Override
    public boolean onBackPressed() {
        return false;
    }
    
    @Override
    public boolean switchCamera(String cameraId) {
        return false;
    }
    
    @Override
    public void onPlay() {
        
    }
    
    @Override
    public void onRetake() {
        
    }
    
    /**
     * Cared setting items should be added in sub-class's constructors.
     */
    protected void updateCaredSettingChangedKeys() {
        addCaredSettingChangedKeys(SettingKeys.KEY_PICTURE_RATIO);
        addCaredSettingChangedKeys(SettingKeys.KEY_PICTURE_SIZE);
    }

    protected void addCaredSettingChangedKeys(String key) {
        if (key != null && !mCaredSettingChangedKeys.contains(key)) {
            mCaredSettingChangedKeys.add(key);
        }
    }
    
    protected abstract int getModeId();
    
    protected PreviewStreamCallback getPreviewStreamCallback() {
        if (mPreviewStreamCallback == null) {
            mPreviewStreamCallback = new PreviewStreamCallback() {
                @Override
                public void onFirstFrameAvailable() {
                    mAppController.onPreviewStarted();
                }
            };
        }
        return mPreviewStreamCallback;
    }
    
    protected CaptureStreamCallback getCaptureStreamCallback() {
        if (mCaptureStreamCallback == null) {
            mCaptureStreamCallback = new CaptureStreamCallback() {
                @Override
                public void onCaptureCompleted(Image image) {
                    Assert.assertNotNull(image);
                    Log.i(TAG, "onCaptureCompleted format = " + image.getFormat());
                    if (image.getFormat() == ImageFormat.JPEG) {
                        saveJpegInPath(Utils.acquireJpegBytesAndClose(image), "/sdcard/DCIM/Camera/test.jpeg");
                    }
                }
            };
        }
        return mCaptureStreamCallback;
    }

    protected RecordStreamStatus getRecordStreamCallback() {
        return null;
    }

    /**
     * get the required capture format, default value is ImageForamt.JPEG
     * @return
     */
    protected int getCaptureFormat() {
        return ImageFormat.JPEG;
    }

    
    protected Size getPreviewSize() {
        return mSettingServant.getPreviewSize();
    }

    /**
     * 
     * @return
     */
    protected Size getCaptureSize() {
        Log.i(TAG, "[getCaptureSize]+");
        String currentPictureSize = mSettingServant.getSettingValue(SettingKeys.KEY_PICTURE_SIZE);
        Size size = Utils.getSize(currentPictureSize);
        Log.i(TAG, "[getCaptureSize]-");
        return size;
    }
    
    /**
     * when set preview size, sub-class can use this method do their preview size change action.
     * @return true indicate preview surface changes, need configure new session outputs
     */
    protected boolean changingModePreviewSize() {
        return false;
    }

    /**
     * when set picture size, sub-class can use this method do their picture size change action.
     * @return true indicate picture surface changes, need configure new session outputs
     */
    protected boolean changingModePictureSize() {
        return false;
    }

    /**
     *  update preview stream's preview size.
     *  <p>
     *  Note: if preview size changes, notify app level preview change.
     */
    protected boolean updatePreviewSize(PreviewSurfaceCallback surfaceCallback) {
        Log.i(TAG, "[updatePreviewSize]+");
        Size previewSize = getPreviewSize();
        if (previewSize == null) {
            Log.i(TAG, "why preview size is nulll?");
            return false;
        }
        
        if (surfaceCallback != null) {
            mPreviewController.setOneShotPreviewSurfaceCallback(surfaceCallback);
        }
        boolean previewSizeChanged = mPreviewController.updatePreviewSize(previewSize);
        previewSizeChanged = changingModePreviewSize() || previewSizeChanged;
        
        if(previewSizeChanged) {
            // tell preview view surface size changed
            mAppController.updatePreviewSize(previewSize.getWidth(),previewSize.getHeight());
        }
        Log.i(TAG, "[updatePreviewSize]- previewSizeChanged:" + previewSizeChanged + 
                " preview size: " + previewSize.getWidth() + " x " + previewSize.getHeight());
        return previewSizeChanged;
    }

    protected boolean updatePictureSize() {
        Log.i(TAG, "[updatePictureSize]+");
        int format = getCaptureFormat();
        Size pictureSize = getCaptureSize();
        if (pictureSize == null) {
            Log.i(TAG, "why picture size is nulll?");
            return false;
        }
        Log.i(TAG,"[updatePictureSize]- pictureSize = " + pictureSize.getWidth() + " x " + pictureSize.getHeight()
                + " format = " + format);
        boolean pictureSizeChange = mCaptureController.updateCaptureSize(pictureSize, format);
        mCaptureController.setCaptureStreamCallback(getCaptureStreamCallback());
        pictureSizeChange = changingModePictureSize() || pictureSizeChange;
        if (pictureSizeChange && !MediaStore.ACTION_VIDEO_CAPTURE.equals(mIntent.getAction())) {
            String pictureFormat = pictureSize.getWidth() + "x" + pictureSize.getHeight() + "-superfine";
            mAppUi.showLeftCounts(Utils.getImageSize(pictureFormat), true);
        }
        return pictureSizeChange;
    }
    
    private String getTagByModeId(int modeId) {
        String tagName = null;
        switch (modeId) {
        case ModeChangeListener.MODE_CAPTURE:
            tagName = CaptureMode.class.getSimpleName();
            break;
        case ModeChangeListener.MODE_FACE_BEAUTY:
            tagName = CfbCaptureMode.class.getSimpleName();
            break;
        case ModeChangeListener.MODE_PIP:
            tagName = PipMode.class.getSimpleName();
            break;
        default:
            break;
        }
        return tagName;
    }
    
    private void initializeStreamControllers(StreamManager streamManager) {
        mPreviewController = streamManager.getPreviewController(getModeId());
        mPreviewController.setPreviewStreamCallback(getPreviewStreamCallback());
        
        mCaptureController = streamManager.getCaptureController(getModeId());
        
        mRecordController = streamManager.getRecordController(getModeId());
        mRecordController.registerRecordingObserver(getRecordStreamCallback());
    }
    
    private void unInitializeStreamControllers() {
        if (mPreviewController != null) {
            mPreviewController.setPreviewStreamCallback(null);
        }
        if (mCaptureController != null) {
            mCaptureController.setCaptureStreamCallback(null);
        }
        if (mRecordController != null) {
            mRecordController.unregisterCaptureObserver(getRecordStreamCallback());
        }
    }
    
    private void saveJpegInPath(byte[] jepgdata, String path) {
        Log.i(TAG, "[saveJpegInPath]+ path = " + path);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(jepgdata);
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "[saveJpegInPath]Failed to write image,exception:", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "[saveJpegInPath]ioexception:", e);
                }
            }
            Log.i(TAG, "[saveJpegInPath]-");
        }
    }
}
