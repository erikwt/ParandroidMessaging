package org.parandroid.sms.ui;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.R;
import org.parandroid.sms.data.ContactList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class HelpActivity extends Activity implements OnClickListener {
	
    private ViewFlipper viewFlipper;  
    private Button nextButton;
    private Button previousButton;
    private Button generateNewKeypairButton;
    private ProgressBar progressBar;
    private float oldTouchValue;
    
    private static final String TAG = "ParandroidHelp";
    
    public static final int REQUEST_CODE_SET_FIRST_PASSWORD	= 0;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.help_activity);
        setTitle(R.string.help_title);
        
        viewFlipper = (ViewFlipper) findViewById(R.id.help_flipper);
        
        // if we already have a keypair, remove the generate keypair child
        if(MessageEncryptionFactory.hasKeypair(this)){
        	View generateNewKeypair = viewFlipper.findViewById(R.id.generate_new_keypair);
        	viewFlipper.removeView(generateNewKeypair);
        } else {
        	generateNewKeypairButton = (Button) findViewById(R.id.button_generate_new_keypair);
        	generateNewKeypairButton.setOnClickListener(this);
        }

        nextButton = (Button) findViewById(R.id.button_next);
        nextButton.setOnClickListener(this);
        
        previousButton = (Button) findViewById(R.id.button_previous);
        previousButton.setOnClickListener(this);
        previousButton.setVisibility(View.INVISIBLE);
        
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        onViewChanged();
    }
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == RESULT_CANCELED) return;
    	
    	switch(requestCode){
	    	case REQUEST_CODE_SET_FIRST_PASSWORD:
	    		generateFirstKeypair();
	    		break;
    		
    	default:
    		Log.i(TAG, "Unknown requestCode for onActivityResult: " + requestCode);
    		break;
    	}
    }
    

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldTouchValue = event.getX();
                return true;
                
            case MotionEvent.ACTION_UP:
                float currentX = event.getX();
                
                if(oldTouchValue > currentX) {
                	showNext();
                    return true;
                } else if(oldTouchValue < currentX){
                	showPrevious();
                    return true;
                }
        }
        
        return false;
    }
	
    public void onClick(View v) {
		if(v.equals(nextButton)) {
			showNext();
		} else if(v.equals(previousButton)) {
			showPrevious();
		} else if(v.equals(generateNewKeypairButton)) {
			generateFirstKeypair();
		}
		
	}
    
    private void onViewChanged(){
    	updateProgressBar();
    	
    }
	
    private void showNext(){
		int childIndex = viewFlipper.getDisplayedChild();

		// show next, only if this is not the last child
		if(childIndex < viewFlipper.getChildCount()-1){
			viewFlipper.setInAnimation(inFromRightAnimation());
	        viewFlipper.setOutAnimation(outToLeftAnimation());
	        viewFlipper.showNext();
	        
	        int newChildIndex = viewFlipper.getDisplayedChild();
	        
	        // remove the next button if we are in the last child 
	        if(newChildIndex == viewFlipper.getChildCount()-1){
	        	nextButton.setVisibility(View.INVISIBLE);
	        }
	        
	        // add the next button if we came from the first child
	        if(childIndex == 0){
	        	previousButton.setVisibility(View.VISIBLE);
	        }
	        
	        onViewChanged();
		}
	}
	
    private void showPrevious(){
		int childIndex = viewFlipper.getDisplayedChild();
		
		// show previous, only if this is not the first child
		if(childIndex > 0){
			viewFlipper.setInAnimation(inFromLeftAnimation());
	        viewFlipper.setOutAnimation(outToRightAnimation());
	        viewFlipper.showPrevious();
	        
	        int newChildIndex = viewFlipper.getDisplayedChild();
	        
	        // remove the previous button if we are in the first child 
	        if(newChildIndex == 0){
	        	previousButton.setVisibility(View.INVISIBLE);
	        }
	        
	        // add the next button if we came from the last child
	        if(childIndex == viewFlipper.getChildCount()-1){
	        	nextButton.setVisibility(View.VISIBLE);
	        }
	        
	        onViewChanged();
		}
		
	}
	
    private Animation inFromRightAnimation() {
		Animation inFromRight = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, +1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
		
		inFromRight.setDuration(350);
		inFromRight.setInterpolator(new AccelerateInterpolator());
		return inFromRight;
	}

	private Animation outToLeftAnimation() {
		Animation outtoLeft = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, -1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
		
		outtoLeft.setDuration(350);
		outtoLeft.setInterpolator(new AccelerateInterpolator());
		return outtoLeft;
	}

	private Animation inFromLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, -1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
		
		inFromLeft.setDuration(350);
		inFromLeft.setInterpolator(new AccelerateInterpolator());
		return inFromLeft;
	}

	private Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, +1.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f,
			Animation.RELATIVE_TO_PARENT, 0.0f);
		
		outtoRight.setDuration(350);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}
	
	private void updateProgressBar(){
		float percentage = (float) viewFlipper.getDisplayedChild() / (viewFlipper.getChildCount()-1) * 100;
		progressBar.setProgress(Math.round(percentage));
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
		ProgressDialog generateKeypairProgressDialog = ProgressDialog.show(this, "", getString(R.string.generating_keypair), true);
		Toast generateKeypairErrorToast = Toast.makeText(this, R.string.generated_keypair_failure, Toast.LENGTH_SHORT);
		
        AlertDialog.Builder generateKeypairSuccessDialogBuilder = new AlertDialog.Builder(this);
    	generateKeypairSuccessDialogBuilder.setMessage(getText(R.string.generated_keypair_success))
    			.setTitle(getText(R.string.generate_keypair_title))
    			.setCancelable(false)
    	        .setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
	        	    	Intent intent = new Intent(HelpActivity.this, SendPublicKeyActivity.class);
	        	        startActivity(intent);
    	           }
    	       })
    	       .setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	
    	AlertDialog generateKeypairSuccessDialog = generateKeypairSuccessDialogBuilder.create();
		
		try{
			MessageEncryptionFactory.generateKeyPair(this);
			generateKeypairProgressDialog.dismiss();
			generateKeypairSuccessDialog.show();
			showNext();
		} catch (Exception e) {
			String message = "Error generating keypair: " + e.getMessage();
			Log.e(TAG, message);
			e.printStackTrace();
			
			generateKeypairProgressDialog.dismiss();
			generateKeypairErrorToast.show();             	 
		}
	}

}

//- generate key pair                 >> generate key now? yes/no
//- password                          >> insert pass
//- exchange pub key with friends     >> want to send to friends now? yes/no
//- send and receive encrypted msgs
//- want to know more?
