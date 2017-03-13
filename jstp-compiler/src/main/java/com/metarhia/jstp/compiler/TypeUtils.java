package com.metarhia.jstp.compiler;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Created by lundibundi on 3/13/17.
 */
public final class TypeUtils {

  private Types types;

  private Elements elements;

  public TypeUtils(Types types, Elements elements) {
    this.types = types;
    this.elements = elements;
  }

  public TypeMirror getTypeMirror(Class clazz) {
    return types.erasure(elements.getTypeElement(clazz.getCanonicalName()).asType());
  }

  public TypeMirror getTypeElement(CharSequence name) {
    return elements.getTypeElement(name).asType();
  }

  public boolean isSubtype(TypeMirror type, Class<?> clazz) {
    return types.isSubtype(type, getTypeMirror(clazz));
  }

  public boolean isSameType(TypeMirror type, Class<?> clazz) {
    return isSameType(type, clazz.getCanonicalName());
  }

  public Element asElement(TypeMirror t) {
    return types.asElement(t);
  }

  public boolean isSameType(TypeMirror t1, TypeMirror t2) {
    return types.isSameType(t1, t2);
  }

  public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
    return types.isSubtype(t1, t2);
  }

  public boolean isSameType(TypeMirror type, String canonicalName) {
    return types.isSameType(type, erasure(canonicalName));
  }

  public TypeMirror erasure(CharSequence typeName) {
    return erasure(getTypeElement(typeName));
  }

  public TypeMirror erasure(TypeMirror type) {
    return types.erasure(type);
  }

  public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
    return types.isAssignable(t1, t2);
  }

  public Types getTypes() {
    return types;
  }

  public Elements getElements() {
    return elements;
  }
}
