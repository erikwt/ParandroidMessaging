package org.parandroid.sms.ui;

import org.parandroid.sms.transaction.MessagingNotification;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class EncryptedMessageNotificationActivity extends Activity {
	
	private static final String TAG = "PD EncMsgNotifyAct";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(i.getIntExtra("notificationId", MessageUtils.DEFAULT_NOTIFICATION_ID));

		Intent clickIntent = MessagingNotification.getAppIntent();
        clickIntent.setData(Uri.withAppendedPath(clickIntent.getData(), Long.toString(i.getLongExtra("threadId", -1))));
        clickIntent.setAction(Intent.ACTION_VIEW);
		
		startActivity(clickIntent);
		
		finish();
	}
	
	@Override
	protected void onResume() {
		Intent targetIntent = new Intent(this, ConversationList.class);
		startActivity(targetIntent);
		
		finish();
	}
}
