package org.parandroid.sms.ui;

import org.parandroid.sms.R;

import android.app.NotificationManager;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

public class ManagePublicKeysActivity extends TabActivity {
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.public_key_manager_activity);
	    setTitle(R.string.menu_manage_public_keys);

	    Resources res = getResources();
	    TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent;

	    intent = new Intent().setClass(this, AcceptedKeysActivity.class);

	    spec = tabHost.newTabSpec("accepted").setIndicator("Accepted", res.getDrawable(R.drawable.public_key_manager_tab)).setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, PendingKeysActivity.class);
	    
	    int tab = 0; 
	    if(getIntent().hasExtra("id")){
	    	long id = getIntent().getLongExtra("id", -1);
	    	intent.putExtra("id", id);
	    	NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    	mNotificationManager.cancel((int) id);
	    	tab = 1;
	    }
	    
	    spec = tabHost.newTabSpec("pending").setIndicator("Pending", res.getDrawable(R.drawable.public_key_manager_tab)).setContent(intent);
	    tabHost.addTab(spec);

	    TabWidget tw = getTabWidget();
	    for(int i = 0; i < tw.getChildCount(); i++){
	    	RelativeLayout relLayout = (RelativeLayout)tw.getChildAt(i);
	    	TextView tv = (TextView)relLayout.getChildAt(1);
	    	tv.setTextColor(Color.GRAY);
	    }
	    
	    tabHost.setCurrentTab(tab);
	}
}
