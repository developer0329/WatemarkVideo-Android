package com.animation.golanzo.greenshot.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.animation.golanzo.greenshot.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Markus Shaker on 19.03.2017.
 */
public class Utils {
    public static ArrayList<String> getListFiles(File parentDir) {
        ArrayList<String> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
//            if (file.isDirectory()) {
//                inFiles.addAll(getListFiles(file));
//            } else {
                if(file.getName().endsWith(".png")){
                    inFiles.add(file.getAbsolutePath());
//                }
            }
        }
        return inFiles;
    }
    public static void saveBitmap(Bitmap bitmap, String filename, Context context){

        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        File file = new File(root, filename);
        Log.e("TAG", "FILE:"+file.getAbsolutePath());
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, file.getName());
            values.put(MediaStore.Images.Media.DESCRIPTION, context.getString(R.string.picture_description));
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
            values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
            values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
            values.put("_data", file.getAbsolutePath());

            ContentResolver cr = context.getContentResolver();
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
