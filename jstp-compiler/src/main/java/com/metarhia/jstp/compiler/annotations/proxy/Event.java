package com.metarhia.jstp.compiler.annotations.proxy;

/**
 * Created by lundibundi on 5/25/17.
 */
public @interface Event {

  String[] value() default "";
}
