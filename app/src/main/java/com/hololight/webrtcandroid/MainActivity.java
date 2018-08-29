package com.hololight.webrtcandroid;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.RtpReceiver;
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

public class MainActivity extends AppCompatActivity {

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

        localPeer = pcFactory.createPeerConnection(iceServers, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d("localPeer", "signaling state changed to " + signalingState.toString());
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d("localPeer", "ice connection changed to " + iceConnectionState.toString());
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d("localPeer", "ice connection receiving change");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d("localPeer", "ice gathering changed");
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("localPeer", "ice candidate");
                remotePeer.addIceCandidate(iceCandidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d("localPeer", "ice candidates removed");
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d("localPeer", "stream added");
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d("localPeer", "stream removed");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d("localPeer", "data channel");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d("localPeer", "renegotiation needed");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d("localPeer", "track added");
            }
        });

        localPeer.addStream(stream);

        remotePeer = pcFactory.createPeerConnection(iceServers, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d("remotePeer", "signaling state changed to " + signalingState.toString());
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d("remotePeer", "ice connection changed to " + iceConnectionState.toString());
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d("remotePeer", "ice connection receiving change");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d("remotePeer", "ice gathering changed");
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("remotePeer", "ice candidate");
                localPeer.addIceCandidate(iceCandidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d("remotePeer", "ice candidates removed");
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

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d("remotePeer", "stream removed");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d("remotePeer", "data channel");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d("remotePeer", "renegotiation needed");
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                Log.d("remotePeer", "track added");
            }
        });

        localObserver = new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                localPeer.setLocalDescription(this, sessionDescription);
                remotePeer.setRemoteDescription(remoteObserver, sessionDescription);
            }

            @Override
            public void onSetSuccess() {
                Log.d("localObserver", "description set successfully");
            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        };

        remoteObserver = new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                remotePeer.setLocalDescription(this, sessionDescription);
                localPeer.setRemoteDescription(localObserver, sessionDescription);
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        };

        //needs media projection capability
//        MediaProjectionManager projectionManager = (MediaProjectionManager)getApplication().getSystemService(MEDIA_PROJECTION_SERVICE);
//        Intent mediaProjectionIntent = projectionManager.createScreenCaptureIntent();
//        startActivityForResult(mediaProjectionIntent, REQUEST_MEDIA_PROJECTION);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                MediaProjection.Callback callback = new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        super.onStop();
                        Log.d("your mom", "is so fat media projection stopped");
                    }
                };

                SurfaceTextureHelper textureHelper = SurfaceTextureHelper.create("testthread", eglBase.getEglBaseContext());

                ScreenCapturerAndroid capturer = new ScreenCapturerAndroid(data, callback);
                capturer.initialize(textureHelper, getApplicationContext(), videoSource.getCapturerObserver());
                capturer.startCapture(720, 1280, 0);

//                this actually requires the camera permission. we're not asking at runtime (bad developer!), so just
//                give it the permission in your settings. don't do this in production, though!
//                Camera2Enumerator enumerator = new Camera2Enumerator(getApplicationContext());
//                String[] names = enumerator.getDeviceNames();
//                CameraVideoCapturer capturer = enumerator.createCapturer(names[0], null);
//                capturer.initialize(textureHelper, getApplicationContext(), videoSource.getCapturerObserver());
//                capturer.startCapture(1280, 720, 0);

                localPeer.createOffer(localObserver, constraints);
                remotePeer.createAnswer(remoteObserver, constraints);
            }
        }
    }
}
