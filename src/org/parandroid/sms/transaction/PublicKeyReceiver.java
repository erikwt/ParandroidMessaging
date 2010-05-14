package org.parandroid.sms.transaction;

import java.util.ArrayList;
import java.util.HashMap;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.ManagePublicKeysActivity;
import org.parandroid.sms.ui.MessagingPreferenceActivity;

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
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

public class PublicKeyReceiver extends BroadcastReceiver {

	private static final String TAG = "PublicKeyReceiver";
	
	private static HashMap<String, HashMap<Integer, HashMap<Integer, byte[]>>> messageMap = new HashMap<String, HashMap<Integer, HashMap<Integer, byte[]>>>();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String uricontent = intent.getDataString();
        String port = uricontent.substring(uricontent.lastIndexOf(":") + 1);
        Log.v(TAG, "Reveiced package on port " + port);
        if(Integer.parseInt(port) != MessageEncryptionFactory.PUBLIC_KEY_PORT) return;
        
		
		SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
		if(messages.length == 0) return;
		SmsMessage message = messages[0];
        String sender = message.getOriginatingAddress();
                
        byte[] data = message.getUserData();
        if(data.length < 3){
        	Log.e(TAG, "Encrypted message too short: '" + data + "'");
        	return;
        }
        
        int protocolVersion = data[0];
        int currentMessage = data[1] >> 4;
        int totalMessages = data[1] & 15;
        int messageId = data[2];
        
        Log.i(TAG, "Message header:\nProtocol version: " + protocolVersion + " (" + (Integer.toBinaryString(protocolVersion)) + ")");
        Log.i(TAG, "Message count: " + currentMessage + " (" + (Integer.toBinaryString(currentMessage)) + ")");
        Log.i(TAG, "Total messages: " + totalMessages + " (" + (Integer.toBinaryString(totalMessages)) + ")");
        Log.i(TAG, "Message ID: " + messageId + " (" + (Integer.toBinaryString(messageId)) + ")");

        HashMap<Integer, HashMap<Integer, byte[]>> messageStore;
        if(messageMap.containsKey(sender)) messageStore = messageMap.get(sender);
        else{
            messageStore = new HashMap<Integer, HashMap<Integer, byte[]>>();
            messageMap.put(sender, messageStore);
        }

        if(!messageStore.containsKey(messageId)){
        	HashMap<Integer, byte[]> messageParts = new HashMap<Integer, byte[]>();
        	messageStore.put(messageId, messageParts);
        }
        
        HashMap<Integer, byte[]> messageParts = messageStore.get(messageId);
        
        byte[] part = new byte[data.length - 3];
        System.arraycopy(data, 3, part, 0, part.length);
        
        messageParts.put(currentMessage, part);
        
        if(messageParts.size() == totalMessages){
            Log.v(TAG, "Received all parts of message: " + messageId);
        	handleMultipartDataMessage(context, sender, messages, messageId);
        	messageStore.remove(messageId);
        }
	}

	private void handleMultipartDataMessage(Context context, String sender, SmsMessage[] messages, int messageId) {		
		HashMap<Integer, byte[]> messageParts = messageMap.get(sender).get(messageId);
		
		int len = 0, offset = 0;
		ArrayList<byte[]> dataParts = new ArrayList<byte[]>();
		
		for(int i = messageParts.size() - 1; i >= 0; i--){
			byte[] part = messageParts.get(i);
			len += part.length;
			dataParts.add(part);
		}
		
		byte[] body = new byte[len];
		for(byte[] part : dataParts){
			System.arraycopy(part, 0, body, offset, part.length);
			offset += part.length;
		}
		
		SQLiteDatabase keyRing = MessageEncryptionFactory.openKeyring(context);
		
		ContentValues cv = new ContentValues();
		cv.put("number", sender);
		cv.put("publickey", new String(body));
		cv.put("accepted", false);
		
		long id = keyRing.insert(MessageEncryptionFactory.PUBLIC_KEY_TABLE, "", cv);
		keyRing.close();
		
		Log.i(TAG, "Inserted public key at id: " + id);
		
		Intent targetIntent = new Intent(context, ManagePublicKeysActivity.class);
		targetIntent.putExtra("id", id);
		
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String notificationMessage = context.getString(R.string.received_public_key);
		
		Notification n = new Notification(context, R.drawable.stat_notify_public_key_recieved, notificationMessage, System.currentTimeMillis(), notificationMessage, notificationMessage, targetIntent);
		
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
