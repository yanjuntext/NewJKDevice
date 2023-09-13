package com.tutk.IOTC.util;

public class LoadLibrary {
    public static final boolean TRACE = false;
    public static final String TAG = "LoadLibrary";

    public static void load(String library) {
        System.loadLibrary(TRACE ? library + "T" : library);
    }
}
