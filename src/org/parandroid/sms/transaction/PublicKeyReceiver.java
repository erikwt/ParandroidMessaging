package org.parandroid.sms.transaction;

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
		for(SmsMessage pk : messages){
			Intent targetIntent = new Intent(context, PublicKeyReceived.class);
			targetIntent.putExtra("sender", pk.getDisplayOriginatingAddress());
			targetIntent.putExtra("publickey", pk.getUserData());
			Notification n = new Notification(context, R.drawable.stat_notify_public_key_recieved, message, System.currentTimeMillis(), message, message, targetIntent);
			mNotificationManager.notify(NOTIFICATIONID, n);
		}
    }
}
