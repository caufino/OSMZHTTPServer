package com.vsb.kru13.osmzhttpserver;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private static boolean saveImageToSD;
    private static byte[] savedBytePicture;
    private CountDownTimer countDownTimer;

    private Button captureButton;

    private SocketServerService socketServerService = null;
    private Intent intent = null;
    private boolean isBound = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
            }
        }
    }

    public void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);

        if(permissionsAcquired())
            createAll();
    }

    public boolean permissionsAcquired() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    public void createAll() {
        setContentView(R.layout.activity_camera);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Add a listener to the Capture button
        captureButton = (Button)findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        Log.d("CameraActivity".toUpperCase(), "Taking picture");
                        saveImageToSD = true;
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        this.intent = new Intent(this.getBaseContext(), SocketServerService.class);
        startService(this.intent);
        this.doBindService();

        saveImageToSD = false;
        savedBytePicture = null;
        this.startTimer(Long.MAX_VALUE, 10000);
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            captureButton.setEnabled(false);
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d("CameraActivity".toUpperCase(), "Error creating media file, check storage permissions");
                return;
            }

            mCamera.startPreview();
            try {
                if(saveImageToSD) {
                    Log.d("CameraActivity".toUpperCase(), "Saving picture to SD: " + pictureFile.getAbsolutePath());
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    saveImageToSD = false;
                } else {
                    Log.d("CameraActivity".toUpperCase(), "Saving picture to byte array");
                    savedBytePicture = data;
                }
            } catch (FileNotFoundException e) {
                Log.d("CameraActivity".toUpperCase(), "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("CameraActivity".toUpperCase(), "Error accessing file: " + e.getMessage());
            }
            captureButton.setEnabled(true);
        }
    };

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "OSMZ_HTTP");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("CameraActivity".toUpperCase(), "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(Environment.getExternalStorageDirectory() +
                    "/IMG.jpg");
                    //"IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void startTimer(long millisInFuture, long countDownInterval){
        countDownTimer = new CountDownTimer(millisInFuture, countDownInterval){
            public void onTick(long millisUntilDone){
                if(mPreview.isSurfaceCreated()) {
                    Log.d("TIMER", "Taking picture");
                    mCamera.takePicture(null, null, mPicture);
                }
            }

            public void onFinish() {
                Log.d("TIMER", "Finished");
            }
        }.start();
    }

    public boolean getSaveImageToSD() {
        return saveImageToSD;
    }

    public static void setSaveImageToSD(boolean saveImageToSD) {
        saveImageToSD = saveImageToSD;
    }

    public static byte[] getSavedBytePicture() {
        return savedBytePicture;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            Log.d("CameraActivity".toUpperCase(), "onServiceConnected");
            socketServerService = ((SocketServerService.LocalBinder)iBinder).getService();
            socketServerService.startSocketServerService(null);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            socketServerService = null;
            doUnbindService();
        }
    };

    private void doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        bindService(new Intent(this, SocketServerService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
        isBound = true;
    }

    private void doUnbindService() {
        if(isBound) {
            // Release information about the service's state.
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.doUnbindService();
        this.countDownTimer.cancel();

        if(this.socketServerService != null) {
            this.socketServerService.stopSocketServerService();
        }
    }
}
