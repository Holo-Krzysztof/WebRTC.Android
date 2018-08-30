package com.hololight.webrtcandroid.Observers;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class CustomSdpObserver implements SdpObserver {
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d("SdpObserver", "description created successfully");
    }

    @Override
    public void onSetSuccess() {
        Log.d("SdpObserver", "description set successfully");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.d("SdpObserver", "description creation failed");

    }

    @Override
    public void onSetFailure(String s) {
        Log.d("SdpObserver", "description set failed");
    }
}
