package com.hololight.webrtcandroid.Observers;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

public class CustomPeerConnectionObserver implements PeerConnection.Observer {

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d("PeerObserver", "signaling state changed to " + signalingState.toString());
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d("PeerObserver", "ice connection changed to " + iceConnectionState.toString());
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d("PeerObserver", "ice connection receiving change");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d("PeerObserver", "ice gathering changed");
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d("PeerObserver", "ice candidate");
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d("PeerObserver", "ice candidates removed");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d("PeerObserver", "stream added");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d("PeerObserver", "stream removed");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d("PeerObserver", "data channel");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d("PeerObserver", "renegotiation needed");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d("PeerObserver", "track added");
    }
}
