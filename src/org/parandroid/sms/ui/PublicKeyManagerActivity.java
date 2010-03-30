package org.parandroid.sms.ui;

import java.util.ArrayList;

import org.parandroid.encryption.DHAESKeyFactory;
import org.parandroid.sms.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class PublicKeyManagerActivity extends Activity {

	public static final String TAG = "PublicKeyManagerActivity";

    public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.public_key_manager);
	    init();
	}
    
    private void init(){
        final ArrayList<String> items = DHAESKeyFactory.getPublicKeys(this);
	    ListView publicKeysList = (ListView) findViewById(R.id.public_keys);
	    final ArrayAdapter<String> publicKeys = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
	    
	    publicKeysList.setAdapter(publicKeys);
	    publicKeysList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO
			}
	    });
    }
}
