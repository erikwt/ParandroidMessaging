package org.parandroid.sms.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;

public class SendPublicKeyActivity extends Activity implements OnClickListener{

	
    private static final String PHONE_NUMBER_SEPARATORS = " ()-./";
	private static final String TAG = "Parandroid SendPublicKeyActivity";
	
	private MultiAutoCompleteTextView receipients;
	private Button sendButton;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_public_key_activity);
        
        sendButton = (Button) findViewById(R.id.sendbutton);
        sendButton.setOnClickListener(this);
        
        Cursor peopleCursor = getContentResolver().query(Contacts.People.CONTENT_URI, PEOPLE_PROJECTION, null, null, Contacts.People.DEFAULT_SORT_ORDER);  
        ContactListAdapter contactadapter = new ContactListAdapter(this,peopleCursor);  
          
        receipients = (MultiAutoCompleteTextView) findViewById(R.id.receipients);  
        receipients.setHint("Contact name"); // TODO: Localize
        receipients.setAdapter(contactadapter);  
        receipients.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
    }

	public void onClick(View v) {		
		if(v.equals(sendButton)){
			ArrayList<String> receipientNumbers = new ArrayList<String>();
			String selection = receipients.getText().toString();
			for(String s : selection.split(", ")){
				// TODO: Safe query
				Cursor contactCursor = getContentResolver().query(Contacts.People.CONTENT_URI, PEOPLE_PROJECTION, "NAME LIKE '" + s + "'", null, Contacts.People.DEFAULT_SORT_ORDER);
				
				String number = filterPhoneNumber(s);
				if(number == null || !isNumeric(number)){
					if(contactCursor.getCount() != 1 || !contactCursor.moveToFirst()){
						// TODO: Good errormessage
						Toast.makeText(this, "Error for contact " + s + ", found " + new Integer(contactCursor.getCount()).toString() + " records.", Toast.LENGTH_SHORT).show();
						Log.e(TAG,"Multiple results for contact " + s);
						continue;
					}
					
					int columnIndex = contactCursor.getColumnIndex(Contacts.People.NUMBER);
					number = contactCursor.getString(columnIndex);
				}

				receipientNumbers.add(number);
			}
			
			for(String num : receipientNumbers){
				num = filterPhoneNumber(num);
				SmsManager sm = SmsManager.getDefault();
				Log.i(TAG,"Sending public key to " + num);
				
				try { 
					byte[] publicKey = MessageEncryptionFactory.getOwnPublicKey(SendPublicKeyActivity.this);
					sm.sendDataMessage(num, null, MessageEncryptionFactory.PUBLIC_KEY_PORT, publicKey, null, null);
					Toast.makeText(this, getText(R.string.send_public_key_success) + " " +  num, Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(this, getText(R.string.send_public_key_failure) + " " +  num, Toast.LENGTH_SHORT).show();
				}
				
			}
			
			finish();
		}
	}
    
    /**
     * Returns a string with all phone number separator characters removed.
     * @param phoneNumber A phone number to clean (e.g. "+1 (212) 479-7990")
     * @return A string without the separators (e.g. "12124797990")
     */
    public static String filterPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
        	Log.e(TAG, "Phonenumber is null!");
            return null;
        }

        int length = phoneNumber.length();
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            char character = phoneNumber.charAt(i);

            if (PHONE_NUMBER_SEPARATORS.indexOf(character) == -1) {
                builder.append(character);
            }
        }
        return builder.toString();
    }
	
	public static class ContactListAdapter extends CursorAdapter implements Filterable {  
        public ContactListAdapter(Context context, Cursor c) {  
            super(context, c);  
            mContent = context.getContentResolver();
        }  
  
        @Override  
        public View newView(Context context, Cursor cursor, ViewGroup parent) {  
            final LayoutInflater inflater = LayoutInflater.from(context);  
            final TextView view = (TextView) inflater.inflate(  
                    android.R.layout.simple_dropdown_item_1line, parent, false);  
            view.setText(cursor.getString(5));  
            return view;  
        }  
  
        @Override  
        public void bindView(View view, Context context, Cursor cursor) {  
            ((TextView) view).setText(cursor.getString(5));  
         }  
  
        @Override  
        public String convertToString(Cursor cursor) {  
            return cursor.getString(5);  
        }  
  
        @Override  
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {  
            if (getFilterQueryProvider() != null) {  
                return getFilterQueryProvider().runQuery(constraint);  
            }  
  
            StringBuilder buffer = null;  
            String[] args = null;  
            if (constraint != null) {  
                buffer = new StringBuilder();  
                buffer.append("UPPER(");  
                buffer.append(Contacts.ContactMethods.NAME);  
                buffer.append(") GLOB ?");  
                args = new String[] { constraint.toString().toUpperCase() + "*" };  
            }  
  
            return mContent.query(Contacts.People.CONTENT_URI, PEOPLE_PROJECTION,  
                    buffer == null ? null : buffer.toString(), args,  
                    Contacts.People.DEFAULT_SORT_ORDER);  
        }  
  
        private ContentResolver mContent;          
    }  
  
    private static final String[] PEOPLE_PROJECTION = new String[] {  
        Contacts.People._ID,  
        Contacts.People.PRIMARY_PHONE_ID,  
        Contacts.People.TYPE,  
        Contacts.People.NUMBER,  
        Contacts.People.LABEL,  
        Contacts.People.NAME,  
    };
    
    public boolean isNumeric(String input)
    {
       try
       {
          Integer.parseInt( input );
          return true;
       }
       catch( Exception e)
       {
          return false;
       }
    }
}

