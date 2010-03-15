package org.parandroid.encryption;

import org.parandroid.encoding.Base64Coder;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.InputStream;

/**
 * Encrypt and decrypt sms messages using RSA private/public key encryption.
 * 
 * The public key of the sender/receiver is stored in a file xxxxxxxx.pub (where xxx is the senders/receivers number)
 * @author erikw
 *
 */
public abstract class SmsEncrypter {

	public static final String TAG = "org.parandroid.encryption.SmsEncrypter";
	public static final String MY_PRIVATE_KEY_NAME = "self.priv";
	public static final String MY_PUBLIC_KEY_NAME = "self.pub";
	public static final String PDMMS_HEADER = "%pdm%";
	
	
    public static String encrypt(String text, String[] dests, Context context) {
    	if(dests.length == 1){
	    	try {
				RsaEncryption rsa = new RsaEncryption();

				String destPublicKeyName = dests[0].concat(".pub");
				InputStream privateKey = context.openFileInput(MY_PRIVATE_KEY_NAME);
				InputStream destPublicKey = context.openFileInput(destPublicKeyName);
			
				byte[] message = text.getBytes();
				byte[] encryptedMsg = rsa.encrypt(privateKey, destPublicKey, message);
	
				return PDMMS_HEADER.concat(new String(Base64Coder.encode(encryptedMsg)));
	    	} catch (Exception e){
	    		Log.e(TAG, "Error while encrypting: ".concat(e.getMessage()));
	    		
	    		//FIXME: this is not safe!
	    		return text;
	    	}
    	}
    	
    	// we cant encrypt if we have more, or less than 1 destination
    	Log.e(TAG, "Error while encrypting: ".concat("Unable to encrypt with " + dests.length + " destinations"));
    	return text;
    }
    
    public static String decrypt(String text, String sender, Context context){
    	try {
    		// If the text doesn't start with the PDMMS_HEADER, the text is not encrypted so we don't need to decrypt it
    		if(!text.startsWith(PDMMS_HEADER)) return text;
    		
			RsaEncryption rsa = new RsaEncryption();

			String senderPublicKeyName = sender.concat(".pub");
			InputStream privateKey = context.openFileInput(MY_PRIVATE_KEY_NAME);
			InputStream senderPublicKey = context.openFileInput(senderPublicKeyName);
		
			byte[] message = Base64Coder.decode(text.substring(PDMMS_HEADER.length()));
			byte[] decryptedMsg = rsa.decrypt(privateKey, senderPublicKey, message);
			
			return new String(decryptedMsg);
    	} catch (Exception e){
    		Log.e(TAG, "Error while decrypting: ");
    		
    		//FIXME: this is not safe!
    		return text;
    	}
    }
}
