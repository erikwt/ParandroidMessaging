package org.parandroid.encryption;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import org.parandroid.encoding.Base64Coder;
import org.parandroid.sms.ui.ConversationList;
//import org.parandroid.mms.ui.SendPublicKeyActivity;

import android.content.Context;
import android.util.Log;

public abstract class DHAESKeyFactory {

	public static final String PUBLIC_KEY_FILENAME = "self.pub";
	public static final String PRIVATE_KEY_FILENAME = "self.priv";
	
	public static final String SECRET_KEY_SUFFIX = ".secret";	// shared secret key name is 0612345678.secret
	public static final String PUBLIC_KEY_SUFFIX = ".pub";		// public key name is 0612345678.pub
	
	public static final String PUBLIC_KEY_ALGORITHM = "DH";		// Important: it's not possible to change the algorithm by only changing this value
	public static final String SECRET_KEY_ALGORITHM = "AES";
	
	public static final String SHARE_PUBLIC_KEY_HEADER = "%pdpub%";
	public static final String ENCRYPTED_MSG_HEADER = "%pdmsg%";
	
	public static final int KEY_SIZE = 256;
	
	public static final BigInteger G_128_BIT = new BigInteger("36633455825311975040466006092289991605");
	public static final BigInteger P_128_BIT = new BigInteger("26364004858991988642926226571728229659");
	public static final int L_128_BIT = 0;
	
	public static final BigInteger G_256_BIT = new BigInteger("56049311486568487092072397528376649227987297532466944895394922797714993166476");
	public static final BigInteger P_256_BIT = new BigInteger("23182371893214465678917756685478547584564464564564564561231237867864534675815");
	public static final int L_256_BIT = 0;
	
	
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
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance(PUBLIC_KEY_ALGORITHM);
        DHParameterSpec dhSpec = new DHParameterSpec(P_128_BIT, G_128_BIT);
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
     * Generate an AES shared, secrey key, using the private and public keys given
     * 
     * @param privateKey
     * @param publicKey
     * @return secreyKey
     * 
     * @throws NoSuchAlgorithmException
     * @throws GeneralSecurityException
     */
    public static SecretKey generateSecretKey(PrivateKey privateKey, PublicKey publicKey) throws NoSuchAlgorithmException, GeneralSecurityException {
//    	byte[] publicKeyBytes = publicKey.getEncoded();
//
//    	// Convert the public key bytes into a PublicKey object
//        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
//        KeyFactory keyFact = KeyFactory.getInstance("DH");
//        publicKey = keyFact.generatePublic(x509KeySpec);
    
        // Prepare to generate the secret key with the private key and public key of the other party
        KeyAgreement ka = KeyAgreement.getInstance(PUBLIC_KEY_ALGORITHM);
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        
        // Generate the secret key
        SecretKey secretKey = ka.generateSecret(SECRET_KEY_ALGORITHM);
        
        return secretKey;
    }
    
    public static boolean isPublicKey(String message){
    	return message.startsWith(SHARE_PUBLIC_KEY_HEADER);
    }
    
    /**
     * Generate random values used to generate private and public keys 
     * 
     * @return comma-separated string of 3 values:
     * The first number is the prime modulus P.
     * The second number is the base generator G.
     * The third number is bit size of the random exponent L.
     */
    private static String genDhParams() {
        try {
            // Create the parameter generator for a 1024-bit DH key pair
            AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance(PUBLIC_KEY_ALGORITHM);
            paramGen.init(KEY_SIZE);
    
            // Generate the parameters
            AlgorithmParameters params = paramGen.generateParameters();
            DHParameterSpec dhSpec
                = (DHParameterSpec)params.getParameterSpec(DHParameterSpec.class);

            
            Log.i("P", dhSpec.getP().toString());
            Log.i("G", dhSpec.getG().toString());
            Log.i("L", Integer.toString(dhSpec.getL()));
            
            return ""+dhSpec.getP()+","+dhSpec.getG()+","+dhSpec.getL();
        } catch (Exception e) {
        	Log.e("genDH", e.getMessage());
        	return null;
        } 
        
    }

    private static String getSecretKeyFilename(String number){
    	return number.concat(SECRET_KEY_SUFFIX);
    }
    
    public static String getPublicKeyFilename(String number){
    	return number.concat(PUBLIC_KEY_SUFFIX);
    }
    
    private static byte[] getKeyFileBytes(Context context, String keyFilename) throws IOException{
    	InputStream key = context.openFileInput(keyFilename);
    	
    	DataInputStream dis = new DataInputStream(key);
		byte[] keyBytes = new byte[dis.available()];
		
		dis.readFully(keyBytes);
		dis.close();
		
		return keyBytes;
    }
    
    public static PrivateKey getPrivateKey(Context context, String keyFilename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
    	byte[] keyBytes = getKeyFileBytes(context, keyFilename);
    	
    	EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance(PUBLIC_KEY_ALGORITHM);
		PrivateKey privateKey = kf.generatePrivate(spec);
		
//    	// Convert the public key bytes into a PublicKey object
//        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
//        KeyFactory keyFact = KeyFactory.getInstance("DH");
//        PrivateKey privateKey = keyFact.generatePrivate(x509KeySpec);
    	
    	return privateKey;
    }
    
    public static PublicKey getPublicKey(Context context, String keyFilename) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
    	byte[] keyBytes = getKeyFileBytes(context, keyFilename);
    	
    	// Convert the public key bytes into a PublicKey object
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFact = KeyFactory.getInstance(PUBLIC_KEY_ALGORITHM);
        PublicKey publicKey = keyFact.generatePublic(x509KeySpec);
    	
    	return publicKey;
    }
    
    public static void savePublicKey(Context context, String sender, String msg) throws IOException{
    	String publicKeyFilename = getPublicKeyFilename(sender);
    	
        // save the key in the file system
		FileOutputStream out = context.openFileOutput(publicKeyFilename, Context.MODE_PRIVATE);
		out.write(Base64Coder.decode(msg.substring(SHARE_PUBLIC_KEY_HEADER.length())));
		out.flush();
		out.close();
    }
    
    public static String getPublicKeyToSend(Context context) throws IOException{
    	byte[] keyBytes = getKeyFileBytes(context, PUBLIC_KEY_FILENAME);
    	
    	return SHARE_PUBLIC_KEY_HEADER.concat(new String(Base64Coder.encode(keyBytes)));
    }
    
    public static SecretKey getSecretKey(Context context, String number) throws GeneralSecurityException, IOException{
    	PrivateKey privateKey = getPrivateKey(context, PRIVATE_KEY_FILENAME);
    	Log.i("PUBLIC KEY NAME", getPublicKeyFilename(number));
    	PublicKey publicKey = getPublicKey(context, getPublicKeyFilename(number));
    	
    	return generateSecretKey(privateKey, publicKey);
    }
	
	// FIXME: This is hacky, nasty and slow
	public static boolean hasKeypair(Context context){
		try{
			context.openFileInput(PUBLIC_KEY_FILENAME);
			context.openFileInput(PRIVATE_KEY_FILENAME);
		}catch(Exception e){
			Log.e("DH AES",e.getMessage());
			for(int i = 0; i < e.getStackTrace().length; i++)
				Log.e("DH AES",e.getStackTrace()[i].toString());
			return false;
		}
		return true;
	}
	
	// FIXME: This is hacky, nasty and slow
	public static boolean hasSecretKey(String number, Context context){
		try{
			context.openFileInput(number.concat(SECRET_KEY_SUFFIX));
		}catch(Exception e){
			Log.e("DH AES",e.getMessage());
			for(int i = 0; i < e.getStackTrace().length; i++)
				Log.e("DH AES",e.getStackTrace()[i].toString());
			return false;
		}
		return true;
	}
}
