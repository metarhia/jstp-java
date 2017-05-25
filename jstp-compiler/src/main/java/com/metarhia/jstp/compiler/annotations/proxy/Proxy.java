package com.metarhia.jstp.compiler.annotations.proxy;

/**
 * Created by lundibundi on 5/25/17.
 */
public @interface Proxy {

  boolean singleton() default false;
  String interfaceName() default "";
}
