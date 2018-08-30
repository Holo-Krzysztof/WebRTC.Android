package com.hololight.webrtcandroid;

import android.content.Context;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;

public class CameraCaptureHelper {

    public static void RegisterCameraCapturer(Context applicationContext, VideoSource videoSource, EglBase eglBase)
    {
        SurfaceTextureHelper textureHelper = SurfaceTextureHelper.create("testthread", eglBase.getEglBaseContext());

        Camera2Enumerator enumerator = new Camera2Enumerator(applicationContext);
        String[] names = enumerator.getDeviceNames();
        CameraVideoCapturer capturer = enumerator.createCapturer(names[0], null);
        capturer.initialize(textureHelper, applicationContext, videoSource.getCapturerObserver());
        capturer.startCapture(1280, 720, 0);
    }
}
