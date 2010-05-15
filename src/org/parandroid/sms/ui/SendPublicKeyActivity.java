package org.parandroid.sms.ui;

import java.io.IOException;
import java.util.ArrayList;

import org.parandroid.encoding.Base64Coder;
import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SendPublicKeyActivity extends Activity implements OnClickListener, OnItemClickListener{

	private static final String TAG = "Parandroid SendPublicKeyActivity";
	private static final int REQUEST_SELECTED_CONTACT = 0;
	
	private static final String[] NUMBER_PROJECTION = { ContactsContract.CommonDataKinds.Phone.NUMBER };
	
	private Button sendButton, addRecipientButton;
	private TextView noRecipient, tapToRemove;
	private ArrayList<String> recipientLabels;
	private ArrayList<String> recipientNumbers;
	private ArrayAdapter<String> recipientAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(R.string.menu_send_public_key);
        setContentView(R.layout.send_public_key_activity);

        recipientLabels = new ArrayList<String>(); 
        recipientNumbers = new ArrayList<String>(); 
        
        addRecipientButton = (Button) findViewById(R.id.addrecipientbutton);
        addRecipientButton.setOnClickListener(this);
        
        sendButton = (Button) findViewById(R.id.sendbutton);
        sendButton.setOnClickListener(this);
		sendButton.setClickable(false);

        noRecipient = (TextView) findViewById(R.id.notify_no_recipient);
        tapToRemove = (TextView) findViewById(R.id.notify_tap_to_remove);
        tapToRemove.setVisibility(View.GONE);
        
        recipientAdapter = new ArrayAdapter<String>(this, R.layout.public_key_recipient_item, recipientLabels);
        ListView recipientList = (ListView) findViewById(R.id.recipients);
        recipientList.setAdapter(recipientAdapter);
        recipientList.setOnItemClickListener(this);
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode != RESULT_OK){
			Log.i(TAG, "onActivityResult: resultCode not RESULT_OK. requestCode/resultcode: " + requestCode + "/" + resultCode);
			return;
		}
		
		switch(requestCode){
		case REQUEST_SELECTED_CONTACT:
			Uri uri = data.getData();
			Cursor cursor = getContentResolver().query(uri, null, null, null, null);
			
			if(cursor.moveToFirst()){
				int contactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				
				Cursor privateCursor = getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						NUMBER_PROJECTION,
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
						null,
						null);
				
				if(privateCursor.moveToFirst()){
					do{
						String number = privateCursor.getString(privateCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						
						recipientLabels.add(name + " <" + number + ">");
						recipientNumbers.add(number);
					}while(privateCursor.moveToNext());
					onDataSetChanged();
				}else{
					Toast.makeText(this, R.string.no_number_for_contact, Toast.LENGTH_SHORT).show();
				}
			}else{
				Toast.makeText(this, R.string.unsupported_contact, Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}
	
	private void onDataSetChanged(){
		if(recipientNumbers.size() == 0){
			sendButton.setClickable(false);
			sendButton.setText(getString(R.string.send));
			
			noRecipient.setVisibility(View.VISIBLE);
			tapToRemove.setVisibility(View.GONE);
		}else{
			sendButton.setClickable(true);
			sendButton.setText(getString(R.string.send) + " (" + recipientNumbers.size() + ")");
			
			noRecipient.setVisibility(View.GONE);
			tapToRemove.setVisibility(View.VISIBLE);
		}
				
		recipientAdapter.notifyDataSetChanged();
	}
	
	public void onClick(View v) {		
		if(v.equals(addRecipientButton)){
			Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
			startActivityForResult(i, REQUEST_SELECTED_CONTACT);
		}else if(v.equals(sendButton)){
			sendButton.setClickable(false);
			addRecipientButton.setClickable(false);
			for(String number : recipientNumbers){				
				try{
					if(!MessageEncryptionFactory.sendPublicKey(this, number)){
						throw new Exception("Failed sending public key, got false from MultipartMessage.send()");
					}
					Toast.makeText(this, getText(R.string.send_public_key_success) + " " +  number, Toast.LENGTH_SHORT).show();
				}catch(Exception e){
					e.printStackTrace();
					Toast.makeText(this, getText(R.string.send_public_key_failure) + " " +  number, Toast.LENGTH_LONG).show();
				}
			}
			
			finish();
		}
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		recipientLabels.remove(position);
		recipientNumbers.remove(position);
		onDataSetChanged();
	}
}

