package com.metarhia.jstp.core.JSTypes;

import java.io.ObjectInput;
import java.io.Serializable;

/**
 * Created by lida on 19.04.16.
 */
public interface JSValue extends Serializable {
    Object getGeneralizedValue();
}
