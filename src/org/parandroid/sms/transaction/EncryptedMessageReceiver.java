package org.parandroid.sms.transaction;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import org.parandroid.encoding.Base64Coder;
import org.parandroid.encryption.MessageEncryption;
import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.ComposeMessageActivity;
import org.parandroid.sms.ui.MessageItem;
import org.parandroid.sms.ui.MessageListItem;

import com.google.android.mms.util.SqliteWrapper;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;
import android.util.Log;

public class EncryptedMessageReceiver extends BroadcastReceiver {

	private static final String TAG = "ParandroidEncryptedMessageReceiver";
	public static final int NOTIFICATIONID = 31338;

	@Override
	public void onReceive(Context context, Intent intent) {
		String uricontent = intent.getDataString();
        String port = uricontent.substring(uricontent.lastIndexOf(":") + 1);
        Log.v(TAG, "Reveiced package on port " + port);
        if(Integer.parseInt(port) != MessageEncryptionFactory.ENCRYPTED_MESSAGE_PORT) return;

		Log.v(TAG, "Received encrypted message");
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String notificationString = context.getString(R.string.received_encrypted_message);
		
		SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
		if(messages.length == 0) return;
		
		long threadId = Threads.getOrCreateThreadId(context, messages[0].getOriginatingAddress());

		Intent targetIntent = new Intent(context, ComposeMessageActivity.class);
		insertEncryptedMessage(context, messages, threadId);

		Notification n = new Notification(context, R.drawable.stat_notify_encrypted_msg, notificationString, System.currentTimeMillis(), notificationString, notificationString, targetIntent);
		mNotificationManager.notify(NOTIFICATIONID, n);
	}
	
	
	/**
	 * Insert the encrypted message into the database. Uses base64 encoding for the data.
	 * 
	 * @param messages
	 * @param threadId
	 */
	private void insertEncryptedMessage(Context context, SmsMessage[] messages, long threadId){
		SmsMessage msg = messages[0];
		ContentValues values = extractContentValues(msg);
        
		int len = 0, offset = 0;
		ArrayList<byte[]> dataParts = new ArrayList<byte[]>();
		for(SmsMessage m : messages){
			byte[] data = m.getUserData();
			len += data.length;
			dataParts.add(data);
		}
		byte[] body = new byte[len];
		for(byte[] part : dataParts){
			System.arraycopy(part, 0, body, offset, part.length);
			offset += part.length;
		}
		
        values.put(Inbox.BODY, new String(Base64Coder.encode(body)));
        values.put(Sms.THREAD_ID, threadId);
        values.put(Inbox.TYPE, MessageItem.MESSAGE_TYPE_PARANDROID_INBOX);
        
        ContentResolver resolver = context.getContentResolver();
        SqliteWrapper.insert(context, resolver, Inbox.CONTENT_URI, values);

// Jeffrey: TODO: cupcake backporting: it looks like 1.5 does not delete old 
// messages when they are received, to I commented these lines. @see SmsReceiverService::insertMessage()
//        threadId = values.getAsLong(Sms.THREAD_ID);
//        Recycler.getSmsRecycler().deleteOldMessagesByThreadId(context, threadId);
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
