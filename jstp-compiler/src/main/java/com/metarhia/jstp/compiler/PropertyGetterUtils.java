package com.metarhia.jstp.compiler;

import com.metarhia.jstp.compiler.annotations.handlers.Array;
import com.metarhia.jstp.compiler.annotations.handlers.Mixed;
import com.metarhia.jstp.compiler.annotations.handlers.Object;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;

public final class PropertyGetterUtils {

  public static final String TYPE_CAST_PATTERN = "(($T) ";
  public static final String OBJECT_GETTER = ".get($S)";
  public static final String CASTED_OBJECT_GETTER = ")" + OBJECT_GETTER;
  public static final String OBJECT_INDEX_GETTER = ".getByIndex($L)";
  public static final String CASTED_OBJECT_INDEX_GETTER = ")" + OBJECT_GETTER;
  public static final String ARRAY_GETTER = ".get($L)";
  public static final String CASTED_ARRAY_GETTER = ")" + ARRAY_GETTER;

  private static final Pattern SIMPLE_DOUBLE_PATTERN = Pattern.compile("\\d+\\.?\\d*");

  private PropertyGetterUtils() {
  }

  static CodeBlock composeGetterFromAnnotations(String name, Element element)
      throws PropertyFormatException {
    return composeGetterFromAnnotations(CodeBlock.of(name), element);
  }

  static CodeBlock composeGetterFromAnnotations(CodeBlock name, Element element)
      throws PropertyFormatException {
    if (element.getAnnotation(Mixed.class) != null) {
      Mixed annotation = element.getAnnotation(Mixed.class);
      return composeCustomGetter(name, annotation.value());
    } else if (element.getAnnotation(Object.class) != null) {
      Object annotation = element.getAnnotation(Object.class);
      return composeObjectGetter(name, annotation.value());
    } else if (element.getAnnotation(Array.class) != null) {
      Array annotation = element.getAnnotation(Array.class);
      return composeArrayGetter(name, annotation.value());
    }
    return null;
  }

  public static CodeBlock composeArrayGetter(String identifier, int... indices) {
    return composeArrayGetter(CodeBlock.of(identifier), indices);
  }

  public static CodeBlock composeArrayGetter(CodeBlock identifier, int... indices) {
    CodeBlock.Builder builder = CodeBlock.builder();
    // -2 because we don't need to cast for the last element and the identifier itself
    for (int i = indices.length - 2; i >= 0; i--) {
      builder.add(TYPE_CAST_PATTERN, ClassName.get(List.class));
    }
    builder.add(identifier);
    builder.add(ARRAY_GETTER, indices[0]);
    for (int i = 1; i < indices.length; i++) {
      builder.add(CASTED_ARRAY_GETTER, indices[i]);
    }
    return builder.build();
  }

  public static CodeBlock composeObjectGetter(String identifier, String... properties) {
    return composeObjectGetter(CodeBlock.of(identifier), properties);
  }

  public static CodeBlock composeObjectGetter(CodeBlock identifier, String... properties) {
    CodeBlock.Builder builder = CodeBlock.builder();
    // -2 because we don't need to cast for the last element and the identifier itself
    for (int i = properties.length - 2; i >= 0; i--) {
      builder.add(TYPE_CAST_PATTERN, ClassName.get(JSObject.class));
    }
    builder.add(identifier);
    builder.add(OBJECT_GETTER, properties[0]);
    for (int i = 1; i < properties.length; i++) {
      builder.add(CASTED_OBJECT_GETTER, properties[i]);
    }
    return builder.build();
  }

  public static CodeBlock composeCustomGetter(String identifier, String... properties)
      throws PropertyFormatException {
    return composeCustomGetter(CodeBlock.of(identifier), properties);
  }

  public static CodeBlock composeCustomGetter(CodeBlock identifier, String... properties)
      throws PropertyFormatException {
    LinkedList<CustomProperty> customProperties = new LinkedList<>();
    CodeBlock.Builder builder = CodeBlock.builder();
    for (int i = properties.length - 1; i >= 0; i--) {
      String property = properties[i];
      if (property.startsWith("[") && property.endsWith("]")
          || property.startsWith("{") && property.endsWith("}")) {
        ClassName type;
        String pattern;
        boolean array = false;
        if (property.startsWith("[")) {
          type = ClassName.get(List.class);
          pattern = CASTED_ARRAY_GETTER;
          array = true;
        } else /* if (property.startsWith("{")) */ {
          type = ClassName.get(JSObject.class);
          pattern = CASTED_OBJECT_INDEX_GETTER;
          array = false;
        }
        if (property.length() < 3) {
          throw new PropertyFormatException("Empty array of indices");
        }
        property = property.substring(1, property.length() - 1);
        String[] indices = property.split("[, ]+");
        for (int j = indices.length - 1; j >= 0; j--) {
          String currPattern = pattern;
          if (i == 0 && j == 0) {
            currPattern = array ? ARRAY_GETTER : OBJECT_INDEX_GETTER;
          } else {
            builder.add(TYPE_CAST_PATTERN, type);
          }
          java.lang.Object value = array ? indices[j] : Integer.valueOf(indices[j]);
          customProperties.addFirst(new CustomProperty(currPattern, value));
        }
      } else {
        String pattern = CASTED_OBJECT_GETTER;
        if (i == 0) {
          pattern = OBJECT_GETTER;
        } else {
          builder.add(TYPE_CAST_PATTERN, ClassName.get(JSObject.class));
        }
        customProperties.addFirst(new CustomProperty(pattern, property));
      }
    }
    builder.add(identifier);
    for (CustomProperty cp : customProperties) {
      builder.add(cp.pattern, cp.value);
    }
    return builder.build();
  }

  private static class CustomProperty {

    public String pattern;
    public java.lang.Object value;

    public CustomProperty(String pattern, java.lang.Object value) {
      this.pattern = pattern;
      this.value = value;
    }
  }
}
