package com.metarhia.jstp.compiler.annotations.handlers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
public @interface Has {

  String[] value();
}
