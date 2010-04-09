package org.parandroid.sms.ui;

import org.parandroid.sms.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class AboutActivity extends Activity{
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.about_activity);
        setTitle(R.string.about_title);
    }
}
