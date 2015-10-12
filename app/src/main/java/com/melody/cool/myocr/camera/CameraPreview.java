package com.melody.cool.myocr.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.FloatMath;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melody.cool.myocr.R;
import com.melody.cool.myocr.ui.AspectFrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraPreview {

    private Context mContext;

    private int mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean mCameraInPreview;

    private Camera mCamera;
    private float mDist;

    private int mCameraRotation;

    private CameraSurfaceTexture mCameraSurfaceTexture;

    private List<String> mFlashModes = new ArrayList<>();
    private String mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
    private FlashCallback mFlashCallback;

    private boolean mRecording;
    private int mTimeElapsed;

    private Handler mTimeHandler;
    private Runnable mTimeRunnable;

    private MediaRecorder mMediaRecorder;
    private RecordingCallback mRecordingCallback;

    private File mOutputFile;

    private MaterialDialog mProgressDialog;

    public CameraPreview(Context context){

        mContext = context;

        MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(mContext);
        dialogBuilder.progress(true, 0);
        dialogBuilder.content(mContext.getString(R.string.info_one_moment));
        mProgressDialog = dialogBuilder.build();
        mProgressDialog.setCancelable(false);

        mTimeHandler = new Handler();
        mMediaRecorder = new MediaRecorder();

        mCameraSurfaceTexture = new CameraSurfaceTexture(mContext);
        mCameraSurfaceTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

                if (mCamera == null) {

                    initCamera(mCameraID);
                    try {
                        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                        //mCameraSurfaceTexture.adjustSurfaceLayoutSize(previewSize, true, width, height);
                        ((AspectFrameLayout) mCameraSurfaceTexture.getParent()).setLayoutParams(new FrameLayout.LayoutParams(previewSize.height, previewSize.width, Gravity.CENTER));
                        startPreview();
                    } catch(Exception e){
                        e.printStackTrace();
                        if(mRecordingCallback != null){
                            mRecordingCallback.onError(e);
                        }
                    }

                } else {
                    startPreview();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

                if(mCamera != null){

                    Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                    //mCameraSurfaceTexture.adjustSurfaceLayoutSize(previewSize, true, width, height);
                    ((AspectFrameLayout)mCameraSurfaceTexture.getParent()).setAspectRatio((double) previewSize.height / previewSize.width);
                    mCameraSurfaceTexture.getParent().requestLayout();

                }

                /*
                if(mCamera != null) {
                    try {
                        Camera.Parameters parameters = mCamera.getParameters();
                        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
                        Camera.Size size = getBestPreviewSize(sizeList, w, h);
                        //parameters.setPreviewSize(size.width, size.height);
                        mCamera.setParameters(parameters);
                        startPreview();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
                */
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                stopRecording();
                releaseCamera();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });

        mTimeRunnable = new Runnable() {
            @Override
            public void run() {
                mTimeElapsed++;
                if(mRecordingCallback != null){
                    mRecordingCallback.timeElapsed(mTimeElapsed);
                    mTimeHandler.postDelayed(mTimeRunnable, 1000);
                }
            }
        };

        mCameraSurfaceTexture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                if(mCamera != null){

                    int action = event.getAction();

                    if (event.getPointerCount() > 1) {
                        // handle multi-touch events
                        if (action == MotionEvent.ACTION_POINTER_DOWN) {
                            mDist = getFingerSpacing(event);
                        } else if (action == MotionEvent.ACTION_MOVE && mCamera.getParameters().isZoomSupported()) {
                            mCamera.cancelAutoFocus();
                            handleZoom(event);
                        }
                    } else {
                        if (action == MotionEvent.ACTION_UP) {
                            handleFocus(event);
                        }
                    }

                    return true;

                }

                return false;

            }
        });

    }

    public int getCameraID(){
        return mCameraID;
    }

    public void attachToView(FrameLayout frameLayout){
        frameLayout.addView(mCameraSurfaceTexture);
    }

    public void setRecordingCallback(RecordingCallback recordingCallback){
        mRecordingCallback = recordingCallback;
    }

    public void setFlashCallback(FlashCallback flashCallback){
        mFlashCallback = flashCallback;
    }

    public void setOutputFile(File outputFile){
        mOutputFile = outputFile;
    }

    public CameraSurfaceTexture getCameraSurfaceTexture(){
        return mCameraSurfaceTexture;
    }

    public void initCamera(int cameraID){

        pausePreview();
        mCameraID = cameraID;

        try {

            mCamera = Camera.open(mCameraID);

            setCameraDisplayOrientation(mCameraID, mCamera);

            Camera.Parameters parameters = mCamera.getParameters();

            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            if(!isFrontCam() && parameters.getSupportedFlashModes() != null) {

                mFlashModes.clear();
                List<String> flashModes = parameters.getSupportedFlashModes();

                if(flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)){
                    mFlashModes.add(Camera.Parameters.FLASH_MODE_OFF);
                }
                if(flashModes.contains(Camera.Parameters.FLASH_MODE_ON)){
                    mFlashModes.add(Camera.Parameters.FLASH_MODE_ON);
                }
                if(flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)){
                    mFlashModes.add(Camera.Parameters.FLASH_MODE_TORCH);
                }

            }

            Camera.Size size = getPictureSize();
            parameters.setPictureSize(size.width, size.height);
            mCamera.setParameters(parameters);

        } catch(Exception e){
            if(mRecordingCallback != null){
                e.printStackTrace();
                mRecordingCallback.onError(e);
            }
        }

    }

    public void handleFocus(MotionEvent event) {

        focus();
        if(true) {
            return;
        }

        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = mCamera.getParameters().getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch
                }
            });
        }
    }

    private void handleZoom(MotionEvent event) {

        Camera.Parameters params = mCamera.getParameters();

        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }


    public void focus(){
        try {

            mCamera.cancelAutoFocus();

            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            mCamera.setParameters(parameters);

            mCamera.autoFocus(null);

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public void releaseCamera(){
        if (mCamera != null){
            if(mCameraInPreview){
                mCameraInPreview = false;
                mCamera.stopPreview();
            }
            mCamera.release();
            mCamera = null;
        }
    }

    public void startPreview(){
        try {

            mCamera.setPreviewTexture(mCameraSurfaceTexture.getSurfaceTexture());
            mCamera.startPreview();
            mCameraInPreview = true;

            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            double aspectRatio = (double) previewSize.height / previewSize.width;
            Log.d("AspectRatio", String.valueOf(aspectRatio));
            ((AspectFrameLayout)mCameraSurfaceTexture.getParent()).setAspectRatio(aspectRatio);

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void pausePreview(){
        if (mCamera != null){
            if(mCameraInPreview){
                mCameraInPreview = false;
                mCamera.stopPreview();
            }
        }
    }

    protected void startRecording() {

        if(mRecordingCallback != null) {
            mRecordingCallback.timeElapsed(0);
        }

        mCameraInPreview = false;
        mTimeElapsed = 0;

        try {

            Camera.Size size = getVideoSize();
            CamcorderProfile profile = getCamcorderProfile();

            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraID, cameraInfo);
            int rotation = (cameraInfo.orientation + 360) % 360;

            mMediaRecorder = new MediaRecorder();  // Works well
            mCamera.unlock();

            mMediaRecorder.setOrientationHint(rotation);
            /*
            if(mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mMediaRecorder.setOrientationHint(270);
            } else {
                mMediaRecorder.setOrientationHint(90);
            }
            */

            //mMediaRecorder.setPreviewDisplay(mCameraSurfaceTexture.getSurfaceHolder().getSurface());
            mMediaRecorder.setCamera(mCamera);

            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            //mMediaRecorder.setProfile(profile);
            mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
            mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
            mMediaRecorder.setVideoSize(size.width, size.height);

            mMediaRecorder.setMaxDuration(10 * 1000); //aprox 10 seconds...
            mMediaRecorder.setMaxFileSize(4500000); //4.5mb

            mMediaRecorder.setOutputFile(mOutputFile.getAbsolutePath());

            mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {

                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        if (mRecordingCallback != null) {
                            finishRecording();
                        }
                    }

                }
            });

            mMediaRecorder.prepare();
            mMediaRecorder.start();

            mRecording = true;

            if(mRecordingCallback != null){
                mRecordingCallback.startedRecording();
            }

            mTimeHandler.postDelayed(mTimeRunnable, 1000);

        } catch(Exception e){
            if(mRecordingCallback != null){
                mRecordingCallback.onError(e);
            }
        }

    }

    protected void stopRecording() {

        mTimeHandler.removeCallbacks(mTimeRunnable);
        mTimeElapsed = 0;

        try {
            if(isRecording()) {
                mMediaRecorder.stop();
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        try {
            releaseMediaRecorder();
        } catch(Exception e){
            e.printStackTrace();
        }

        mRecording = false;

        startPreview();

    }

    protected void finishRecording() {

        stopRecording();
        if(mRecordingCallback != null){
            mRecordingCallback.stoppedRecording();
            mRecordingCallback.onMediaSaved(mOutputFile);
        }

    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    public void setFlashMode(String flashMode){

        mFlashMode = flashMode;

        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(mFlashMode);
            mCamera.setParameters(parameters);
        } catch(Exception e){
            e.printStackTrace();
            if(mRecordingCallback != null){
                mRecordingCallback.onError(e);
            }
        }

        if(mFlashCallback != null){
            mFlashCallback.onFlashModeSet(mFlashMode);
        }

    }

    public void setNextFlashMode(){

        try {
            Camera.Parameters parameters = mCamera.getParameters();

            int currentFlash = mFlashModes.indexOf(parameters.getFlashMode());
            if (currentFlash < mFlashModes.size() - 1) {
                currentFlash++;
                mFlashMode = mFlashModes.get(currentFlash);
            } else {
                mFlashMode = mFlashModes.get(0);
            }

            setFlashMode(mFlashMode);
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    public String getFlashMode(){
        return mFlashMode;
    }

    public void switchCamera(){

        if(mRecording){
            stopRecording();
        }

        releaseCamera();

        if(mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK){
            mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        initCamera(mCameraID);
        startPreview();

    }

    public boolean isRecording(){
        return mRecording;
    }

    public void takePhoto(final File outputFile, final PhotoCallback photoCallback){

        if(!isRecording()){

            pausePreview();
            mProgressDialog.show();

            final Bitmap frame = getCameraSurfaceTexture().getBitmap();

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {

                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                        frame.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                startPreview();
                                mProgressDialog.dismiss();
                                photoCallback.onMediaSaved(outputFile);
                            }
                        });

                    } catch (final Exception e) {
                        startPreview();
                        if (photoCallback != null) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.dismiss();
                                    photoCallback.onError(e);
                                }
                            });

                        }
                    }

                }
            }).start();

        }

    }

    public boolean isFrontCam(){
        return mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public interface RecordingCallback {
        public void startedRecording();
        public void stoppedRecording();
        public void onMediaSaved(File file);
        public void timeElapsed(int time);
        public void onError(Throwable throwable);
    }

    public interface PhotoCallback {
        public void onError(Throwable throwable);
        public void onMediaSaved(File file);
    }

    public interface FlashCallback {
        public void onFlashModeSet(String flashMode);
    }

    public CamcorderProfile getCamcorderProfile() {

        CamcorderProfile profile = null;
        if( CamcorderProfile.hasProfile(mCameraID, CamcorderProfile.QUALITY_720P) ) {
            profile = CamcorderProfile.get(mCameraID, CamcorderProfile.QUALITY_720P);
            Log.d("video quality","QUALITY_720P");
        }
        else if( CamcorderProfile.hasProfile(mCameraID, CamcorderProfile.QUALITY_480P) ) {
            profile = CamcorderProfile.get(mCameraID, CamcorderProfile.QUALITY_480P);
            Log.d("video quality","QUALITY_480P");
        }
        else {
            profile = CamcorderProfile.get(mCameraID, CamcorderProfile.QUALITY_HIGH);
            Log.d("video quality", "QUALITY_HIGH");
        }

        profile.videoBitRate = 1500 * 1000;
        //profile.videoFrameRate = 20;

        return profile;

    }

    public void setCameraDisplayOrientation(int cameraId, Camera camera) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(result);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(result);
        camera.setParameters(parameters);

        mCameraRotation = result;

    }

    public Camera.Size getPictureSize(){

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizeList = parameters.getSupportedPictureSizes();

        if(sizeList == null){
            sizeList = parameters.getSupportedPreviewSizes();
        }

        Collections.sort(sizeList, sizeComparator);

        for(Camera.Size size : sizeList){
            Log.d("Camera Size:", "w=" + size.width + " h=" + size.height);
        }

        for(Camera.Size size : sizeList){
            if(size.width <= 1280 && size.height <= 720){
                Log.d("Chosen Picture Size:", "w=" + size.width + " h=" + size.height);
                return size;
            }
        }

        return null;

    }

    public Camera.Size getVideoSizej(){

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizeList = parameters.getSupportedVideoSizes();

        if(sizeList == null){
            sizeList = parameters.getSupportedPreviewSizes();
        }

        Collections.sort(sizeList, sizeComparator);

        for(Camera.Size size : sizeList){
            if(size.width <= 1280 && size.height <= 720){
                Log.d("Chosen Video Size:", "w=" + size.width + " h=" + size.height);
                return size;
            }
        }

        return null;

    }

    public Camera.Size getVideoSize() {

        int maxAllowedWidth = 1280;
        int maxAllowedHeight = 720;
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedVideoSizes();

        Camera.Size optimalSize = null;
        int calcWidth = 0;
        int calcHeight = 0;

        for (Camera.Size size : sizes) {
            Log.d("CameraPreview", "SupportedVideoSize(W, H) -> " + "(" + size.width + "," + size.height + ")");
            int width = size.width;
            int height = size.height;

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = size.width;
                    calcHeight = size.height;
                    optimalSize = size;
                }
            }
        }

        return optimalSize;
    }

    public Comparator<Camera.Size> sizeComparator = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size size, Camera.Size size2) {

            Integer area1 = (size.width * size.height);
            Integer area2 = (size2.width * size2.height);

            return area2.compareTo(area1);

        }
    };

}
