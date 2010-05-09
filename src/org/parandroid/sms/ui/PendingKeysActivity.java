package org.parandroid.sms.ui;

import java.util.HashMap;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.RecipientList.Recipient;
import org.parandroid.sms.util.ContactInfoCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
                ContactInfoCache.CacheEntry c = ContactInfoCache.getInstance().getContactInfo(this, descriptions[i]);
            	if(c != null){
            		String name = c.name;
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
                AlertDialog.Builder generateKeypairDialogBuilder = new AlertDialog.Builder(this);
        	    generateKeypairDialogBuilder.setMessage(getText(R.string.decline_public_key_dialog))
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
        	
	        	AlertDialog alert = generateKeypairDialogBuilder.create();
	        	alert.show();
	
	            return true;
            
            case CONTEXT_MENU_ACCEPT:
            	Log.i(TAG, "Accepting public key");
            	SQLiteDatabase keyRing = MessageEncryptionFactory.openKeyring(this);
            	
            	ContentValues cv = new ContentValues();
            	cv.put("accepted", true);
            	keyRing.update(MessageEncryptionFactory.PUBLIC_KEY_TABLE, cv, "_ID=" + ids[info.position], null);
            	
            	keyRing.close();
            	init();
            	
            	return true;
            	
            default:
                return super.onContextItemSelected(item);
        }
    }
}

