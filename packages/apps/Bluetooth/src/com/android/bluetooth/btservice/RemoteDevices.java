/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012-2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMasInstance;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.RemoteDevices.DeviceProperties;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;


final class RemoteDevices {
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothRemoteDevices";


    private static BluetoothAdapter mAdapter;
    private static AdapterService mAdapterService;
    private static ArrayList<BluetoothDevice> mSdpTracker;
    private static ArrayList<BluetoothDevice> mSdpMasTracker;

    private Object mObject = new Object();

    private static final int UUID_INTENT_DELAY = 6000;
    private static final int MESSAGE_UUID_INTENT = 1;

    private static final int MAS_INSTANCE_INTENT_DELAY = 6000;
    private static final int MESSAGE_MAS_INSTANCE_INTENT = 2;

    private HashMap<BluetoothDevice, DeviceProperties> mDevices;

    RemoteDevices(AdapterService service) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapterService = service;
        mSdpTracker = new ArrayList<BluetoothDevice>();
        mSdpMasTracker = new ArrayList<BluetoothDevice>();
        mDevices = new HashMap<BluetoothDevice, DeviceProperties>();
    }


    void cleanup() {
        if (mSdpTracker !=null)
            mSdpTracker.clear();

        if (mSdpMasTracker != null)
            mSdpMasTracker.clear();

        if (mDevices != null)
            mDevices.clear();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    DeviceProperties getDeviceProperties(BluetoothDevice device) {
        synchronized (mDevices) {
            return mDevices.get(device);
        }
    }

    BluetoothDevice getDevice(byte[] address) {
        for (BluetoothDevice dev : mDevices.keySet()) {
            if (dev.getAddress().equals(Utils.getAddressStringFromByte(address))) {
                return dev;
            }
        }
        return null;
    }

    DeviceProperties addDeviceProperties(byte[] address) {
        synchronized (mDevices) {
            debugLog("addDeviceProperties: Remote Address = " + Utils.getAddressStringFromByte(address));
            DeviceProperties prop = new DeviceProperties();
            BluetoothDevice device =
                    mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(address));
            prop.mAddress = address;
            mDevices.put(device, prop);
            return prop;
        }
    }

    class DeviceProperties {
        private String mName;
        private byte[] mAddress;
        private int mBluetoothClass;
        private short mRssi;
        private ParcelUuid[] mUuids;
        private int mDeviceType;
        private String mAlias;
        private int mBondState;

        DeviceProperties() {
            mBondState = BluetoothDevice.BOND_NONE;
        }

        /**
         * @return the mName
         */
        String getName() {
            synchronized (mObject) {
                return mName;
            }
        }

        /**
         * @return the mClass
         */
        int getBluetoothClass() {
            synchronized (mObject) {
                return mBluetoothClass;
            }
        }

        /**
         * @return the mUuids
         */
        ParcelUuid[] getUuids() {
            synchronized (mObject) {
                return mUuids;
            }
        }

        /**
         * @return the mAddress
         */
        byte[] getAddress() {
            synchronized (mObject) {
                return mAddress;
            }
        }

        /**
         * @return mRssi
         */
        short getRssi() {
            synchronized (mObject) {
                return mRssi;
            }
        }

        /**
         * @return mDeviceType
         */
        int getDeviceType() {
            synchronized (mObject) {
                return mDeviceType;
            }
        }

        /**
         * @return the mAlias
         */
        String getAlias() {
            synchronized (mObject) {
                debugLog("getAlias: mAlias = " + mAlias);
                return mAlias;
            }
        }

        /**
         * @param mAlias the mAlias to set
         */
        void setAlias(String mAlias) {
            synchronized (mObject) {
                this.mAlias = mAlias;
                debugLog("setAlias: mAlias = " + mAlias);
                mAdapterService.setDevicePropertyNative(mAddress,
                    AbstractionLayer.BT_PROPERTY_REMOTE_FRIENDLY_NAME, mAlias.getBytes());
            }
        }

        /**
         * @param mBondState the mBondState to set
         */
        void setBondState(int mBondState) {
            synchronized (mObject) {
                this.mBondState = mBondState;
                if (mBondState == BluetoothDevice.BOND_NONE)
                {
                    /* Clearing the Uuids local copy when the device is unpaired. If not cleared,
                    cachedBluetoothDevice issued a connect using the local cached copy of uuids,
                    without waiting for the ACTION_UUID intent.
                    This was resulting in multiple calls to connect().*/
                    mUuids = null;
                }
            }
        }

        /**
         * @return the mBondState
         */
        int getBondState() {
            synchronized (mObject) {
                return mBondState;
            }
        }
    }

    private void sendUuidIntent(BluetoothDevice device) {
        DeviceProperties prop = getDeviceProperties(device);
        debugLog("sendUuidIntent: device = " + device + ", UUID = " + prop.mUuids);
        mAdapterService.initProfilePriorities(device, prop.mUuids);
        /// M: ALPS01776848: Send UUID intent after profile priorities are set
        mAdapterService.sendUuidDelay(device, prop == null ? null : prop.mUuids);
        //Remove the outstanding UUID request
        mSdpTracker.remove(device);
    }


    private void sendMasInstanceIntent(BluetoothDevice device,
            ArrayList<BluetoothMasInstance> instances) {
        Intent intent = new Intent(BluetoothDevice.ACTION_MAS_INSTANCE);

        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        if (instances != null)  intent.putExtra(BluetoothDevice.EXTRA_MAS_INSTANCE, instances);
        mAdapterService.sendBroadcast(intent, AdapterService.BLUETOOTH_ADMIN_PERM);

        //Remove the outstanding UUID request
        mSdpMasTracker.remove(device);
    }
    void devicePropertyChangedCallback(byte[] address, int[] types, byte[][] values) {
        Intent intent;
        byte[] val;
        int type;
        BluetoothDevice bdDevice = getDevice(address);
        DeviceProperties device;
        if (bdDevice == null) {
            device = addDeviceProperties(address);
            bdDevice = getDevice(address);
        } else {
            device = getDeviceProperties(bdDevice);
        }
        debugLog("devicePropertyChangedCallback: bdDevice = " + bdDevice);
        for (int j = 0; j < types.length; j++) {
            type = types[j];
            val = values[j];
            if(val.length <= 0)
                errorLog("devicePropertyChangedCallback: bdDevice: " + bdDevice + ", value is empty for type: " + type);
            else {
                synchronized(mObject) {
                    /// M: ALPS01276963: Add log to confirm whether mObject lock is hold by this callback thread caused by send broadcast
                    debugLog("devicePropertyChangedCallback: Acquire Lock - RemoteDevices.mObject");
                    switch (type) {
                        case AbstractionLayer.BT_PROPERTY_BDNAME:
                            device.mName = new String(val);
                            intent = new Intent(BluetoothDevice.ACTION_NAME_CHANGED);
                            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, bdDevice);
                            intent.putExtra(BluetoothDevice.EXTRA_NAME, device.mName);
                            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                            /// M: ALPS01276963: Add log to confirm whether mObject lock is hold by this callback thread caused by send broadcast
                            debugLog("Send Broadcast: Device Name");
                            mAdapterService.sendBroadcast(intent, mAdapterService.BLUETOOTH_PERM);
                            debugLog("Remote Device name is: " + device.mName);
                            break;
                        case AbstractionLayer.BT_PROPERTY_REMOTE_FRIENDLY_NAME:
                            /// M: ALPS01263586: Google issue, can not copy array to String
                            device.mAlias = new String(val);
                            debugLog("Remote Alias is: " + device.mAlias);
                            break;
                        case AbstractionLayer.BT_PROPERTY_BDADDR:
                            device.mAddress = val;
                            debugLog("Remote Address is:" + Utils.getAddressStringFromByte(val));
                            break;
                        case AbstractionLayer.BT_PROPERTY_CLASS_OF_DEVICE:
                            device.mBluetoothClass =  Utils.byteArrayToInt(val);
                            intent = new Intent(BluetoothDevice.ACTION_CLASS_CHANGED);
                            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, bdDevice);
                            intent.putExtra(BluetoothDevice.EXTRA_CLASS,
                                    new BluetoothClass(device.mBluetoothClass));
                            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                            /// M: ALPS01276963: Add log to confirm whether mObject lock is hold by this callback thread caused by send broadcast
                            debugLog("Send Broadcast: Device Class");
                            mAdapterService.sendBroadcast(intent, mAdapterService.BLUETOOTH_PERM);
                            debugLog("Remote class is:" + device.mBluetoothClass);
                            break;
                        case AbstractionLayer.BT_PROPERTY_UUIDS:
                            int numUuids = val.length/AbstractionLayer.BT_UUID_SIZE;
                            device.mUuids = Utils.byteArrayToUuid(val);
                            /// M: ALPS01276963: Add log to confirm whether mObject lock is hold by this callback thread caused by send broadcast
                            sendUuidIntent(bdDevice);
                            debugLog("Remote UUID size is: " + numUuids);
                            ParcelUuid[] uuids = device.mUuids;
                            if (null != uuids) {
                                for (int i = 0; i < uuids.length; i++) {
                                    debugLog("Remote UUID is: uuids[" + i + "] = " + uuids[i]);
                                }
                            }
                            break;
                        case AbstractionLayer.BT_PROPERTY_TYPE_OF_DEVICE:
                            // The device type from hal layer, defined in bluetooth.h,
                            // matches the type defined in BluetoothDevice.java
                            device.mDeviceType = Utils.byteArrayToInt(val);
                            debugLog("Remote type is:" + device.mDeviceType);
                            break;
                        case AbstractionLayer.BT_PROPERTY_REMOTE_RSSI:
                            // RSSI from hal is in one byte
                            device.mRssi = val[0];
                            debugLog("Remote RSSI is:" + device.mRssi);
                            break;
                    }
                }
            }
            /// M: ALPS01276963: Add log to confirm whether mObject lock is hold by this callback thread caused by send broadcast
            debugLog("devicePropertyChangedCallback: Release Lock - RemoteDevices.mObject");
        }
    }

    void deviceFoundCallback(byte[] address) {
        // The device properties are already registered - we can send the intent
        // now
        BluetoothDevice device = getDevice(address);
        debugLog("deviceFoundCallback: Remote Address is:" + device);
        DeviceProperties deviceProp = getDeviceProperties(device);
        if (deviceProp == null) {
            errorLog("Device Properties is null for Device:" + device);
            return;
        }

        Intent intent = new Intent(BluetoothDevice.ACTION_FOUND);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothDevice.EXTRA_CLASS,
                new BluetoothClass(Integer.valueOf(deviceProp.mBluetoothClass)));
        intent.putExtra(BluetoothDevice.EXTRA_RSSI, deviceProp.mRssi);
        intent.putExtra(BluetoothDevice.EXTRA_NAME, deviceProp.mName);

        mAdapterService.sendBroadcast(intent, mAdapterService.BLUETOOTH_PERM);
    }

    void aclStateChangeCallback(int status, byte[] address, int newState) {
        BluetoothDevice device = getDevice(address);

        if (device == null) {
            errorLog("aclStateChangeCallback: Device is NULL");
            return;
        }

        DeviceProperties prop = getDeviceProperties(device);
        if (prop == null) {
            errorLog("aclStateChangeCallback reported unknown device " + Arrays.toString(address));
        }
        Intent intent = null;
        if (newState == AbstractionLayer.BT_ACL_STATE_CONNECTED) {
            intent = new Intent(BluetoothDevice.ACTION_ACL_CONNECTED);
            debugLog("aclStateChangeCallback: State:Connected to Device:" + device);
        } else {
            intent = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            debugLog("aclStateChangeCallback: State:DisConnected to Device:" + device);
        }
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mAdapterService.sendBroadcast(intent, mAdapterService.BLUETOOTH_PERM);
    }

    void deviceMasInstancesFoundCallback(int status, byte[] address, String[] name, int[] scn,
            int[] id, int[] msgtype) {
        BluetoothDevice device = getDevice(address);

        if (device == null) {
            errorLog("deviceMasInstancesFoundCallback: Device is NULL");
            return;
        }

        debugLog("deviceMasInstancesFoundCallback: found " + name.length + " instances");

        ArrayList<BluetoothMasInstance> instances = new ArrayList<BluetoothMasInstance>();

        for (int i = 0; i < name.length; i++) {
            BluetoothMasInstance inst = new BluetoothMasInstance(id[i], name[i],
                    scn[i], msgtype[i]);

            debugLog(inst.toString());

            instances.add(inst);
        }

        sendMasInstanceIntent(device, instances);
    }

    void fetchUuids(BluetoothDevice device) {
        if (mSdpTracker.contains(device)) return;
        mSdpTracker.add(device);

        Message message = mHandler.obtainMessage(MESSAGE_UUID_INTENT);
        message.obj = device;
        mHandler.sendMessageDelayed(message, UUID_INTENT_DELAY);

        //mAdapterService.getDevicePropertyNative(Utils.getBytesFromAddress(device.getAddress()), AbstractionLayer.BT_PROPERTY_UUIDS);
        mAdapterService.getRemoteServicesNative(Utils.getBytesFromAddress(device.getAddress()));
    }

    void fetchMasInstances(BluetoothDevice device) {
        if (mSdpMasTracker.contains(device)) return;
        mSdpMasTracker.add(device);

        Message message = mHandler.obtainMessage(MESSAGE_MAS_INSTANCE_INTENT);
        message.obj = device;
        mHandler.sendMessageDelayed(message, MAS_INSTANCE_INTENT_DELAY);

        mAdapterService.getRemoteMasInstancesNative(Utils.getBytesFromAddress(device.getAddress()));
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_UUID_INTENT:
                BluetoothDevice device = (BluetoothDevice)msg.obj;
                if (device != null) {
                    sendUuidIntent(device);
                }
                break;
            case MESSAGE_MAS_INSTANCE_INTENT:
                BluetoothDevice dev = (BluetoothDevice)msg.obj;
                if (dev != null) {
                    sendMasInstanceIntent(dev, null);
                }
                break;
            }
        }
    };

    private void errorLog(String msg) {
        Log.e(TAG, msg);
    }

    private void debugLog(String msg) {
        if (DBG) Log.d(TAG, msg);
    }

    private void infoLog(String msg) {
        if (DBG) Log.i(TAG, msg);
    }

    private void warnLog(String msg) {
        Log.w(TAG, msg);
    }

}
