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
import android.widget.Toast;

public class AuthenticateActivity extends Activity implements OnClickListener {

	private static final String TAG = "PD AuthAct";
		
	private EditText passwordField;
	private Button submitButton, cancelButton;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.authenticate_activity);
        setTitle(R.string.parandroid_authenticate_title);

        submitButton = (Button) findViewById(R.id.submitbutton);
        submitButton.setOnClickListener(this);
        
        cancelButton = (Button) findViewById(R.id.cancelbutton);
        cancelButton.setOnClickListener(this);
        
        passwordField = (EditText) findViewById(R.id.password);  
        passwordField.setHint(getString(R.string.enter_password));
    }
    
    public void onClick(View v) {
		if(v.equals(submitButton)){
			String password = passwordField.getText().toString(); 
			
			try{
				MessageEncryptionFactory.setPassword(password);
				
				// If this fails, the password is wrong.
				MessageEncryptionFactory.getPrivateKey(this);
				
				MessageEncryptionFactory.setAuthenticating(false);
				setResult(RESULT_OK);
				finish();
			}catch(Exception e){
				MessageEncryptionFactory.forgetPassword();
				
				passwordField.setText("");
				Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_LONG).show();
			}
		}else if(v.equals(cancelButton)){
			MessageEncryptionFactory.setAuthenticating(false);
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	@Override
    public void onStop(){
    	super.onStop();
    	
		MessageEncryptionFactory.setAuthenticating(false);
    }
}
