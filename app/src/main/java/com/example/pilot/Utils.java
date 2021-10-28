package com.example.pilot;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Utils{

    public static void LogAndToast(Context mContext , String TAG,String message) {
        Log.e(TAG, message);
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }
}

