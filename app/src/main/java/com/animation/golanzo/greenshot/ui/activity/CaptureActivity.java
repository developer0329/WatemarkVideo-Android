package com.animation.golanzo.greenshot.ui.activity;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.animation.golanzo.greenshot.AppConstants;
import com.animation.golanzo.greenshot.AppController;
import com.animation.golanzo.greenshot.R;
import com.animation.golanzo.greenshot.network.NetworkClient;
import com.animation.golanzo.greenshot.ui.fragment.WorkActivity;
import com.animation.golanzo.greenshot.ui.fragment.RegisterFragment;
import com.crashlytics.android.Crashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import io.fabric.sdk.android.Fabric;

public class CaptureActivity extends AppCompatActivity implements RegisterFragment.Listener {

    private static final String TAG = "CaptureActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 55;
    private RegisterFragment mRegisterFragment;
    private WorkActivity mCaptureFragment;
    private FragmentTransaction ft;

    private SharedPreferences prefs;

    private NetworkClient networkClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            } else {
                initFragment();
            }
        } else {
            initFragment();
        }
//        networkClient = NetworkClient.getInstance();

        setContentView(R.layout.activity_capture);
    }

    private void initFragment() {
        prefs = getSharedPreferences("greenshot_prefs", Context.MODE_PRIVATE);

        Boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
        Log.d(TAG, "isFIRSTRUN=" + isFirstRun);

        ft = getFragmentManager().beginTransaction();
        if (isFirstRun) {
            mRegisterFragment = RegisterFragment.getInstance(this);

            ft.replace(R.id.container, mRegisterFragment).commit();
        } else {
            startActivity(new Intent(this, WorkActivity.class));
            finish();
//            mCaptureFragment = new WorkActivity();
//            ft.add(R.id.container, mCaptureFragment).commit();
//                try{
//                    checkActive();
//                }catch (Exception e){
//                }
        }
    }

    @Override
    public void onRegister(String result) {
        switch (result) {
            case "success":
                prefs.edit().putBoolean("isFirstRun", false).apply();
                startActivity(new Intent(this, WorkActivity.class));
                finish();
//                ft = getFragmentManager().beginTransaction();
//                mCaptureFragment = new WorkActivity();
//                ft.add(R.id.container, mCaptureFragment).commit();
                break;
            case "not_exists":
                Toast.makeText(this, AppConstants.USER_NOT_EXISTS, Toast.LENGTH_SHORT).show();
                break;
            case "not_match":
                Toast.makeText(this, AppConstants.NOT_MATCHED_CREDENTIALS, Toast.LENGTH_SHORT).show();
                break;
            case "inactive":
                Toast.makeText(this, AppConstants.USER_IS_INACTIVE, Toast.LENGTH_SHORT).show();
                break;
            case "error":
                Toast.makeText(this, AppConstants.USER_IS_INACTIVE, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    private void checkActive() {
        String tag_json_arry = "json_array_req";
        String url = AppConstants.API_URL + "?" + "id=" + prefs.getString("id", "-1") + "&" + "secure_num=" + prefs.getString("secret", "-1");

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String apiNum = response.getString("apiNum");
                            switch (apiNum) {
                                case "2":
                                    break;
                                default:
                                    Toast.makeText(CaptureActivity.this, AppConstants.USER_IS_INACTIVE, Toast.LENGTH_LONG).show();
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    prefs = getSharedPreferences("greenshot_prefs", Context.MODE_PRIVATE);

                    Boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
                    Log.d(TAG, "isFIRSTRUN=" + isFirstRun);

                    ft = getFragmentManager().beginTransaction();
                    if (isFirstRun) {
                        mRegisterFragment = RegisterFragment.getInstance(this);

                        ft.replace(R.id.container, mRegisterFragment).commit();
                    } else {
                        startActivity(new Intent(this, WorkActivity.class));
                        finish();
//                        mCaptureFragment = new WorkActivity();
//                        ft.add(R.id.container, mCaptureFragment).commit();

//            showAlert(this,"Before checkActive");
//                        try{
//                            checkActive();
//                        }catch (Exception e){
//                        }
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

}
