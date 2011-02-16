package org.parandroid.sms.ui;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.sms.ParandroidSmsApp;

import org.parandroid.sms.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class PrintPublicKeyActivity extends Activity {

	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;
	
	private static final String TAG = "Parandroid QRPrinter";
	public static final String KEY_NUMBER_SEPARATOR = "@";
	public static final String PHONE_NUMBER_INTENT_EXTRA = "phone_number_for_pub_key";
		
	private Button doneButton;
	private ImageView qrImage; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.print_public_key);
		
		qrImage = (ImageView) findViewById(R.id.qr_image);
		doneButton = (Button) findViewById(R.id.button_done);
		
		doneButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(!getIntent().hasExtra(PHONE_NUMBER_INTENT_EXTRA)){
			Toast.makeText(getBaseContext(), R.string.missing_phone_number, Toast.LENGTH_LONG).show();
			finish();
		}
		
		String phoneNumber = getIntent().getStringExtra(PHONE_NUMBER_INTENT_EXTRA);		
		
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth();
		int height = display.getHeight();
		
		try {
			byte[] publicKey = MessageEncryptionFactory.getOwnPublicKey(getApplicationContext());
			String encodedPublicKey = new String(Base64.encode(publicKey));
			
			String keyAndNumber = encodedPublicKey + KEY_NUMBER_SEPARATOR + phoneNumber;
			
			Bitmap code = encodeAsBitmap(
				keyAndNumber,
				BarcodeFormat.QR_CODE,
				((Double)(width*1.5)).intValue(),
				((Double)(height*1.5)).intValue()
			);
			
			Log.i(TAG, "Got bitmap code");
			qrImage.setImageBitmap(code);
			
		} catch (WriterException e) {
			Log.e(TAG, "WriterException", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String md5(String s) {
	    try {
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();
	        
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        return hexString.toString();
	        
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    return "";
	}

	
	/**
	 * Get the raw bytes of a file
	 * 
	 * @param context
	 * @param keyFilename
	 * @return
	 * @throws IOException
	 */
	private byte[] getKeyFileBytes(Context context, String keyFilename)
			throws IOException {
		File sdcard = Environment.getExternalStorageDirectory();
		File keyFile = new File(sdcard,"keys/self.pub");
		
		if(!keyFile.exists()){
			Toast.makeText(getApplicationContext(), "File does not exist. FUCK", Toast.LENGTH_SHORT);
			return null;
		}
		
		StringBuilder text = new StringBuilder();

		try {
		    BufferedReader br = new BufferedReader(new FileReader(keyFile));
		    String line;

		    while ((line = br.readLine()) != null) {
		        text.append(line);
		        text.append('\n');
		    }
		}
		catch (IOException e) {
		    //You'll need to add proper error handling here
		}

		return text.toString().getBytes();
	}
	
	static Bitmap encodeAsBitmap(String contents, BarcodeFormat format,
			int desiredWidth, int desiredHeight) throws WriterException {
		Hashtable<EncodeHintType, Object> hints = null;
		String encoding = guessAppropriateEncoding(contents);
		if (encoding != null) {
			hints = new Hashtable<EncodeHintType, Object>(2);
			hints.put(EncodeHintType.CHARACTER_SET, encoding);
		}
		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix result = writer.encode(contents, format, desiredWidth,
				desiredWidth, hints);
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		Log.i(TAG, "Code is " + width + "x" + height);
		
		// All are 0, or black, by default
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}
	
	private static String guessAppropriateEncoding(CharSequence contents) {
		// Very crude at the moment
		for (int i = 0; i < contents.length(); i++) {
			if (contents.charAt(i) > 0xFF) {
				return "UTF-8";
			}
		}
		return null;
	}
	
	private class BitMapView extends View {
		Bitmap mBitmap = null;

		public BitMapView(Context context, Bitmap bm) {
			super(context);
			mBitmap = bm;
			setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			setBackgroundColor(R.color.white);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// called when view is drawn
			Paint paint = new Paint();
			paint.setFilterBitmap(true);

			int size = Math.min(this.getWidth(), this.getHeight());
			Rect dest = new Rect(0, 0, size, size);
			canvas.drawBitmap(mBitmap, null, dest, paint);
		}
	}
	
	
}
