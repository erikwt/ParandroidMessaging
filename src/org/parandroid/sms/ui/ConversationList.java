/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.parandroid.sms.ui;

import java.io.FileOutputStream;
import java.security.PrivateKey;

import org.parandroid.sms.LogTag;
import org.parandroid.sms.R;
import org.parandroid.sms.data.Contact;
import org.parandroid.sms.data.ContactList;
import org.parandroid.sms.data.Conversation;
import org.parandroid.sms.transaction.MessagingNotification;
import org.parandroid.sms.transaction.SmsRejectedReceiver;
import org.parandroid.sms.util.DraftCache;
import org.parandroid.sms.util.Recycler;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.util.SqliteWrapper;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms.Inbox;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.bouncycastle.util.encoders.Base64;
import org.parandroid.encryption.MessageEncryption;
import org.parandroid.encryption.MessageEncryptionFactory;

/**
 * This activity provides a list view of existing conversations.
 */
public class ConversationList extends ListActivity
            implements DraftCache.OnDraftChangedListener {
    private static final String TAG = "ConversationList";
    private static final boolean DEBUG = false;
    private static final boolean LOCAL_LOGV = DEBUG;
    
    private static final String FIRST_LAUNCH_FILE = "firstParandroidLaunch"; 

    private static final int THREAD_LIST_QUERY_TOKEN = 1701;
    public static final int DELETE_CONVERSATION_TOKEN = 1801;
    public static final int HAVE_LOCKED_MESSAGES_TOKEN = 1802;

    // IDs of the main menu items.
    public static final int MENU_COMPOSE_NEW          = 0;
    public static final int MENU_SEARCH               = 1;
    public static final int MENU_DELETE_ALL           = 3;
    public static final int MENU_PREFERENCES          = 4;
    public static final int MENU_GENERATE_KEYPAIR     = 5;
    public static final int MENU_SEND_PUBLIC_KEY	  = 6;
    public static final int MENU_MANAGE_PUBLIC_KEYS	  = 7;
    public static final int MENU_CHANGE_PASSWORD	  = 8;
    public static final int MENU_ABOUT				  = 9;
    public static final int MENU_HELP				  = 10;

    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;
    public static final int MENU_VIEW_CONTACT         = 2;
    public static final int MENU_ADD_TO_CONTACTS      = 3;

    public static final int REQUEST_CODE_SET_PASSWORD 					= 0;
    public static final int REQUEST_CODE_SET_FIRST_PASSWORD				= 1;
    public static final int REQUEST_CODE_CHANGE_PASSWORD				= 2;
    public static final int REQUEST_CODE_CHANGE_PASSWORD_AUTHENTICATE	= 3;
    public static final int REQUEST_CODE_SET_NEW_PASSWORD				= 4;

    private ThreadListQueryHandler mQueryHandler;
    private ConversationListAdapter mListAdapter;
    private CharSequence mTitle;
    private SharedPreferences mPrefs;
    private Handler mHandler;

    static private final String CHECKED_MESSAGE_LIMITS = "checked_message_limits";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(firstParandroidLaunch()) {
            Intent helpIntent = new Intent(this, HelpActivity.class);
            startActivity(helpIntent);
        }
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.conversation_list_screen);

        mQueryHandler = new ThreadListQueryHandler(getContentResolver());

        ListView listView = getListView();
        LayoutInflater inflater = LayoutInflater.from(this);
        ConversationHeaderView headerView = (ConversationHeaderView)
                inflater.inflate(R.layout.conversation_header, listView, false);
        headerView.bind(getString(R.string.new_message),
                getString(R.string.create_new_message));
        listView.addHeaderView(headerView, null, true);

        listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        listView.setOnKeyListener(mThreadListKeyListener);

        initListAdapter();

        mTitle = getString(R.string.app_label);

        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean checkedMessageLimits = mPrefs.getBoolean(CHECKED_MESSAGE_LIMITS, false);
        if (DEBUG) Log.v(TAG, "checkedMessageLimits: " + checkedMessageLimits);
        if (!checkedMessageLimits || DEBUG) {
            runOneTimeStorageLimitCheckForLegacyMessages();
        }
       
    }
    
    private boolean firstParandroidLaunch(){
        try{
            openFileInput(FIRST_LAUNCH_FILE);
        }catch(Exception e){
            Log.e(TAG,e.getMessage());
            
            // if this is the first launch, we touch the file
            try {
                FileOutputStream out = openFileOutput(FIRST_LAUNCH_FILE, MODE_PRIVATE);
                out.write("".getBytes());
                out.flush();
                out.close();
            } catch (Exception fileError){
                // don't report exception
            }
            
            return true;
        }
        return false;
    }

    private final ConversationListAdapter.OnContentChangedListener mContentChangedListener =
        new ConversationListAdapter.OnContentChangedListener() {
        public void onContentChanged(ConversationListAdapter adapter) {
            startAsyncQuery();
        }
    };

    private void initListAdapter() {
        mListAdapter = new ConversationListAdapter(this, null);
        mListAdapter.setOnContentChangedListener(mContentChangedListener);
        setListAdapter(mListAdapter);
        getListView().setRecyclerListener(mListAdapter);
    }

    /**
     * Checks to see if the number of MMS and SMS messages are under the limits for the
     * recycler. If so, it will automatically turn on the recycler setting. If not, it
     * will prompt the user with a message and point them to the setting to manually
     * turn on the recycler.
     */
    public synchronized void runOneTimeStorageLimitCheckForLegacyMessages() {
        if (Recycler.isAutoDeleteEnabled(this)) {
            if (DEBUG) Log.v(TAG, "recycler is already turned on");
            // The recycler is already turned on. We don't need to check anything or warn
            // the user, just remember that we've made the check.
            markCheckedMessageLimit();
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                if (Recycler.checkForThreadsOverLimit(ConversationList.this)) {
                    if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit TRUE");
                    // Dang, one or more of the threads are over the limit. Show an activity
                    // that'll encourage the user to manually turn on the setting. Delay showing
                    // this activity until a couple of seconds after the conversation list appears.
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            Intent intent = new Intent(ConversationList.this,
                                    WarnOfStorageLimitsActivity.class);
                            startActivity(intent);
                        }
                    }, 2000);
                } else {
                    if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit silently turning on recycler");
                    // No threads were over the limit. Turn on the recycler by default.
                    runOnUiThread(new Runnable() {
                        public void run() {
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putBoolean(MessagingPreferenceActivity.AUTO_DELETE, true);
                            editor.commit();
                        }
                    });
                }
                // Remember that we don't have to do the check anymore when starting MMS.
                runOnUiThread(new Runnable() {
                    public void run() {
                        markCheckedMessageLimit();
                    }
                });
            }
        }).start();
    }

    /**
     * Mark in preferences that we've checked the user's message limits. Once checked, we'll
     * never check them again, unless the user wipe-data or resets the device.
     */
    private void markCheckedMessageLimit() {
        if (DEBUG) Log.v(TAG, "markCheckedMessageLimit");
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(CHECKED_MESSAGE_LIMITS, true);
        editor.commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Handle intents that occur after the activity has already been created.
        privateOnStart();
    }

    @Override
    protected void onStart() {
        super.onStart();

        MessagingNotification.cancelNotification(getApplicationContext(),
                SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);

        Conversation.cleanup(this);

        DraftCache.getInstance().addOnDraftChangedListener(this);

        // We used to refresh the DraftCache here, but
        // refreshing the DraftCache each time we go to the ConversationList seems overly
        // aggressive. We already update the DraftCache when leaving CMA in onStop() and
        // onNewIntent(), and when we delete threads or delete all in CMA or this activity.
        // I hope we don't have to do such a heavy operation each time we enter here.

        privateOnStart();

        // we invalidate the contact cache here because we want to get updated presence
        // and any contact changes. We don't invalidate the cache by observing presence and contact
        // changes (since that's too untargeted), so as a tradeoff we do it here.
        // If we're in the middle of the app initialization where we're loading the conversation
        // threads, don't invalidate the cache because we're in the process of building it.
        // TODO: think of a better way to invalidate cache more surgically or based on actual
        // TODO: changes we care about
        if (!Conversation.loadingThreads()) {
            Contact.invalidateCache();
        }
    }

    protected void privateOnStart() {
        startAsyncQuery();
    }


    @Override
    protected void onStop() {
        super.onStop();

        DraftCache.getInstance().removeOnDraftChangedListener(this);
        mListAdapter.changeCursor(null);
    }

    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        // Run notifyDataSetChanged() on the main thread.
        mQueryHandler.post(new Runnable() {
            public void run() {
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("onDraftChanged: threadId=" + threadId + ", hasDraft=" + hasDraft);
                }
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void startAsyncQuery() {
        try {
            setTitle(getString(R.string.refreshing));
            setProgressBarIndeterminateVisibility(true);

            Conversation.startQueryForAll(mQueryHandler, THREAD_LIST_QUERY_TOKEN);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    /**
     * TODO: Icons
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        if(MessageEncryptionFactory.hasKeypair(this)){
        	menu.add(0, MENU_GENERATE_KEYPAIR, 0, R.string.menu_generate_new_keypair).setIcon(R.drawable.ic_generate_keypair);

            menu.add(0, MENU_SEND_PUBLIC_KEY, 0, R.string.menu_send_public_key).setIcon(
        		R.drawable.ic_send_public_key);
        
            menu.add(0, MENU_CHANGE_PASSWORD, 0, R.string.menu_change_password).setIcon(
        		R.drawable.ic_change_password);
        }else
        	menu.add(0, MENU_GENERATE_KEYPAIR, 0, R.string.menu_generate_keypair).setIcon(R.drawable.ic_generate_keypair);

        menu.add(0, MENU_MANAGE_PUBLIC_KEYS, 0, R.string.menu_manage_public_keys).setIcon(
        		R.drawable.ic_manage_public_keys);

        menu.add(0, MENU_HELP, 0, R.string.menu_help).setIcon(
                R.drawable.ic_gallery_video_overlay);
        
        menu.add(0, MENU_ABOUT, 0, R.string.menu_about).setIcon(
                R.drawable.ic_generate_keypair);

        if (mListAdapter.getCount() > 0) {
            menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setIcon(
                    android.R.drawable.ic_menu_delete);
        }

        menu.add(0, MENU_SEARCH, 0, android.R.string.search_go).
            setIcon(android.R.drawable.ic_menu_search).
            setAlphabeticShortcut(android.app.SearchManager.MENU_KEY);

        menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences).setIcon(
                android.R.drawable.ic_menu_preferences);

        return true;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null /*appData*/, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
	        case MENU_COMPOSE_NEW:
	            createNewMessage();
	            break;
	        case MENU_GENERATE_KEYPAIR:
	        	generateKeypair();
	            break;
	        case MENU_SEND_PUBLIC_KEY:
	        	sendPublicKey();
	            break;
	        case MENU_MANAGE_PUBLIC_KEYS:
	        	managePublicKeys();
	            break;
	        case MENU_CHANGE_PASSWORD:
	        	changePassword();
	        	break;
	        case MENU_ABOUT:
	        	Intent aboutIntent = new Intent(this, AboutActivity.class);
	        	startActivity(aboutIntent);
	        	break;
	        case MENU_HELP:
	        	Intent helpIntent = new Intent(this, HelpActivity.class);
	        	startActivity(helpIntent);
	        	break;
            case MENU_SEARCH:
                onSearchRequested();
                break;
            case MENU_DELETE_ALL:
                // The invalid threadId of -1 means all threads here.
                confirmDeleteThread(-1L, mQueryHandler);
                break;
            case MENU_PREFERENCES: {
                Intent intent = new Intent(this, MessagingPreferenceActivity.class);
                startActivityIfNeeded(intent, -1);
                break;
            }
            default:
                return true;
        }
        return false;
    }
    
    private String oldPassword = null;
    private void changePassword(){
    	if(MessageEncryptionFactory.isAuthenticating()) return;
    	
    	if(!MessageEncryptionFactory.isAuthenticated()){
    		MessageEncryptionFactory.setAuthenticating(true);
    		oldPassword = MessageEncryptionFactory.getPassword();
    		
    		Intent intent = new Intent(this, AuthenticateActivity.class);
        	startActivityForResult(intent, REQUEST_CODE_CHANGE_PASSWORD_AUTHENTICATE);
        	
        	return;
        }else{
        	MessageEncryptionFactory.setAuthenticating(true);
    		oldPassword = MessageEncryptionFactory.getPassword();
    		
    		Intent intent = new Intent(this, SetPasswordActivity.class);
        	startActivityForResult(intent, REQUEST_CODE_CHANGE_PASSWORD);
        	
        	return;
        }
    }
    
    private void doChangePassword(){
    	if(!MessageEncryptionFactory.isAuthenticated()) return;
    	
    	String newPassword = MessageEncryptionFactory.getPassword();

    	if(newPassword.equals(oldPassword)){
    		oldPassword = null;
			Toast.makeText(this, R.string.password_not_changed, Toast.LENGTH_LONG).show();
			return;
    	}
    	
    	MessageEncryptionFactory.setPassword(oldPassword);
    	
    	try {
			PrivateKey pk = MessageEncryptionFactory.getPrivateKey(this);
			MessageEncryptionFactory.setPassword(newPassword);
			MessageEncryptionFactory.writePrivateKey(this, pk);
			oldPassword = null;
			Toast.makeText(this, R.string.successfully_changed_password, Toast.LENGTH_LONG).show();
    	}catch(Exception e){
			Toast.makeText(this, R.string.failed_changing_password, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
    }
    
    private void sendPublicKey(){
    	Intent intent = new Intent(this, SendPublicKeyActivity.class);
        startActivity(intent);
    }
    
    private void managePublicKeys(){
    	Intent intent = new Intent(this, ManagePublicKeysActivity.class);
        startActivity(intent);
    }
    
    private void generateKeypair(){
    	if(!MessageEncryptionFactory.hasKeypair(this)){
    		generateFirstKeypair();
    		return;
    	}
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(getText(R.string.generate_new_keypair_dialog))
    			.setTitle(getText(R.string.generate_keypair_title))
		   	 	.setCancelable(false)
    	       .setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   try{
    	        		   generateNewKeypair();
    	        	   }catch(Exception e){
    	        		   e.printStackTrace();
    	        		   Toast.makeText(ConversationList.this, R.string.generated_keypair_failure, Toast.LENGTH_LONG);
    	        	   }
    	           }
    	       })
    	       .setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	
    	AlertDialog alert = builder.create();
    	alert.show();
    }
    
    
    private PrivateKey oldPrivateKey = null;
    private boolean hasSetNewPassword = false;
	
    private void generateNewKeypair() {
		if(MessageEncryptionFactory.isAuthenticating()) return;
        
    	if(!MessageEncryptionFactory.isAuthenticated()){
    		MessageEncryptionFactory.setAuthenticating(true);
    		
    		Intent intent = new Intent(this, AuthenticateActivity.class);
        	startActivityForResult(intent, REQUEST_CODE_SET_PASSWORD);
        	
        	return;
        }
    	
    	if(!hasSetNewPassword){
 		   	try {
				oldPrivateKey = MessageEncryptionFactory.getPrivateKey(ConversationList.this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
    		MessageEncryptionFactory.setAuthenticating(true);
    		
    		Intent intent = new Intent(this, SetPasswordActivity.class);
        	startActivityForResult(intent, REQUEST_CODE_SET_NEW_PASSWORD);
        	
        	return;
    	}else hasSetNewPassword = false;
    	
    	doGenerateKeypair();
		try {
			encryptBackward();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	
	private void generateFirstKeypair(){
		if(!MessageEncryptionFactory.isAuthenticated()){
    		MessageEncryptionFactory.setAuthenticating(true);
    		
    		Intent intent = new Intent(this, SetPasswordActivity.class);
        	startActivityForResult(intent, REQUEST_CODE_SET_FIRST_PASSWORD);
        	
        	return;
        }
		
		doGenerateKeypair();
	}
	
	
	private void doGenerateKeypair(){
		ProgressDialog generateKeypairProgressDialog = ProgressDialog.show(ConversationList.this, "", getString(R.string.generating_keypair), true);
		Toast generateKeypairErrorToast = Toast.makeText(ConversationList.this, R.string.generated_keypair_failure, Toast.LENGTH_SHORT);
		
		try{
			MessageEncryptionFactory.generateKeyPair(ConversationList.this);
			generateKeypairProgressDialog.dismiss();
		} catch (Exception e) {
			String message = "Error generating keypair: " + e.getMessage();
			Log.e(TAG, message);
			e.printStackTrace();
			
			generateKeypairProgressDialog.dismiss();
			generateKeypairErrorToast.show();             	 
		}
	}
	
//	public Cursor backwardCursor;
//	public ProgressDialog progressDialog;
	private String[] BACKWARD_PROJECTION = new String[] { Inbox._ID, Inbox.ADDRESS, Inbox.BODY };
	private void encryptBackward() throws Exception{		
		Uri uriSms = Uri.parse("content://sms");
		String selection = Inbox.TYPE + "='" + MessageItem.MESSAGE_TYPE_PARANDROID_INBOX + 
			"' OR " + Inbox.TYPE + "='" + MessageItem.MESSAGE_TYPE_PARANDROID_OUTBOX + "'";
		
		final Cursor backwardCursor = getContentResolver().query(uriSms, BACKWARD_PROJECTION, selection, null, null);
		if(!backwardCursor.moveToFirst()){
			Log.i(TAG, "backward: No messages");
			return;
		}
		
		int numMessages = backwardCursor.getCount();
		Log.i(TAG, "Re-encrypting " + numMessages + " messages");
		
		final ProgressDialog progressDialog = new ProgressDialog(ConversationList.this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMessage(getString(R.string.reencrypting_messages));
		progressDialog.setCancelable(false);
		progressDialog.setMax(numMessages);
		progressDialog.show();
		
		new Thread(new Runnable() {
			public void run(){
				do{
					try{
						String address = backwardCursor.getString(backwardCursor.getColumnIndex(Inbox.ADDRESS));
		
						Log.i(TAG, "backward: oldpriv: " + oldPrivateKey);
						Log.i(TAG, "backward: address: " + address);
						
						String body = backwardCursor.getString(backwardCursor.getColumnIndex(Inbox.BODY));
						Log.i(TAG, "backward: oldbody: " + body);
						
						String clearBody = MessageEncryption.decrypt(ConversationList.this, oldPrivateKey, address, Base64.decode(body));
						Log.i(TAG, "backward: clear: " + clearBody);
						
						String newBody = new String(Base64.encode(MessageEncryption.encrypt(ConversationList.this, address, clearBody)));
						Log.i(TAG, "backward: newbody: " + newBody);
						
						backwardCursor.updateString(backwardCursor.getColumnIndex(Inbox.BODY), newBody);
					}catch(Exception e){
						e.printStackTrace();
					}
					progressDialog.incrementProgressBy(1);
				}while(backwardCursor.moveToNext());

				progressDialog.dismiss();
				
				backwardCursor.commitUpdates();
				backwardCursor.close();
				oldPrivateKey = null;
			}
		}).start();
	}
	
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == RESULT_CANCELED) return;
    	
    	switch(requestCode){
    	case REQUEST_CODE_SET_PASSWORD:
    		generateNewKeypair();
    		break;
    		
    	case REQUEST_CODE_SET_FIRST_PASSWORD:
    		generateFirstKeypair();
    		break;

    	case REQUEST_CODE_CHANGE_PASSWORD_AUTHENTICATE:
    		changePassword();
    		break;

    	case REQUEST_CODE_CHANGE_PASSWORD:
    		doChangePassword();
    		break;
    		
    	case REQUEST_CODE_SET_NEW_PASSWORD:
    		hasSetNewPassword = true;
    		generateNewKeypair();
    		break;
    		
    	default:
    		Log.i(TAG, "Unknown requestCode for onActivityResult: " + requestCode);
    		break;
    	}
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "onListItemClick: position=" + position + ", id=" + id);
        }

        if (position == 0) {
            createNewMessage();
        } else if (v instanceof ConversationHeaderView) {
            ConversationHeaderView headerView = (ConversationHeaderView) v;
            ConversationHeader ch = headerView.getConversationHeader();
            openThread(ch.getThreadId());
        }
    }

    private void createNewMessage() {
        startActivity(ComposeMessageActivity.createIntent(this, 0));
    }

    private void openThread(long threadId) {
        startActivity(ComposeMessageActivity.createIntent(this, threadId));
    }

    public static Intent createAddContactIntent(String address) {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        return intent;
    }

    private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener =
        new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            Cursor cursor = mListAdapter.getCursor();
            if (cursor.getPosition() < 0) {
                return;
            }
            Conversation conv = Conversation.from(ConversationList.this, cursor);
            ContactList recipients = conv.getRecipients();
            menu.setHeaderTitle(recipients.formatNames(","));

            AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (info.position > 0) {
                menu.add(0, MENU_VIEW, 0, R.string.menu_view);

                // Only show if there's a single recipient
                if (recipients.size() == 1) {
                    // do we have this recipient in contacts?
                    if (recipients.get(0).existsInDatabase()) {
                        menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact);
                    } else {
                        menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts);
                    }
                }
                menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
            }
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Cursor cursor = mListAdapter.getCursor();
        if (cursor.getPosition() >= 0) {
            Conversation conv = Conversation.from(ConversationList.this, cursor);
            long threadId = conv.getThreadId();
            switch (item.getItemId()) {
            case MENU_DELETE: {
                confirmDeleteThread(threadId, mQueryHandler);
                break;
            }
            case MENU_VIEW: {
                openThread(threadId);
                break;
            }
            case MENU_VIEW_CONTACT: {
                Contact contact = conv.getRecipients().get(0);
                Intent intent = new Intent(Intent.ACTION_VIEW, contact.getUri());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                break;
            }
            case MENU_ADD_TO_CONTACTS: {
                String address = conv.getRecipients().get(0).getNumber();
                startActivity(createAddContactIntent(address));
                break;
            }
            default:
                break;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // We override this method to avoid restarting the entire
        // activity when the keyboard is opened (declared in
        // AndroidManifest.xml).  Because the only translatable text
        // in this activity is "New Message", which has the full width
        // of phone to work with, localization shouldn't be a problem:
        // no abbreviated alternate words should be needed even in
        // 'wide' languages like German or Russian.

        super.onConfigurationChanged(newConfig);
        if (DEBUG) Log.v(TAG, "onConfigurationChanged: " + newConfig);
    }

    /**
     * Start the process of putting up a dialog to confirm deleting a thread,
     * but first start a background query to see if any of the threads or thread
     * contain locked messages so we'll know how detailed of a UI to display.
     * @param threadId id of the thread to delete or -1 for all threads
     * @param handler query handler to do the background locked query
     */
    public static void confirmDeleteThread(long threadId, AsyncQueryHandler handler) {
        Conversation.startQueryHaveLockedMessages(handler, threadId,
                HAVE_LOCKED_MESSAGES_TOKEN);
    }

    /**
     * Build and show the proper delete thread dialog. The UI is slightly different
     * depending on whether there are locked messages in the thread(s) and whether we're
     * deleting a single thread or all threads.
     * @param listener gets called when the delete button is pressed
     * @param deleteAll whether to show a single thread or all threads UI
     * @param hasLockedMessages whether the thread(s) contain locked messages
     * @param context used to load the various UI elements
     */
    public static void confirmDeleteThreadDialog(final DeleteThreadListener listener,
            boolean deleteAll,
            boolean hasLockedMessages,
            Context context) {
        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView)contents.findViewById(R.id.message);
        msg.setText(deleteAll
                ? R.string.confirm_delete_all_conversations
                        : R.string.confirm_delete_conversation);
        final CheckBox checkbox = (CheckBox)contents.findViewById(R.id.delete_locked);
        if (!hasLockedMessages) {
            checkbox.setVisibility(View.GONE);
        } else {
            listener.setDeleteLockedMessage(checkbox.isChecked());
            checkbox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.setDeleteLockedMessage(checkbox.isChecked());
                }
            });
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.confirm_dialog_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
        .setCancelable(true)
        .setPositiveButton(R.string.delete, listener)
        .setNegativeButton(R.string.no, null)
        .setView(contents)
        .show();
    }

    private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL: {
                        long id = getListView().getSelectedItemId();
                        if (id > 0) {
                            confirmDeleteThread(id, mQueryHandler);
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    };

    public static class DeleteThreadListener implements OnClickListener {
        private final long mThreadId;
        private final AsyncQueryHandler mHandler;
        private final Context mContext;
        private boolean mDeleteLockedMessages;

        public DeleteThreadListener(long threadId, AsyncQueryHandler handler, Context context) {
            mThreadId = threadId;
            mHandler = handler;
            mContext = context;
        }

        public void setDeleteLockedMessage(boolean deleteLockedMessages) {
            mDeleteLockedMessages = deleteLockedMessages;
        }

        public void onClick(DialogInterface dialog, final int whichButton) {
            MessageUtils.handleReadReport(mContext, mThreadId,
                    PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ, new Runnable() {
                public void run() {
                    int token = DELETE_CONVERSATION_TOKEN;
                    if (mThreadId == -1) {
                        Conversation.startDeleteAll(mHandler, token, mDeleteLockedMessages);
                        DraftCache.getInstance().refresh();
                    } else {
                        Conversation.startDelete(mHandler, token, mDeleteLockedMessages,
                                mThreadId);
                        DraftCache.getInstance().setDraftState(mThreadId, false);
                    }
                }
            });
        }
    }

    private final class ThreadListQueryHandler extends AsyncQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case THREAD_LIST_QUERY_TOKEN:
                mListAdapter.changeCursor(cursor);
                setTitle(mTitle);
                setProgressBarIndeterminateVisibility(false);
                break;

            case HAVE_LOCKED_MESSAGES_TOKEN:
                long threadId = (Long)cookie;
                confirmDeleteThreadDialog(new DeleteThreadListener(threadId, mQueryHandler,
                        ConversationList.this), threadId == -1,
                        cursor != null && cursor.getCount() > 0,
                        ConversationList.this);
                break;

            default:
                Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token) {
            case DELETE_CONVERSATION_TOKEN:
                // Make sure the conversation cache reflects the threads in the DB.
                Conversation.init(ConversationList.this);

                // Update the notification for new messages since they
                // may be deleted.
                MessagingNotification.updateNewMessageIndicator(ConversationList.this);
                // Update the notification for failed messages since they
                // may be deleted.
                MessagingNotification.updateSendFailedNotification(ConversationList.this);

                // Make sure the list reflects the delete
                startAsyncQuery();

                onContentChanged();
                break;
            }
        }
    }

    private void log(String format, Object... args) {
        String s = String.format(format, args);
        Log.d(TAG, "[" + Thread.currentThread().getId() + "] " + s);
    }
}
