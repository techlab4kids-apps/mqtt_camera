package info.staticfree.mqtt_camera.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import info.staticfree.mqtt_camera.mqtt.MqttRemote;

/**
 * Publishes the image to an MQTT subTopic.
 */
public class ImagePublisher implements Runnable {
    private final String fileName;
    private final String subTopic;
    private final MqttRemote mqttRemote;

    public ImagePublisher(@NonNull String fileName, @NonNull MqttRemote mqttRemote,
                          @NonNull String subTopic) {
        this.fileName = fileName;
        this.mqttRemote = mqttRemote;
        this.subTopic = subTopic;
    }

    @Override
    public void run() {
        //byte[] bytes = new byte[buffer.remaining()];

        byte[] bytes = getFileToByte(fileName);
        try {
            mqttRemote.publish(subTopic, bytes);
        } finally {
        }
    }

    private byte[] getFileToByte(String filePath){
        Bitmap bmp = null;
        ByteArrayOutputStream bos = null;
        byte[] bt = null;
        String encodeString = null;
        try{
            bmp = BitmapFactory.decodeFile(filePath);
            bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bt = bos.toByteArray();
            encodeString = Base64.encodeToString(bt, Base64.DEFAULT);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return bt;
    }

}
