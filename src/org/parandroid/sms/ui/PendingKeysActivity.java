package org.parandroid.sms.ui;

import java.util.HashMap;

import org.parandroid.encoding.Base64Coder;
import org.parandroid.encryption.MessageEncryption;
import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.data.Contact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Sms.Inbox;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class PendingKeysActivity extends Activity {

	private static final String TAG = "PublicKeyManagerActivity";
    private static final int CONTEXT_MENU_ACCEPT = 0;
    private static final int CONTEXT_MENU_DECLINE = 1;
    
    private String[] descriptions;
    private Integer[] ids;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.accepted_keys_activity);
	}
    
    @Override
    public void onResume(){
    	super.onResume();
    	init();
    }
    
    private void init(){
        HashMap<Integer,String> items = MessageEncryptionFactory.getPublicKeyList(this, false);
	    ListView publicKeysList = (ListView) findViewById(R.id.public_keys);
	    
        if(items.size() == 0){
        	descriptions = new String[]{ getString(R.string.no_public_keys) };
        	unregisterForContextMenu(publicKeysList);
        }else{
        	registerForContextMenu(publicKeysList);
        	Integer[] intArray = {};
            String[] stringArray = {};
            ids = items.keySet().toArray(intArray);
            descriptions = items.values().toArray(stringArray);
            
            for(int i = 0; i < descriptions.length; i++){
            	Contact c = Contact.get(descriptions[i], true);
            	if(c != null){
            		String name = c.getName();
            		if(name != null){
            			descriptions[i] = name + " <" + descriptions[i] + ">";
            		}
            	}
            }
        }

        final String[] descriptionList = this.descriptions;
	    final ArrayAdapter<String> publicKeys = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, descriptionList);
	    publicKeysList.setAdapter(publicKeys);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CONTEXT_MENU_ACCEPT, 0, getString(R.string.accept_public_key));
        menu.add(0, CONTEXT_MENU_DECLINE, 0, getString(R.string.decline_public_key));
    }

    public boolean onContextItemSelected(MenuItem item){
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()){
            case CONTEXT_MENU_DECLINE:
            	Log.i(TAG, "Declining public key");
                AlertDialog.Builder declinePublicKeyDialogBuilder = new AlertDialog.Builder(this);
        	    declinePublicKeyDialogBuilder.setMessage(getText(R.string.decline_public_key_dialog))
        		   .setTitle(descriptions[info.position])
        		   .setCancelable(false)
        	       .setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
                            MessageEncryptionFactory.deletePublicKey(PendingKeysActivity.this, ids[info.position]);
                            init();
        	           }
        	       })
        	       .setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	
	        	AlertDialog alert = declinePublicKeyDialogBuilder.create();
	        	alert.show();
	
	            return true;
            
            case CONTEXT_MENU_ACCEPT:
            	Log.i(TAG, "Accepting public key");
            	
            	String number;
            	if(descriptions[info.position].contains("<")){
            		number = descriptions[info.position].substring(descriptions[info.position].indexOf("<") + 1, descriptions[info.position].length() - 1);
            	}else{
            		number = descriptions[info.position];
            	}
            	
            	if(MessageEncryptionFactory.hasPublicKey(this, number)){
            		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
            		 builder.setMessage(getText(R.string.import_public_key_dialog_overwrite) + " " + descriptions[info.position])
            		 	.setTitle(getText(R.string.import_public_key_overwrite))
            		 	.setCancelable(false)
            		 	.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
            		 		public void onClick(DialogInterface dialog, int id) {
            		 			doOverwrite(ids[info.position]);
            		 		}
            		 	}).setNegativeButton(PendingKeysActivity.this.getText(R.string.no), new DialogInterface.OnClickListener() {
            		 		public void onClick(DialogInterface dialog, int id) {
            		 			dialog.cancel();
            		 			finish();
            		 		}
            		 	});

            		 AlertDialog overwriteAlert = builder.create();
            		 overwriteAlert.show();
            	}else{
            		AlertDialog.Builder acceptPublicKeyDialogBuilder = new AlertDialog.Builder(this);
            	    acceptPublicKeyDialogBuilder.setMessage(getText(R.string.accept_public_key))
            		   .setTitle(descriptions[info.position])
            		   .setCancelable(false)
            	       .setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	        	   SQLiteDatabase keyRing = MessageEncryptionFactory.openKeyring(PendingKeysActivity.this);
            	        	   ContentValues cv = new ContentValues();
            	        	   cv.put("accepted", true);
            	        	   keyRing.update(MessageEncryptionFactory.PUBLIC_KEY_TABLE, cv, "_ID=" + ids[info.position], null);
            	        	   keyRing.close();
            	        	   init();
            	           }
            	       })
            	       .setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       });
            	
    	        	AlertDialog acceptAlert = acceptPublicKeyDialogBuilder.create();
    	        	acceptAlert.show();
            	}
            	
            	return true;
            	
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    private String[] BACKWARD_PROJECTION = new String[] { Inbox._ID, Inbox.ADDRESS, Inbox.BODY };
    private void doOverwrite(int id){
    	SQLiteDatabase keyRing = MessageEncryptionFactory.openKeyring(this);
    	Cursor c = keyRing.query(MessageEncryptionFactory.PUBLIC_KEY_TABLE, null, "_ID=" + id, null, null, null, null);
    	if(!c.moveToNext()){
    		Log.e(TAG, "Public key with doesn't exist: " + id);
    		return;
    	}

    	String number = c.getString(c.getColumnIndex("number"));
    	String publicKey = c.getString(c.getColumnIndex("publicKey"));
    	
    	c.close();
    	keyRing.close();
    	
	    Uri uriSms = Uri.parse("content://sms");
	    String selection = Inbox.TYPE + "='" + MessageItem.MESSAGE_TYPE_PARANDROID_INBOX +
	    	"' OR " + Inbox.TYPE + "='" + MessageItem.MESSAGE_TYPE_PARANDROID_OUTBOX + "'";
	
	    c = getContentResolver().query(uriSms, BACKWARD_PROJECTION, selection, null, null);
	    if(!c.moveToFirst()){
	    	Log.i(TAG, "backward: No messages");
	    }else{
	    	do{
	    		try{
	    			String address = c.getString(c.getColumnIndex(Inbox.ADDRESS));
	
	    			// TODO: Fix this in SQL
	    			if(PhoneNumberUtils.compare(number, address)){
	    				String body = c.getString(c.getColumnIndex(Inbox.BODY));
	    				Log.i(TAG, "address: " + address);
	    				Log.i(TAG, "body: " + body);
	    				String clearBody = MessageEncryption.decrypt(this, address, Base64Coder.decode(body));
	    				String newBody = new String(Base64Coder.encode(MessageEncryption.encrypt(this, Base64Coder.decode(publicKey), clearBody)));
	    				
	    				c.updateString(c.getColumnIndex(Inbox.BODY), newBody);
	    			}
	    		}catch(Exception e){
	    			e.printStackTrace();
	    		}
	    	}while(c.moveToNext());
	    		c.commitUpdates();
	    	}
	    
	    try {
	    	int oldId = MessageEncryptionFactory.getPublicKeyId(this, number);
	    	MessageEncryptionFactory.deletePublicKey(this, oldId);
	    	
			ContentValues cv = new ContentValues();
			cv.put("accepted", true);
			
			keyRing = MessageEncryptionFactory.openKeyring(this);
			keyRing.update(MessageEncryptionFactory.PUBLIC_KEY_TABLE, cv, "_ID=" + id, null);
			keyRing.close();
			
	    	Toast.makeText(PendingKeysActivity.this, R.string.import_public_key_success, Toast.LENGTH_SHORT).show();
	    }catch (Exception e) {
	    	Log.e(TAG, e.getMessage());
	    	e.printStackTrace();
	    	Toast.makeText(PendingKeysActivity.this, R.string.import_public_key_failure, Toast.LENGTH_SHORT).show();
	    }
    }
}

