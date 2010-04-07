package org.parandroid.encryption;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;

import org.parandroid.sms.ui.InsertPasswordActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public abstract class MessageEncryptionFactory {

	public static final short PUBLIC_KEY_PORT = 31337;
	public static final short ENCRYPTED_MESSAGE_PORT = 31338;

	private static final String TAG = "Parandroid MessageEncryptionFactory";

	protected static final String PUBLIC_KEY_FILENAME = "self.pub";
	protected static final String PRIVATE_KEY_FILENAME = "self.priv";
	protected static final String PUBLIC_KEY_SUFFIX = ".pub";
	
	protected static final String KEY_EXCHANGE_PROTOCOL = "DH";
	protected static final String ENCRYPTION_ALGORITHM = "AES";
	
	public static String password = null;
	public static boolean passwordDialogPending = false;
	
	/**
	 * Diffie Hellman parameters P and G. Diffie-Hellman establishes a shared secret that can be 
	 * used for secret communications by exchanging data over a public network.
	 * 
	 * @see http://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange
	 */
	private static final BigInteger G = new BigInteger("2");
	private static final BigInteger P = new BigInteger("23182371893214465678917756685478547584564464564564564561231237867864534675815");
	
	
	/**
	* Generate a keypair using diffie - hellman
	* The keys are saved in the filesystem
	* 
	* @param context
	* @return keyPair
	* 
	* @throws NoSuchAlgorithmException
	* @throws IOException 
	* @throws InvalidAlgorithmParameterException 
	*/
    public static KeyPair generateKeyPair(Context context) throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException {  	
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_EXCHANGE_PROTOCOL);
        DHParameterSpec dhSpec = new DHParameterSpec(P, G);
        keyGen.initialize(dhSpec);
        KeyPair keyPair = keyGen.generateKeyPair();

        // save the keys in the file system
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
    
    
    /**
     * Compute a shared secret for the ENCRYPTION_ALGORITHM. This should always be computed, and may never be stored.
     * 
     * @param privateKey
     * @param publicKey
     * @return secretKey
     * 
     * @throws NoSuchAlgorithmException
     * @throws GeneralSecurityException
     */
    public static SecretKey generateSecretKey(PrivateKey privateKey, PublicKey publicKey) throws NoSuchAlgorithmException, GeneralSecurityException {
        if(privateKey == null)
        	throw new GeneralSecurityException("Missing private key");
        
        if(publicKey == null)
        	throw new GeneralSecurityException("Missing public key");
    	
    	KeyAgreement ka = KeyAgreement.getInstance(KEY_EXCHANGE_PROTOCOL);
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        
        SecretKey secretKey = ka.generateSecret(ENCRYPTION_ALGORITHM);
        return secretKey; 
    }
    
    
    /**
     * Get a list of the currently stored public keys, excluding your own
     * 
     * @param context
     * @return Public key list
     */
    public static ArrayList<String> getPublicKeys(Context context){
    	ArrayList<String> publicKeys = new ArrayList<String>();
    	
    	for(String f : context.getFilesDir().list()){
    		if(f.endsWith(PUBLIC_KEY_SUFFIX) && !PUBLIC_KEY_FILENAME.equals(f))
    			publicKeys.add(f.substring(0, f.length() - PUBLIC_KEY_SUFFIX.length()));
    	}
    	
    	return publicKeys;
    }
    

    /**
     * Delete a public key
     * 
     * @param context
     * @param number
     * @return Boolean successful
     */
    public static boolean deletePublicKey(Context context, String number){
        File pk = new File(context.getFilesDir(), MessageEncryptionFactory.getPublicKeyFilename(number));
        if(!pk.exists()){
            Log.e(TAG, "Delete: File does not exist: " + pk.getAbsoluteFile());
            return false;
        }
        return pk.delete();
    }
    
    
    /**
     * Get the local filename of a stored public key.
     * Trailing country-codes will be omitted
     * 
     * @param number
     * @return filename
     */
    public static String getPublicKeyFilename(String number){
    	try{
	    	if(number.startsWith("00")) 	number = number.substring(4);
	    	else if(number.startsWith("0"))	number = number.substring(3);
	    	else if(number.startsWith("+")) number = number.substring(3);
    	}catch(Exception e){
    		Log.e(TAG, "Unknown number format detected. Number: " + number);
    	}
    	
    	return number.concat(PUBLIC_KEY_SUFFIX);
    }
    
    
    /**
     * Get a stored private key in PKCS8 format (Used to carry private certificate keypairs (encrypted or unencrypted)
     * @see http://en.wikipedia.org/wiki/PKCS
     * @see http://tools.ietf.org/html/rfc5208
     * 
     * @param context
     * @param keyFilename
     * @return private key
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey getPrivateKey(Context context) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
    	if(passwordInMemory()){
    		byte[] keyBytes = getKeyFileBytes(context, PRIVATE_KEY_FILENAME);
        	
        	PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
    		KeyFactory kf = KeyFactory.getInstance(KEY_EXCHANGE_PROTOCOL);
    		PrivateKey privateKey = kf.generatePrivate(spec);
        	
        	return privateKey;
    	} 
    	
    	if(!passwordDialogPending){
    		passwordDialogPending = true;
    		
    		Log.i(TAG, "The password is disabled. User needs to enter it again");
    		
    		Intent intent = new Intent(context, InsertPasswordActivity.class);
            context.startActivity(intent);
    	} 
    	
    	return null;
    }
    
    
    /**
     * Get a stored public key in x509 format
     * @see http://en.wikipedia.org/wiki/X.509
     * 
     * @param context
     * @param keyFilename
     * @return public key
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PublicKey getPublicKey(Context context, String number) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
    	byte[] keyBytes = getKeyFileBytes(context, getPublicKeyFilename(number));
    	
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFact = KeyFactory.getInstance(KEY_EXCHANGE_PROTOCOL);
        PublicKey publicKey = keyFact.generatePublic(x509KeySpec);
    	
    	return publicKey;
    }
    
    
    /**
     * Save a public key
     * 
     * @param context
     * @param sender
     * @param key
     * @throws Exception
     */
    public static void savePublicKey(Context context, String sender, byte[] key) throws Exception{
    	String publicKeyFilename = getPublicKeyFilename(sender);
    	
		FileOutputStream out = context.openFileOutput(publicKeyFilename, Context.MODE_PRIVATE);
		out.write(key);
		out.flush();
		out.close();
    }
    
    
    /**
     * Get your own public key in bytes
     * 
     * @param context
     * @return your own public key
     * @throws IOException
     */
    public static byte[] getOwnPublicKey(Context context) throws IOException{
    	return getKeyFileBytes(context, PUBLIC_KEY_FILENAME);
    }
	
    /**
     * Check if we have the public key for the given number
     * 
     * @param context
     * @param number
     * @return boolean
     */
    public static boolean hasPublicKey(Context context, String number){
    	try{
			context.openFileInput(getPublicKeyFilename(number));
		}catch(Exception e){
			Log.e(TAG,e.getMessage());
			return false;
		}
		return true;
    }
    
    
    /**
     * Check if the current device has a keypair
     * 
     * @param context
     * @return boolean
     */
	public static boolean hasKeypair(Context context){
		try{
			context.openFileInput(PUBLIC_KEY_FILENAME);
			context.openFileInput(PRIVATE_KEY_FILENAME);
		}catch(Exception e){
			Log.e(TAG,e.getMessage());
			return false;
		}
		return true;
	}
    
	
	/**
	 * Get the raw bytes of a file
	 * 
	 * @param context
	 * @param keyFilename
	 * @return
	 * @throws IOException
	 */
    private static byte[] getKeyFileBytes(Context context, String keyFilename) throws IOException{
    	InputStream key = context.openFileInput(keyFilename);
    	
    	DataInputStream dis = new DataInputStream(key);
		byte[] keyBytes = new byte[dis.available()];
		
		dis.readFully(keyBytes);
		dis.close();
		
		return keyBytes;
    }
    
    /**
     * Disables the password, forcing the user to insert it again when trying
     * to read encrypted messages.
     */
    public static void forgetPassword(){
    	password = null;
    	Log.i(TAG, "Disabled the password");
    }
    
    public static boolean passwordInMemory(){
    	return password != null;
    }
    
    public static void setPassword(String password){
    	MessageEncryptionFactory.password = password;
    }
    
}
