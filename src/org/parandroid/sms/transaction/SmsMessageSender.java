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

package org.parandroid.sms.transaction;

import org.parandroid.encoding.Base64Coder;
import org.parandroid.encryption.MessageEncryption;
import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.MmsConfig;
import org.parandroid.sms.LogTag;
import org.parandroid.sms.ParandroidSmsApp;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.MessageItem;
import org.parandroid.sms.ui.MessagingPreferenceActivity;
import org.parandroid.sms.ui.MessageUtils;
import com.google.android.mms.MmsException;
import com.google.android.mms.util.SqliteWrapper;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms.Outbox;
import android.provider.Telephony.Sms.Inbox;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

public class SmsMessageSender implements MessageSender {
    private final Context mContext;
    private final int mNumberOfDests;
    private final String[] mDests;
    private final String mMessageText;
    private final String mServiceCenter;
    private final long mThreadId;
    private long mTimestamp;
    private boolean mTryToEncrypt;

    private static final String TAG = "ParandroidSmsMessageSender";
    
    // Default preference values
    private static final boolean DEFAULT_DELIVERY_REPORT_MODE  = false;

    private static final String[] SERVICE_CENTER_PROJECTION = new String[] {
        Sms.Conversations.REPLY_PATH_PRESENT,
        Sms.Conversations.SERVICE_CENTER,
    };

    private static final int COLUMN_REPLY_PATH_PRESENT = 0;
    private static final int COLUMN_SERVICE_CENTER     = 1;
    
    public SmsMessageSender(Context context, String[] dests, String msgText, long threadId, boolean tryToEncrypt) {
        mContext = context;
        mMessageText = msgText;
        mNumberOfDests = dests.length;
        mDests = new String[mNumberOfDests];
        System.arraycopy(dests, 0, mDests, 0, mNumberOfDests);
        mTimestamp = System.currentTimeMillis();
        mThreadId = threadId;
        mServiceCenter = getOutgoingServiceCenter(mThreadId);
        mTryToEncrypt = tryToEncrypt;
    }

    public boolean sendMessage(long token) throws MmsException {
        if ((mMessageText == null) || (mNumberOfDests == 0)) {
            // Don't try to send an empty message.
            throw new MmsException("Null message body or dest.");
        }

        SmsManager smsManager = SmsManager.getDefault();

        for (int i = 0; i < mNumberOfDests; i++) {
        	boolean isEncrypted = false;
        	
        	byte[] encryptedMessage = null;
        	if(mTryToEncrypt && MessageEncryptionFactory.hasPublicKey(mContext, mDests[i])){
        		try {
					encryptedMessage = MessageEncryption.encrypt(mContext, mDests[i], mMessageText);
					isEncrypted = true;
				} catch (Exception e) {
					Log.e(TAG, "Error while encrypting message");
					e.printStackTrace();
				}
        	}
        	
            ArrayList<String> messages = null;
            if ((MmsConfig.getEmailGateway() != null) &&
                    (Mms.isEmailAddress(mDests[i]) || MessageUtils.isAlias(mDests[i]))) {
                String msgText;
                msgText = mDests[i] + " " + mMessageText;
                mDests[i] = MmsConfig.getEmailGateway();
                messages = smsManager.divideMessage(msgText);
            } else {
               messages = smsManager.divideMessage(mMessageText);
            }
            int messageCount = messages.size();

            if (messageCount == 0) {
                // Don't try to send an empty message.
                throw new MmsException("SmsMessageSender.sendMessage: divideMessage returned " +
                        "empty messages. Original message is \"" + mMessageText + "\"");
            }

            ArrayList<PendingIntent> deliveryIntents =
                    new ArrayList<PendingIntent>(messageCount);
            ArrayList<PendingIntent> sentIntents =
                    new ArrayList<PendingIntent>(messageCount);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean requestDeliveryReport = prefs.getBoolean(
                    MessagingPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                    DEFAULT_DELIVERY_REPORT_MODE);
            Uri uri = null;
            try {
            	if(isEncrypted){
            		String outboxText = new String(Base64Coder.encode(encryptedMessage));
            		//TODO: remove this statement
            		Log.i(TAG, "Trying to insert encrypted message into db");
            		addToParandroidOutbox(i, outboxText);
            		
            	} else {
            		uri = Sms.Outbox.addMessage(mContext.getContentResolver(), mDests[i],
            			mMessageText, null, mTimestamp, requestDeliveryReport, mThreadId);
            	}
            } catch (SQLiteException e) {
            	// TODO: remove this statement
            	Log.e(TAG, "SQLiteException while inserting into outbox");
                SqliteWrapper.checkSQLiteException(mContext, e);
            }

            for (int j = 0; j < messageCount; j++) {
                if (requestDeliveryReport) {
                    // TODO: Fix: It should not be necessary to
                    // specify the class in this intent.  Doing that
                    // unnecessarily limits customizability.
                    deliveryIntents.add(PendingIntent.getBroadcast(
                            mContext, 0,
                            new Intent(
                                    MessageStatusReceiver.MESSAGE_STATUS_RECEIVED_ACTION,
                                    uri,
                                    mContext,
                                    MessageStatusReceiver.class),
                            0));
                }
                sentIntents.add(PendingIntent.getBroadcast(
                        mContext, 0,
                        new Intent(SmsReceiverService.MESSAGE_SENT_ACTION,
                                uri,
                                mContext,
                                SmsReceiver.class),
                        0));
            }

            if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
                log("sendMessage: address[" + i + "]=" + mDests[i] + ", threadId=" + mThreadId +
                        ", uri=" + uri + ", msgs.count=" + messageCount);
            }

            if(!isEncrypted){
            	// we send the msg unencrypted if we don't have the public key, or when the number of
            	// messages is larger than 1 (there is no support to send multipart datamessages yet)
            	try {
	                smsManager.sendMultipartTextMessage(
	                        mDests[i], mServiceCenter, messages, sentIntents,
	                        deliveryIntents);
            	} catch (Exception ex) {
                    throw new MmsException("SmsMessageSender.sendMessage: caught " + ex +
                            " from SmsManager.sendMultipartTextMessage()");
                }
        	} else {
        		try {
            		PendingIntent sentIntent = sentIntents.isEmpty() ? null : sentIntents.get(0);
            		PendingIntent deliveryIntent = deliveryIntents.isEmpty() ? null : deliveryIntents.get(0);

            		// TODO: remove comment
            		// smsManager.sendDataMessage(mDests[i], null, MessageEncryptionFactory.ENCRYPTED_MESSAGE_PORT, encryptedMessage, sentIntent, deliveryIntent);
        		} catch (Exception ex) {
        			Log.e(TAG, ex.getMessage());
        			
                    throw new MmsException("SmsMessageSender.sendMessage (encrypted): caught " + ex +
                    " from SmsManager.sendDataMessage()");
        		}
        	}
            
        }

        return false;
    }
    
    private Uri addToParandroidOutbox(int destIndex, String outboxText){
    	ContentValues values = new ContentValues(7);

        values.put(Telephony.TextBasedSmsColumns.ADDRESS, mDests[destIndex]);
        values.put(Telephony.TextBasedSmsColumns.DATE, mTimestamp);

        values.put(Telephony.TextBasedSmsColumns.READ, Integer.valueOf(1));
        values.put(Telephony.TextBasedSmsColumns.BODY, outboxText);
        
        // currently we do not support delivery reports
        values.put(Telephony.TextBasedSmsColumns.STATUS, Telephony.TextBasedSmsColumns.STATUS_NONE);
        
        if (mThreadId != -1L) {
            values.put(Telephony.TextBasedSmsColumns.THREAD_ID, mThreadId);
        }
        
        values.put(Inbox.TYPE, MessageItem.MESSAGE_TYPE_PARANDROID_OUTBOX);
        
        return mContext.getContentResolver().insert(Telephony.Sms.Outbox.CONTENT_URI, values);
    }

    /**
     * Get the service center to use for a reply.
     *
     * The rule from TS 23.040 D.6 is that we send reply messages to
     * the service center of the message to which we're replying, but
     * only if we haven't already replied to that message and only if
     * <code>TP-Reply-Path</code> was set in that message.
     *
     * Therefore, return the service center from the most recent
     * message in the conversation, but only if it is a message from
     * the other party, and only if <code>TP-Reply-Path</code> is set.
     * Otherwise, return null.
     */
    private String getOutgoingServiceCenter(long threadId) {
        Cursor cursor = null;

        try {
            cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                            Sms.CONTENT_URI, SERVICE_CENTER_PROJECTION,
                            "thread_id = " + threadId, null, "date DESC");

            if ((cursor == null) || !cursor.moveToFirst()) {
                return null;
            }

            boolean replyPathPresent = (1 == cursor.getInt(COLUMN_REPLY_PATH_PRESENT));
            return replyPathPresent ? cursor.getString(COLUMN_SERVICE_CENTER) : null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void log(String msg) {
        Log.d(LogTag.TAG, "[SmsMsgSender] " + msg);
    }
}
