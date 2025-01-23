package com.particlesdevs.photoncamera.processing;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.util.Log;

import com.hunter.library.debug.HunterDebug;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.api.ParseExif;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.control.GyroBurst;
import com.particlesdevs.photoncamera.processing.processor.HdrxProcessor;
import com.particlesdevs.photoncamera.processing.processor.UnlimitedProcessor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DefaultSaver extends SaverImplementation {
    private static final String TAG = "DefaultSaver";
    final UnlimitedProcessor mUnlimitedProcessor;
    final HdrxProcessor hdrxProcessor;

    public DefaultSaver(ProcessingEventsListener processingEventsListener) {
        super(processingEventsListener);
        this.hdrxProcessor = new HdrxProcessor(processingEventsListener);
        this.mUnlimitedProcessor = new UnlimitedProcessor(processingEventsListener);
    }

    @HunterDebug
    public void runRaw(int imageFormat, CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, ArrayList<GyroBurst> burstShakiness, int cameraRotation, HashMap<Long, Double> exposures) {
        super.runRaw(imageFormat, characteristics, captureResult,captureRequest, burstShakiness, cameraRotation, exposures);
        //Wait for one frame at least.
        Log.d(TAG, "Acquiring:" + IMAGE_BUFFER.size());
        while (bufferLock){}
        Log.d(TAG, "Acquired:" + IMAGE_BUFFER.size());
        bufferLock = true;
        Log.d(TAG,"Size:"+IMAGE_BUFFER.size());
        if (PhotonCamera.getSettings().frameCount == 1) {
            Path dngFile = ImagePath.newDNGFilePath();
            Log.d(TAG, "Size:" + IMAGE_BUFFER.size());
            boolean imageSaved = ImageSaver.Util.saveSingleRaw(dngFile, IMAGE_BUFFER.get(0),
                    characteristics, captureResult, cameraRotation);
            processingEventsListener.notifyImageSavedStatus(imageSaved, dngFile);
            processingEventsListener.onProcessingFinished("Saved Unprocessed RAW");
            IMAGE_BUFFER.clear();
            bufferLock = false;
            return;
        }
        Path dngFile = ImagePath.newDNGFilePath();
        Path jpgFile = ImagePath.newJPGFilePath();
        //Remove broken images
            /*for(int i =0; i<IMAGE_BUFFER.size();i++){
                try{
                    IMAGE_BUFFER.get(i).getFormat();
                } catch (IllegalStateException e){
                    IMAGE_BUFFER.remove(i);
                    i--;
                    Log.d(TAG,"IMGBufferSize:"+IMAGE_BUFFER.size());
                    e.printStackTrace();
                }
            }*/
        hdrxProcessor.configure(
                PhotonCamera.getSettings().alignAlgorithm,
                PhotonCamera.getSettings().rawSaver,
                PhotonCamera.getSettings().selectedMode
        );
        ArrayList<Image> slicedBuffer = new ArrayList<>();
        ArrayList<Image> imagebuffer = new ArrayList<>();
        for(int i =0; i<frameCount;i++){
            slicedBuffer.add(IMAGE_BUFFER.get(i));
        }
        for(int i = frameCount; i<IMAGE_BUFFER.size();i++){
            imagebuffer.add(IMAGE_BUFFER.get(i));
        }
        IMAGE_BUFFER.clear();
        IMAGE_BUFFER = imagebuffer;
        bufferLock = false;
        for(int i =0; i<slicedBuffer.size();i++){
            try{
                slicedBuffer.get(i).getFormat();
            } catch (IllegalStateException e){
                slicedBuffer.remove(i);
                i--;
                Log.d(TAG,"IMGBufferSize:"+slicedBuffer.size());
                Log.d(TAG,Log.getStackTraceString(e));
            }
        }

        Log.d(TAG,"moving images");
        //Log.d(TAG,"moved images:"+slicedBuffer.size());
        hdrxProcessor.start(
                dngFile,
                jpgFile,
                ParseExif.parse(captureResult, captureRequest),
                burstShakiness,
                slicedBuffer,
                exposures,
                imageFormat,
                cameraRotation,
                characteristics,
                captureResult,
                captureRequest,
                processingCallback
        );
        slicedBuffer.clear();
    }

    public void unlimitedStart(int imageFormat, CameraCharacteristics characteristics, CaptureResult captureResult, CaptureRequest captureRequest, int cameraRotation) {
        super.unlimitedStart(imageFormat, characteristics, captureResult, captureRequest, cameraRotation);
        Path dngFile = ImagePath.newDNGFilePath();
        Path jpgFile = ImagePath.newJPGFilePath();

        mUnlimitedProcessor.configure(PhotonCamera.getSettings().rawSaver);
        mUnlimitedProcessor.unlimitedStart(
                dngFile,
                jpgFile,
                ParseExif.parse(captureResult, captureRequest),
                characteristics,
                captureResult,
                captureRequest,
                cameraRotation,
                processingCallback
        );
    }

    public void unlimitedEnd() {
        mUnlimitedProcessor.unlimitedEnd();
    }
}
