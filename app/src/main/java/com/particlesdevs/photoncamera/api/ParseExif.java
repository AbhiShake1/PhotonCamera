package com.particlesdevs.photoncamera.api;

import android.hardware.camera2.CaptureResult;
import android.os.Build;
import android.util.Log;
import androidx.exifinterface.media.ExifInterface;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.processing.parameters.IsoExpoSelector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static android.hardware.camera2.CaptureResult.*;
import static androidx.exifinterface.media.ExifInterface.*;

public class ParseExif {
    public static final SimpleDateFormat sFormatter;
    private static final String TAG = "ParseExif";

    static {
        sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
        sFormatter.setTimeZone(TimeZone.getDefault());
    }

    public static String getTime(long exposureTime) {
        long sec = 1000000000;
        double time = (double) (exposureTime) / sec;
        return String.valueOf((time));
    }

    public static String getResult(CaptureResult res, Key<?> key) {
        Object out = res.get(key);
        return out != null ? out.toString() : "";
    }

    public static ExifData parse(CaptureResult result) {
        int rotation = PhotonCamera.getCaptureController().cameraRotation;
        String TAG = "ParseExif";
        Log.d(TAG, "Gravity rotation:" + PhotonCamera.getGravity().getRotation());
        Log.d(TAG, "Sensor rotation:" + PhotonCamera.getCaptureController().mSensorOrientation);
        int orientation = ORIENTATION_NORMAL;
        switch (rotation) {
            case 90:
                orientation = ExifInterface.ORIENTATION_ROTATE_90;
                break;
            case 180:
                orientation = ExifInterface.ORIENTATION_ROTATE_180;
                break;
            case 270:
                orientation = ExifInterface.ORIENTATION_ROTATE_270;
                break;
        }
        Log.d(TAG, "rotation:" + rotation);
        Log.d(TAG, "orientation:" + orientation);

        Integer iso = result.get(SENSOR_SENSITIVITY);
        int isonum = 100;
        if (iso != null) isonum = (int) (iso * IsoExpoSelector.getMPY());
        Log.d(TAG, "sensivity:" + isonum);

        /*
        //saving for later use
        float sensorWidth = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getWidth();
        String mm35 = String.valueOf((short) (36 * (result.get(LENS_FOCAL_LENGTH) / sensorWidth)));
        inter.setAttribute(TAG_FOCAL_LENGTH_IN_35MM_FILM, mm35);
        Log.d(TAG, "Saving 35mm FocalLength = " + mm35);
        */
        int finalIsonum = isonum;
        return new ExifData(){
            //instance initializer for anonymous class
            {
                SENSITIVITY_TYPE = String.valueOf(ExifInterface.SENSITIVITY_TYPE_ISO_SPEED);
                PHOTOGRAPHIC_SENSITIVITY = String.valueOf(finalIsonum);
                F_NUMBER = getResult(result, LENS_APERTURE);
                FOCAL_LENGTH = ((int) (100 * Double.parseDouble(getResult(result, LENS_FOCAL_LENGTH)))) + "/100";
                APERTURE_VALUE = String.valueOf(result.get(LENS_APERTURE));
                EXPOSURE_TIME = getTime(Long.parseLong(getResult(result, SENSOR_EXPOSURE_TIME)));
                DATETIME = sFormatter.format(new Date(System.currentTimeMillis()));
                COMPRESSION = "97";
                COLOR_SPACE = "sRGB";
                EXIF_VERSION = "0231";
                IMAGE_DESCRIPTION = PhotonCamera.getParameters().toString();
            }
        };
    }

    public static ExifInterface setAllAttributes(File file, ExifData data) {
        try {
            return new ExifInterface(file){
                {
                    setAttribute(TAG_SENSITIVITY_TYPE, data.SENSITIVITY_TYPE);
                    setAttribute(TAG_PHOTOGRAPHIC_SENSITIVITY, data.PHOTOGRAPHIC_SENSITIVITY);
                    setAttribute(TAG_F_NUMBER, data.F_NUMBER);
                    setAttribute(TAG_FOCAL_LENGTH, data.FOCAL_LENGTH);
                    setAttribute(TAG_COPYRIGHT, data.COPYRIGHT);
                    setAttribute(TAG_APERTURE_VALUE, data.APERTURE_VALUE);
                    setAttribute(TAG_EXPOSURE_TIME, data.EXPOSURE_TIME);
                    setAttribute(ExifInterface.TAG_DATETIME, data.DATETIME);
                    setAttribute(TAG_MODEL, data.MODEL);
                    setAttribute(TAG_MAKE, data.MAKE);
                    setAttribute(TAG_COMPRESSION, data.COMPRESSION);
                    setAttribute(TAG_COLOR_SPACE, data.COLOR_SPACE);
                    setAttribute(TAG_EXIF_VERSION, data.EXIF_VERSION);
                    setAttribute(TAG_IMAGE_DESCRIPTION, data.IMAGE_DESCRIPTION);
                }
            };
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getOrientation(int cameraRotation) {
        Log.d(TAG, "Gravity rotation:" + PhotonCamera.getGravity().getRotation());
        Log.d(TAG, "Sensor rotation:" + PhotonCamera.getCaptureController().mSensorOrientation);
        int orientation = ORIENTATION_NORMAL;
        switch (cameraRotation) {
            case 90:
                orientation = ExifInterface.ORIENTATION_ROTATE_90;
                break;
            case 180:
                orientation = ExifInterface.ORIENTATION_ROTATE_180;
                break;
            case 270:
                orientation = ExifInterface.ORIENTATION_ROTATE_270;
                break;
        }
        return orientation;
    }

    public static class ExifData {
        public final String MODEL = Build.MODEL;
        public final String MAKE = Build.BRAND;
        public final String COPYRIGHT = "PhotonCamera";
        public String SENSITIVITY_TYPE;
        public String PHOTOGRAPHIC_SENSITIVITY;
        public String APERTURE_VALUE;
        public String COMPRESSION;
        public String COLOR_SPACE;
        public String EXIF_VERSION;
        public String IMAGE_DESCRIPTION;
        public String DATETIME;
        public String EXPOSURE_TIME;
        public String F_NUMBER;
        public String FOCAL_LENGTH;
    }
}
