package com.melody.cool.myocr.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceHolder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.IntBuffer;

public class CameraSurface extends GLSurfaceView implements SurfaceHolder.Callback, GLSurfaceView.Renderer {

    private SurfaceHolder mSurfaceHolder;
    private SurfaceListener mSurfaceListener;

    private boolean mCaptureFrame;
    private CaptureFrameListener mCaptureFrameListener;

    public SurfaceTexture mSurfaceTexture;

    public CameraSurface(Context context) {

        super(context);

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mSurfaceTexture = new SurfaceTexture(0);

    }

    public SurfaceHolder getSurfaceHolder(){
        return mSurfaceHolder;
    }

    public void setSurfaceListener(SurfaceListener surfaceListener){
        mSurfaceListener = surfaceListener;
    }

    public void captureFrame(CaptureFrameListener captureFrameListener){
        Log.d("CameraSurface", "capturedFrame(CaptureFrameListener) called");
        mCaptureFrameListener = captureFrameListener;
        mCaptureFrame = true;
        requestRender();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mSurfaceListener != null){
            mSurfaceListener.onSurfaceChanged(width, height);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(mSurfaceListener != null){
            mSurfaceListener.onSurfaceCreated();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mSurfaceListener != null){
            mSurfaceListener.onSurfaceDestroyed();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        if(mSurfaceListener != null){
            mSurfaceListener.onMeasure(width, height);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        Log.d("CameraSurface", "onDrawFrame called");
        if (mCaptureFrame) {

            Log.d("CameraSurface", "onDrawFrame, mCaptureFrame=true");

            Bitmap frame = captureFrame(gl10);
            if(mCaptureFrameListener != null){
                mCaptureFrameListener.onFrameCaptured(frame);
            }

            mCaptureFrame = false;

        }
    }

    private Bitmap captureFrame(GL10 gl10){

        final int mWidth = getWidth();
        final int mHeight = getHeight();
        IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
        IntBuffer ibt = IntBuffer.allocate(mWidth * mHeight);
        gl10.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

        // Convert upside down mirror-reversed image to right-side up normal image.
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                ibt.put((mHeight - i - 1) * mWidth + j, ib.get(i * mWidth + j));
            }
        }

        Bitmap mBitmap = Bitmap.createBitmap(mWidth, mHeight,Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(ibt);
        return mBitmap;

    }

    public interface SurfaceListener {
        public void onSurfaceCreated();
        public void onSurfaceChanged(int w, int h);
        public void onSurfaceDestroyed();
        public void onMeasure(int w, int h);
    }

    public interface CaptureFrameListener {
        public void onFrameCaptured(Bitmap frame);
    }

}
