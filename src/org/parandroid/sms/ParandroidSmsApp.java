/*
 * Copyright (C) 2008 Esmertec AG.
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

package org.parandroid.sms;


import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.drm.DrmUtils;
import org.parandroid.sms.layout.LayoutManager;
import org.parandroid.sms.ui.ComposeMessageActivity;
import org.parandroid.sms.ui.ConversationList;
import org.parandroid.sms.ui.AuthenticateActivity;
import org.parandroid.sms.util.ContactInfoCache;
import org.parandroid.sms.util.DownloadManager;
import org.parandroid.sms.util.DraftCache;
import org.parandroid.sms.util.SmileyParser;
import org.parandroid.sms.util.RateController;
import org.parandroid.sms.MmsConfig;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

public class ParandroidSmsApp extends Application {
    public static final String TAG = "ParandroidSms";
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Load the default preference values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        MmsConfig.init(this);
        ContactInfoCache.init(this);
        DraftCache.init(this);
        DownloadManager.init(this);
        RateController.init(this);
        DrmUtils.cleanupStorage(this);
        LayoutManager.init(this);
        SmileyParser.init(this);
        
        registerReceiver(mScreenOffReceiver, mScreenOffFilter);
    }

    @Override
    public void onTerminate() {
        DrmUtils.cleanupStorage(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LayoutManager.getInstance().onConfigurationChanged(newConfig);
    }
    
    private final IntentFilter mScreenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    
    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            	boolean forgetPasswordOnSleep = prefs.getBoolean("pref_key_forget_on_sleep", true);
            	boolean closeOnSleep = prefs.getBoolean("pref_key_close_on_sleep", true);
            	
                if(closeOnSleep && ComposeMessageActivity.onForeground && MessageEncryptionFactory.isAuthenticated()){
                	ComposeMessageActivity.onForeground = false;
                	
                	// Escape from running conversation
	                Intent targetIntent = new Intent(context, ConversationList.class);
	                targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                getApplicationContext().startActivity(targetIntent);
                }
                
                if(forgetPasswordOnSleep){
                	MessageEncryptionFactory.forgetPassword();
                }
            }
        }
    };
    
}
