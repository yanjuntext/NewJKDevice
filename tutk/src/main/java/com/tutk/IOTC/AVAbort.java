package com.tutk.IOTC;

import androidx.annotation.Keep;

import java.util.ArrayList;
@Keep
public class AVAbort {
    private ArrayList<Long> mAbortTargets = new ArrayList();
    private boolean mAbort = false;

    synchronized public void abort() {
        mAbort = true;
        for (long target : mAbortTargets) {
            AV_API_Abort(target);
        }
    }

    synchronized private void onAvailable(long abort) {
        if (mAbort) {
            AV_API_Abort(abort);
        } else {
            mAbortTargets.add(abort);
        }
    }

    synchronized private void onUnavailable(long abort) {
        mAbortTargets.remove(abort);
    }

    private native void AV_API_Abort(long abort);
}
