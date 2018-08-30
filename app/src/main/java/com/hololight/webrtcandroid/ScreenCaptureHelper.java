package com.hololight.webrtcandroid;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;

import javax.annotation.Nullable;

public class ScreenCaptureHelper {

    // TODO: Figure out what data is and give it a better name
    public static void RegisterScreenCapturer(Context applicationContext, @Nullable Intent data, VideoSource videoSource, EglBase eglBase)
    {
        MediaProjection.Callback mpCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d("your mom", "is so fat media projection stopped");
            }
        };

        SurfaceTextureHelper textureHelper = SurfaceTextureHelper.create("testthread", eglBase.getEglBaseContext());

        ScreenCapturerAndroid capturer = new ScreenCapturerAndroid(data, mpCallback);
        capturer.initialize(textureHelper, applicationContext, videoSource.getCapturerObserver());
        capturer.startCapture(720, 1280, 0);
    }
}
