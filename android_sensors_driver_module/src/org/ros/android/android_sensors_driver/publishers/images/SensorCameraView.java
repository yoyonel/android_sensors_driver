package org.ros.android.android_sensors_driver.publishers.images;

/**
 * Created by main on 26.01.17.
 */


import java.io.FileOutputStream;
import java.util.List;

import org.opencv.android.JavaCameraView;
import org.ros.android.android_sensors_driver.publishers.images3.CameraPublisher;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.util.Log;

public class SensorCameraView extends JavaCameraView implements PictureCallback {

    private static final String TAG = "Android_Sensors_Driver::SensorCameraView";
    private String mPictureFileName;
    private CameraPublisher mPictureListener;

    private boolean safeToTakePricture = true;

    private int imageCount = 0;

    public SensorCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public List<String> getEffectList() {
        return mCamera.getParameters().getSupportedColorEffects();
    }

    public boolean isEffectSupported() {
        return (mCamera.getParameters().getColorEffect() != null);
    }

    public String getEffect() {
        return mCamera.getParameters().getColorEffect();
    }

    public void setEffect(String effect) {
        Camera.Parameters params = mCamera.getParameters();
        params.setColorEffect(effect);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void cameraPictureResolutions() {
        Log.i(TAG, "camera params:" + mCamera.getParameters().flatten());
    }

    public void setupParameters() {
        Camera.Parameters params = mCamera.getParameters();
        params.setPictureSize(3264, 1836);
        params.setJpegQuality(85);
        params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);
    }

    public void takePicture(final String fileName) {
        if(! safeToTakePricture) {
            Log.w(TAG, "not safe to take picture! Stop here");
            return;
        }
        Log.i(TAG, "Taking picture");
        // block taking another picture
        safeToTakePricture = false;

        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();

        //safeToTakePricture = true; set in callback
        mCamera.setPreviewCallback(this);

        // call picture callback to e.g. publis on ROS
        if (mPictureListener != null) {
            mPictureListener.onPictureTaken(data);
            imageCount += 1;
        }
    }

    public boolean hasActiveCamera() {
        if (mCamera != null) {
            return true;
        }
        return false;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setCameraPictureListener(CameraPublisher listener) {
        mPictureListener = listener;
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        super.onPreviewFrame(frame, arg1);
        safeToTakePricture = true;
    }
}