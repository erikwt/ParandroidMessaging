package org.parandroid.sms.transaction;

import java.util.HashMap;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.EncryptedMessageNotificationActivity;
import org.parandroid.sms.ui.ManagePublicKeysActivity;

import org.parandroid.sms.ui.MessageUtils;
import org.parandroid.sms.ui.MessagingPreferenceActivity;
import org.parandroid.sms.ui.MessageListItem;

import com.google.android.mms.util.SqliteWrapper;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Threads;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

public class EncryptedMessageReceiver extends BroadcastReceiver {

	private static final String TAG = "PD EncMsgRec";

	private static HashMap<String, HashMap<Integer, HashMap<Integer, byte[]>>> messageMap = new HashMap<String, HashMap<Integer, HashMap<Integer, byte[]>>>();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		SmsMessage[] messages = Intents.getMessagesFromIntent(intent);        
        SmsMessage message = messages[0];
        if(message.getMessageBody().startsWith(MultipartDataMessage.MESSAGE_HEADER)){
        	handleMessage(context, messages);
        }else if(message.getMessageBody().startsWith(MultipartDataMessage.PUBLIC_KEY_HEADER)){
        	handlePublicKey(context, messages);
        }else{
        	Log.i(TAG, "Got message, but not with a Parandroid header. Skipping.");
        	return;
        }
	}
	
	
	public void handleMessage(Context context, SmsMessage[] messages){
		SmsMessage message = messages[0];
		String sender = message.getOriginatingAddress();
		
		String body = message.getMessageBody();
		for(int i = 1; i < messages.length; i++)
			body += messages[i].getMessageBody();

		int protocolVersion = MessageEncryptionFactory.getProcolVersion(body);
        
        Log.i(TAG, "Received message with protocol version: " + protocolVersion);
        
        String notificationString = null;
        String notificationTitle = context.getString(R.string.received_encrypted_message);
        if(protocolVersion == -1){
        	notificationString = context.getString(R.string.corrupted_message);
		}else if(protocolVersion > MultipartDataMessage.PROTOCOL_VERSION){
        	// Protocol version too high, user might need to update
        	notificationString = context.getString(R.string.protocol_too_high);
        }else{
        	notificationString = notificationTitle;
        }
        
        
        Log.v(TAG, "Received encrypted message");
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		long threadId = Threads.getOrCreateThreadId(context, sender);
		
		int notificationId = MessageUtils.getNotificationId(sender);

		Intent targetIntent = new Intent(context, EncryptedMessageNotificationActivity.class);
		targetIntent.putExtra("notificationId", notificationId);
		targetIntent.putExtra("threadId", threadId);
		
		Notification n = new Notification(context, R.drawable.stat_notify_encrypted_msg, notificationString, System.currentTimeMillis(), notificationTitle, notificationString, targetIntent);
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		if (sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true)) {
			if(sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_VIBRATE, true))
				n.defaults |= Notification.DEFAULT_VIBRATE;
			
			String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
			n.sound = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
			
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
	        n.ledARGB = 0xff00ff00;
	        n.ledOnMS = 500;
	        n.ledOffMS = 2000;
        }
		
		mNotificationManager.notify(notificationId, n);
	}
	
	public void handlePublicKey(Context context, SmsMessage[] messages){
		SmsMessage message = messages[0];
		String sender = message.getOriginatingAddress();
		
		String body = message.getMessageBody();
		for(int i = 1; i < messages.length; i++)
			body += messages[i].getMessageBody();
		
		int protocolVersion = MessageEncryptionFactory.getProcolVersion(body);
        
        Log.i(TAG, "Received message with protocol version: " + protocolVersion);
        
        String notificationString = null;
        String notificationTitle = context.getString(R.string.received_encrypted_message);
        if(protocolVersion == -1){
        	notificationString = context.getString(R.string.corrupted_message);
		}else if(protocolVersion > MultipartDataMessage.PROTOCOL_VERSION){
        	// Protocol version too high, user might need to update
        	notificationString = context.getString(R.string.protocol_too_high);
        }else{
        	notificationString = notificationTitle;
        }
		
        String publicKey = MessageEncryptionFactory.stripHeader(body);
        Log.i(TAG, "Inserting public key (length: " + publicKey.length() + "): " + publicKey);
        
		SQLiteDatabase keyRing = MessageEncryptionFactory.openKeyring(context);
		
		ContentValues cv = new ContentValues();
		cv.put("number", sender);
		cv.put("publickey", publicKey);
		cv.put("accepted", false);
		
		long id = keyRing.insert(MessageEncryptionFactory.PUBLIC_KEY_TABLE, "", cv);
		keyRing.close();
		
		Log.i(TAG, "Inserted public key at id: " + id);
		
		Intent targetIntent = new Intent(context, ManagePublicKeysActivity.class);
		targetIntent.putExtra("id", id);
		
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Notification n = new Notification(context, R.drawable.stat_notify_public_key_recieved, notificationString, System.currentTimeMillis(), notificationTitle, notificationString, targetIntent);
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		if (sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true)) {
			if(sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_VIBRATE, true))
				n.defaults |= Notification.DEFAULT_VIBRATE;
			
			String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
			n.sound = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
			
			n.flags |= Notification.FLAG_SHOW_LIGHTS;
	        n.ledARGB = 0xff00ff00;
	        n.ledOnMS = 500;
	        n.ledOffMS = 2000;
        }
		
		mNotificationManager.notify((int) id, n);
	}
}
