package org.parandroid.encryption;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
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
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.bouncycastle.util.encoders.Base64;
import org.parandroid.sms.transaction.MultipartDataMessage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public abstract class MessageEncryptionFactory {

	public static final short PUBLIC_KEY_PORT = 31337;
	public static final short ENCRYPTED_MESSAGE_PORT = 31338;

	private static final String TAG = "Parandroid MessageEncryptionFactory";

	protected static final String PUBLIC_KEY_FILENAME = "self.pub";
	protected static final String PRIVATE_KEY_FILENAME = "self.priv";
	
	protected static final String KEY_EXCHANGE_PROTOCOL = "DH";
	protected static final String ENCRYPTION_ALGORITHM = "AES";
	protected static final String PRIVATE_KEY_ENCRYPTION_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
	
	protected static final int SECRET_KEY_LENGTH = 24;

	public static final String PUBLIC_KEY_DATABASE = "keyring";
	public static final String PUBLIC_KEY_TABLE = "publickeys";
	
	protected static final byte[] PRIVATE_KEY_ENCRYPTION_SALT = {
        (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
        (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
    };
	
	
	public static String password = null;
	private static boolean isAuthenticating = false;
	
	/**
	 * Diffie Hellman parameters P and G. Diffie-Hellman establishes a shared secret that can be 
	 * used for secret communications by exchanging data over a public network.
	 * 
	 * @see http://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange
	 */
	private static final BigInteger G = new BigInteger("2");
	private static final BigInteger P = new BigInteger("9289986504414644358097953743225136849757607686008190055292886218364533315560867997658782522702215600927448569831496872430051834791379663335482694978773549");
	
	
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
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws InvalidKeyException 
	 * @throws InvalidKeySpecException 
	*/
    public static KeyPair generateKeyPair(Context context) throws Exception {  	
    	
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_EXCHANGE_PROTOCOL);
        DHParameterSpec dhSpec = new DHParameterSpec(P, G);
        keyGen.initialize(dhSpec);
        KeyPair keyPair = keyGen.generateKeyPair();

        // save the keys in the file system
		FileOutputStream pubOut = context.openFileOutput(PUBLIC_KEY_FILENAME, Context.MODE_PRIVATE);
		FileOutputStream privOut = context.openFileOutput(PRIVATE_KEY_FILENAME, Context.MODE_PRIVATE);
		
		pubOut.write(keyPair.getPublic().getEncoded());
		privOut.write(encryptPrivateKey(keyPair.getPrivate()));
		
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
     * Get a list of the currently stored public keys, excluding your own. Hashmap with number and description
     * 
     * @param context
     * @return Public key list
     */
    public static HashMap<Integer, String> getPublicKeyList(Context context, boolean accepted){
    	HashMap<Integer, String> publicKeys = new HashMap<Integer, String>();
    	
    	SQLiteDatabase keyRing = openKeyring(context);
    	Cursor c = keyRing.query(PUBLIC_KEY_TABLE, null, "accepted=" + (accepted ? "1" : "0"), null, null, null, "_ID DESC");
    	
    	while(c.moveToNext()){
    		int id = c.getInt(c.getColumnIndex("_ID"));
    		String number = c.getString(c.getColumnIndex("number"));
    		
    		publicKeys.put(id, number);
    	}
    	
    	c.close();
    	keyRing.close();
    	
    	return publicKeys;
    }
    
    public static HashMap<Integer, String> getPublicKeyList(Context context){
    	return getPublicKeyList(context, true);
    }
    
    
    public static ArrayList<String> getPublicKeys(Context context){
    	ArrayList<String> publicKeys = new ArrayList<String>();
    	
    	SQLiteDatabase keyRing = openKeyring(context);
    	Cursor c = keyRing.query(PUBLIC_KEY_TABLE, null, "accepted=1", null, null, null, null);
    	
    	while(c.moveToNext()){
    		String number = c.getString(c.getColumnIndex("number"));
    		publicKeys.add(number);
    	}
    	
    	c.close();
    	keyRing.close();
    	
    	return publicKeys;
    }
    

    /**
     * Delete a public key
     * 
     * @param context
     * @param number
     * @return Boolean successful
     */
    public static boolean deletePublicKey(Context context, int id){
        SQLiteDatabase keyRing = openKeyring(context);
        int num = keyRing.delete(PUBLIC_KEY_TABLE, "_ID=" + id, null);
        
        keyRing.close();
        return num > 0;
    }
    
    
    /**
     * Get the local filename of a stored public key.
     * Trailing country-codes will be omitted
     * 
     * @param number
     * @return filename
     */
    public static String getPublicKeyFilename(Context context, String number){
    	ArrayList<String> publicKeys = getPublicKeys(context);
    	for(String publicKey : publicKeys){
    		if(PhoneNumberUtils.compare(number, publicKey)){
    			Log.v(TAG, "Public key exists for number '" + number + "'; '" + publicKey + "'");
    			return publicKey;
    		}
    	}

		Log.v(TAG, "No public key for '" + number + "'");
    	return number;
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
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     * @throws NoSuchPaddingException 
     * @throws InvalidKeyException 
     */
    public static PrivateKey getPrivateKey(Context context) throws Exception {
    	
    	if(!isAuthenticated()) return null;
    	
		byte[] keyBytes = getKeyFileBytes(context, PRIVATE_KEY_FILENAME);
    	byte[] pk = decryptPrivateKey(keyBytes);
		
    	PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pk);
		KeyFactory kf = KeyFactory.getInstance(KEY_EXCHANGE_PROTOCOL);
		PrivateKey privateKey = kf.generatePrivate(spec);
    	
    	return privateKey;
    }
    
    
    public static void writePrivateKey(Context context, PrivateKey pk) throws Exception {
    	FileOutputStream privOut = context.openFileOutput(PRIVATE_KEY_FILENAME, Context.MODE_PRIVATE);
    	
    	privOut.write(encryptPrivateKey(pk));
		
		privOut.flush();
        privOut.close();
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
    	byte[] keyBytes = null;
    	
    	SQLiteDatabase keyRing = openKeyring(context);
    	Cursor c = keyRing.query(PUBLIC_KEY_TABLE, null, "accepted=1", null, null, null, null);
    	
    	while(c.moveToNext()){
    		String n = c.getString(c.getColumnIndex("number"));
    		if(PhoneNumberUtils.compare(number, n)){
    			String publicKey = c.getString(c.getColumnIndex("publicKey"));
    			keyBytes = Base64.decode(publicKey);
    			break;
    		}
    	}
    	
    	c.close();
    	keyRing.close();
    	
    	if(keyBytes == null) return null;
    	
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFact = KeyFactory.getInstance(KEY_EXCHANGE_PROTOCOL);
        PublicKey publicKey = keyFact.generatePublic(x509KeySpec);
    	
    	return publicKey;
    }
    
    
    public static int getPublicKeyId(Context context, String number) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{    	
    	SQLiteDatabase keyRing = openKeyring(context);
    	Cursor c = keyRing.query(PUBLIC_KEY_TABLE, null, "accepted=1", null, null, null, null);
    	
    	while(c.moveToNext()){
    		String n = c.getString(c.getColumnIndex("number"));
    		if(PhoneNumberUtils.compare(number, n)){
    	    	int id = c.getInt(c.getColumnIndex("_ID"));
    			c.close();
    	    	keyRing.close();
    			return id;
    		}
    	}
    	
    	return -1;
    }
    
    
    /**
     * Save a public key
     * 
     * @param context
     * @param sender
     * @param key
     * @throws Exception
     */
//    public static void savePublicKey(Context context, String sender, byte[] key) throws Exception{
//    	String publicKeyFilename = getPublicKeyFilename(context, sender);
//    	
//		FileOutputStream out = context.openFileOutput(publicKeyFilename, Context.MODE_PRIVATE);
//		out.write(key);
//		out.flush();
//		out.close();
//    }
    
    
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
    	ArrayList<String> publicKeys = getPublicKeys(context);
    	for(String publicKey : publicKeys){
    		if(PhoneNumberUtils.compare(number, publicKey)){
    			Log.v(TAG, "Public key exists for number '" + number + "'; '" + publicKey + "'");
    			return true;
    		}
    	}

		Log.v(TAG, "No public key for number '" + number + "'");
    	return false;
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
	
	
	public static void sendPublicKey(Context context, String number) throws IOException{
		byte[] publicKey = getOwnPublicKey(context);
		MultipartDataMessage m = new MultipartDataMessage(MultipartDataMessage.TYPE_PUBLIC_KEY, number, publicKey, null, null);
		m.send();
	}
	
    
    /**
     * Disables the password, forcing the user to insert it again when trying
     * to read encrypted messages.
     */
    public static void forgetPassword(){
    	password = null;
    	Log.i(TAG, "Disabled the password");
    }
    
    public static boolean isAuthenticated(){
    	return password != null;
    }
    
    public static void setPassword(String password){
    	MessageEncryptionFactory.password = password;
    }
    
    public static String getPassword(){
    	return MessageEncryptionFactory.password;
    }
    
    public static boolean isAuthenticating(){
    	return isAuthenticating;
    }
    
    public static void setAuthenticating(boolean auth){
    	isAuthenticating = auth;
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
    
    private static Cipher getCipher(int mode) throws Exception {
    	if(!isAuthenticated()) return null;

    	char[] passwordChars = new char[password.length()];
    	password.getChars(0, password.length(), passwordChars, 0);
    	
    	PBEParameterSpec pbeParamSpec = new PBEParameterSpec(MessageEncryptionFactory.PRIVATE_KEY_ENCRYPTION_SALT, 20);
    	PBEKeySpec pbeKeySpec = new PBEKeySpec(passwordChars);
    	
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance(MessageEncryptionFactory.PRIVATE_KEY_ENCRYPTION_ALGORITHM);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        Cipher cipher = Cipher.getInstance(MessageEncryptionFactory.PRIVATE_KEY_ENCRYPTION_ALGORITHM);
    	cipher.init(mode, pbeKey, pbeParamSpec);
    	
    	return cipher;
    }
    
    private static byte[] encryptPrivateKey(PrivateKey pk) throws Exception {
    	Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
        return cipher.doFinal(pk.getEncoded());
    }
    
    private static byte[] decryptPrivateKey(byte[] cipherText) throws Exception {
    	Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
        return cipher.doFinal(cipherText);
    }
    
    public static SQLiteDatabase openKeyring(Context context){
    	SQLiteDatabase keyRing = context.openOrCreateDatabase(PUBLIC_KEY_DATABASE, Context.MODE_PRIVATE, null);
    	keyRing.execSQL("CREATE TABLE IF NOT EXISTS " + PUBLIC_KEY_TABLE + 
    			" (_ID INTEGER PRIMARY KEY, " +
    			"number VARCHAR, " +
    			"publicKey VARCHAR, " +
    			"accepted BOOLEAN, " +
    			"received TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    	
    	return keyRing;
    }
    
    public static String stripHeader(String message){
    	if(!message.startsWith(MultipartDataMessage.MESSAGE_HEADER) && !message.startsWith(MultipartDataMessage.PUBLIC_KEY_HEADER))
    		return message;
    	
    	String header = message.startsWith(MultipartDataMessage.MESSAGE_HEADER) ? MultipartDataMessage.MESSAGE_HEADER : MultipartDataMessage.PUBLIC_KEY_HEADER;
    	int lastSeparator = message.indexOf(MultipartDataMessage.HEADER_SEPERATOR, header.length());
    	
    	if(lastSeparator == -1 || message.length() <= lastSeparator)
    		return message.substring(header.length());
    	
    	return message.substring(lastSeparator + 1);
    }
    
    public static int getProcolVersion(String message){
    	if(!message.startsWith(MultipartDataMessage.MESSAGE_HEADER) && !message.startsWith(MultipartDataMessage.PUBLIC_KEY_HEADER))
    		return -1;
    	
    	String header = message.startsWith(MultipartDataMessage.MESSAGE_HEADER) ? MultipartDataMessage.MESSAGE_HEADER : MultipartDataMessage.PUBLIC_KEY_HEADER;
    	
    	int metadataStart = header.length();
		int metadataEnd = message.indexOf(MultipartDataMessage.HEADER_SEPERATOR, metadataStart);
		if(metadataEnd == -1)
    		return -1;
		
		try{
			int protocolVersion = Integer.parseInt(message.substring(metadataStart, metadataEnd)) >> 8;
	    	return protocolVersion;
		}catch(Exception e){
			Log.e(TAG, "Corrupted message, no protocol version: " + message);
			return -1;
		}
    }
}
