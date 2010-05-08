package org.parandroid.sms.ui;

import org.parandroid.sms.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

public class AboutActivity extends Activity{
	
    // IDs of the main menu items.
    public static final int MENU_CONVERSATION_LIST   = 0;
    public static final int MENU_HELP                = 1;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.about_activity);
        setTitle(R.string.about_title);
    }
	
   @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        menu.add(0, MENU_CONVERSATION_LIST, 0, R.string.all_threads).setIcon(
                com.android.internal.R.drawable.ic_menu_friendslist);
        
        menu.add(0, MENU_HELP, 0, R.string.menu_help).setIcon(
                R.drawable.ic_gallery_video_overlay);
        
        return true;
    }
   
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_CONVERSATION_LIST:
                finish();
                Intent conversationListIntent = new Intent(this, ConversationList.class);
                startActivity(conversationListIntent);
                break;
            case MENU_HELP:
                Intent helpIntent = new Intent(this, HelpActivity.class);
                startActivity(helpIntent);
                break;
            default:
                return true;
        }
        return false;
    }
}
