package com.animation.golanzo.greenshot;

/**
 * Created by Markus Shaker on 19.03.2017.
 */
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.animation.golanzo.greenshot.ui.fragment.WorkActivity;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RecorderService extends Service {
    private static final String TAG = "RecorderService";
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private static Camera mServiceCamera;
    private boolean mRecordingStatus;
    private MediaRecorder mMediaRecorder;
    private int counter;

    @Override
    public void onCreate() {
        mRecordingStatus = false;
        //mServiceCamera = CameraRecorder.mCamera;
        mServiceCamera = Camera.open();
        mServiceCamera.setDisplayOrientation(90);
        mSurfaceView = WorkActivity.surfaceView;
        mSurfaceHolder = WorkActivity.holder;

        super.onCreate();
        if (!mRecordingStatus)
            startRecording();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        mRecordingStatus = false;

        super.onDestroy();
    }

    public boolean startRecording(){
        try {
            Toast.makeText(getApplicationContext(), "Recording Started", Toast.LENGTH_SHORT).show();

            //mServiceCamera = Camera.open();
            Camera.Parameters params = mServiceCamera.getParameters();
            mServiceCamera.setParameters(params);
            Camera.Parameters p = mServiceCamera.getParameters();

            final List<Size> listSize = p.getSupportedPreviewSizes();
            Size mPreviewSize = listSize.get(2);
            Log.v(TAG, "use: width = " + mPreviewSize.width
                    + " height = " + mPreviewSize.height);
            p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
            mServiceCamera.setParameters(p);

            try {
                mServiceCamera.setPreviewDisplay(mSurfaceHolder);
                mServiceCamera.startPreview();
            }
            catch (IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }

            mServiceCamera.unlock();

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setCamera(mServiceCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

            counter++;
            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
            File videoFile = new File(root + "/GreenShotMedia/video"+"GreenShot-"+counter+".mp4");
            try {
                videoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mMediaRecorder.setOutputFile(videoFile.getAbsolutePath());
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(mPreviewSize.width, mPreviewSize.height);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            mMediaRecorder.prepare();
            mMediaRecorder.start();

            mRecordingStatus = true;

            return true;
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void stopRecording() {
        Toast.makeText(getApplicationContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
        try {
            mServiceCamera.reconnect();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        mServiceCamera.stopPreview();
        mMediaRecorder.release();

        mServiceCamera.release();
        mServiceCamera = null;
    }
}
