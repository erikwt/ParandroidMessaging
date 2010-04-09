package org.parandroid.sms.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.ui.RecipientList.Recipient;
import org.parandroid.sms.util.ContactInfoCache.CacheEntry;
import org.parandroid.sms.util.ContactInfoCache;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SpecifyRecipientsActivity extends Activity implements OnClickListener {

	private static final String TAG = "Parandroid SpecifyRecipientsActivity";
	
	private String[] recipientNumbers;
	private RecipientList recipients;
	private ArrayList<CheckBox> checkboxes;
	private Button sendButton;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        checkboxes = new ArrayList<CheckBox>(); 
        recipientNumbers = (String[]) intent.getExtras().get("recipients");
        String addresses = "";
        for(String number : recipientNumbers){
        	addresses.concat(number).concat(";");
        }
        recipients = RecipientList.from(addresses, this);

        ArrayList<CheckBox> encryptCheckboxes = new ArrayList<CheckBox>();
        ArrayList<CheckBox> plainTextCheckboxes = new ArrayList<CheckBox>();
        Iterator<Recipient> recipientIterator = recipients.iterator();
        for(int i = 0; recipientIterator.hasNext(); i++) {
        	Recipient contact = recipientIterator.next();
        	CheckBox recipientCheckBox = new CheckBox(this);
        	recipientCheckBox.setText(contact.nameAndNumber);
        	recipientCheckBox.setChecked(true);
        	recipientCheckBox.setId(i);
        	
        	checkboxes.add(recipientCheckBox);
        	if(MessageEncryptionFactory.hasPublicKey(this, contact.number)) 
	            encryptCheckboxes.add(recipientCheckBox);
        	else
        		plainTextCheckboxes.add(recipientCheckBox);
        }
        
        setTitle(R.string.specify_recipients);
        
        ScrollView view = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        view.addView(layout);

        TextView textEncrypted = new TextView(this);
        textEncrypted.setText(R.string.send_encrypted_to);
        textEncrypted.setTextSize(18);
        layout.addView(textEncrypted);
        
        for(CheckBox checkbox : encryptCheckboxes)
        	layout.addView(checkbox);
        
        TextView textPlain = new TextView(this);
        textPlain.setText(R.string.send_plain_to);
        textPlain.setTextSize(18);
        layout.addView(textPlain);
        
        for(CheckBox checkbox : plainTextCheckboxes)
        	layout.addView(checkbox);
        
        sendButton = new Button(this);
        sendButton.setText(R.string.send);
        sendButton.setOnClickListener(this);
        layout.addView(sendButton);
        
        setContentView(view);
        
    }

	public void onClick(View v) {		
		if(v.equals(sendButton)){
			RecipientList newRecipients = new RecipientList();
			Iterator<Recipient> recipientIterator = recipients.iterator();
			for(CheckBox recipientCheckBox : checkboxes){
	        	Recipient recipient = recipientIterator.next();
				if(recipientCheckBox.isChecked())
	        		newRecipients.add(recipient);
	        }

			ComposeMessageActivity.setRecipients(newRecipients);
			
			finish();
		}
	}
	
	
}
