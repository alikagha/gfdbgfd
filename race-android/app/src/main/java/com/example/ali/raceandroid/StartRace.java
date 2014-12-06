package com.example.ali.raceandroid;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.media.MediaRecorder;
import android.hardware.Camera;
import java.io.IOException;
import java.util.List;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.io.File;

/**
 * Created by Ali on 11/25/14.
 */
public class StartRace extends Activity implements SurfaceHolder.Callback {

     Camera mCamera;
    private View mToggleButton;
    private MediaRecorder mMediaRecorder;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private boolean mInitSuccesful;
    private Context ctx;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final String TAG = "TAG";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera_view);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        mHolder = mSurfaceView.getHolder();

        mHolder.addCallback(this);

        mToggleButton = (ToggleButton) findViewById(R.id.btn_switch);


//        try {
//            mMediaRecorder.start();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        initRecorder(mHolder.getSurface());

        mMediaRecorder.start();


        System.out.println("OnCreateEnd");
    }


    private void initRecorder(Surface surface) {

//    if(mCamera == null){
//        mCamera = Camera.open();
//        mCamera.open();
//        mCamera.unlock();
//    }

//        int numCams = Camera.getNumberOfCameras();
//        if(numCams > 0) {
//            try {
//                mCamera = Camera.open(0);
//            } catch (Exception e) {
//                Log.e(getString(R.string.app_name), "failed to open Camera");
//                Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
//                e.printStackTrace();
//            }
//        }

        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            mCamera = Camera.open();
        }


        if(mMediaRecorder == null)
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setPreviewDisplay(surface);
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(640, 480);

        mMediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(
                CameraHelper.MEDIA_TYPE_VIDEO).toString());



        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
//            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            e.printStackTrace();
        }

        mInitSuccesful = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    if (!mInitSuccesful)
            initRecorder(mHolder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        shutdown();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    private void shutdown() {
        // Release MediaRecorder and especially the Camera as it's a shared
        // object that can be used by other applications
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mCamera.release();

        // once the objects have been released they can't be reused
        mMediaRecorder = null;
        mCamera = null;
    }



}
