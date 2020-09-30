package com.sharelinks.utilities;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.inject.Singleton;
import java.io.*;

@Slf4j
@Singleton
public class CacheUtility {
    private static final String DIR_PATH = RuneLite.RUNELITE_DIR.getPath() + File.separator + "share-links";

    public Object ReadObjectFromDisk(String path) {
        try {
            File file = new File(DIR_PATH + File.separator + path);
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object object = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            return object;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void WriteObjectToDisk(Object object, String path) {
        try {
            File file = new File(DIR_PATH + File.separator + path);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
