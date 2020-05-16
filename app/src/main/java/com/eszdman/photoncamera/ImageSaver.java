package com.eszdman.photoncamera;

import android.graphics.ImageFormat;
import android.hardware.camera2.DngCreator;
import androidx.exifinterface.media.ExifInterface;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.content.ContentValues.TAG;

public class ImageSaver implements Runnable {

    /**
     * The Stream image
     */
    private final Image mImage;

    static int bcnt = 0;
    private final File mFile;

    /**
     * Image frame buffer
     */
    static ArrayList<Image> imageBuffer = new ArrayList<>();

    ImageSaver(Image image, File file) {
        mImage = image;
        mFile = file;
    }
    public ImageProcessing processing(){
        return new ImageProcessing(imageBuffer);
    }
    public void done(ImageProcessing proc){
        proc.Run();
        for(int i =0; i<imageBuffer.size()-1;i++){
            imageBuffer.get(i).close();
        }
        imageBuffer = new ArrayList<>();
        Log.e("ImageSaver","ImageSaver Done!");
        bcnt =0;
    }
    static String curName(){
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String dateText = dateFormat.format(currentDate);
        return "IMG_"+dateText;
    }
    static String curDir(){
        File dir = new File(Environment.getExternalStorageDirectory()+"//DCIM//EsCamera//");
        if(!dir.exists()) dir.mkdirs();
        return dir.getAbsolutePath();
    }
    @Override
    public void run() {
        int format = mImage.getFormat();
        FileOutputStream output = null;
        switch (format){
            case ImageFormat.JPEG: {

                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                try {
                    imageBuffer.add(mImage);
                    bcnt++;
                    byte[] bytes = new byte[buffer.remaining()];
                    if(bcnt == Camera2Api.mburstcount && Camera2Api.mburstcount != 1) {
                        File out =  new File(curDir(),curName()+".jpg");
                        output = new FileOutputStream(out);
                        buffer.duplicate().get(bytes);
                        output.write(bytes);
                        //output.close();
                        ExifInterface inter = new ExifInterface(out.getAbsolutePath());
                        MainActivity.inst.showToast("Processing...");
                        ImageProcessing processing = processing();
                        processing.isyuv = false;
                        processing.israw = false;
                        processing.path = out.getAbsolutePath();
                        done(processing);
                        MainActivity.inst.showToast("Done!");
                        Thread.sleep(25);
                        inter.saveAttributes();
                        //output = new FileOutputStream(new File(curDir(),"test.jpg"));
                        //output.write(bytes);
                        //output.close();
                        mImage.close();
                    }
                    if(Camera2Api.mburstcount == 1){
                        buffer.get(bytes);
                        output.write(bytes);
                        mImage.close();
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    //mImage.close();
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }
            case ImageFormat.YUV_420_888: {
                File out =  new File(curDir(),curName()+".jpg");
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                try {
                    output = new FileOutputStream(out);
                    imageBuffer.add(mImage);
                    bcnt++;
                    //byte[] bytes = new byte[buffer.remaining()];
                    if(bcnt == Camera2Api.mburstcount && Camera2Api.mburstcount != 1) {
                        //buffer.duplicate().get(bytes);
                        //output.write(bytes);
                        //ExifInterface inter = new ExifInterface(out.getAbsolutePath());
                        MainActivity.inst.showToast("Processing...");
                        ImageProcessing processing = processing();
                        processing.isyuv = true;
                        processing.israw = false;
                        processing.path = out.getAbsolutePath();
                        done(processing);
                        MainActivity.inst.showToast("Done!");
                        Thread.sleep(25);
                        ExifInterface inter = ParseExif.Parse(Camera2Api.mCaptureResult,processing.path);
                        inter.saveAttributes();
                        mImage.close();
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    //mImage.close();
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }

            case ImageFormat.RAW_SENSOR: {
                Log.e("ImageSaver","RawSensor:"+mImage);
                DngCreator dngCreator = new DngCreator(Camera2Api.mCameraCharacteristics,Camera2Api.mCaptureResult);
                try {
                    output = new FileOutputStream(new File(curDir(),curName()+".dng"));
                    imageBuffer.add(mImage);
                    bcnt++;
                    if(bcnt == Camera2Api.mburstcount && Camera2Api.mburstcount != 1) {
                        MainActivity.inst.showToast("Processing...");
                        ImageProcessing processing = processing();
                        processing.isyuv = false;
                        processing.israw = true;
                        done(processing);
                        dngCreator.writeImage(output, mImage);
                        MainActivity.inst.showToast("Done!");
                        mImage.close();
                    }
                    if(Camera2Api.mburstcount == 1) {
                        dngCreator.writeImage(output, mImage);
                        imageBuffer = new ArrayList<>();
                        mImage.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    //mImage.close();
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            default: {
                Log.e(TAG, "Cannot save image, unexpected image format:" + format);
                break;
            }
            }

        }

}