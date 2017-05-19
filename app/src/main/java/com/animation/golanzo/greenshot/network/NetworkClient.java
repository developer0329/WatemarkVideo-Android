package com.animation.golanzo.greenshot.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.animation.golanzo.greenshot.AppConstants;
import com.animation.golanzo.greenshot.AppController;
import com.animation.golanzo.greenshot.ui.fragment.WorkActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Markus Shaker on 21.03.2017.
 */
public class NetworkClient {

    public interface Callback {
        void onResponse(JSONObject jsonObject);

        void isActive(boolean isActive);

        void onFinished(String filePath);
    }

    private static NetworkClient instance;

    private NetworkClient() {
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }


    public void checkActive(String id, String secret, final Callback callback) {
        String tag_json_arry = "json_array_req";
        String url = AppConstants.API_URL + "?" + "id=" + id + "&" + "secure_num=" + secret;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String apiMsg = response.getString("apiMsg");
                            String apiNum = response.getString("apiNum");
                            switch (apiNum) {
                                case "2":
                                    callback.isActive(true);
                                    break;
                                default:
                                    callback.isActive(false);
                                    break;
                            }

                        } catch (JSONException e) {
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_arry);
    }

    public void getJSONObjectResponse(String id, String secret, boolean isWatermarksNeeded, final Callback callback) {
        String tag_json_arry = "json_array_req";

        String url = AppConstants.API_URL + "?" + "id=" + id + "&" + "secure_num=" + secret;
        if (isWatermarksNeeded) {
            url += "&" + "files=1";
        }

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onResponse(response);
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_arry);
    }

    public void downloadImage(String url, final String name, final Context context, final Callback callback) {
        final String imageUrl = url;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String filepath = "";
                File myDir = null;
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setDoOutput(true);
                    urlConnection.connect();
                    String root = context.getFilesDir() + "/watermarks/";
                    //String root = Environment.getExternalStorageDirectory()+"/watermarks/";
                    myDir = new File(root);
                    if (!myDir.exists())
                        myDir.createNewFile();
                    String filename = name;
                    Log.i("Local filename:", "" + filename);
                    WorkActivity.watermarkCounter++;
                    File file = new File(root, filename);
                    if (file.createNewFile()) {
                        file.createNewFile();
                    }
                    FileOutputStream fileOutput = new FileOutputStream(file);
                    InputStream inputStream = urlConnection.getInputStream();
                    int totalSize = urlConnection.getContentLength();
                    int downloadedSize = 0;
                    byte[] buffer = new byte[1024];
                    int bufferLength = 0;
                    while ((bufferLength = inputStream.read(buffer)) > 0) {
                        fileOutput.write(buffer, 0, bufferLength);
                        downloadedSize += bufferLength;
                        Log.i("Progress:", "downloadedSize:" + downloadedSize + "totalSize:" + totalSize);
                    }
                    fileOutput.close();
                    if (downloadedSize == totalSize) filepath = file.getPath();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    filepath = null;
                    e.printStackTrace();
                }
                Log.i("filepath:", " " + filepath);
                callback.isActive(true);
                callback.onFinished(filepath);
            }
        }).start();
    }
}
