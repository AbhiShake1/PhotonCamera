package com.eszdman.photoncamera.processing;


import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import com.eszdman.photoncamera.api.Camera2ApiAutoFix;
import com.eszdman.photoncamera.app.PhotonCamera;
import com.eszdman.photoncamera.capture.CaptureController;
import com.eszdman.photoncamera.processing.opengl.postpipeline.PostPipeline;
import com.eszdman.photoncamera.processing.opengl.scripts.AverageParams;
import com.eszdman.photoncamera.processing.opengl.scripts.AverageRaw;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public class UnlimitedProcessor extends ProcessorBase {
    private static final String TAG = "UnlimitedProcessor";
    public static int unlimitedCounter = 1;
    private static boolean unlimitedEnd = false;
    private AverageRaw averageRaw;
    private boolean lock = false;
    private boolean fillParams = false;

    public UnlimitedProcessor(ProcessingEventsListener processingEventsListener) {
        super(processingEventsListener);
    }

    public void unlimitedStart(Path dngFile, Path jpgFile,
                               CameraCharacteristics characteristics,
                               CaptureResult captureResult) {
        this.dngFile = dngFile;
        this.jpgFile = jpgFile;
        this.characteristics = characteristics;
        this.captureResult = captureResult;
        unlimitedEnd = false;
        lock = false;
        fillParams = false;
    }

    public void unlimitedCycle(Image image) {
        if (lock) {
            image.close();
            return;
        }
        int width = image.getPlanes()[0].getRowStride() / image.getPlanes()[0].getPixelStride();
        int height = image.getHeight();

        PhotonCamera.getParameters().rawSize = new Point(width, height);

        if (averageRaw == null) {


            averageRaw = new AverageRaw(PhotonCamera.getParameters().rawSize, "UnlimitedAvr");
        }
        if(!fillParams) {
            fillParams = true;
            PhotonCamera.getParameters().FillParameters(captureResult,
                    characteristics, PhotonCamera.getParameters().rawSize);
        }
        averageRaw.additionalParams = new AverageParams(null, image.getPlanes()[0].getBuffer());
        averageRaw.Run();
        unlimitedCounter++;
        if (unlimitedEnd) {
            unlimitedEnd = false;
            lock = true;
            unlimitedCounter = 0;
            processUnlimited(image);
        }
        image.close();//code block
    }

    private void processUnlimited(Image image) {
//        PhotonCamera.getParameters().path = ImageSaver.jpgFilePathToSave.getAbsolutePath();
        processingEventsListener.onProcessingStarted("Unlimited Processing Started");

        averageRaw.FinalScript();
        ByteBuffer unlimitedBuffer = averageRaw.Output;
        averageRaw.close();
        averageRaw = null;
        image.getPlanes()[0].getBuffer().position(0);
        image.getPlanes()[0].getBuffer().put(unlimitedBuffer);
        image.getPlanes()[0].getBuffer().position(0);
        if (PhotonCamera.getSettings().rawSaver) {

            processingEventsListener.onProcessingFinished("Unlimited rawSaver Processing Finished");

            Camera2ApiAutoFix.patchWL(characteristics, captureResult, (int) FAKE_WL);

            boolean imageSaved = ImageSaver.Util.saveStackedRaw(dngFile, image,
                    characteristics, captureResult);

            Camera2ApiAutoFix.resetWL(characteristics, captureResult, (int) FAKE_WL);

            processingEventsListener.notifyImageSavedStatus(imageSaved, dngFile);

            return;
        }

        IncreaseWLBL();
        PostPipeline pipeline = new PostPipeline();
        Bitmap bitmap = pipeline.Run(image.getPlanes()[0].getBuffer(), PhotonCamera.getParameters());

        processingEventsListener.onProcessingFinished("Unlimited JPG Processing Finished");

        boolean imageSaved = ImageSaver.Util.saveBitmapAsJPG(jpgFile, bitmap,
                ImageSaver.JPG_QUALITY,  CaptureController.mCaptureResult);

        processingEventsListener.notifyImageSavedStatus(imageSaved, jpgFile);

        pipeline.close();

    }

    public void unlimitedEnd() {
        unlimitedEnd = true;
    }

}