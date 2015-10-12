package com.melody.cool.myocr.camera;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.melody.cool.myocr.R;
//import io.casper.android.activity.EditorActivity;
import com.melody.cool.myocr.activity.BaseActivity;
import com.melody.cool.myocr.manager.InternalCacheManager;
import com.melody.cool.myocr.ui.AspectFrameLayout;


import java.io.File;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends BaseActivity {

    private Context mContext;

    private InternalCacheManager mInternalCacheManager;

    private CameraPreview mCameraPreview;

    private ImageView mCaptureImageButton;
    //private ImageView mCaptureVideoButton;
    private ImageView mSwitchCameraButton;
    private ImageView mBackFlashButton;
    private ImageView mFrontFlashButton;

    private AspectFrameLayout mCameraFrame;
    private FrameLayout mFrontFlashFrame;
    private LinearLayout mRecordingView;

    private TextView mTimeText;
    private boolean mFrontFlashEnabled;

    @Override
    protected void onResume() {
        super.onResume();

        if(mCameraPreview != null){
            mCameraPreview.initCamera(mCameraPreview.getCameraID());
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mContext = this;

        mInternalCacheManager = new InternalCacheManager(mContext);

        mTimeText = (TextView) findViewById(R.id.timeText);
        mCaptureImageButton = (ImageView) findViewById(R.id.captureImageButton);
       // mCaptureVideoButton = (ImageView) findViewById(R.id.captureVideoButton);
        mSwitchCameraButton = (ImageView) findViewById(R.id.switchCameraButton);
        mBackFlashButton = (ImageView) findViewById(R.id.backFlashButton);
        mFrontFlashButton = (ImageView) findViewById(R.id.frontFlashButton);
        mCameraFrame = (AspectFrameLayout) findViewById(R.id.cameraFrame);
        mRecordingView = (LinearLayout) findViewById(R.id.recordingView);
        mFrontFlashFrame = (FrameLayout) findViewById(R.id.frontFlashFrame);

        mCameraPreview = new CameraPreview(mContext);
        mCameraPreview.attachToView(mCameraFrame);

        mCameraPreview.setOutputFile(mInternalCacheManager.getInternalTempFile());

        mCameraPreview.setRecordingCallback(new CameraPreview.RecordingCallback() {
            @Override
            public void startedRecording() {
                mRecordingView.setVisibility(View.VISIBLE);
               // mCaptureVideoButton.setImageResource(R.drawable.ic_stop);
            }

            @Override
            public void stoppedRecording() {
                mRecordingView.setVisibility(View.INVISIBLE);
               // mCaptureVideoButton.setImageResource(R.drawable.ic_video);
            }

            @Override
            public void onMediaSaved(final File file) {
                /*
                Intent editorIntent = new Intent(mContext, EditorActivity.class);
                editorIntent.putExtra("file", file.getAbsolutePath());
                editorIntent.putExtra("type", "video");
                editorIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(editorIntent);
                */
            }

            @Override
            public void timeElapsed(int time) {
                mTimeText.setText(formatSeconds(time));
            }

            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(mContext, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        mCameraPreview.setFlashCallback(new CameraPreview.FlashCallback() {
            @Override
            public void onFlashModeSet(String flashMode) {

                enableFrontFlash(false);

                switch (mCameraPreview.getFlashMode()) {

                    case Camera.Parameters.FLASH_MODE_OFF: {
                        mBackFlashButton.setImageResource(R.drawable.ic_flash_off);
                        return;
                    }

                    case Camera.Parameters.FLASH_MODE_ON: {
                        mBackFlashButton.setImageResource(R.drawable.ic_flash_on);
                        return;
                    }

                    case Camera.Parameters.FLASH_MODE_AUTO: {
                        mBackFlashButton.setImageResource(R.drawable.ic_flash_auto);
                        return;
                    }

                    case Camera.Parameters.FLASH_MODE_TORCH: {
                        mBackFlashButton.setImageResource(R.drawable.ic_flash_torch);
                        return;
                    }

                }

            }
        });

        /*
        mCaptureVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mCameraPreview.isRecording()) {
                    mCameraPreview.finishRecording();
                } else {
                    mCameraPreview.startRecording();
                }

            }
        }); */

        mCaptureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                takePhoto();

            }
        });

        mSwitchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mCameraPreview.setOutputFile(mInternalCacheManager.getInternalTempFile());
                mCameraPreview.switchCamera();

                //mCaptureVideoButton.setImageResource(R.drawable.ic_video);
                mRecordingView.setVisibility(View.INVISIBLE);

                enableFrontFlash(false);
                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);

                mBackFlashButton.setVisibility(mCameraPreview.isFrontCam() ? View.INVISIBLE : View.VISIBLE);
                mFrontFlashButton.setVisibility(mCameraPreview.isFrontCam() ? View.VISIBLE : View.INVISIBLE);

            }
        });

        mBackFlashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!mCameraPreview.isFrontCam()) {
                    mCameraPreview.setNextFlashMode();
                }

            }
        });

        mFrontFlashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableFrontFlash(!mFrontFlashEnabled);
            }
        });

    }

    public void enableFrontFlash(boolean enableFrontFlash){
        mFrontFlashEnabled = enableFrontFlash;
        mFrontFlashButton.setImageResource(mFrontFlashEnabled ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        mFrontFlashFrame.setVisibility(mFrontFlashEnabled ? View.VISIBLE : View.INVISIBLE);
    }

    public void takePhoto(){

        final File file = mInternalCacheManager.getInternalTempFile();

        mCameraPreview.takePhoto(file, new CameraPreview.PhotoCallback() {
            @Override
            public void onError(Throwable throwable) {
                //mCameraPreview.startPreview();
                Toast.makeText(mContext, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMediaSaved(File file) {
                /*
                enableFrontFlash(false);
                Intent editorIntent = new Intent(mContext, EditorActivity.class);
                editorIntent.putExtra("file", file.getAbsolutePath());
                editorIntent.putExtra("type", "image");
                if (mCameraPreview.isFrontCam()) {
                    editorIntent.putExtra("front_cam", true);
                }
                editorIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(editorIntent);
                //mCameraPreview.startPreview();
                */
            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            takePhoto();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private String formatSeconds(int seconds){
        return String.format("%02d:%02d", TimeUnit.SECONDS.toMinutes(seconds), TimeUnit.SECONDS.toSeconds(seconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
    }
}
