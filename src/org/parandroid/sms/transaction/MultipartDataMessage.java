package org.parandroid.sms.transaction;

import java.util.HashMap;

import org.parandroid.encoding.Base64Coder;

import android.app.PendingIntent;
import android.telephony.gsm.SmsManager;

/**
 * Send Parandroid messages using the Parandroid Messaging protocol.
 *
 * The message is base64-encoded and then split in parts with the maximum length of MAX_BYTES.
 * Each part is then sent appended to a 'header', containing:
 * 8 bits: Parandroid Messaging protocol version (for backward compatibility)
 * 4 bits: Current message count
 * 4 bits: Total message count
 * 8 bits: Message id
 */
public class MultipartDataMessage {
	
	private static final String TAG = "Parandroid MultipartDataMessageSender";
    private static final int ID_LENGTH = 255;
    private static final int PROTOCOL_VERSION = 0;

	public static short MAX_BYTES = 140;
	
    private static HashMap<String, Integer> messageIds = new HashMap<String, Integer>();

	public static void send(SmsManager smsManager, String dest, short port, byte[] message, PendingIntent sentIntent, PendingIntent deliveryIntent){
		message = new String(Base64Coder.encode(message)).getBytes();
		int rest = message.length % MAX_BYTES;
		int numMessages = message.length / MAX_BYTES + (rest == 0 ? 0 : 1);
        
        int messageId = 0;
		if(messageIds.containsKey(dest)) messageId = (messageIds.get(dest) + 1) % ID_LENGTH;
        messageIds.put(dest, messageId);
		
		for(int i = 0; i < numMessages; i++){
			boolean last = i == numMessages - 1;
			
			byte[] part = new byte[3 + ((!last || rest == 0) ? MAX_BYTES : rest)];
			part[0] = new Integer(PROTOCOL_VERSION).byteValue();
			part[1] = new Integer((numMessages - i - 1) << 4 | numMessages).byteValue();
			part[2] = new Integer(messageId).byteValue();
			
			System.arraycopy(message, i * MAX_BYTES, part, 3, part.length - 3);
			
			smsManager.sendDataMessage(dest, null, port, part, sentIntent, deliveryIntent);
		}
	}
}
