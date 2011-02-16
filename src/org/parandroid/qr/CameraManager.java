package org.parandroid.qr;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

import org.parandroid.sms.R;
import org.parandroid.sms.ui.ScanPublicKeyActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class CameraManager {

    public static final int MIN_FRAME_WIDTH = 240;
    public static final int MIN_FRAME_HEIGHT = 240;
    public static final int MAX_FRAME_WIDTH = 480;
    public static final int MAX_FRAME_HEIGHT = 360;
    
    public static final String TAG = "Parandroid CameraManager";
    
    static CameraManager instance = null;
    
    private ScanPublicKeyActivity activity;
    private Context context;
    private Camera mCamera = null;
    private boolean previewRunning = false;
    private Rect framingRect = null;
    private Rect framingRectInPreview = null;
    private Point screenResolution;
    private Point cameraResolution;
    private int previewFormat;
    private String previewFormatString;
    private MultiFormatReader multiFormatReader;
    
    private QRCodeReader qrCodeReader;
    
    private AutoFocusCallback autoFocusCallBack = new AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
        	if(previewRunning)
        		mCamera.setOneShotPreviewCallback(previewCallBack);
        }
    };
    
    private PreviewCallback previewCallBack = new PreviewCallback() {
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			Size previewSize = camera.getParameters().getPreviewSize();
            decode(data, previewSize.width, previewSize.height);
		}
	};
    
    
    private CameraManager(Context context, ScanPublicKeyActivity activity){
        // private constructor to force singleton usage
        this.context = context; 
        this.activity = activity;
        
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        screenResolution = new Point(display.getWidth(), display.getHeight());
        multiFormatReader = new MultiFormatReader();
        qrCodeReader = new QRCodeReader();
        
        Log.i(TAG, "ScreenResolution: " + screenResolution.toString());
    }
    
    public static CameraManager getInstance(Context context, ScanPublicKeyActivity activity){
//    	if(instance == null)
        instance = new CameraManager(context, activity);
        
        return instance;
    }
    
    public static CameraManager getInstance(){
        return instance;
    }
    
    public void destroy(){
    	instance = null;
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Called surfaceCreated. Opening camera now");
        
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        mCamera = Camera.open();
        try {
           mCamera.setPreviewDisplay(holder);
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
            // TODO: add more exception handling logic here
        }
    }
    
    public void surfaceDestroyed() {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        if(!previewRunning) return;
        
    	mCamera.stopPreview();
        previewRunning = false;
        mCamera.release();
    }
    
    public void surfaceChanged(int w, int h){
        setCameraParameters(w, h);
        mCamera.startPreview();
        previewRunning = true;
        
        autoFocus();
    }
    
    public void setCameraParameters(int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        
        // Get closest supported preview size
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Iterator<Size> it = sizes.iterator();
        
        Size size;
        int width = 0, height = 0;
        
        while(it.hasNext()){
            size = it.next();
            
            if(size.width <= w && size.width > width){
                width = size.width;
                height = size.height;
            }
        }
        
        parameters.setPreviewSize(width, height);
        Log.i(TAG, "Setting preview size (WxH): " + width + "x" + height);
        
        mCamera.setParameters(parameters);
        
        previewFormat = parameters.getPreviewFormat();
        previewFormatString = parameters.get("preview-format");
        cameraResolution = getCameraResolution();
    }
    
    private void autoFocus(){
        if(previewRunning){
            mCamera.autoFocus(autoFocusCallBack);
        }
    }    
    
    /**
     * Calculates the framing rect which the UI should draw to show the user
     * where to place the barcode. This target helps with alignment as well as
     * forces the user to hold the device far enough away to ensure the image
     * will be in focus.
     * 
     * @return The rectangle to draw on screen in window coordinates.
     */
    public Rect getFramingRect() {
        Log.i(TAG, "getFramingRect called");
        
        if (framingRect == null) {
            Log.i(TAG, "FramingRect is null, so calculate it");
            
            if (mCamera == null) {
                Log.i(TAG, "Camera is null, return null");
                return null;
            }
            
            Point cameraResolution = getCameraResolution();
            
            int width = cameraResolution.x * 3 / 4;
            if (width < MIN_FRAME_WIDTH) {
                width = MIN_FRAME_WIDTH;
            } else if (width > MAX_FRAME_WIDTH) {
                width = MAX_FRAME_WIDTH;
            }
            int height = cameraResolution.y * 3 / 4;
            if (height < MIN_FRAME_HEIGHT) {
                height = MIN_FRAME_HEIGHT;
            } else if (height > MAX_FRAME_HEIGHT) {
                height = MAX_FRAME_HEIGHT;
            }
            
            Log.i(TAG, "Calculated rect size (WxH): " + width + "x" + height);
            
            int leftOffset = (cameraResolution.x - width) / 2;
            int topOffset = (cameraResolution.y - height) / 2;
            
            Log.i(TAG, "Calculated left offset: " + leftOffset);
            Log.i(TAG, "Calculated top offset: " + topOffset);
            
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
       }
        
        return framingRect;
    }
    
    private Point getCameraResolution(){
    	if(mCamera == null) return null;
    	
    	Camera.Parameters p = mCamera.getParameters();
    	int width = p.getPreviewSize().width;
    	int height = p.getPreviewSize().height;
        
        return new Point(width, height);
    }
    
//    private void save(byte[] data, int width, int height) {
//        final Rect framingRect = CameraManager.getInstance().getFramingRect();
//        int framingWidth = framingRect.width();
//        int framingHeight = framingRect.height();
//        if (framingWidth > width || framingHeight > height) {
//            throw new IllegalArgumentException();
//        }
//
//        int leftOffset = framingRect.left;
//        int topOffset = framingRect.top;
//        int[] colors = new int[framingWidth * framingHeight];
//
//        for (int y = 0; y < framingHeight; y++) {
//            int rowOffset = (y + topOffset) * width + leftOffset;
//            for (int x = 0; x < framingWidth; x++) {
//                int pixel = (int)data[rowOffset + x];
//                pixel = 0xff000000 + (pixel << 16) + (pixel << 8) + pixel;
//                colors[y * framingWidth + x] = pixel;
//            }
//        }
//
//        Bitmap bitmap = Bitmap.createBitmap(colors, framingWidth, framingHeight,
//                Bitmap.Config.ARGB_8888);
////        OutputStream outStream = getNewPhotoOutputStream();
////        if (outStream != null) {
////            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
////            try {
////                outStream.close();
////                
////                return;
////            } catch (IOException e) {
////                Log.e(TAG, "Exception closing stream: " + e.toString());
////            }
////        }
//        
//        Toast.makeText(context, "Took picture and tested it", Toast.LENGTH_SHORT).show();
//        
//        Reader reader = new QRCodeReader();
//        LuminanceSource source = new RGBLuminanceSource(bitmap);
//        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
//        
//            Result result;
//            try {
//                result = reader.decode(binaryBitmap);
//                byte[] rawBytes = result.getRawBytes();
//                
//                ResultPoint[] points = result.getResultPoints();
//                Log.i(TAG, "points: " + points.toString());
//            } catch (NotFoundException e) {
//                Log.i(TAG, "NotFoundException");
//                autoFocus();
//            } catch (ChecksumException e) {
//                Toast.makeText(context, "barcode was successfully detected and decoded, but was not returned because its checksum feature failed.", Toast.LENGTH_SHORT).show();
//                Log.i(TAG, "ChecksumException");
//            } catch (FormatException e) {
//                Toast.makeText(context, "successfully detected, but some aspect of the content did not conform to the barcode's format rules.", Toast.LENGTH_SHORT).show();
//                Log.i(TAG, "FormatException");
//                autoFocus();
//            }
//          
//        
//
//    }
    
//    private static OutputStream getNewPhotoOutputStream() {
//        File sdcard = Environment.getExternalStorageDirectory();
//        if (sdcard.exists()) {
//            File barcodes = new File(sdcard, "barcodes");
//            if (barcodes.exists()) {
//                if (!barcodes.isDirectory()) {
//                     Log.e(TAG, "/sdcard/barcodes exists but is not a directory");
//                    return null;
//                }
//            } else {
//                if (!barcodes.mkdir()) {
//                     Log.e(TAG, "Could not create /sdcard/barcodes directory");
//                    return null;
//                }
//            }
//            Date now = new Date();
//            String fileName = now.getTime() + ".png";
//            try {
//                return new FileOutputStream(new File(barcodes, fileName));
//            } catch (FileNotFoundException e) {
//                 Log.e(TAG, "Could not create FileOutputStream");
//            }
//        } else {
//             Log.e(TAG, "/sdcard does not exist");
//        }
//        return null;
//    }

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data The YUV preview frame.
	 * @param width The width of the preview frame.
	 * @param height The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		Result rawResult = null;
		PlanarYUVLuminanceSource source = CameraManager.getInstance()
				.buildLuminanceSource(data, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		try {
			rawResult = qrCodeReader.decode(bitmap);
//			rawResult = multiFormatReader.decodeWithState(bitmap);
		} catch (ReaderException re) {
			// continue
		} finally {
			qrCodeReader.reset();
//			multiFormatReader.reset();
		}

		if (rawResult != null) {
			activity.vibrate();
			activity.drawResultPoints(source.renderCroppedGreyscaleBitmap(), rawResult);
			surfaceDestroyed();
			
			Log.i(TAG, "KEY,text: " + rawResult.getText());
			Log.i(TAG, "KEY,bytes: " + rawResult.getRawBytes());
//			Toast.makeText(context, "Successfully decoded code", Toast.LENGTH_SHORT).show();
//			Toast.makeText(activity, md5(rawResult.getText()), Toast.LENGTH_LONG).show();
			
			activity.storePublicKey(rawResult.getText());
			
//			long end = System.currentTimeMillis();
//			Log.d(TAG, "Found barcode (" + (end - start) + " ms):\n"
//					+ rawResult.toString());
//			Message message = Message.obtain(activity.getHandler(),
//					R.id.decode_succeeded, rawResult);
//			Bundle bundle = new Bundle();
//			bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source
//					.renderCroppedGreyscaleBitmap());
//			message.setData(bundle);
//			// Log.d(TAG, "Sending decode succeeded message...");
//			message.sendToTarget();
		} else {
			SystemClock.sleep(500);
			autoFocus();
			// decode failed
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
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
			int width, int height) {
		Rect rect = getFramingRectInPreview();
		switch (previewFormat) {
		// This is the standard Android format which all devices are REQUIRED to
		// support.
		// In theory, it's the only one we should ever care about.
		case PixelFormat.YCbCr_420_SP:
			// This format has never been seen in the wild, but is compatible as
			// we only care
			// about the Y channel, so allow it.
		case PixelFormat.YCbCr_422_SP:
			return new PlanarYUVLuminanceSource(data, width, height, rect.left,
					rect.top, rect.width(), rect.height());
		default:
			// The Samsung Moment incorrectly uses this variant instead of the
			// 'sp' version.
			// Fortunately, it too has all the Y data up front, so we can read
			// it.
			if ("yuv420p".equals(previewFormatString)) {
				return new PlanarYUVLuminanceSource(data, width, height,
						rect.left, rect.top, rect.width(), rect.height());
			}
		}
		throw new IllegalArgumentException("Unsupported picture format: "
				+ previewFormat + '/' + previewFormatString);
	}
	
	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview
	 * frame, not UI / screen.
	 */
	public Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect rect = new Rect(getFramingRect());
			rect.left = rect.left * cameraResolution.x / screenResolution.x;
			rect.right = rect.right * cameraResolution.x / screenResolution.x;
			rect.top = rect.top * cameraResolution.y / screenResolution.y;
			rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
			framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}

}
