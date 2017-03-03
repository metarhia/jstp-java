package com.metarhia.jstp.storage;

import java.io.Serializable;

/**
 * Created by lundibundi on 3/3/17.
 */
public interface StorageInterface {

  void putSerializable(String key, Serializable value);

  Object getSerializable(String key, Object defaultValue);
}
