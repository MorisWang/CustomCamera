package com.wdd.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    private Camera mCamera;
    private SurfaceView mCameraView;
    private SurfaceHolder mSurfaceHolder;
    private boolean isBackCameraOn = true;
    private boolean isPreviewMode = true;   //预览模式
    private SensorManager sensorManager;
    private float mLastX = 0;
    private float mLastY = 0;
    private float mLastZ = 0;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mSurfaceHolder.getSurface() == null) {
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        bindSurfaceHolder();
        bindEvent();
    }

    //绑定SurfaceHolder
    private void bindSurfaceHolder(){
        mCameraView = (SurfaceView) findViewById(R.id.camera_view);
        mSurfaceHolder = mCameraView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    //绑定事件
    private void bindEvent(){

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorManager.registerListener(sensorEventListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        findViewById(R.id.switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        findViewById(R.id.take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    private void takePhoto(){
        if (!isPreviewMode) {
            return;
        }

        //不聚焦了，直接拍
        try {
            mCamera.takePicture(shutterCallback, null, jpegCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

        isPreviewMode = false;
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            startPreview();
            //TODO:保存图片
            File photoFile = CameraUtils.getPhoto();
            if(photoFile == null){
                startPreview();
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(photoFile);
                fos.write(data);
                fos.close();
            }catch (IOException e){

            }
        }
    };

    //预览
    private void startPreview(){
        isPreviewMode = true;
        try{
            //获取屏幕宽高
            WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
            int screenWidth = wm.getDefaultDisplay().getWidth();
            int screenHeight = wm.getDefaultDisplay().getHeight();

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPictureFormat(ImageFormat.JPEG);
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
            List<String> focusModes = parameters.getSupportedFocusModes();

            Camera.Size previewSize = getOptimalPreviewSize(previewSizes, screenWidth, screenHeight);
            Camera.Size pictureSize = getOptimalPreviewSize(pictureSizes, screenWidth, screenHeight);

            parameters.setPreviewSize(previewSize.width, previewSize.height);
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            parameters.setRotation(getPhotoDegree());

            mCamera.setDisplayOrientation(90);
            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //获取照片旋转的角度
    private int getPhotoDegree() {
        if (isBackCameraOn) {
            //后置摄像头
            return 90;
        } else {
            //前置摄像头
            return 270;
        }
    }

    //获取合适的尺寸
    public static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h){
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
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

    //切换前后摄像头
    private void switchCamera(){
        int cameraCount;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        // 遍历可用摄像头
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (isBackCameraOn) {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    releaseCamera();
                    mCamera = null;
                    mCamera = Camera.open(i);
                    isBackCameraOn = false;
                    startPreview();
                    break;
                }
            } else {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    releaseCamera();
                    mCamera = null;
                    mCamera = Camera.open(i);
                    isBackCameraOn = true;
                    startPreview();
                    break;
                }
            }
        }
    }

    //聚焦
    private void autoFocus() {
        if (mCamera != null && isPreviewMode) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (!success) {
                        //失败了就一直对焦吧
                        autoFocus();
                    }
                }
            });
        }
    }

    //释放相机
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    //监听传感器
    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float deltaX = Math.abs(mLastX - x);
            float deltaY = Math.abs(mLastY - y);
            float deltaZ = Math.abs(mLastZ - z);

            if (deltaX > .5) {
                autoFocus();
            }
            if (deltaY > .5) {
                autoFocus();
            }
            if (deltaZ > .5) {
                autoFocus();
            }

            mLastX = x;
            mLastY = y;
            mLastZ = z;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public static boolean checkHardware(Context mContext){
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkHardware(MainActivity.this) && mCamera == null) {
            //判断前后摄像头
            int cameraCount;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            // 遍历可用摄像头
            for (int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (isBackCameraOn) {
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        releaseCamera();
                        mCamera = null;
                        try {
                            mCamera = Camera.open(i);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        startPreview();
                        break;
                    }
                } else {
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        releaseCamera();
                        mCamera = null;
                        try {
                            mCamera = Camera.open(i);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        startPreview();
                        break;
                    }
                }
            }

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        if (sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }
}
