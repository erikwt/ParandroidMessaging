package org.parandroid.encryption;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import android.content.Context;

public class RsaKeyPairGenerator {

	public static final String PUBLIC_KEY_FILENAME = "self.pub";
	public static final String PRIVATE_KEY_FILENAME = "self.priv";
	
	/**
	* Generate key which contains a pair of private and public key using 1024 bytes
	* @return key pair
	* @throws NoSuchAlgorithmException
	 * @throws IOException 
	*/
    public static KeyPair generateKeyPair(Context context) throws NoSuchAlgorithmException, IOException {
	    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	    keyGen.initialize(512);
	    KeyPair keyPair = keyGen.generateKeyPair();
	    
		FileOutputStream pubOut = context.openFileOutput(PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE);
		FileOutputStream privOut = context.openFileOutput(PRIVATE_KEY_FILENAME, Context.MODE_PRIVATE);
		
		pubOut.write(keyPair.getPublic().getEncoded());
		privOut.write(keyPair.getPrivate().getEncoded());
		
		pubOut.flush();
		privOut.flush();
        
		pubOut.close();
        privOut.close();
        
	    return keyPair;
	}
	
}
