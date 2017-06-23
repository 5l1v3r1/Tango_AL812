/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.android.internal.util.XmlUtils;


// MVNO-API START
import java.util.ArrayList;
import java.lang.Integer;
// MVNO-API END

public class SevenConfigOverride {
    private static HashMap<String, MmsSevenConfigBean> enableSevenBitMaps;
    private static SevenConfigOverride sInstance;
    static final Object sInstSync = new Object();
    static final String LOG_TAG = "SevenConfigOverride";
    static final String PARTNER_SEVENBIT_OVERRIDE_PATH ="etc/globalAutoAdapt-conf.xml";
    static final String PARTNER_VIRTUAL_ECC_OVERRIDE_PATH ="etc/virtualNets-conf.xml";

    public class MmsSevenConfigBean{
    	public String mcc;
    	public String mnc;
        public String mMccMnc;
    	public String sevenBitConfig;
        public String sevenBitCountry;
    	MmsSevenConfigBean(){
    	}
    }

    public static SevenConfigOverride getInstance() {
        synchronized (sInstSync) {
            if (sInstance == null) {
                sInstance = new SevenConfigOverride();
            }
        }
        return sInstance;
    }

    private SevenConfigOverride() {
        enableSevenBitMaps = new HashMap<String, MmsSevenConfigBean>();
        loadSevenConfigOverrides();
        loadVirtualSevenConfigOverrides();
    }

    public boolean containsMccmncForSevenBit(String mccmnc) {
        return enableSevenBitMaps.containsKey(mccmnc);
    }

    public MmsSevenConfigBean getSevenBitWithSim(String mccmnc) {
        return enableSevenBitMaps.get(mccmnc);
    }

    private void loadSevenConfigOverrides() {
        FileReader mReader;
        Log.d(LOG_TAG, "loadSevenConfigOverrides");
        final File mFile = new File(Environment.getRootDirectory(),
                PARTNER_SEVENBIT_OVERRIDE_PATH);

        try {
            mReader = new FileReader(mFile);
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "Can't open " +
                    Environment.getRootDirectory() + "/" + PARTNER_SEVENBIT_OVERRIDE_PATH);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(mReader);

            XmlUtils.beginDocument(parser, "Datas");

            while (true) {
                XmlUtils.nextElement(parser);

                String name = parser.getName();
                if (!"numMatch".equals(name)) {
                    break;
                }
                MmsSevenConfigBean bean = new MmsSevenConfigBean();
                bean.mcc = parser.getAttributeValue(null, "mcc");
		bean.mnc = parser.getAttributeValue(null, "mnc");
                bean.mMccMnc = bean.mcc + bean.mnc;
                bean.sevenBitConfig  = parser.getAttributeValue(null, "sms_7bit_enabled");
                bean.sevenBitCountry  = parser.getAttributeValue(null, "sms_coding_national");
                String keyword = bean.mMccMnc;
		Log.i(LOG_TAG, "mncmcc" + keyword);
                enableSevenBitMaps.put(keyword, bean);
            }
                mReader.close();
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "Exception in spn-conf parser " + e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Exception in spn-conf parser " + e);
        }
    }

    private void loadVirtualSevenConfigOverrides() {
			FileReader eccReader;
			Log.d(LOG_TAG, "loadVirtualSevenConfigOverrides");
			Log.w(LOG_TAG, "open file:	" + Environment.getRootDirectory() + "/" + PARTNER_VIRTUAL_ECC_OVERRIDE_PATH);
			final File eccFile = new File(Environment.getRootDirectory(), PARTNER_VIRTUAL_ECC_OVERRIDE_PATH);
	
			try {
				eccReader = new FileReader(eccFile);
			} catch (FileNotFoundException e) {
				Log.w(LOG_TAG, "Can't open " + Environment.getRootDirectory() + "/" + PARTNER_VIRTUAL_ECC_OVERRIDE_PATH);
				return;
			}
	
			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(eccReader);
	
				XmlUtils.beginDocument(parser, "virtualNets");
	
				while (true) {
					XmlUtils.nextElement(parser);
					String name = parser.getName();
					Log.w(LOG_TAG, "name = "+name);
					if (name==null || !"virtualNet".equals(name)) {
						break;
					}
					MmsSevenConfigBean bean = new MmsSevenConfigBean();
					String mccmnc = parser.getAttributeValue(null, "numeric");
					bean.sevenBitConfig  = parser.getAttributeValue(null, "sms_7bit_enabled");
                                        bean.sevenBitCountry  = parser.getAttributeValue(null, "sms_coding_national");
					//sms_7bit_enabled sms_coding_national
                                        String keyword = mccmnc;
		                        Log.i(LOG_TAG, "mncmcc" + keyword);
                                        enableSevenBitMaps.put(keyword, bean);
				    }
				eccReader.close();
				Log.w(LOG_TAG, "loadVirtualEccOverrideByEfSpn.close()");
			} catch (XmlPullParserException e) {
				Log.w(LOG_TAG, "Exception in global-specialecc-conf parser " + e);
			} catch (IOException e) {
				Log.w(LOG_TAG, "Exception in global-specialEcc-conf parser " + e);
			}
		}


}

