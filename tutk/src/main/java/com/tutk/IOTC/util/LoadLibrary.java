package com.tutk.IOTC.util;

import android.util.Log;

public class LoadLibrary {
    public static final boolean TRACE = true;
    public static final String TAG = "LoadLibrary";

    public static void load(String library) {
        Log.d(TAG, "load: " + library + "---TRACE=" + TRACE);
        System.loadLibrary(TRACE ? library + "T" : library);
    }
}

