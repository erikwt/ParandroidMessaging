package org.parandroid.encryption;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.crypto.*;

import org.bouncycastle.util.encoders.Base64Encoder;
import org.parandroid.compression.Huffman;
import org.parandroid.encoding.Base64Coder;

import android.content.Context;
import android.util.Log;


public abstract class DHAESEncryption {
	
	public static String encrypt(Context context, String[] numbers, String text){
		try {
			// FIXME: this does only work with one recipient
			String number = numbers[0];
			SecretKey secretKey = DHAESKeyFactory.getSecretKey(context, number);
			
			Cipher cipher = Cipher.getInstance(DHAESKeyFactory.SECRET_KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // FIXME: needs to be refactored, especially the huffMan class.
            byte[] bytes = text.getBytes();
            
//            byte[] bytesCompressed = new byte[bytes.length];
//            Huffman huffmanCompression = new Huffman();
//            int[] treeout = new int[Huffman.ALPHABETSIZE];
//			int compressedLength = huffmanCompression.compress(bytes, bytesCompressed, treeout);
//            
//			Log.i("MSG", text);
//			Log.i("MSG COMPRESSED", bytesCompressed.toString());
//			Log.i("MSG TREE", Arrays.toString(treeout));
//			Log.i("MSG COMPRESSED ENCRYPTED", DHAESKeyFactory.ENCRYPTED_MSG_HEADER.concat(Base64Coder.encodeString(Arrays.toString(treeout).concat(bytesCompressed.toString()))));

			byte[] cipherText = cipher.doFinal(bytes);
			
			return DHAESKeyFactory.ENCRYPTED_MSG_HEADER.concat(new String(Base64Coder.encode(cipherText)));
		} catch (Exception e){
			e.printStackTrace();
			
			return text;
		}
	}
	
	public static String encryptForOutbox(Context context, String text){
		try {
			// FIXME: this does not add security, since public key is public (doh!)
			SecretKey secretKey = DHAESKeyFactory.getSecretKey(context, "self");
			
			Cipher cipher = Cipher.getInstance(DHAESKeyFactory.SECRET_KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] bytes = text.getBytes();
			byte[] cipherText = cipher.doFinal(bytes);
			
			return DHAESKeyFactory.ENCRYPTED_MSG_HEADER.concat(new String(Base64Coder.encode(cipherText)));
		} catch (Exception e){
			e.printStackTrace();
			
			return text;
		}
	}
	
	
	public static String decrypt(Context context, String number, String cipherText){
		try {
			if(!cipherText.startsWith(DHAESKeyFactory.ENCRYPTED_MSG_HEADER)) return cipherText;
			
			SecretKey secretKey = DHAESKeyFactory.getSecretKey(context, number);
			
			Cipher cipher = Cipher.getInstance(DHAESKeyFactory.SECRET_KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
			
//            byte[] bytesCompressed = Base64Coder.decode(cipherText.substring(DHAESKeyFactory.ENCRYPTED_MSG_HEADER.length()));
//            byte[] bytes = null;
//            Huffman huffmanCompression = new Huffman();
//            huffmanCompression.uncompress(bytesCompressed, bytes, treein);
            byte[] bytes = Base64Coder.decode(cipherText.substring(DHAESKeyFactory.ENCRYPTED_MSG_HEADER.length()));
            byte[] text = cipher.doFinal(bytes);

			return new String(text);
		} catch (Exception e){
			e.printStackTrace();

			return cipherText;
		}
	}
}
