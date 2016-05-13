package com.wdd.camera;

import android.os.Environment;

import java.io.File;

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
}
