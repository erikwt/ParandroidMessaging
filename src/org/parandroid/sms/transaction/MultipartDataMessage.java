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
 * - Parandroid Messaging protocol version (for backward compatibility)
 * - Compression type (0 is none): For future use (different compression tables etc)
 */
public class MultipartDataMessage {
	
	public static final String TAG = "Parandroid MultipartDataMessageSender";
    public static final int PROTOCOL_VERSION = 0;
	
	public static final short TYPE_MESSAGE		= 0;
	public static final short TYPE_PUBLIC_KEY	= 1;

	public static final String HEADER_SEPERATOR		= "$";
	public static final String PROTOCOL_SEPERATOR	= ",";
	public static final String MESSAGE_HEADER		= "$pdm$";
	public static final String PUBLIC_KEY_HEADER	= "$pdpk$";
	
	private short type;
	private ArrayList<String> messageParts;
	private String extraMessage;
	private SmsManager smsManager;
	private String destination;
	private ArrayList<PendingIntent> sentIntents;
	private ArrayList<PendingIntent> deliveryIntents;
	
	private int compressionType = 0; // For future use.
    
	public MultipartDataMessage(short type, String destination, byte[] message, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents){
		init(type, destination, message, sentIntents, deliveryIntents, null);
	}
	
	public MultipartDataMessage(short type, String destination, byte[] message, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, String extraMessage){
		init(type, destination, message, sentIntents, deliveryIntents, extraMessage);
	}
	
    private void init(short type, String destination, byte[] message, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents, String extraMessage){
    	if(type != TYPE_MESSAGE && type != TYPE_PUBLIC_KEY)
    		throw new IllegalArgumentException("Unknown message-type");
    	
    	smsManager = SmsManager.getDefault();
    	
    	this.type = type;
    	this.destination = destination;
    	this.sentIntents = sentIntents;
    	this.deliveryIntents = deliveryIntents;
    	this.extraMessage = extraMessage;
    	
    	setMessage(message);
    }
    
    private void setMessage(byte[] m){
    	String message = new String(Base64.encode(m));
    	
		String header = type == TYPE_MESSAGE ? MESSAGE_HEADER : PUBLIC_KEY_HEADER;
		String metadata = Integer.toString(PROTOCOL_VERSION);
		
		if(compressionType != 0)
			metadata += PROTOCOL_SEPERATOR + Integer.toString(compressionType);
		
		if(extraMessage != null)
			metadata += PROTOCOL_SEPERATOR + extraMessage;
		
		messageParts = smsManager.divideMessage(header + metadata + HEADER_SEPERATOR + message);
    }
    
    public int getPartCount(){
    	return messageParts.size();
    }
        
	public void send(){
		smsManager.sendMultipartTextMessage(destination, null, messageParts, sentIntents, deliveryIntents);
	}
}
