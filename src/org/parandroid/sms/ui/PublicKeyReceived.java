package org.parandroid.sms.ui;

import org.parandroid.encoding.Base64Coder;
import org.parandroid.encryption.MessageEncryption;
import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.util.ContactInfoCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Sms.Inbox;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class PublicKeyReceived extends Activity {

	private static final String TAG = "PublicKeyReceivedActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent i = getIntent();
		final String sender = i.getStringExtra("sender");
		final byte[] publicKey = Base64Coder.decode(new String(i.getByteArrayExtra("publickey")));

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(i.getIntExtra("notificationId", MessageUtils.DEFAULT_NOTIFICATION_ID));
		
		String name = ContactInfoCache.getInstance().getContactName(PublicKeyReceived.this, sender);
		if(name == null) name = "";
		
		if(MessageEncryptionFactory.hasPublicKey(this, sender)){
			overwriteImport(name, sender, publicKey);
		}else{
			normalImport(name, sender, publicKey);
		}
	}
	
	private void overwriteImport(final String name, final String sender, final byte[] publicKey){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getText(R.string.import_public_key_dialog_overwrite) + " " + name + " (" + sender + ")")
				.setTitle(getText(R.string.import_public_key_overwrite))
		   	 	.setCancelable(false)
		   	 	.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                doOverwrite(name, sender, publicKey);
		                askSendPublicKey(name, sender);
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
	
	//public ProgressDialog progressDialog;
	private String[] BACKWARD_PROJECTION = new String[] { Inbox._ID, Inbox.ADDRESS, Inbox.BODY };
	private void doOverwrite(final String name, final String sender, final byte[] publicKey){
		Uri uriSms = Uri.parse("content://sms");
		String selection = Inbox.TYPE + "='" + MessageItem.MESSAGE_TYPE_PARANDROID_INBOX + 
			"' OR " + Inbox.TYPE + "='" + MessageItem.MESSAGE_TYPE_PARANDROID_OUTBOX + "'";
		
		final Cursor backwardCursor = getContentResolver().query(uriSms, BACKWARD_PROJECTION, selection, null, null);
		if(!backwardCursor.moveToFirst()){
			Log.i(TAG, "backward: No messages");
		}else{
			int numMessages = backwardCursor.getCount();
			Log.i(TAG, "Re-encrypting " + numMessages + " messages");
			
			final ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMessage(getString(R.string.reencrypting_messages));
			progressDialog.setCancelable(false);
			progressDialog.setMax(numMessages);
			progressDialog.show();
			
			new Thread(new Runnable() {
				public void run(){
					do{
						try{
							String address = backwardCursor.getString(backwardCursor.getColumnIndex(Inbox.ADDRESS));
							
							// TODO: Fix this in SQL
							if(address.equals(sender)){
								String body = backwardCursor.getString(backwardCursor.getColumnIndex(Inbox.BODY));
								String clearBody = MessageEncryption.decrypt(PublicKeyReceived.this, address, Base64Coder.decode(body));
								String newBody = new String(Base64Coder.encode(MessageEncryption.encrypt(PublicKeyReceived.this, publicKey, clearBody)));
								
								backwardCursor.updateString(backwardCursor.getColumnIndex(Inbox.BODY), newBody);
							}
						}catch(Exception e){
							e.printStackTrace();
						}
						progressDialog.incrementProgressBy(1);
					}while(backwardCursor.moveToNext());
					
					backwardCursor.commitUpdates();
					progressDialog.dismiss();

					savePublicKey(sender, publicKey);
				}
			}).start();
		}
	}
	
	private void savePublicKey(String sender, byte[] publicKey){
		try {
			MessageEncryptionFactory.savePublicKey(this, sender, publicKey);
			//Toast.makeText(PublicKeyReceived.this, R.string.import_public_key_success, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			//Toast.makeText(PublicKeyReceived.this, R.string.import_public_key_failure, Toast.LENGTH_SHORT).show();
		}
	}
	
	private void normalImport(final String name, final String sender, final byte[] publicKey){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getText(R.string.import_public_key_dialog) + " " + name + " (" + sender + ")")
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
		                askSendPublicKey(name, sender);
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
	
	private void askSendPublicKey(final String name, final String sender){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getText(R.string.send_back_public_key_dialog) + " " + name + " (" + sender + ")")
				.setTitle(getText(R.string.menu_send_public_key))
		   	 	.setCancelable(false)
		   	 	.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               SmsManager sm = SmsManager.getDefault(); 
		        	   try {
		                	byte[] publicKey = MessageEncryptionFactory.getOwnPublicKey(PublicKeyReceived.this);
    						sm.sendDataMessage(sender, null, MessageEncryptionFactory.PUBLIC_KEY_PORT, publicKey, null, null);
    						Toast.makeText(PublicKeyReceived.this, getText(R.string.send_public_key_success) + " " +  sender, Toast.LENGTH_SHORT).show();
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
