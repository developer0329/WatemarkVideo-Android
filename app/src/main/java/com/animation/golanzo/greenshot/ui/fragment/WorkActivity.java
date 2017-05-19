package com.animation.golanzo.greenshot.ui.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.animation.golanzo.greenshot.AppConstants;
import com.animation.golanzo.greenshot.AppController;
import com.animation.golanzo.greenshot.R;
import com.animation.golanzo.greenshot.network.NetworkClient;
import com.animation.golanzo.greenshot.utils.Transformation;
import com.animation.golanzo.greenshot.utils.Utils;
import com.bumptech.glide.Glide;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.os.Environment.getExternalStoragePublicDirectory;

/**
 * Created by Markus Shaker on 17.03.2017.
 */
public class WorkActivity extends AppCompatActivity {
    private static final String TAG = "CaptureFragment";

    public static final int PHOTO_MODE = 0;
    public static final int CAMERA_MODE = 1;

    public static final String EXTRA_IMAGE_COUNT = "image_counter";
    public static final String EXTRA_VIDEO_COUNT = "video_counter";
    public static final String EXTRA_WATERMARK_COUNT = "watermark_counter";
    private static final String EXTRA_AVAILABLE_WATERMARKS = "available_watermarks";

    public static SurfaceView surfaceView;

    Camera camera;
    MediaRecorder mediaRecorder;

    @BindView(R.id.captureButton)
    ImageView captureButton;

    @BindView(R.id.leftButton)
    ImageView leftButton;

    @BindView(R.id.rightButton)
    ImageView rightButton;

    @BindView(R.id.modeSelector)
    ImageView modeSelectorButton;

    @BindView(R.id.changeCamera)
    ImageView changeCameraButton;

    @BindView(R.id.galeryButton)
    ImageView galeryButton;

    @BindView(R.id.updateWatermarks)
    ImageView updateWatermarksButton;

    @BindView(R.id.watermark)
    ImageView watermark;

    private ProgressDialog dialog;

    private static int counter = 1;
    private static boolean isFront;
    public boolean isRecord;
    private int mode;

    public static SurfaceHolder holder;

//    private HashSet<String> availableWatermarks;

    private SharedPreferences prefs;

    private ArrayList<String> watermarks = new ArrayList<>();
    private ArrayList<String> tempList;
    private int currentIndex = 0;

    private SurfaceHolder.Callback holderCallback;

    private FFmpeg ffmpeg;

    private int imageCounter;
    private int videoCounter;
    public static int watermarkCounter;

    private File currentVideoFile;

    ContentResolver resolver;

    private boolean safeToTakePicture = false;
    private int videoWidth;
    private int videoHeight;

    boolean continueTask = true;
    private int count = 0, allCount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_capture);
        ButterKnife.bind(this);

        showDialog(getString(R.string.downloading_watermarks));

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        File parentDir = new File(getFilesDir(), "/watermarks/");
        //parentDir = new File(Environment.getExternalStorageDirectory(),"/watermarks/");

        parentDir.mkdirs();

        resolver = getContentResolver();

//        watermarks = Utils.getListFiles(parentDir);
        watermarks = Transformation.fromJSON(this);

        if (watermarks.size() > 0) {
            Glide.with(this).load(watermarks.get(currentIndex)).fitCenter().into(watermark);
        }

        prefs = getSharedPreferences("greenshot_prefs", Context.MODE_PRIVATE);

        imageCounter = prefs.getInt(EXTRA_IMAGE_COUNT, 0);
        imageCounter++;
        videoCounter = prefs.getInt(EXTRA_VIDEO_COUNT, 0);
        videoCounter++;
        watermarkCounter = prefs.getInt(EXTRA_WATERMARK_COUNT, 0);
        watermarkCounter++;

//        availableWatermarks = (HashSet<String>) prefs.getStringSet(EXTRA_AVAILABLE_WATERMARKS, null);
//        if (availableWatermarks == null) {
//            availableWatermarks = new HashSet<>();
//        }
//        camera = Camera.open(0);

        mode = PHOTO_MODE;

        holder = surfaceView.getHolder();
        holderCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setDisplayOrientation(90);
                    camera.setPreviewDisplay(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                if (camera == null) {
                    camera = Camera.open(PHOTO_MODE);
                }

                camera = setCameraParameters(camera, width, height);

                List psizes = camera.getParameters().getSupportedVideoSizes();
                if (psizes != null) {
                    for (Object o : psizes) {
                        Camera.Size size = (Camera.Size) o;
                        Log.e("TAG", "width:" + ((Camera.Size) o).width + ", height:" + ((Camera.Size) o).height);
                    }
                }
                camera.startPreview();
                safeToTakePicture = true;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        };

        isFront = false;

        holder.addCallback(holderCallback);
        leftButton.setEnabled(false);
        rightButton.setEnabled(false);
        refreshWatermarks();
    }

    private void showDialog(String message) {
        dialog = new ProgressDialog(WorkActivity.this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.show();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int arg = msg.arg1;
            switch (arg) {
                case 1:
                    Log.d("MyLogs", "Load first image");
                    Glide.with(WorkActivity.this).load((String) msg.obj).into(watermark);
                    break;
                case 2:
                    Log.d("MyLogs", "Finished");
                    for (String watermark : tempList) {
                        if (watermarks.indexOf(watermark) == -1) {
                            Log.d("MyLogs", "add");
                            watermarks.add(watermark);
                        } else
                            Log.d("MyLogs", "exist watermark");
                    }

                    Transformation.toJson(WorkActivity.this, watermarks);
                    currentIndex = 0;
                    Glide.with(WorkActivity.this).load(watermarks.get(currentIndex)).into(watermark);
                    updateWatermarksButton.setSelected(false);
                    captureButton.setEnabled(true);
                    rightButton.setEnabled(true);
                    if (dialog != null)
                        dialog.dismiss();
                    break;
            }
        }
    };

    private Camera setCameraParameters(Camera camera, int width, int height) {
        int pictureWidth, pictureHeight = 0, previewWidth = 0, previewHeight = 0;

        Log.d("MyLogs", "before - " + width + "x" + height);

        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        Iterator<Camera.Size> itor = sizeList.iterator();
        while (itor.hasNext()) {
            Camera.Size cur = itor.next();
            Log.i("CJT", "所有的  width = " + cur.width + " height = " + cur.height);
            if (cur.width >= previewWidth && cur.height >= previewHeight) {
                width = cur.width;
                height = cur.height;
                previewWidth = cur.width;
                previewHeight = cur.height;
            }
//                    if (cur.width <= 1600 && cur.width >= 960 && cur.height <= 1600 && cur.height >= 960) {
//                    width = cur.width;
//                    height = cur.height;
//                    previewWidth = cur.width;
//                    previewHeight = cur.height;
//                    }
        }

        Log.d("MyLogs", "final preview size - " + previewWidth + "x" + previewHeight);

        pictureWidth = 90000;
        List<Camera.Size> pictureList = parameters.getSupportedPictureSizes();
        Iterator<Camera.Size> itor2 = pictureList.iterator();
        while (itor2.hasNext()) {
            Camera.Size cur = itor2.next();
            Log.i("CJT", "所有的  width = " + cur.width + " height = " + cur.height);
//                    if (cur.width >= pictureWidth && cur.height >= pictureHeight) {
//                        pictureWidth = cur.width;
//                        pictureHeight = cur.height;
//                    }
//                    if (cur.width <= 2000 && cur.width >= 960 && cur.height <= 2000 && cur.height >= 960) {
            if (cur.width >= 960 && pictureWidth > cur.width && cur.height >= 960) {
                pictureWidth = cur.width;
                pictureHeight = cur.height;
//                        Toast.makeText(WorkActivity.this, "RESOLUTION - " + pictureWidth + "x" + pictureHeight, Toast.LENGTH_SHORT).show();
            }
        }

        if (pictureHeight == 0) {
            pictureWidth = 0;
            pictureHeight = 0;
            Iterator<Camera.Size> itor3 = pictureList.iterator();
            while (itor3.hasNext()) {
                Camera.Size cur = itor3.next();
                Log.i("CJT", "所有的  width = " + cur.width + " height = " + cur.height);
                if (cur.width >= pictureWidth && cur.height >= pictureHeight) {
                    pictureWidth = cur.width;
                    pictureHeight = cur.height;
                }
            }
        }

        Log.d("MyLogs", "final picture size - " + pictureWidth + "x" + pictureHeight);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
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

        parameters.setRotation(degrees);
//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setPreviewSize(previewWidth, previewHeight);
        parameters.setPictureSize(pictureWidth, pictureHeight);
        videoWidth = width;
        videoHeight = height;
        camera.setParameters(parameters);
        return camera;
    }

    @Override
    public void onStart() {
        super.onStart();
        ffmpeg = FFmpeg.getInstance(this);
    }

    public void addWatermarkVideo(final File file, Context context) {
        captureButton.setEnabled(false);
//        final ProgressDialog pDialog = new ProgressDialog(this);
//        pDialog.setIndeterminate(true);
        final ProgressDialog progressDialog = new ProgressDialog(context, R.style.CustomDialog);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(AppConstants.ADDING_WATERMARK_PROGRESS);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onFailure() {
                        }

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFinish() {

                        }
                    });
                } catch (FFmpegNotSupportedException e) {

                    // Handle if FFmpeg is not supported by device
                }
                try {
                    final String watermakedFileName = file.getName();
                    final File markedFile = new File(file.getPath() + watermakedFileName.substring(0, watermakedFileName.length() - 1) + "-marked" + ".mp4");
                    Log.e("TAG-MarkedFileName", markedFile.getAbsolutePath());

                    final File f = new File(watermarks.get(currentIndex));
                    Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                    Bitmap scaled = Bitmap.createScaledBitmap(b, b.getWidth() / 9 * 10, b.getHeight() * 2, false);

                    String path = getFilesDir().getAbsolutePath();
                    //String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                    OutputStream fOut = null;
                    Integer counter = 0;
                    final File file = new File(path, "mark" + ".png"); // the File to save , append increasing numeric counter to prevent files from getting overwritten.

                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        scaled.compress(Bitmap.CompressFormat.PNG, 90, out);
                        out.flush();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (videoWidth > 960 && videoHeight > 960) {
                        int x = (videoWidth - 960) / 2;
                        int y = (videoHeight - 960) / 2;

                        String cropFilter = String.format("crop=960:960:%d:%d", y, x);
                        final File cropedFile = new File(file.getPath() + watermakedFileName.substring(0, watermakedFileName.length() - 1) + "-croped" + ".mp4");
                        // Crop Center Video
                        String[] cmd = {"-i", currentVideoFile.getAbsolutePath(), "-filter:v", cropFilter, cropedFile.getAbsolutePath()};

                        // to execute "ffmpeg -version" command you just need to pass "-version"
                        ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                            @Override
                            public void onStart() {
                                Log.e("TAG", "ONSTART");
                            }

                            @Override
                            public void onProgress(String message) {
                                Log.e("TAG", "ONPROGRESS=" + message);
                            }

                            @Override
                            public void onFailure(String message) {
                                Log.e("TAG", "ONFAILURE=" + message);
                            }

                            @Override
                            public void onSuccess(String message) {
                                Log.e("TAG", "ONSUCCESS=" + message);
                            }

                            @Override
                            public void onFinish() {
                                Log.e("TAG", "ONFINISH");
                                ContentValues values = new ContentValues(3);
                                values.put(MediaStore.Video.Media.TITLE, "TITLE");
                                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                                values.put(MediaStore.Video.Media.DATA, cropedFile.getAbsolutePath());
                                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                                try {
                                    String[] cmd = {"-i", cropedFile.getAbsolutePath(), "-i", f.getAbsolutePath(), "-filter_complex", "overlay", markedFile.getAbsolutePath()};
                                    // to execute "ffmpeg -version" command you just need to pass "-version"
                                    ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                                        @Override
                                        public void onStart() {
                                            Log.e("TAG", "ONSTART");
                                        }

                                        @Override
                                        public void onProgress(String message) {
                                            Log.e("TAG", "ONPROGRESS=" + message);
                                        }

                                        @Override
                                        public void onFailure(String message) {
                                            Log.e("TAG", "ONFAILURE=" + message);
                                        }

                                        @Override
                                        public void onSuccess(String message) {
                                            Log.e("TAG", "ONSUCCESS=" + message);
                                        }

                                        @Override
                                        public void onFinish() {
                                            Log.e("TAG", "ONFINISH");

                                            ContentValues values = new ContentValues(3);
                                            values.put(MediaStore.Video.Media.TITLE, "TITLE");
                                            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                                            values.put(MediaStore.Video.Media.DATA, markedFile.getAbsolutePath());
                                            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                                            progressDialog.dismiss();
                                            captureButton.setEnabled(true);
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {

                        String[] cmd = {"-i", currentVideoFile.getAbsolutePath(), "-i", file.getAbsolutePath(), "-c:v", "libx264", "-preset", "ultrafast", "-s", "960x960", "-r", "25", "-filter_complex", "overlay=0:0", markedFile.getAbsolutePath()};
                        // to execute "ffmpeg -version" command you just need to pass "-version"
                        ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                            @Override
                            public void onStart() {
                                Log.e("TAG", "ONSTART");
                            }

                            @Override
                            public void onProgress(String message) {
                                Log.e("TAG", "ONPROGRESS=" + message);
                            }

                            @Override
                            public void onFailure(String message) {
                                Log.e("TAG", "ONFAILURE=" + message);
                            }

                            @Override
                            public void onSuccess(String message) {
                                Log.e("TAG", "ONSUCCESS=" + message);
                            }

                            @Override
                            public void onFinish() {
                                Log.e("TAG", "ONFINISH");

                                ContentValues values = new ContentValues(3);
                                values.put(MediaStore.Video.Media.TITLE, "TITLE");
                                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                                values.put(MediaStore.Video.Media.DATA, markedFile.getAbsolutePath());
                                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                                progressDialog.dismiss();
                                captureButton.setEnabled(true);
                            }
                        });
                    }

                } catch (FFmpegCommandAlreadyRunningException e) {
                    Log.e("TAG", e.getMessage());
                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        }).start();

    }


    private void ffmpegMarkToVideo(File videoFile, File imagefile, final File markedFile, final ProgressDialog progressDialog) {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        prefs.edit().putInt(EXTRA_IMAGE_COUNT, imageCounter)
                .putInt(EXTRA_VIDEO_COUNT, videoCounter)
                .putInt(EXTRA_WATERMARK_COUNT, watermarkCounter)
//                .putStringSet(EXTRA_AVAILABLE_WATERMARKS, availableWatermarks)
                .apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            isFront = false;
            if (camera == null) {
                camera = Camera.open();
            }
        } catch (RuntimeException e) {

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseMediaRecorder();
        if (camera != null)
            camera.release();
        camera = null;
        isFront = false;
    }

    @OnClick(R.id.changeCamera)
    public void changeCamera(View view) {
        camera.stopPreview();
        camera.release();

        if (!isFront) {
            isFront = true;
            camera = Camera.open(1);
        } else {
            isFront = false;
            camera = Camera.open(0);
        }
        if (isFront) {
            changeCameraButton.setSelected(true);
        } else {
            changeCameraButton.setSelected(false);
        }
        try {

            SurfaceHolder holder = surfaceView.getHolder();
            holder.addCallback(holderCallback);
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setCameraDisplayOrientation(this, isFront ? 1 : 0, camera);
        camera.startPreview();
    }

    @OnClick(R.id.modeSelector)
    public void changeMode(View view) {
        if (mode == PHOTO_MODE) {
            captureButton.setImageDrawable(getResources().getDrawable(R.drawable.capture_btn_video));
            mode = CAMERA_MODE;
            modeSelectorButton.setSelected(true);

        } else {
            captureButton.setImageDrawable(getResources().getDrawable(R.drawable.camera_photo_selector));
            mode = PHOTO_MODE;
            modeSelectorButton.setSelected(false);
        }
    }

    @OnClick(R.id.leftButton)
    public void leftButtonPressed(View view) {
        changeWatermark("toleft");
    }

    @OnClick(R.id.captureButton)
    public void capture(View view) {
        if (mode == PHOTO_MODE) {
            if (safeToTakePicture) {
                camera.takePicture(new Camera.ShutterCallback() {
                    public void onShutter() {
                        AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
                    }
                }, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        showDialog(getString(R.string.saving_images));
                        camera.startPreview();
                        new Thread(new ImageSaver(data)).start();
                    }
                });
                safeToTakePicture = false;
            }
            captureButton.setSelected(false);
        } else {
            if (!isRecord) {
                captureButton.setSelected(true);
                isRecord = true;
                startRecord();
            } else {
                isRecord = false;
                captureButton.setSelected(false);
                stopRecord();
            }
        }
    }

    @OnClick(R.id.rightButton)
    public void rightButtonPressed(View view) {
        Log.d("MyLogs", "Click");
        changeWatermark("toright");
    }

    @OnClick(R.id.galeryButton)
    public void openGalery(View view) {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setType("image/*");
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);

//        Intent intent = new Intent();
//        intent.setType("image/*");
//        intent.setAction(Intent.ACTION_GET_CONTENT);//
//        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "content://media/internal/images/media"));
        startActivity(intent);
    }

    @OnClick(R.id.updateWatermarks)
    public void updateWatermarks(View view) {
        showDialog(getString(R.string.downloading_watermarks));
        refreshWatermarks();
    }

    public void changeWatermark(String leftright) {
        if (watermarks != null && watermarks.size() != 0) {
            if (leftright.equals("toright")) {
                currentIndex = currentIndex == watermarks.size() - 1 ? currentIndex : ++currentIndex;
            } else if (leftright.equals("toleft")) {
                currentIndex = currentIndex == 0 ? currentIndex : --currentIndex;
            }
            Glide.with(this).load(watermarks.get(currentIndex)).into(watermark);

            if (currentIndex == 0) {
                leftButton.setEnabled(false);
                rightButton.setEnabled(true);
            } else if (currentIndex == watermarks.size() - 1) {
                rightButton.setEnabled(false);
                leftButton.setEnabled(true);
            } else {
                rightButton.setEnabled(true);
                leftButton.setEnabled(true);
            }
        }
    }

    private boolean prepareVideoRecorder() {
        mediaRecorder = new MediaRecorder();
        // Both are required for Portrait Video
        camera.setDisplayOrientation(90);
        mediaRecorder.setOrientationHint(90);

        camera.unlock();

        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        String root = getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        int number = new Random().nextInt(100000);
        File videoFile = new File(root + "GreenShot-Movie-" + number + ".mp4");

        ContentValues values = new ContentValues(3);
        values.put(MediaStore.Video.Media.TITLE, "TITLE");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
        resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);


            /*MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(your_data_source);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInmillisec = Long.parseLong( time );
            long duration = timeInmillisec / 1000;
            long hours = duration / 3600;
            long minutes = (duration - hours * 3600) / 60;
            long seconds = duration - (hours * 3600 + minutes * 60);
*/
        ContentResolver cr = getContentResolver();
        cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        currentVideoFile = videoFile;
        videoCounter++;
        try {
            videoFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
//            mediaRecorder.setVideoSize(960,960);
        mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
            addWatermarkVideo(currentVideoFile, this);
        }
    }

    public void startRecord() {
        if (prepareVideoRecorder()) {
            mediaRecorder.start();
        } else {
            releaseMediaRecorder();
        }
    }

    public void stopRecord() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            releaseMediaRecorder();
        }
    }

    public Bitmap mark(Bitmap src, Bitmap watermark, Point location, boolean underline) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(960, 960, src.getConfig());

        Canvas canvas = new Canvas(result);

        Bitmap newSrc;
        if (w >= 960 && h >= 960) {
            newSrc = Bitmap.createBitmap(src, (w - 960) / 2, (h - 960) / 2, result.getWidth(), result.getHeight());
        } else {
            newSrc = Bitmap.createScaledBitmap(src, result.getWidth(), result.getHeight(), false);
        }

        canvas.drawBitmap(newSrc, 0, 0, null);

//        Bitmap biggerWatermark = Bitmap.createScaledBitmap(watermark, 960,960, false);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setUnderlineText(underline);
//        canvas.drawBitmap(biggerWatermark, 10, canvas.getHeight()-biggerWatermark.getHeight()-20, paint);
        if (isFront) {
            Bitmap biggerWatermark = Bitmap.createScaledBitmap(watermark, 960, 960, false);
            canvas.drawBitmap(biggerWatermark, 0, 0, paint);
        } else {
            canvas.drawBitmap(watermark, 0, 0, paint);
        }

        return result;
    }

    public void saveBitmaps(Bitmap original, Bitmap marked) {
        Random random = new Random();
        int number = random.nextInt(100000);
        String fname = "Image-" + number + ".jpg";
        String fmarked = "Image-" + number + "-marked" + ".jpg";

//        if (isFront){
//            original = Bitmap.createScaledBitmap(original,960,960,false);
//        }
        Utils.saveBitmap(original, fname, this);


        Utils.saveBitmap(marked, fmarked, this);
        imageCounter += 2;
    }

    private class ImageSaver implements Runnable {
        private byte[] data;

        public ImageSaver(byte[] d) {
            data = d;
        }

        @Override
        public void run() {
            try {
                Log.d("MyLogs", "Start saving2");
                String photoFile = new Date().toString() + ".jpg";
                String imageFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + photoFile;
//                String imageFilePath = getFilesDir().getPath() + File.separator + photoFile;
                File pictureFile = new File(imageFilePath);
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();


                Bitmap mBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
                Log.d("MyLogs", "Width source - " + mBitmap.getWidth());
                Log.d("MyLogs", "Height source - " + mBitmap.getHeight());
//
                ExifInterface exif = new ExifInterface(imageFilePath);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Log.d("MyLogs", "rotation - " + rotation);

                if (isFront) {
                    switch (rotation) {
                        case ExifInterface.ORIENTATION_NORMAL:
                            Log.d("MyLogs", "0");
                            mBitmap = rotateImage(mBitmap, 270);
                            break;
                        case ExifInterface.ORIENTATION_UNDEFINED:
                            mBitmap = rotateImage(mBitmap, 270);
                            Log.d("MyLogs", "1");
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            mBitmap = rotateImage(mBitmap, 180 + 270);
                            Log.d("MyLogs", "2");
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            mBitmap = rotateImage(mBitmap, 90 + 270);
                            Log.d("MyLogs", "3");
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            mBitmap = rotateImage(mBitmap, 270 + 270);
                            Log.d("MyLogs", "4");
                            break;
                    }
                } else {
                    switch (rotation) {
                        case ExifInterface.ORIENTATION_NORMAL:
                            Log.d("MyLogs", "0");
                            mBitmap = rotateImage(mBitmap, 90);
                            break;
                        case ExifInterface.ORIENTATION_UNDEFINED:
                            mBitmap = rotateImage(mBitmap, 90);
                            Log.d("MyLogs", "1");
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            mBitmap = rotateImage(mBitmap, 180 + 90);
                            Log.d("MyLogs", "2");
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            mBitmap = rotateImage(mBitmap, 90 + 90);
                            Log.d("MyLogs", "3");
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            mBitmap = rotateImage(mBitmap, 270 + 90);
                            Log.d("MyLogs", "4");
                            break;
                    }
                }

                Log.d("MyLogs", "Width new - " + mBitmap.getWidth());
                Log.d("MyLogs", "Height new - " + mBitmap.getHeight());

//                Bitmap watermark = BitmapFactory.decodeFile(Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream()););
                Bitmap watermark = BitmapFactory.decodeFile(watermarks.get(currentIndex));
                Bitmap result = mark(mBitmap, watermark, new Point(), true);
                saveBitmaps(mBitmap, result);
                pictureFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Log.d("MyLogs", "Finally");
                if (dialog != null)
                    dialog.dismiss();
            }
            safeToTakePicture = true;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
//        Bitmap rotatedImg = ;
//        img.recycle();
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public void setCameraDisplayOrientation(Activity activity,
                                            int cameraId, android.hardware.Camera camera) {


        camera = setCameraParameters(camera, 0, 0);

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        Log.d("MyLogs", "ROTATION - " + rotation);
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
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void refreshWatermarks() {
        Log.e(TAG, "refreshWatermarks()");
        updateWatermarksButton.setSelected(true);
        captureButton.setEnabled(false);

//        if (watermarks == null)
//        watermarks = new ArrayList<>();
        tempList = new ArrayList<>();

        String url = AppConstants.API_URL + "&" + "id=" + prefs.getString("id", "-1") + "&" + "secure_num=" + prefs.getString("secret", "-1") + "&" + "files=1";
        Log.d("MyLogs", "URL - " + url);

        String tag_json_arry = "json_array_req";
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, response.toString());
                        try {
                            String apiNum = response.getString("apiNum");
                            count = 0;
                            allCount = 0;
                            String path = getFilesDir() + "/watermarks/";
                            if (apiNum.equals("-4")) {
                                Iterator<String> keys = response.keys();
                                while (keys.hasNext()) {
                                    String ck = keys.next();
                                    if (ck.contains("apifile")) {
                                        String url = response.getString(ck);
                                        String name = url.substring(url.lastIndexOf("/") + 1);
                                        if (watermarks.indexOf(path + name) == -1) {
                                            count++;
                                            Log.d("MyLogs", "Count - " + count);
                                            NetworkClient.getInstance().downloadImage(url, name, WorkActivity.this, new NetworkClient.Callback() {
                                                @Override
                                                public void onResponse(JSONObject jsonObject) {
                                                }

                                                @Override
                                                public void isActive(boolean isActive) {
                                                }

                                                @Override
                                                public void onFinished(String filePath) {
                                                    allCount++;
                                                    tempList.add(filePath);
                                                    if (tempList.size() == 1) {
                                                        Message msg = new Message();
                                                        msg.arg1 = 1;
                                                        msg.obj = filePath;
                                                        mHandler.sendMessage(msg);
                                                    }
                                                    if (count == allCount) {
                                                        Message msg = new Message();
                                                        msg.arg1 = 2;
                                                        mHandler.sendMessage(msg);
                                                    }
                                                }
                                            });
//                                        } else {
//                                            Log.d("MyLogs", "Contain");
//                                        }
                                        } else if (count == 0) {
                                            Log.d("MyLogs", "EXIST");
                                            updateWatermarksButton.setSelected(false);
                                            captureButton.setEnabled(true);
                                            rightButton.setEnabled(true);
                                            if (dialog != null)
                                                dialog.dismiss();
                                        }
                                    }
                                }

//                                if (watermark.getDrawable() == null && watermarks == null) {
//
//                                    //File parentDir = new File(CaptureFragment.this.getFilesDir()+"/watermarks/");
//                                    File parentDir = new File(Environment.getDataDirectory(), "/watermarks/");
//                                    parentDir.mkdir();
//                                    watermarks = Utils.getListFiles(parentDir);
//                                    if (watermarks == null)
//                                        return;
////                                        Glide.with(CaptureFragment.this).load(watermarks.get(currentIndex)).into(watermark);
//                                    currentIndex = 0;
//                                    changeWatermark("center");
//                                }

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_arry);
    }

    private void refreshArrayList(File parent) {
        watermarks = Utils.getListFiles(parent);
        currentIndex = 0;
        Glide.with(this).load(watermarks.get(currentIndex)).into(watermark);

    }
/*    private void checkNewWatermarks(){
        NetworkClient client = NetworkClient.getInstance();
        client.getJSONObjectResponse(prefs.getString("id", "-1"), prefs.getString("secret", "-1"), true
                , new NetworkClient.Callback() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    String apiNum = jsonObject.getString("apiNum");

                    switch (apiNum){
                        case "-4":
                            Iterator<String> keys = jsonObject.keys();
                            while (keys.hasNext()){
                                String ck = keys.next();
                                if (ck.contains("apifile")){
                                    if (!availableWatermarks.contains(ck)){
                                        updateWatermarksButton.setEnabled(true);
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    *//*NOP*//*
                }
            }
            @Override
            public void isActive(boolean isActive) {
                *//*NOP*//*
            }
        });
    }*/
}