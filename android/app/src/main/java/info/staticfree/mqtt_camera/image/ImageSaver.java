package info.staticfree.mqtt_camera.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import info.staticfree.mqtt_camera.mqtt.MqttRemote;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */
public class ImageSaver implements Runnable {

    private static final String TAG = ImageSaver.class.getSimpleName();
    /**
     * The JPEG image
     */
    private final ByteBuffer byteBuffer;
    /**
     * The outputFile we save the image into.
     */
    private final File parentFolder;
    private final MqttRemote mqttRemote;
    private File file;
    private String fileName;
    public ImageSaver(ByteBuffer image, File mFile, MqttRemote mqttRemote) {
        byteBuffer = image;
        parentFolder = mFile;
        this.mqttRemote = mqttRemote;

        SimpleDateFormat simpleFormatter = new SimpleDateFormat("yyyy-MM-dd-HHmmssZ");
        fileName = String.format("mqtt_img_%s.", simpleFormatter.format(new Date()));

        file = new File(parentFolder, fileName + "bmp");
    }

    @Override
    public void run() {

        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
        } finally {
            if (null != output) {
                try {
                    output.close();

                    File jpgFile = saveJpgFile(this.file);

                    ImagePublisher imagePublisher = new ImagePublisher(jpgFile.getCanonicalPath(), mqttRemote, "image");
                    imagePublisher.run();

                } catch (IOException e) {
                    Log.e(TAG, "Error saving image", e);
                }
            }
        }
    }

    private File saveJpgFile(File bitmapFile) {
        Bitmap bitmap = null;

        //Decode image size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(bitmapFile);
            BitmapFactory.decodeStream(fileInputStream, null, options);
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int IMAGE_MAX_SIZE = 1024;
        int scale = 1;
        if (options.outHeight > IMAGE_MAX_SIZE || options.outWidth > IMAGE_MAX_SIZE) {
            scale = (int) Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
                    (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
        }

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        try {
            fileInputStream = new FileInputStream(bitmapFile);
            bitmap = BitmapFactory.decodeStream(fileInputStream, null, o2);
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, String.format("Width :%d Height :%d", bitmap.getWidth(), bitmap.getHeight()));

        File jpgDestFile = new File(parentFolder,  fileName + "jpg");
        FileOutputStream jpgFile = null;
        try {
            jpgFile = new FileOutputStream(jpgDestFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, jpgFile);
            jpgFile.flush();
            jpgFile.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jpgDestFile;
    }
}
