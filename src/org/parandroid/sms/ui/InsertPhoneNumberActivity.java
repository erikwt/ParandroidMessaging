package org.parandroid.sms.ui;

import org.parandroid.sms.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class InsertPhoneNumberActivity extends Activity {

	private TextView insertText;
	private EditText phoneNumber;
	private Button button;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.insert_phone_number);
		
		insertText = (TextView) findViewById(R.id.insert_text);
		phoneNumber = (EditText) findViewById(R.id.phone_number);
		button = (Button) findViewById(R.id.button);
		
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startPrintCodeActivity(phoneNumber.getText().toString());
			}
		});
	}
	
	private void startPrintCodeActivity(String phoneNumber){
		Intent intent = new Intent(getBaseContext(), PrintPublicKeyActivity.class);
		intent.putExtra(PrintPublicKeyActivity.PHONE_NUMBER_INTENT_EXTRA, phoneNumber);
		startActivity(intent);
		finish();
	}
}
