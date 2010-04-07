package org.parandroid.sms.ui;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class PublicKeyReceived extends Activity {

	private static final String TAG = "PublicKeyReceivedActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent i = getIntent();
		final String sender = i.getStringExtra("sender");
		final byte[] publicKey = i.getByteArrayExtra("publickey");

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(i.getIntExtra("notificationId", MessageUtils.DEFAULT_NOTIFICATION_ID));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getText(R.string.import_public_key_dialog) + " " + sender)
				.setTitle(getText(R.string.import_public_key))
		   	 	.setCancelable(false)
		   	 	.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                try {
							MessageEncryptionFactory.savePublicKey(PublicKeyReceived.this, sender, publicKey);
							Toast.makeText(PublicKeyReceived.this, R.string.import_public_key_success, Toast.LENGTH_SHORT).show();
						} catch (Exception e) {
							Log.e(TAG, e.getMessage());
							Toast.makeText(PublicKeyReceived.this, R.string.import_public_key_failure, Toast.LENGTH_SHORT).show();
						}
						finish();
		           }
		       }).setNegativeButton(PublicKeyReceived.this.getText(R.string.no), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
						finish();
		           }
		       });
		
		AlertDialog alert = builder.create();
		alert.show();
	}
}
