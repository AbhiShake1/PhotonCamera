package com.particlesdevs.photoncamera.api;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.SizeF;
import androidx.core.util.Pair;
import com.particlesdevs.photoncamera.settings.SettingsManager;

import java.util.*;

import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.*;

/**
 * Responsible for Scanning all Camera IDs on a Device and Storing them in SharedPreferences as a {@link Set<String>}
 */
public final class CameraManager2 {
    public static final String _CAMERAS = CAMERAS_PREFERENCE_FILE_NAME.mValue;
    private static final String TAG = "CameraManager2";
    public final Map<String, Pair<Float, Float>> mFocalLengthAperturePairList = new LinkedHashMap<>();
    private final SettingsManager mSettingsManager;
    private Set<String> mAllCameraIDs = new LinkedHashSet<>();
    private Set<String> mFrontIDs = new LinkedHashSet<>();
    private Set<String> mBackIDs = new LinkedHashSet<>();
    private Set<String> mFocals = new LinkedHashSet<>();

    /**
     * Initialise this class.
     *
     * @param cameraManager   {@link CameraManager} instance from {@link android.content.Context#CAMERA_SERVICE}
     * @param settingsManager {@link SettingsManager}
     */
    public CameraManager2(CameraManager cameraManager, SettingsManager settingsManager) {
        this.mSettingsManager = settingsManager;
        init(cameraManager);
    }

    private void init(CameraManager cameraManager) {
        if (!mSettingsManager.isSet(_CAMERAS, ALL_CAMERA_IDS_KEY)) {
            scanAllCameras(cameraManager);
            save();
        } else {
            mAllCameraIDs = mSettingsManager.getStringSet(_CAMERAS, ALL_CAMERA_IDS_KEY, null);
            mFrontIDs = mSettingsManager.getStringSet(_CAMERAS, FRONT_IDS_KEY, null);
            mBackIDs = mSettingsManager.getStringSet(_CAMERAS, BACK_IDS_KEY, null);
            mFocals = mSettingsManager.getStringSet(_CAMERAS, FOCAL_IDS_KEY, null);
            mFocals.forEach(entry -> {
                String[] split = entry.split(":");
                String id = split[0];
                String focal = split[1];
                String aperture = split[2];
                mFocalLengthAperturePairList.put(id, new Pair<>(Float.parseFloat(focal), Float.parseFloat(aperture)));
            });
        }
    }

    private void scanAllCameras(CameraManager cameraManager) {
        for (int num = 0; num < 121; num++) {
            CameraCharacteristics cameraCharacteristics;
            try {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(String.valueOf(num));
                log("BitAnalyser:" + num + ":" + intToReverseBinary(num));
                float[] focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                SizeF size = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                if (size != null && focalLength != null) {
                    focalLength[0] /= size.getWidth();
                }
                float[] aperture = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                if (focalLength != null && aperture != null) {
                    Pair<Float, Float> focalAperturePair = new Pair<>(focalLength[0], aperture[0]);
                    if (!getBit(6, num) && !mFocalLengthAperturePairList.containsValue(focalAperturePair)) {
                        mFocalLengthAperturePairList.put(String.valueOf(num), focalAperturePair);
                        mAllCameraIDs.add(String.valueOf(num));
                        mFocals.add(num + ":" + (focalAperturePair.first) + ":" + aperture[0]);
                        fillBackFrontLists(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING), String.valueOf(num));
                    }
                }
            } catch (IllegalArgumentException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }

    private void fillBackFrontLists(Integer lensFacing, String id) {
        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT)
            mFrontIDs.add(id);
        else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK)
            mBackIDs.add(id);
    }

    private void save() {
        mSettingsManager.set(_CAMERAS, CAMERA_COUNT_KEY, mAllCameraIDs.size());
        mSettingsManager.set(_CAMERAS, ALL_CAMERA_IDS_KEY, mAllCameraIDs);
        mSettingsManager.set(_CAMERAS, BACK_IDS_KEY, mBackIDs);
        mSettingsManager.set(_CAMERAS, FRONT_IDS_KEY, mFrontIDs);
        mSettingsManager.set(_CAMERAS, FOCAL_IDS_KEY, mFocals);
    }

    //Getters===========================================================================================================

    /**
     * @return the list of scanned Camera IDs
     */
    public String[] getCameraIdList() {
        log("CameraCount:" + mAllCameraIDs.size()
                + ", CameraIDs:" + Arrays.toString(mAllCameraIDs.toArray(new String[0])));
        return mAllCameraIDs.toArray(new String[0]);
    }

    /**
     * @return the list of scanned Focal's
     */
    public String[] getCameraFocalList() {
        log("FocalsCount:" + mFocals.size()
                + ", Focal's:" + Arrays.toString(mFocals.toArray(new String[0])));
        return mFocals.toArray(new String[0]);
    }

    /**
     * @return the list of scanned Camera IDs as a {@link Set<String>}
     */
    public Set<String> getCameraIDsSet() {
        log("CameraCount:" + mAllCameraIDs.size()
                + ", CameraIDs:" + Arrays.toString(mAllCameraIDs.toArray(new String[0])));
        return mAllCameraIDs;
    }

    /**
     * @return the list of scanned Front Camera IDs as a {@link Set<String>}
     */
    public Set<String> getFrontIDsSet() {
        log("FrontCamerasCount:" + mFrontIDs.size()
                + ", FrontCameraIDs:" + Arrays.toString(mFrontIDs.toArray(new String[0])));
        return mFrontIDs;
    }

    /**
     * @return the list of scanned Back Camera IDs as a {@link Set<String>}
     */
    public Set<String> getBackIDsSet() {
        log("BackCamerasCount:" + mBackIDs.size()
                + ", BackCameraIDs:" + Arrays.toString(mBackIDs.toArray(new String[0])));
        return mBackIDs;
    }

    //Bit analyzer for AUX number=======================================================================================

    private boolean getBit(int pos, int val) {
        return ((val >> (pos - 1)) & 1) == 1;
    }

    private String intToReverseBinary(int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 8; i++) {
            sb.append(getBit(i, num) ? "1" : "0");
        }
        return sb.toString();
    }
}