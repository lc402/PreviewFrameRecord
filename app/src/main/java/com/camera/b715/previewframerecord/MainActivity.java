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
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,
        Camera.PreviewCallback {

    protected final Object mSync = new Object();

    private byte[] mImageCallbackBuffer = new byte[1280
            * 720 * 3 / 2];

    private Camera mCamera;
    private TextureView mPreview;
    private static final String TAG = "liuchang";
    private Camera.Parameters mCameraParamters;
    private Button mButton;
    private VideoEncoderFromBuffer mFirstThread;
    private VideoEncoderFromBuffer mSecondThread;

    public static final String FIRST = "first";
    public static final String SECOND = "second";
    private boolean isRecording = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPreview = (TextureView) findViewById(R.id.camera_preview);
        mButton = (Button) findViewById(R.id.record);
        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isRecording) {
                    isRecording = false;
                    VideoEncoderFromBuffer.pause = true;
                    mButton.setText("isStop");
                } else {
                    mButton.setText("isdoing");
                    if (VideoEncoderFromBuffer.pause) {

                    } else {
                        mFirstThread.start();
                    }
                    isRecording = true;
                }
                
            }
        });
        mPreview.setSurfaceTextureListener(this);
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        mCameraParamters = this.mCamera.getParameters();
        Camera.Size size = mCameraParamters.getPreviewSize();
        Log.d(TAG, "width = " + size.width + "; heigh = " + size.height);
        mCameraParamters.setPreviewFormat(ImageFormat.NV21);
        mCameraParamters.setFlashMode("off");
        this.mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        this.mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        this.mCameraParamters.setPreviewSize(1280, 720);
        //this.mCamera.setDisplayOrientation(90);
        //mCamera.addCallbackBuffer(mImageCallbackBuffer);
        //mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
        mCamera.setPreviewCallback(this);
        mCamera.setParameters(this.mCameraParamters);

        mFirstThread = new VideoEncoderFromBuffer("A_debug", 1280,
                720, mSync, mHandler, FIRST);
        mSecondThread = new VideoEncoderFromBuffer("B_debug", 1280,
                720, mSync, mHandler, SECOND);

        Log.d(TAG, "liuchang oncreat tid = " + Thread.currentThread().getName());
    }

    @Override
    protected void onStop() {
        VideoEncoderFromBuffer.exit= true;
        super.onStop();

    }

    static final int NEXT_THREAD_BEGIN_WORK = 1;
    static final int CURRENT_THREAD_STOP_WORK = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEXT_THREAD_BEGIN_WORK:
                    Log.d(TAG,"NEXT_THREAD_BEGIN_WORK msg is " + msg.obj);
                    if (FIRST.equals(msg.obj)) {
                        if (mSecondThread.isAlive()) {
                            synchronized (mSync) {
                                mSync.notify();
                            }
                        } else {
                            Log.d(TAG,"liuchang second started");
                            mSecondThread.start();
                        }
                    } else if (SECOND.equals(msg.obj)) {
                        synchronized (mSync) {
                            mSync.notify();
                        }
                    }
                    break;
                case CURRENT_THREAD_STOP_WORK:
                    /*if (FIRST.equals(msg.arg1)) {
                        recordingFirst = false;
                    } else if (SECOND.equals(msg.arg1)) {
                        recordingSecond = false;
                    }*/
                    break;
                default:
                    Log.d(TAG, "do nothing");
                    break;
            }
        }
    };

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (mFirstThread.getState() == Thread.State.RUNNABLE) {
            Log.d(TAG, "liuchang add YUVQueue");
            mFirstThread.addByteData(data);
        } else {
            mFirstThread.clearData();
        }
        if (mSecondThread.getState() == Thread.State.RUNNABLE) {
            mSecondThread.addByteData(data);
            Log.d(TAG, "liuchang add YUVQueue1");
        } else {
            mSecondThread.clearData();
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
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
