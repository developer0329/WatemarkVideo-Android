package com.animation.golanzo.greenshot.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Transformation {

    private static final String fileName = "data";

    public static void toJson(Context context, List<String> listArray) {
        Gson gson = new Gson();

        Type type = new TypeToken<List<String>>() {
        }.getType();
        String json = gson.toJson(listArray, type);
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> fromJSON(Context context) {
        String jsonString;
        try {
            FileInputStream fos = context.openFileInput(fileName);
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = fos.read()) != -1) {
                builder.append((char) ch);
            }
            fos.close();
            jsonString = builder.toString();
        } catch (IOException e) {
            return new ArrayList<>();
        }
        return new Gson().fromJson(jsonString, new TypeToken<ArrayList<String>>() {
        }.getType());
    }

    public static void addWatermarkToList(Context context, String watermark) {
        ArrayList<String> list = fromJSON(context);
        list.add(watermark);
        toJson(context, list);
    }
}
