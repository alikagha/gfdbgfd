package com.example.ali.raceandroid;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.StatFs;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;



public class CameraActivity extends MainActivity implements SurfaceHolder.Callback {
    private MediaRecorder recorder;
    private SurfaceHolder surfaceHolder;
    private CamcorderProfile camcorderProfile;
    private Camera camera;
    boolean recording = false;
    boolean usecamera = true;
    boolean previewRunning = false;
    SurfaceView surfaceView;
    Button btnStart, btnStop;
    File root;
    File file;
    Boolean isSDPresent;
    SimpleDateFormat simpleDateFormat;
    String timeStamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("oncreate");

        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.camera_view);


        System.out.println("Set Content View complete");

//        initComs();
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        System.out.println("it was the holder");

        surfaceHolder.addCallback(this);
        btnStop = (Button) findViewById(R.id.btn_switch);

        System.out.println("Init coms complete");

        actionListener();
    }

    private void initComs() {
        System.out.println("InitCam Start");

        simpleDateFormat = new SimpleDateFormat("ddMMyyyyhhmmss");
        timeStamp = simpleDateFormat.format(new Date());
        System.out.println("InitCam Date");
//        camcorderProfile  = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);

        System.out.println("InitCam Camcorder Profile ");
        System.out.println("FAIL HERE");

//        surfaceView = (SurfaceView) findViewById(R.id.surfaceView2);
        System.out.println("a");

        surfaceHolder = surfaceView.getHolder();
        System.out.println("b");

        surfaceHolder.addCallback(this);
        System.out.println("Surface View + SurfaceHolder");

        btnStop = (Button) findViewById(R.id.btn_switch);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        isSDPresent = android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);

        System.out.println("InitCam Complete");

    }

    public static float megabytesAvailable(File f) {
        StatFs stat = new StatFs(f.getPath());
        long bytesAvailable = (long) stat.getBlockSize()
                * (long) stat.getAvailableBlocks();
        return bytesAvailable / (1024.f * 1024.f);
    }



    private void actionListener() {
        btnStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (recording) {
                    recorder.stop();
                    if (usecamera) {
                        try {
                            camera.reconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // recorder.release();
                    recording = false;
                    // Let's prepareRecorder so we can record again
                    prepareRecorder();
                }

            }
        });
    }

    private void prepareRecorder() {
        recorder = new MediaRecorder();
        recorder.setPreviewDisplay(surfaceHolder.getSurface());
        if (usecamera) {
            camera.unlock();
            recorder.setCamera(camera);
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        recorder.setProfile(camcorderProfile);


//        if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
//            recorder.setOutputFile("/sdcard/XYZApp/" + "XYZAppVideo" + ""
//                    + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date())
//                    + ".mp4");
//        } else if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
//            recorder.setOutputFile("/sdcard/XYZApp/" + "XYZAppVideo" + ""
//                    + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date())
//                    + ".mp4");
//        } else {
//            recorder.setOutputFile("/sdcard/XYZApp/" + "XYZAppVideo" + ""
//                    + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date())
//                    + ".mp4");
//        }

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }

    }


    public void surfaceCreated(SurfaceHolder holder) {
        System.out.println("onsurfacecreated");

        if (usecamera) {
            camera = Camera.open();

            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        System.out.println("onsurface changed");

        if (!recording && usecamera) {
            if (previewRunning) {
                camera.stopPreview();
            }

            try {
                Camera.Parameters p = camera.getParameters();

                p.setPreviewSize(camcorderProfile.videoFrameWidth,
                        camcorderProfile.videoFrameHeight);
                p.setPreviewFrameRate(camcorderProfile.videoFrameRate);

                camera.setParameters(p);

                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            prepareRecorder();
            if (!recording) {
                recording = true;
                recorder.start();
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        if (usecamera) {
            previewRunning = false;
            // camera.lock();
            camera.release();
        }
        finish();
    }
}