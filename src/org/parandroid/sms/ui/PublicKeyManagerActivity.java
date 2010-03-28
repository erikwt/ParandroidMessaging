package org.parandroid.sms.ui;

import java.util.ArrayList;

import org.parandroid.sms.R;

import android.app.TabActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;

public class PublicKeyManagerActivity extends TabActivity {

	private TabHost mTabHost;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.public_key_manager);

	    mTabHost = getTabHost();
	    
	    mTabHost.addTab(mTabHost.newTabSpec("pk_tab_1").setIndicator(getString(R.string.public_keys_saved)).setContent(R.id.public_keys_saved));
	    mTabHost.addTab(mTabHost.newTabSpec("pk_tab_2").setIndicator(getString(R.string.public_keys_pending)).setContent(R.id.public_keys_pending));
	    
	    ListView publicKeysSaved = (ListView) findViewById(R.id.public_keys_saved);
	    
	    ArrayList<String> items = new ArrayList<String>();
	    items.add("erik");
	    
	    publicKeysSaved.setAdapter(new ArrayAdapter<String>(this, R.id.public_key_listitem, items));
	    
	    mTabHost.setCurrentTab(0);
	}
}
