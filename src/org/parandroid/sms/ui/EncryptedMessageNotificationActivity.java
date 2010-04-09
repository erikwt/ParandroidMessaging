package org.parandroid.sms.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class EncryptedMessageNotificationActivity extends Activity {
	
	private static final String TAG = "Parandroid EncryptedMessageNotificationActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(i.getIntExtra("notificationId", MessageUtils.DEFAULT_NOTIFICATION_ID));

		Intent targetIntent = new Intent(getApplicationContext(), ComposeMessageActivity.class);
		targetIntent.putExtra("threadId", -1);
		startActivity(targetIntent);
		
		finish();
	}
	
	@Override
	protected void onResume() {
		Intent targetIntent = new Intent(this, ConversationList.class);
		startActivity(targetIntent);
		
		finish();
	}
}
