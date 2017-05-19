package com.animation.golanzo.greenshot.ui.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.animation.golanzo.greenshot.AppConstants;
import com.animation.golanzo.greenshot.AppController;
import com.animation.golanzo.greenshot.R;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by Markus Shaker on 17.03.2017.
 */
public class RegisterFragment extends Fragment {
    private static final String TAG = "RegisterFragment";


    @BindView(R.id.txv_username)
    TextView id;

    @BindView(R.id.txv_password)
    TextView password;
    @BindView(R.id.startButton)
    ImageView startButton;

    private Unbinder unbinder;

    private SharedPreferences prefs;
    private ProgressDialog pDialog;

    public interface Listener {
        void onRegister(String result);
    }

    private Listener listener;
    private static RegisterFragment fragment;


    public static RegisterFragment getInstance(Listener listener) {
        if (fragment == null) {
            fragment = new RegisterFragment();
        }
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register, container, false);

        prefs = getActivity().getSharedPreferences("greenshot_prefs", Context.MODE_PRIVATE);
        unbinder = ButterKnife.bind(this, v);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.startButton)
    public void start(View view) {
        String tag_json_arry = "json_array_req";
        String url = AppConstants.API_URL + "&" + "id=" + id.getText().toString() + "&" + "secure_num=" + password.getText().toString();
        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage("Loading...");
        pDialog.show();

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG, response.toString());
                        try {
                            String apiNum = response.getString("apiNum");
                            String apiMsg = response.getString("apiMsg");
                            if (apiMsg.equals("ACTIVE")) {
                                prefs.edit().putString("id", id.getText().toString())
                                        .putString("secret", password.getText().toString()).apply();
                                listener.onRegister("success");
                            } else {
                                switch (apiNum) {
                                    case "-1":
                                        Log.e(TAG, "CASE -1");
                                        listener.onRegister("not_exists");
                                        break;
                                    case "-2":
                                        Log.e(TAG, "CASE -1");
                                        listener.onRegister("not_match");
                                        break;
                                    case "-3":
                                        Log.e(TAG, "CASE 3");
                                        listener.onRegister("inactive");
                                        break;
                                    default:
                                        listener.onRegister("error");
                                        Log.e(TAG, "DEFAULT");
                                        break;
                                }
                            }
                        } catch (JSONException e) {
                        }
                        pDialog.hide();

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
                pDialog.hide();
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjReq, tag_json_arry);
    }
}

