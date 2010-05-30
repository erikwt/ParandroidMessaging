package org.parandroid.sms.transaction;

import org.bouncycastle.util.encoders.Base64;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.telephony.SmsManager;

/**
 * Send Parandroid messages using the Parandroid Messaging protocol.
 *
 * The message is base64-encoded and then split in parts with the maximum
 * length of a text message minus the header length for the first message.
 * 
 * The header consists of a identifier string, as defined in this class with some metadata:
 * 8 bits: Parandroid Messaging protocol version (for backward compatibility)
 * 8 bits: Compression type (0 is none): For future use (different compression tables etc)
 */
public class MultipartDataMessage {
	
	private static final String TAG = "Parandroid MultipartDataMessageSender";
	private static final String MESSAGE_HEADER		= "$pdm$";
	private static final String PUBLIC_KEY_HEADER	= "$pdpk$";
    private static final int PROTOCOL_VERSION = 0;
	
	public static final short TYPE_MESSAGE		= 0;
	public static final short TYPE_PUBLIC_KEY	= 1;
	
	private short type;
	private ArrayList<String> messageParts;
	private SmsManager smsManager;
	private String destination;
	private ArrayList<PendingIntent> sentIntents;
	private ArrayList<PendingIntent> deliveryIntents;
	
	private int compressionType = 0; // For future use.
    
    public MultipartDataMessage(short type, String destination, byte[] message, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents){
    	if(type != TYPE_MESSAGE && type != TYPE_PUBLIC_KEY)
    		throw new IllegalArgumentException("Unknown message-type");
    	
    	smsManager = SmsManager.getDefault();
    	
    	this.type = type;
    	this.destination = destination;
    	this.sentIntents = sentIntents;
    	this.deliveryIntents = deliveryIntents;
    	
    	setMessage(message);
    }
    
    private void setMessage(byte[] m){
    	String message = new String(Base64.encode(m));
    	
		String header = type == TYPE_MESSAGE ? MESSAGE_HEADER : PUBLIC_KEY_HEADER;
		String metadata = Integer.toString(PROTOCOL_VERSION << 8 | compressionType);
		
		messageParts = smsManager.divideMessage(header + metadata + message);
    }
    
    public int getPartCount(){
    	return messageParts.size();
    }
        
	public void send(){
		smsManager.sendMultipartTextMessage(destination, null, messageParts, sentIntents, deliveryIntents);
	}
}
