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

public class SetPasswordActivity extends Activity implements OnClickListener {

	private static final String TAG = "Parandroid SetPasswordActivity";
	
	private EditText passwordField;
	private EditText passwordConfirmField;
	private Button submitButton, cancelButton;
	
	public static final int PASSWORD_MIN_LENGTH = 4;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.set_password_activity);
        
        submitButton = (Button) findViewById(R.id.submitbutton);
        submitButton.setOnClickListener(this);

        cancelButton = (Button) findViewById(R.id.cancelbutton);
        cancelButton.setOnClickListener(this);
        
        passwordField = (EditText) findViewById(R.id.password);  
        passwordField.setHint(getString(R.string.enter_password));
        
        passwordConfirmField = (EditText) findViewById(R.id.password_confirm);  
        passwordConfirmField.setHint(getString(R.string.confirm_password));
    }
    
    public void onClick(View v) {		
		if(v.equals(submitButton)){
			String password = passwordField.getText().toString(); 
			String passwordConfirm = passwordConfirmField.getText().toString();
			
			if(password.length() < PASSWORD_MIN_LENGTH){
				Toast.makeText(this, getString(R.string.passwords_too_short) + " " + PASSWORD_MIN_LENGTH, Toast.LENGTH_LONG).show();
				return;
			}
			
			if(!password.equals(passwordConfirm)){
				passwordField.setText("");
				passwordConfirmField.setText("");
				
				Toast.makeText(this, R.string.passwords_dont_match, Toast.LENGTH_LONG).show();
				return;
			}
			
			MessageEncryptionFactory.setPassword(password);
			MessageEncryptionFactory.setAuthenticating(false);

			setResult(RESULT_OK);
			finish();
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
