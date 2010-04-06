package org.parandroid.sms.transaction;

import org.parandroid.encoding.Base64Coder;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.ComposeMessageActivity;
import org.parandroid.sms.ui.PublicKeyReceived;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

public class EncryptedMessageReceiver extends BroadcastReceiver {

	private static final String TAG = "ParandroidEncryptedMessageReceiver";
	public static final int NOTIFICATIONID = 31338;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "Received encrypted message");
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String notificationString = context.getString(R.string.received_encrypted_message);
		
		SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
		if(messages.length == 0) return;
		
		long threadId = Threads.getOrCreateThreadId(context, messages[0].getOriginatingAddress());
		Intent targetIntent = new Intent(context, ComposeMessageActivity.class);
		insertEncryptedMessage(messages, threadId);
				
		Notification n = new Notification(context, R.drawable.stat_notify_encrypted_msg, notificationString, System.currentTimeMillis(), notificationString, notificationString, targetIntent);
		mNotificationManager.notify(NOTIFICATIONID, n);
	}
	
	
	/**
	 * Insert the encrypted message into the database. Uses base64 encoding for the data.
	 * 
	 * @param messages
	 * @param threadId
	 */
	private void insertEncryptedMessage(SmsMessage[] messages, long threadId){
		SmsMessage msg = messages[0];
		ContentValues values = extractContentValues(msg);
        
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            body.append(Base64Coder.encode(messages[i].getUserData()));
        }
        values.put(Inbox.BODY, body.toString());
        
        values.put(Sms.THREAD_ID, threadId);
        
        // TODO: Actually insert in some database
        Log.v(TAG, "Encrypted message:");
        Log.v(TAG, "From: " + values.getAsString(Inbox.ADDRESS));
        Log.v(TAG, values.getAsString(Inbox.BODY));
	}
	
	
    /**
     * Extract all the content values except the body from an SMS
     * message.
     * 
     * @see SmsReceiverService.extractContentValues (copy)
     */
    private ContentValues extractContentValues(SmsMessage sms) {
        ContentValues values = new ContentValues();
        values.put(Inbox.ADDRESS, sms.getDisplayOriginatingAddress());
        values.put(Inbox.DATE, new Long(System.currentTimeMillis()));
        values.put(Inbox.PROTOCOL, sms.getProtocolIdentifier());
        values.put(Inbox.READ, Integer.valueOf(0));
        if (sms.getPseudoSubject().length() > 0) {
            values.put(Inbox.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Inbox.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Inbox.SERVICE_CENTER, sms.getServiceCenterAddress());
        return values;
    }
}
