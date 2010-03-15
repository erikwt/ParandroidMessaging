package org.parandroid.encryption;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;

import android.content.Context;
import android.util.Log;


/**
 * Encryption:
 * 1: encrypt with your own private key
 * 2: then encrypt with his public key. 
 * 
 * Decryption:
 * 1: decrypt with your own private key >> nobody could have opened/altered the message
 * 2: decrypt with his public key >> you are sure that alice is the sender (since only she can encrypt with her private key)
 * 
 *  
 * @author erikw
 *
 */
public class RsaEncryption {

	
	// FIXME: Bad.
	public static boolean hasKeypair(String number, Context context){
		try{
			context.openFileInput(number.concat(".pub"));
		}catch(Exception e){
			Log.e("RSA",e.getMessage());
			for(int i = 0; i < e.getStackTrace().length; i++)
				Log.e("RSA",e.getStackTrace()[i].toString());
			return false;
		}
		return true;
	}

	// FIXME: This is hacky, nasty and slow
	public static boolean hasKeypair(Context context){
		try{
			context.openFileInput(SmsEncrypter.MY_PRIVATE_KEY_NAME);
			context.openFileInput(SmsEncrypter.MY_PUBLIC_KEY_NAME);
		}catch(Exception e){
			Log.e("RSA",e.getMessage());
			for(int i = 0; i < e.getStackTrace().length; i++)
				Log.e("RSA",e.getStackTrace()[i].toString());
			return false;
		}
		return true;
	}
	
	public byte[] encrypt(String privateKeyFileName, String publicKeyFileName, byte[] bytes){
		try {
			PrivateKey privateKey = getPrivateKey(privateKeyFileName);
			PublicKey publicKey = getPublicKey(publicKeyFileName);
			
			// first encrypt with your private key, then with his public key
//			return encrypt(publicKey, encrypt(privateKey, bytes));
			return encrypt(privateKey, bytes);
		} catch (Exception e){
			e.printStackTrace();
			
			return null;
		}
		
	}
	
	
	public byte[] encrypt(InputStream privateKeyStream, InputStream publicKeyStream, byte[] bytes){
		try {
			PrivateKey privateKey = getPrivateKey(privateKeyStream);
			PublicKey publicKey = getPublicKey(publicKeyStream);
					
			// first encrypt with your private key, then with his public key
//			return encrypt(publicKey, encrypt(privateKey, bytes));
			return encrypt(privateKey, bytes);
		} catch (Exception e){
			e.printStackTrace();
			
			return null;
		}
		
	}
	
	
	/**
	 * Encrypt a message using a key
	 * 
	 * @param key
	 * @param msg
	 * @return
	 */
	private byte[] encrypt(Key key, byte[] bytes){
		try {			
			byte[] cipherText = new byte[0];
			
			// get an RSA cipher object & encrypt the plaintext using the key
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			
			for(int i = 0; i < bytes.length; i += 50){
				int count = bytes.length - i <= 50 ? bytes.length - i : 50;
				byte[] bytesubset = new byte[count];
				for(int j = 0; j < count; j++) bytesubset[j] = bytes[i+j];
				cipherText = concatByteA(cipherText, cipher.doFinal(bytesubset));
			}
			
			return cipherText;
		} catch (Exception e){
			e.printStackTrace();
			
			return null;
		}
	}
	
	private byte[] concatByteA(byte[] a, byte[] b){
		byte[] result = new byte[a.length + b.length];
		for(int i = 0; i < a.length; i++) result[i] = a[i];
		for(int i = 0; i < b.length; i++) result[i + a.length] = b[i];
		return result;
	}
	
	public byte[] decrypt(InputStream privateKeyStream, InputStream publicKeyStream, byte[] bytes){
		try {
			PrivateKey privateKey = getPrivateKey(privateKeyStream);
			PublicKey publicKey = getPublicKey(publicKeyStream);
					
			// first encrypt with your private key, then with his public key
//			return decrypt(publicKey, decrypt(privateKey, bytes));
			return decrypt(publicKey, bytes);
		} catch (Exception e){
			e.printStackTrace();
			
			return null;
		}
		
	}
	
	public byte[] decrypt(String privateKeyFileName, String publicKeyFileName, byte[] bytes){
		try {
			PrivateKey privateKey = getPrivateKey(privateKeyFileName);
			PublicKey publicKey = getPublicKey(publicKeyFileName);
					
			// first encrypt with your private key, then with his public key
//			return decrypt(publicKey, decrypt(privateKey, bytes));
			return decrypt(publicKey, bytes);
		} catch (Exception e){
			e.printStackTrace();
			
			return null;
		}
		
	}
	
	private byte[] decrypt(Key key, byte[] bytes){
		try {
			byte[] cipherText = new byte[0];
			
			// get an RSA cipher object & decrypt the msg using the key
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, key);
			
			for(int i = 0; i < bytes.length; i += 64){
				int count = bytes.length - i <= 64 ? bytes.length - i : 64;
				byte[] bytesubset = new byte[count];
				for(int j = 0; j < count; j++) bytesubset[j] = bytes[i+j];
				cipherText = concatByteA(cipherText, cipher.doFinal(bytesubset));
			}		
						
			return cipherText;
		} catch (Exception e){
			e.printStackTrace();
			
			return null;
		}
	}
	
	public static PrivateKey getPrivateKey(String filename) throws Exception {
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		
		return getPrivateKey(fis);
	}
	
	public static PrivateKey getPrivateKey(InputStream fis) throws Exception{
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[dis.available()];
		
		dis.readFully(keyBytes);
		dis.close();
		
		KeyFactory kf = KeyFactory.getInstance("RSA");
		EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		
		return kf.generatePrivate(spec);
	}
	
	public static PublicKey getPublicKey(String filename)
		throws Exception {
		
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		
		return getPublicKey(fis);
	}
	
	public static PublicKey getPublicKey(InputStream fis) throws Exception{
		DataInputStream dis = new DataInputStream(fis);
		byte[] keyBytes = new byte[dis.available()];
		
		dis.readFully(keyBytes);
		dis.close();
		
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		
		return kf.generatePublic(spec);
	}
}
