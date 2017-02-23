package com.metarhia.jstp.core.JSTypes;

import java.io.Serializable;

public interface JSValue extends Serializable {

  Object getGeneralizedValue();

  String toString();
}
