package com.camera.b715.previewframerecord;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    private static int yuvqueuesize = 10;
    public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize);
    public static ArrayBlockingQueue<byte[]> YUVQueue1 = new ArrayBlockingQueue<byte[]>(yuvqueuesize);


    private byte[] mImageCallbackBuffer = new byte[1280
            * 720 * 3 / 2];

    private Camera mCamera;
    private MediaCodec mMediaCodec;
    private TextureView mPreview;
    private VideoEncoderFromBuffer mVideoEncoder;
    private static final String TAG = "liuchang";
    private CameraPreviewCallback mCameraPreviewCallback;
    private Camera.Parameters mCameraParamters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPreview = (TextureView) findViewById(R.id.camera_preview);
        mPreview.setSurfaceTextureListener(this);
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        mCameraParamters = this.mCamera.getParameters();
        Camera.Size size  = mCameraParamters.getPreviewSize();
        Log.d(TAG, "width = " + size.width + "; heigh = " + size.height);
        mCameraParamters.setPreviewFormat(ImageFormat.NV21);
        mCameraParamters.setFlashMode("off");
        this.mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        this.mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        this.mCameraParamters.setPreviewSize(1280, 720);
        //this.mCamera.setDisplayOrientation(90);
        mCameraPreviewCallback = new CameraPreviewCallback();
        //mCamera.addCallbackBuffer(mImageCallbackBuffer);
        //mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
        mCamera.setPreviewCallback(mCameraPreviewCallback);
        mCamera.setParameters(this.mCameraParamters);

        new Thread(new EncodecThread(this)).start();
        Log.d(TAG,"liuchang oncreat tid = " + Thread.currentThread().getId());


    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    class CameraPreviewCallback implements Camera.PreviewCallback {
        private static final String TAG = "CameraPreviewCallback";
        private VideoEncoderFromBuffer videoEncoder = null;
        private VideoEncoderFromBuffer videoEncoder1 = null;

        private CameraPreviewCallback() {
            videoEncoder = new VideoEncoderFromBuffer("A_debug",1280,
                    720);
            videoEncoder1 = new VideoEncoderFromBuffer("B_debug2",1280,
                    720);
        }

        void close() {
            videoEncoder.close();
            videoEncoder1.close();
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.i(TAG, "onPreviewFrame");
            Log.d(TAG,"liuchang onPreviewFrame tid = " + Thread.currentThread().getId());
            long startTime = System.currentTimeMillis();
            //System.arraycopy(data,0,datacopy,0,data.length);
            YUVQueue.add(data);
            YUVQueue1.add(data);
            videoEncoder.encodeFrame(data/*, encodeData*/);
            videoEncoder1.encodeFrame(data/*, encodeData*/);
            long endTime = System.currentTimeMillis();
            Log.i(TAG, Integer.toString((int)(endTime-startTime)) + "ms");
            //camera.addCallbackBuffer(data);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            mCamera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.setPreviewCallback(null);
        mCameraPreviewCallback.close();
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private class EncodecThread implements Runnable {

        int m_width,m_height;
        public EncodecThread(Activity activity){
            m_width = mPreview.getWidth();
            m_height = mPreview.getHeight();
        }

        @Override
        public void run() {

        }
    }
}
