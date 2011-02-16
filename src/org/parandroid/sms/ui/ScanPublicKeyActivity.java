package org.parandroid.sms.ui;

import java.util.HashMap;

import org.parandroid.encryption.MessageEncryptionFactory;
import org.parandroid.qr.BarcodeRectView;
import org.parandroid.qr.CameraManager;
import org.parandroid.sms.R;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ScanPublicKeyActivity extends Activity implements
		SurfaceHolder.Callback {

	private static final long VIBRATE_DURATION = 200L;
    private static final int CONTEXT_MENU_ACCEPT = 0;
    private static final int CONTEXT_MENU_DECLINE = 1;
    private static final String TAG = "Parandroid ScanPublicKeyActivity";

	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private CameraManager cameraManager;
	private BarcodeRectView barcodeRectView;
	
	private String publicKeyNumber;
	private String publicKeyContents;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		cameraManager = CameraManager.getInstance(getApplication(), this);

		setContentView(R.layout.camera_preview);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		barcodeRectView = (BarcodeRectView)findViewById(R.id.barcode_rect_view);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		cameraManager.surfaceDestroyed();
	}

	public void vibrate() {
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(VIBRATE_DURATION);
	}

	/**
	 * Superimpose a line for 1D or dots for 2D to highlight the key features of
	 * the barcode.
	 * 
	 * @param barcode
	 *            A bitmap of the captured image.
	 * @param rawResult
	 *            The decoded results which contains the points to draw.
	 */
	public void drawResultPoints(Bitmap barcode, Result rawResult) {
		barcodeRectView.drawResultBitmap(barcode);
		
		ResultPoint[] points = rawResult.getResultPoints();
		
		if (points == null) {
			Log.i(TAG, "Result Points is null ");
		} else {
			Log.i(TAG, "Number of points found: " + points.length); }
		
		if (points != null && points.length > 0) {
			Canvas canvas = new Canvas(barcode);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.result_image_border));
			paint.setStrokeWidth(3.0f);
			paint.setStyle(Paint.Style.STROKE);
			Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode
					.getHeight() - 2);
			canvas.drawRect(border, paint);

			paint.setColor(getResources().getColor(R.color.result_points));
			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				drawLine(canvas, paint, points[0], points[1]);
			} else if (points.length == 4
					&& (rawResult.getBarcodeFormat()
							.equals(BarcodeFormat.UPC_A))
					|| (rawResult.getBarcodeFormat()
							.equals(BarcodeFormat.EAN_13))) {
				// Hacky special case -- draw two lines, for the barcode and
				// metadata
				drawLine(canvas, paint, points[0], points[1]);
				drawLine(canvas, paint, points[2], points[3]);
			} else {
				paint.setStrokeWidth(10.0f);
				for (ResultPoint point : points) {
					canvas.drawPoint(point.getX(), point.getY(), paint);
				}
			}
		}
	}
	
	public void storePublicKey(String publicKey){
		Log.i(TAG, "Received public key: " + publicKey);
	
		// TODO: this should be parsed from the QR code of course
		String[] msg = publicKey.split(PrintPublicKeyActivity.KEY_NUMBER_SEPARATOR);
		
		publicKeyNumber = msg[1];
		publicKeyContents = msg[0];	
		
		AlertDialog.Builder acceptPublicKeyDialogBuilder = new AlertDialog.Builder(ScanPublicKeyActivity.this);
	    acceptPublicKeyDialogBuilder.setMessage(getText(R.string.import_scanned_public_key_dialog))
		   .setTitle("Accept public key from " + publicKeyNumber)
		   .setCancelable(false)
	       .setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   SQLiteDatabase keyRing = MessageEncryptionFactory.openKeyring(ScanPublicKeyActivity.this);

	        	   ContentValues cv = new ContentValues();
	        	   cv.put("number", publicKeyNumber);
	        	   cv.put("publickey", publicKeyContents);
	        	   cv.put("accepted", true);

	        	   long keyId = keyRing.insert(MessageEncryptionFactory.PUBLIC_KEY_TABLE, "", cv);
	        	   keyRing.close();
	        	   
	        	   Toast.makeText(ScanPublicKeyActivity.this, R.string.import_public_key_success, Toast.LENGTH_LONG).show();
	        	   
	        	   Intent intent = new Intent(ScanPublicKeyActivity.this, ConversationList.class);
	        	   startActivity(intent);
	        	   
	        	   finish();
	           }
	       })
	       .setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   SQLiteDatabase keyRing = MessageEncryptionFactory.openKeyring(ScanPublicKeyActivity.this);

	        	   ContentValues cv = new ContentValues();
	        	   cv.put("number", publicKeyNumber);
	        	   cv.put("publickey", publicKeyContents);
	        	   cv.put("accepted", false);

	        	   long keyId = keyRing.insert(MessageEncryptionFactory.PUBLIC_KEY_TABLE, "", cv);
	        	   keyRing.close();
	        	   
	        	   Toast.makeText(ScanPublicKeyActivity.this, R.string.declined_public_key, Toast.LENGTH_SHORT).show();
	        	   dialog.cancel();
	        	   
	        	   Intent intent = new Intent(ScanPublicKeyActivity.this, ConversationList.class);
	        	   startActivity(intent);
	        	   
	        	   finish();
	           }
	       });
	
    	AlertDialog acceptAlert = acceptPublicKeyDialogBuilder.create();
    	
    	acceptAlert.show();
	}
	
	
    

	private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
			ResultPoint b) {
		canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		cameraManager.surfaceCreated(holder);
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		cameraManager.surfaceDestroyed();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		cameraManager.surfaceChanged(w, h);
	}

}
