package com.metarhia.jstp.compiler.annotations.handlers;

import com.metarhia.jstp.core.Handlers.ManualHandler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Created by lundibundi on 3/13/17.
 */
@Target({ElementType.TYPE})
public @interface Handler {

  Class value() default ManualHandler.class;
}
