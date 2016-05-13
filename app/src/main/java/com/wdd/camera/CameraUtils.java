package com.wdd.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;

import java.io.File;
import java.util.List;

/**
 * Created by wangdongdong on 5/13/16.
 */
public class CameraUtils {

    //相机拍照保存的地址
    public static String CAMERA_PHOTO = Environment.getExternalStorageDirectory() + "/custom_camera/";

    /**
     * 获取图片存储路径
     * */
    public static File getPhoto(){
        File photoFile = null;
        File photoDir = new File(CAMERA_PHOTO);
        if(!photoDir.exists()){
            if(!photoDir.mkdirs()){
                return null;
            }
        }

        photoFile = new File(CAMERA_PHOTO + System.currentTimeMillis() + ".jpg");
        return photoFile;
    }

    /**
     * 获取合适的尺寸
     * */
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

    /**
     * 获取照片旋转的角度
     * */
    public static int getPhotoDegree(boolean isBackCameraOn) {
        if (isBackCameraOn) {
            //后置摄像头
            return 90;
        } else {
            //前置摄像头
            return 270;
        }
    }

    /**
     * 检查相机是否可用
     * */
    public static boolean checkHardware(Context mContext){
        return mContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA);
    }
}
