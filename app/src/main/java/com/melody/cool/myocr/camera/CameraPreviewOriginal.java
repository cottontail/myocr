package com.melody.cool.myocr.camera;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melody.cool.myocr.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraPreviewOriginal {

    private Context mContext;

    private int mCameraID = -1;
    private boolean mCameraInPreview;

    private Camera mCamera;

    private CameraSurface mCameraSurface;

    private Camera.Size mPreviewSize;
    private List<Camera.Size> mSupportedPreviewSizes = new ArrayList<>();

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

    public CameraPreviewOriginal(Context context){

        mContext = context;

        mCameraSurface = new CameraSurface(mContext);

        MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(mContext);
        dialogBuilder.progress(true, 0);
        dialogBuilder.content(mContext.getString(R.string.info_one_moment));
        mProgressDialog = dialogBuilder.build();
        mProgressDialog.setCancelable(false);

        mTimeHandler = new Handler();
        mMediaRecorder = new MediaRecorder();

        mCameraSurface.setSurfaceListener(new CameraSurface.SurfaceListener() {

            @Override
            public void onSurfaceCreated() {
                if (mCamera == null && mCameraID != -1) {
                    initCamera(mCameraID);
                    startPreview();
                } else {
                    startPreview();
                }
            }

            @Override
            public void onSurfaceChanged(int w, int h) {
                if(mCamera != null) {
                    try {
                        Camera.Parameters parameters = mCamera.getParameters();
                        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
                        Camera.Size size = getBestPreviewSize(sizeList, w, h);
                        parameters.setPreviewSize(size.width, size.height);
                        mCamera.setParameters(parameters);
                        startPreview();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onSurfaceDestroyed() {
                stopRecording();
                releaseCamera();
            }

            @Override
            public void onMeasure(int width, int height) {
                if (mSupportedPreviewSizes != null) {
                    mPreviewSize = getBestPreviewSize(mSupportedPreviewSizes, width, height);
                }
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

    }

    public void attachToView(ViewGroup viewGroup){
        viewGroup.addView(mCameraSurface);
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

    public void initCamera(int cameraID){

        pausePreview();
        mCameraID = cameraID;

        try {

            mCamera = Camera.open(mCameraID);
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
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
                if(flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)){
                    mFlashModes.add(Camera.Parameters.FLASH_MODE_AUTO);
                }
                if(flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)){
                    mFlashModes.add(Camera.Parameters.FLASH_MODE_TORCH);
                }

            }

            Camera.Size size = getPictureSize();
            //Camera.Size size = mPreviewSize;
            parameters.setPictureSize(size.width, size.height);
            if(mPreviewSize != null){
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            }
            mCamera.setParameters(parameters);

        } catch(Exception e){
            if(mRecordingCallback != null){
                e.printStackTrace();
                mRecordingCallback.onError(e);
            }
        }

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

            mCamera.setPreviewDisplay(mCameraSurface.getSurfaceHolder());
            mCamera.startPreview();
            mCameraInPreview = true;

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

            mMediaRecorder = new MediaRecorder();  // Works well
            mCamera.unlock();

            if(mCameraID == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mMediaRecorder.setOrientationHint(270);
            } else {
                mMediaRecorder.setOrientationHint(90);
            }

            mMediaRecorder.setPreviewDisplay(mCameraSurface.getSurfaceHolder().getSurface());
            mMediaRecorder.setCamera(mCamera);

            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            //mMediaRecorder.setProfile(profile);
            mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
            mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
            mMediaRecorder.setVideoSize(size.width, size.height);

            mMediaRecorder.setMaxDuration(10 * 1000); //aprox 10 seconds...
            mMediaRecorder.setMaxFileSize(2000000); //2mb

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

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(mFlashMode);
        mCamera.setParameters(parameters);

        if(mFlashCallback != null){
            mFlashCallback.onFlashModeSet(mFlashMode);
        }

    }

    public void setNextFlashMode(){

        Camera.Parameters parameters = mCamera.getParameters();

        int currentFlash = mFlashModes.indexOf(parameters.getFlashMode());
        if(currentFlash < mFlashModes.size() - 1){
            currentFlash++;
            mFlashMode = mFlashModes.get(currentFlash);
        } else {
            mFlashMode = mFlashModes.get(0);
        }

        setFlashMode(mFlashMode);

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

    public CameraSurface getCameraSurface(){
        return mCameraSurface;
    }

    public void takePhoto(final File outputFile, final PhotoCallback photoCallback){

        if(!isRecording()){

            try {
                setCameraDisplayOrientation(mCameraID, mCamera);
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(final byte[] bytes, Camera camera) {

                        mProgressDialog.show();

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                try {

                                    if (bytes != null) {

                                        FileOutputStream outputStream = new FileOutputStream(outputFile);
                                        outputStream.write(bytes);

                                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mProgressDialog.dismiss();
                                                photoCallback.onMediaSaved(outputFile);
                                            }
                                        });

                                        /*

                                        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
                                        int screenHeight = mContext.getResources().getDisplayMetrics().heightPixels;

                                        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, (bytes != null) ? bytes.length : 0);

                                        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                                            // Notice that width and height are reversed
                                            Bitmap scaled = Bitmap.createScaledBitmap(bm, screenHeight, screenWidth, true);
                                            int w = scaled.getWidth();
                                            int h = scaled.getHeight();
                                            // Setting post rotate to 90
                                            Matrix mtx = new Matrix();
                                            if(isFrontCam()){
                                                //mtx.postRotate(0);
                                            } else {
                                                //mtx.postRotate(180);
                                            }
                                            mtx.postRotate(90);
                                            // Rotating Bitmap
                                            bm = Bitmap.createBitmap(scaled, 0, 0, w, h, mtx, true);
                                        } else {// LANDSCAPE MODE
                                            //No need to reverse width and height
                                            Bitmap scaled = Bitmap.createScaledBitmap(bm, screenWidth, screenHeight, true);
                                            bm = scaled;
                                        }

                                        bm.compress(Bitmap.CompressFormat.PNG, 80, new FileOutputStream(outputFile));

                                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mProgressDialog.dismiss();
                                                photoCallback.onMediaSaved(outputFile);
                                            }
                                        });
                                        */


                                    } else {
                                        if (photoCallback != null) {
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mProgressDialog.dismiss();
                                                    photoCallback.onError(new Exception("Failed to Take Photo"));
                                                }
                                            });

                                        }
                                    }

                                } catch (final Exception e) {
                                    if (photoCallback != null) {
                                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                photoCallback.onError(e);
                                            }
                                        });

                                    }
                                }

                            }
                        }).start();

                    }
                });
            } catch(Exception e){
                photoCallback.onError(e);
            }

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

    }

    private Camera.Size getBestPreviewSize(List<Camera.Size> sizes, int w, int h) {

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;

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

    public Camera.Size getVideoSize(){

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

    public Comparator<Camera.Size> sizeComparator = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size size, Camera.Size size2) {

            Integer area1 = (size.width * size.height);
            Integer area2 = (size2.width * size2.height);

            return area2.compareTo(area1);

        }
    };

}
