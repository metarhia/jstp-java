package com.metarhia.jstp.compiler.annotations.handlers;

public @interface Error {

  int[] errors() default {};
}
