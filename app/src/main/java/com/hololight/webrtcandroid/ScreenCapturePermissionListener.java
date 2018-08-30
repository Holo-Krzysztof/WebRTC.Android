package com.hololight.webrtcandroid;

import android.content.Intent;
import android.support.annotation.Nullable;

interface ScreenCapturePermissionListener {

    public void onScreenCapturePermissionResult(boolean hasPermission, @Nullable Intent data);
}
