package org.parandroid.sms.transaction;

import java.util.ArrayList;

import org.parandroid.sms.R;
import org.parandroid.sms.ui.PublicKeyReceived;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony.Sms.Intents;
import android.telephony.gsm.SmsMessage;

public class PublicKeyReceiver extends BroadcastReceiver {

	private static final String TAG = "PublicKeyReceiver";
	public static final int NOTIFICATIONID = 31337;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String message = context.getString(R.string.received_public_key);
		
		SmsMessage[] messages = Intents.getMessagesFromIntent(intent);
		if(messages.length == 0) return;
		
		SmsMessage msg = messages[0];

		Intent targetIntent = new Intent(context, PublicKeyReceived.class);
		targetIntent.putExtra("sender", msg.getDisplayOriginatingAddress());
		
		int len = 0, offset = 0;
		ArrayList<byte[]> publicKey = new ArrayList<byte[]>();
		for(SmsMessage m : messages){
			byte[] data = m.getUserData();
			len += data.length;
			publicKey.add(data);
		}
		byte[] pubKeyData = new byte[len];
		for(byte[] part : publicKey){
			System.arraycopy(part, 0, pubKeyData, offset, part.length);
			offset += part.length;
		}
		
		targetIntent.putExtra("publickey", pubKeyData);
		Notification n = new Notification(context, R.drawable.stat_notify_public_key_recieved, message, System.currentTimeMillis(), message, message, targetIntent);
		mNotificationManager.notify(NOTIFICATIONID, n);
	}
}
