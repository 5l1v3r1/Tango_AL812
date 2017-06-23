/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.htmlviewer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;

import java.util.zip.GZIPInputStream;

/**
 * Simple activity that shows the requested HTML page. This utility is
 * purposefully very limited in what it supports, including no network or
 * JavaScript.
 */
public class HTMLViewerActivity extends Activity {
    private static final String TAG = "HTMLViewer";

    private WebView mWebView;
    private View mLoading;
    /// M: add auto-detector for HTML viewer
    private static final String ENCODING_AUTO_DETECTED = "auto-detector";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final Intent intent = getIntent();
        if (intent.hasExtra(Intent.EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(Intent.EXTRA_TITLE));
        }
        
        String url = String.valueOf(intent.getData());
        //HQ_zhangteng added for HQ01393812 at 2015-09-23 begin
	if(url!=null && (url.endsWith(".htm")||url.endsWith(".html") ||url.endsWith(".txt") ||url.contains("downloads/all_downloads")||url.contains("downloads/my_downloads")||url.endsWith(".ini")||url.endsWith(".log"))){//modidy by wangmingyue for HQ01400494
		mWebView = (WebView) findViewById(R.id.webview);
        	mLoading = findViewById(R.id.loading);

        	mWebView.setWebChromeClient(new ChromeClient());
        	mWebView.setWebViewClient(new ViewClient());

        	WebSettings s = mWebView.getSettings();
        	s.setUseWideViewPort(true);
        	s.setSupportZoom(true);
        	s.setBuiltInZoomControls(true);
        	s.setDisplayZoomControls(false);
        	s.setSavePassword(false);
        	s.setSaveFormData(false);
        	s.setBlockNetworkLoads(true);

        	// Javascript is purposely disabled, so that nothing can be
        	// automatically run.
        	s.setJavaScriptEnabled(false);
        	/// M: add auto-detector for HTML viewer
        	//s.setDefaultTextEncodingName("utf-8");
        	s.setDefaultTextEncodingName(ENCODING_AUTO_DETECTED);
        	mWebView.loadUrl(String.valueOf(intent.getData()));
		return;
	}
	 //HQ_zhangteng added for HQ01393812 at 2015-09-23 end
        
        if(! url.equals("file:///system/etc/NOTICE.html.gz")){
        	
        	mLoading = findViewById(R.id.loading);
            TextView webText = (TextView) findViewById(R.id.web_text);
            Log.d(TAG, "intent =  " + intent);
            if (intent.hasExtra(Intent.EXTRA_TITLE)) {
                setTitle(intent.getStringExtra(Intent.EXTRA_TITLE));
            }
            //Get Huawei privacy policy file URL.  
            Log.d(TAG, "The url is " + url);
            
            
            
            if (TextUtils.isEmpty(url)) {
                Log.e(TAG, "The huawei privacy policy file is empty.");
                finish();
                return;
            }
                if(getResources().getConfiguration().locale.getCountry().equals("CN")){
                	url = url.replace("huawei_privacy_policy","huawei_privacy_policy_zh_cn");
                	url = url.replace("huawei_copyright","huawei_copyright_zh_cn");
                }
                if(getResources().getConfiguration().locale.getCountry().equals("TW")){
                	url = url.replace("huawei_privacy_policy","huawei_privacy_policy_tw");
                	url = url.replace("huawei_copyright","huawei_copyright_tw");
                }
                if(getResources().getConfiguration().locale.getCountry().equals("HK")){
                	url = url.replace("huawei_privacy_policy","huawei_privacy_policy_hk");
                	url = url.replace("huawei_copyright","huawei_copyright_hk");
                }     
            String webString = getStringFromHtmlFile(url);
            Log.e(TAG, "webString:" + webString);
            
            Log.e(TAG, "Html.fromHtml(webString) = " + Html.fromHtml(webString));
            
            webText.setText(Html.fromHtml(webString));
            webText.setMovementMethod(LinkMovementMethod.getInstance());
            mLoading.setVisibility(View.GONE);
        	
        	
        }else
        {
        	mWebView = (WebView) findViewById(R.id.webview);
        	mLoading = findViewById(R.id.loading);

        	mWebView.setWebChromeClient(new ChromeClient());
        	mWebView.setWebViewClient(new ViewClient());

        	WebSettings s = mWebView.getSettings();
        	s.setUseWideViewPort(true);
        	s.setSupportZoom(true);
        	s.setBuiltInZoomControls(true);
        	s.setDisplayZoomControls(false);
        	s.setSavePassword(false);
        	s.setSaveFormData(false);
        	s.setBlockNetworkLoads(true);

        	// Javascript is purposely disabled, so that nothing can be
        	// automatically run.
        	s.setJavaScriptEnabled(false);
        	/// M: add auto-detector for HTML viewer
        	//s.setDefaultTextEncodingName("utf-8");
        	s.setDefaultTextEncodingName(ENCODING_AUTO_DETECTED);
        	mWebView.loadUrl(String.valueOf(intent.getData()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mWebView !=null){
        	mWebView.destroy();
        }
    }

    private class ChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (!getIntent().hasExtra(Intent.EXTRA_TITLE)) {
                HTMLViewerActivity.this.setTitle(title);
            }
        }
    }

    private class ViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            mLoading.setVisibility(View.GONE);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())
                    && uri.getPath().endsWith(".gz")) {
                Log.d(TAG, "Trying to decompress " + uri + " on the fly");
                //hanchao add for china language begin
                if(getResources().getConfiguration().locale.getCountry().equals("CN")){
                	String uriStr = uri.toString();
                	uriStr = uriStr.replace("huawei_privacy_policy","huawei_privacy_policy_zh_cn");
                	uriStr = uriStr.replace("huawei_copyright","huawei_copyright_zh_cn");
                	uri = Uri.parse(uriStr);
                }
                if(getResources().getConfiguration().locale.getCountry().equals("TW")){
                	String uriStr = uri.toString();
                	uriStr = uriStr.replace("huawei_privacy_policy","huawei_privacy_policy_tw");
                	uriStr = uriStr.replace("huawei_copyright","huawei_copyright_tw");
                	uri = Uri.parse(uriStr);
                }
                if(getResources().getConfiguration().locale.getCountry().equals("HK")){
                	String uriStr = uri.toString();
                	uriStr = uriStr.replace("huawei_privacy_policy","huawei_privacy_policy_hk");
                	uriStr = uriStr.replace("huawei_copyright","huawei_copyright_hk");
                	uri = Uri.parse(uriStr);
                }
              //hanchao add for china language end
                Log.d(TAG, "Trying to decompress " + uri + " on the fly");
                try {
                    final InputStream in = new GZIPInputStream(
                            getContentResolver().openInputStream(uri));
                    final WebResourceResponse resp = new WebResourceResponse(
                            getIntent().getType(), "utf-8", in);
                    resp.setStatusCodeAndReasonPhrase(200, "OK");
                    return resp;
                } catch (IOException e) {
                    Log.w(TAG, "Failed to decompress; falling back", e);
                }
            }
            return null;
        }
    }
    
	 public String getStringFromHtmlFile (String filePath) {
	        String result = "";
	        if (null == filePath) {
	            Log.e(TAG, "getStringFromHtmlFile null == context || null == filePath");
	            return result;
	        }

	        InputStream stream = null;
	        BufferedReader reader = null;
	        InputStreamReader streamReader = null;
	        try {
	            // Read html file into buffer
	        	stream = new GZIPInputStream(
                     getContentResolver().openInputStream(Uri.parse(filePath)));
		//add by qiyanlong
		        BufferedInputStream in = new BufferedInputStream(stream);
		        in.mark(4);
			byte[] first3bytes = new byte[3];
		        in.read(first3bytes);
		        in.reset();
		        if (first3bytes[0] == (byte) 0xEF && first3bytes[1] == (byte) 0xBB
							&& first3bytes[2] == (byte) 0xBF) {// utf-8

				reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
						
			} else if (first3bytes[0] == (byte) 0xFF
	    			&& first3bytes[1] == (byte) 0xFE) {

				reader = new BufferedReader(new InputStreamReader(in, "unicode"));
			} else if (first3bytes[0] == (byte) 0xFE
				&& first3bytes[1] == (byte) 0xFF) {

				reader = new BufferedReader(new InputStreamReader(in,"utf-16be"));
			} else if (first3bytes[0] == (byte) 0xFF
				&& first3bytes[1] == (byte) 0xFF) {

				reader = new BufferedReader(new InputStreamReader(in,"utf-16le"));
			} else {
				reader = new BufferedReader(new InputStreamReader(in, "GBK"));
			}
		//add end
	            //streamReader = new InputStreamReader(stream, "utf-8");
	            //reader = new BufferedReader(streamReader);
	            StringBuilder builder = new StringBuilder();
	            String line = null;

	            boolean readCurrentLine = true;
	            // Read each line of the html file, and build a string.
	            while ((line = reader.readLine()) != null) {
	                // Don't read the Head tags when CSS styling is not supporeted. 
	                if (line.contains("<style")) {
	                    readCurrentLine = false;
	                } else if (line.contains("</style")) {
	                    readCurrentLine = true;
	                }
	                if (readCurrentLine) {
	                    builder.append(line).append("\n");
	                }
	            }
	            result = builder.toString();
	        } catch (FileNotFoundException ex) {
	            Log.e(TAG, "FileNotFoundException:" + ex.getMessage());
	            Log.e(TAG, ex.getMessage());
	        } catch (Exception ex) {
	            Log.e(TAG, "FoundException:" + ex.getMessage());
	            //Log.e(TAG, ex.getMessage());//HQ_pangxuhui 20150228 delete for bug:HQ01411632
	        } finally {
	            if (null != reader) {
	                try {
	                    reader.close();
	                } catch (IOException ex) {
	                    Log.e(TAG, ex.getMessage());
	                }
	            }
	            if (null != streamReader) {
	                try {
	                    streamReader.close();
	                } catch (IOException ex) {
	                    Log.e(TAG, ex.getMessage());
	                }
	            }
	            if (null != stream) {
	                try {
	                    stream.close();
	                } catch (IOException ex) {
	                    Log.e(TAG, ex.getMessage());
	                }
	            }
	        }
	        return result;
	    }
    
}
