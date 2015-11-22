package com.hypodiabetic.happ;

import android.app.Application;
import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Tim on 22/11/2015.
 * Volley Helper Class for Integration uploads
 */
public class volleyQ {

    private static volleyQ mInstance;
    private RequestQueue mRequestQueue;

    private static Context mCtx;

    private volleyQ(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

    }

    public static synchronized volleyQ getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new volleyQ(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }




}
