package com.particlesdevs.photoncamera.util;

public class Math2 {
    public static double mix(double in, double in2, double t){
        return in*(1.-t)+in2*(t);
    }
    public static float mix(float in, float in2, float t){
        return in*(1.f-t)+in2*(t);
    }
    public static int MirrorCoords(int i, int max){
        if(i < 0) return -i;
        else
            if(i>max-1)
                return max*2-i-1;
        return i;
    }
    public static float pdf(float x,float sigma){
        return (float) (0.39894* java.lang.Math.exp(-0.5*x*x/(sigma*sigma))/sigma);
    }
}
