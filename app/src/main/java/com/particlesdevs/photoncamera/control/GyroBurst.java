package com.particlesdevs.photoncamera.control;

import androidx.annotation.NonNull;

public class GyroBurst {
    private final int maxSamples;
    public float shakiness;
    public float[][] movementss;
    public long[] timestampss;
    public float[] integrated;

    public GyroBurst(int maxSamples) {
        this.maxSamples = maxSamples;
        movementss = new float[3][maxSamples];
        timestampss = new long[maxSamples];
        integrated = new float[3];
    }

    @NonNull
    @Override
    public GyroBurst clone() {
        GyroBurst out = new GyroBurst(maxSamples);
        out.movementss = movementss.clone();
        out.timestampss = timestampss.clone();
        out.integrated = integrated.clone();
        out.shakiness = shakiness;
        return out;
    }
}
