package org.parandroid.sms.ui;


import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class InsertPasswordActivity extends Activity implements OnClickListener {

	private static final String TAG = "Parandroid InsertPasswordActivity";
		
	private EditText passwordField;
	private Button submitButton;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.insert_password_activity);
        
        submitButton = (Button) findViewById(R.id.submitbutton);
        submitButton.setOnClickListener(this);
        
        passwordField = (EditText) findViewById(R.id.password);  
        passwordField.setHint("Enter password"); // TODO: Localize
    }
    
    public void onClick(View v) {		
		if(v.equals(submitButton)){
			Log.i(TAG, "clicked submitButton in password thing");
			String password = passwordField.getText().toString(); 
			
			// TODO: check if the password is correct?
			MessageEncryptionFactory.setPassword(password);
			
			MessageEncryptionFactory.passwordDialogPending = false;
			finish();
		}
	}
}
