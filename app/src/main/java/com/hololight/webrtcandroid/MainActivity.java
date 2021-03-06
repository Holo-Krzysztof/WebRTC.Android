package com.hololight.webrtcandroid;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.hololight.webrtcandroid.Observers.CustomPeerConnectionObserver;
import com.hololight.webrtcandroid.Observers.CustomSdpObserver;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ScreenCapturePermissionListener {

    private static final int CAMERA_PERMISSION_ID = 1337;

    private enum VideoSourceType { ScreenCapture, BackCamera }
    private static final VideoSourceType videoSourceType = VideoSourceType.BackCamera;

    private EglBase eglBase;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private PeerConnection sourcePeer;
    private SdpObserver sourceSdpObserver;
    private PeerConnection sinkPeer;
    private SdpObserver sinkSdpObserver;
    private SurfaceViewRenderer videoOutputSurface;
    private MediaConstraints mediaConstraints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeEglVideoSurfaceView();

        PeerConnectionFactory pcFactory = createPeerConnectionFactory();

        createVideoSource(pcFactory);
        createAudioSource(pcFactory);

        VideoTrack videoTrack = pcFactory.createVideoTrack("selfVideo", videoSource);
        AudioTrack audioTrack = pcFactory.createAudioTrack("selfAudio", audioSource);

        MediaStream stream = pcFactory.createLocalMediaStream("localStream");
        stream.addTrack(audioTrack);
        stream.addTrack(videoTrack);

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        sourcePeer = pcFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver() {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("sourcePeer", "received local ice candidate");
                sinkPeer.addIceCandidate(iceCandidate);
            }
        });

        sourcePeer.addStream(stream);

        sinkPeer = pcFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver() {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("sinkPeer", "ice candidate");
                sourcePeer.addIceCandidate(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d("sinkPeer", "stream added");

                runOnUiThread(() -> {
                    VideoTrack video = mediaStream.videoTracks.get(0);
                    AudioTrack audio = mediaStream.audioTracks.get(0);

                    //comment out to render local video stream
                    video.addSink(videoOutputSurface);
                });
            }

        });

        sourceSdpObserver = new CustomSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                sourcePeer.setLocalDescription(this, sessionDescription);
                sinkPeer.setRemoteDescription(sinkSdpObserver, sessionDescription);
            }
        };

        sinkSdpObserver = new CustomSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                sinkPeer.setLocalDescription(this, sessionDescription);
                sourcePeer.setRemoteDescription(sourceSdpObserver, sessionDescription);
            }
        };

        if (VideoSourceType.ScreenCapture == videoSourceType)
            addScreenCapturePermissionFragment(savedInstanceState);
        else if (VideoSourceType.BackCamera == videoSourceType)
            registerCameraCapturer();
    }

    private PeerConnectionFactory createPeerConnectionFactory() {
        InitializationOptions initOptions = InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        VideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        return PeerConnectionFactory.builder()
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .setOptions(options)
                .createPeerConnectionFactory();
    }

    private void createVideoSource(PeerConnectionFactory pcFactory) {
        videoSource = pcFactory.createVideoSource(false);
    }

    private void createAudioSource(PeerConnectionFactory pcFactory) {
        mediaConstraints = new MediaConstraints();
        audioSource = pcFactory.createAudioSource(mediaConstraints);
    }

    private void addScreenCapturePermissionFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            ScreenCapturePermissionFragment fragment = new ScreenCapturePermissionFragment();
            transaction.add(fragment, "ScreenCapturePermissionDialog");
            transaction.commit();
        }
    }

    private void initializeEglVideoSurfaceView() {
        eglBase = EglBase.create();
        videoOutputSurface = findViewById(R.id.video_view);
        videoOutputSurface.init(eglBase.getEglBaseContext(), null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onScreenCapturePermissionResult(boolean hasPermission, @Nullable Intent data) {

        if (!hasPermission)
            throw new AssertionError("I ain't got no screen capture permission");

        ScreenCaptureHelper.RegisterScreenCapturer(getApplicationContext(), data, videoSource, eglBase);

        createOffer();
        createAnswer();
    }

    private void createOffer() {
        sourcePeer.createOffer(sourceSdpObserver, mediaConstraints);
    }

    private void createAnswer()
    {
        sinkPeer.createAnswer(sinkSdpObserver, mediaConstraints);
    }

    private void registerCameraCapturer()
    {
        // this actually requires the camera permission. we're not asking at runtime (bad developer!), so just
        // give it the permission in your settings. don't do this in production, though!

        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.CAMERA},
                CAMERA_PERMISSION_ID);

        CameraCaptureHelper.RegisterCameraCapturer(getApplicationContext(), videoSource, eglBase);

        createOffer();
        createAnswer();
    }
}
