package com.hololight.webrtcandroid;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ScreenCapturePermissionListener {

    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private EglBase eglBase;
    VideoSource videoSource;
    AudioSource audioSource;
    VideoTrack videoTrack;
    AudioTrack audioTrack;
    ArrayList<PeerConnection.IceServer> iceServers;
    PeerConnection localPeer;
    SdpObserver localObserver;
    PeerConnection remotePeer;
    SdpObserver remoteObserver;
    SurfaceViewRenderer remoteRenderer;
    MediaConstraints constraints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            ScreenCapturePermissionFragment fragment = new ScreenCapturePermissionFragment();
            transaction.add(fragment, "ScreenCapturePermissionDialog");
            transaction.commit();
        }

        eglBase = EglBase.create();
        remoteRenderer = findViewById(R.id.video_view);
        remoteRenderer.init(eglBase.getEglBaseContext(), null);

        InitializationOptions initOptions = InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(initOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        VideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        VideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        PeerConnectionFactory pcFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .setOptions(options)
                .createPeerConnectionFactory();

        constraints = new MediaConstraints();
        videoSource = pcFactory.createVideoSource(false);
        audioSource = pcFactory.createAudioSource(constraints);
        videoTrack = pcFactory.createVideoTrack("selfVideo", videoSource);

        //uncomment to render the local video track
        //videoTrack.addSink(remoteRenderer);

        audioTrack = pcFactory.createAudioTrack("selfAudio", audioSource);
        iceServers = new ArrayList<>();

        MediaStream stream = pcFactory.createLocalMediaStream("localStream");
        stream.addTrack(audioTrack);
        stream.addTrack(videoTrack);

        localPeer = pcFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver() {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("localPeer", "ice candidate");
                remotePeer.addIceCandidate(iceCandidate);
            }
        });

        localPeer.addStream(stream);

        remotePeer = pcFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver() {

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("remotePeer", "ice candidate");
                localPeer.addIceCandidate(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d("remotePeer", "stream added");

                runOnUiThread(() -> {
                    VideoTrack video = mediaStream.videoTracks.get(0);
                    AudioTrack audio = mediaStream.audioTracks.get(0);

                    //comment out to render local video stream
                    video.addSink(remoteRenderer);
                });
            }

        });

        localObserver = new CustomSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                localPeer.setLocalDescription(this, sessionDescription);
                remotePeer.setRemoteDescription(remoteObserver, sessionDescription);
            }
        };

        remoteObserver = new CustomSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                remotePeer.setLocalDescription(this, sessionDescription);
                localPeer.setRemoteDescription(localObserver, sessionDescription);
            }
        };

        //needs media projection capability
//        MediaProjectionManager projectionManager = (MediaProjectionManager)getApplication().getSystemService(MEDIA_PROJECTION_SERVICE);
//        Intent mediaProjectionIntent = projectionManager.createScreenCaptureIntent();
//        startActivityForResult(mediaProjectionIntent, REQUEST_MEDIA_PROJECTION);



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onScreenCapturePermissionResult(boolean hasPermission) {

        SurfaceTextureHelper textureHelper = SurfaceTextureHelper.create("testthread", eglBase.getEglBaseContext());

//                ScreenCapturerAndroid capturer = new ScreenCapturerAndroid(data, callback);
//                capturer.initialize(textureHelper, getApplicationContext(), videoSource.getCapturerObserver());
//                capturer.startCapture(720, 1280, 0);

//                this actually requires the camera permission. we're not asking at runtime (bad developer!), so just
//                give it the permission in your settings. don't do this in production, though!
        Camera2Enumerator enumerator = new Camera2Enumerator(getApplicationContext());
        String[] names = enumerator.getDeviceNames();
        CameraVideoCapturer capturer = enumerator.createCapturer(names[0], null);
        capturer.initialize(textureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        capturer.startCapture(1280, 720, 0);

        localPeer.createOffer(localObserver, constraints);
        remotePeer.createAnswer(remoteObserver, constraints);
    }
}
