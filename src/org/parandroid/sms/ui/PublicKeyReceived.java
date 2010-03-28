package org.parandroid.sms.ui;

import org.parandroid.encryption.DHAESKeyFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.transaction.PublicKeyReceiver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class PublicKeyReceived extends Activity {

	private static final String TAG = "PublicKeyReceivedActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(PublicKeyReceiver.NOTIFICATIONID);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getText(R.string.import_public_key_dialog))
				.setTitle(getText(R.string.import_public_key))
		   	 	.setCancelable(false)
		   	 	.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                try {
		                	// TODO: Get message and save key
							DHAESKeyFactory.savePublicKey(PublicKeyReceived.this, "address", "pubkey");
							Toast.makeText(PublicKeyReceived.this, R.string.import_public_key_success, Toast.LENGTH_SHORT).show();
						} catch (Exception e) {
							Log.e(TAG, e.getMessage());
							Toast.makeText(PublicKeyReceived.this, R.string.import_public_key_failure, Toast.LENGTH_SHORT).show();
						}
		           }
		       }).setNegativeButton(PublicKeyReceived.this.getText(R.string.no), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		
		AlertDialog alert = builder.create();
		alert.show();
	}
}
