package com.metarhia.jstp.storage;

import com.metarhia.jstp.session.SessionPolicy;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File storage for {@link SessionPolicy}
 */
public class FileStorage implements StorageInterface {

  private static final Logger logger = LoggerFactory.getLogger(FileStorage.class);

  private String storageFolder;

  /**
   * Creates file storage instance in specified folder {@param storageFolder}
   *
   * @param storageFolder storage folder to store session data
   */
  public FileStorage(String storageFolder) {
    this.storageFolder = storageFolder;
  }

  @Override
  public void putSerializable(String key, Serializable value) {
    safeToFile(filepathFromKey(key), value);
  }

  @Override
  public Object getSerializable(String key, Object defaultValue) {
    Object result = readFromFile(filepathFromKey(key));
    return result != null ? result : defaultValue;
  }

  private String filepathFromKey(String key) {
    return storageFolder + "/" + key;
  }

  private boolean safeToFile(String filepath, Serializable value) {
    try {
      FileOutputStream fos = new FileOutputStream(filepath);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(value);
      oos.close();
      fos.close();
      return true;
    } catch (IOException e) {
      logger.info("Cannot write file", e);
    }
    return false;
  }

  private Object readFromFile(String filepath) {
    try (FileInputStream fis = new FileInputStream(filepath);
        ObjectInputStream ois = new ObjectInputStream(fis)) {
      return ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      logger.info("Cannot write file", e);
    }
    return null;
  }
}
