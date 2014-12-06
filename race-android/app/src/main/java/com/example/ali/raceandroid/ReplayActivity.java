package com.example.ali.raceandroid;

import android.content.Intent;
import android.media.MediaPlayer;
import android.view.View;


/**
 * Created by Ali on 11/4/14.
 */
public class ReplayActivity extends MainActivity {

    private static final int PICK_VIDEO_REQUEST = 1001;
    private static final String TAG = "SurfaceSwitch";
    private MediaPlayer mMediaPlayer;




    public void doStartStop(View view) {
        if (mMediaPlayer == null) {
            Intent pickVideo = new Intent(Intent.ACTION_PICK);
            pickVideo.setTypeAndNormalize("video/*");
            startActivityForResult(pickVideo, PICK_VIDEO_REQUEST);
        } else {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }



}
