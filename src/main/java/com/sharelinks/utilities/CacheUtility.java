package com.sharelinks.utilities;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.inject.Singleton;
import java.io.*;

@Slf4j
@Singleton
public class CacheUtility {

    public static final String SHARE_LINKS_DIR = RuneLite.RUNELITE_DIR.getPath() + File.separator + "share-links";

    public void CreateShareLinksDir() {
        File dir = new File(SHARE_LINKS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public Object ReadObjectFromDisk(String path) {
        try {
            File file = new File(SHARE_LINKS_DIR + File.separator + path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Object object = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            return object;
        } catch (FileNotFoundException e) {
            log.warn("[External Plugin][Share Links] When reading from disk, did not find path=" + path, e);
        } catch (IOException e) {
            log.warn("[External Plugin][Share Links] When reading from disk, ran into IOException for path=" + path, e);
        } catch (ClassNotFoundException e) {
            log.warn("[External Plugin][Share Links] When reading from disk, ran into ClassNotFoundException for path=" + path, e);
        }

        return null;
    }

    public void WriteObjectToDisk(Object object, String path) {
        try {
            File file = new File(SHARE_LINKS_DIR + File.separator + path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            log.warn("[External Plugin][Share Links] When writing object to disk, did not find path=" + path, e);
        } catch (IOException e) {
            log.warn("[External Plugin][Share Links] When writing object to disk, ran into IOException for path=" + path, e);
        }

    }

}
