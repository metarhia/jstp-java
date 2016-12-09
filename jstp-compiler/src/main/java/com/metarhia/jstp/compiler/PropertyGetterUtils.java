package com.metarhia.jstp.compiler;

import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import java.util.LinkedList;
import java.util.regex.Pattern;

public class PropertyGetterUtils {

    public static final String OBJECT_GETTER = ").get($S)";
    public static final String ARRAY_GETTER = ").get($L)";
    private static final Pattern simpleDoublePattern = Pattern.compile("\\d+\\.?\\d*");

    public static CodeBlock composeArrayGetter(String identifier, int... indices) {
        return composeArrayGetter(CodeBlock.of(identifier), indices);
    }

    public static CodeBlock composeArrayGetter(CodeBlock identifier, int... indices) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (int i = indices.length - 1; i >= 0; i--) {
            builder.add("(($T) ", ClassName.get(JSArray.class));
        }
        builder.add(identifier);
        for (int i = 0; i < indices.length; i++) {
            builder.add(ARRAY_GETTER, indices[i]);
        }
        return builder.build();
    }

    public static CodeBlock composeObjectGetter(String identifier, String... properties) {
        return composeObjectGetter(CodeBlock.of(identifier), properties);
    }

    public static CodeBlock composeObjectGetter(CodeBlock identifier, String... properties) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (int i = properties.length - 1; i >= 0; i--) {
            builder.add("(($T) ", ClassName.get(JSObject.class));
        }
        builder.add(identifier);
        for (int i = 0; i < properties.length; i++) {
            builder.add(OBJECT_GETTER, properties[i]);
        }
        return builder.build();
    }

    public static CodeBlock composeObjectGetter(CodeBlock leftHandSide, CodeBlock identifier, String... properties) {
        CodeBlock.Builder builder = leftHandSide.toBuilder();
        for (int i = properties.length - 1; i >= 0; i--) {
            builder.add("(($T) ", ClassName.get(JSObject.class));
        }
        builder.add(identifier);
        for (int i = 0; i < properties.length; i++) {
            builder.add(OBJECT_GETTER, properties[i]);
        }
        return builder.build();
    }


    public static CodeBlock composeCustomGetter(String identifier, String... properties) throws PropertyFormatException {
        return composeCustomGetter(CodeBlock.of(identifier), properties);
    }

    public static CodeBlock composeCustomGetter(CodeBlock identifier, String... properties) throws PropertyFormatException {
        LinkedList<CustomProperty> customProperties = new LinkedList<>();
        CodeBlock.Builder builder = CodeBlock.builder();
        for (int i = properties.length - 1; i >= 0; i--) {
            String property = properties[i];
            if (property.startsWith("[") && property.endsWith("]")
                    ||
                    (property.startsWith("{") && property.endsWith("}"))) {
                ClassName type = ClassName.get(property.startsWith("[") ? JSArray.class : JSObject.class);
                if (property.length() < 3) {
                    throw new PropertyFormatException("Empty array of indices");
                }
                property = property.substring(1, property.length() - 1);
                String[] indices = property.split("[, ]+");
                for (int j = indices.length - 1; j >= 0; j--) {
                    builder.add("(($T) ", type);
                    customProperties.addFirst(new CustomProperty(ARRAY_GETTER, indices[j]));
                }
            } else {
                builder.add("(($T) ", ClassName.get(JSObject.class));
                customProperties.addFirst(new CustomProperty(OBJECT_GETTER, property));
            }
        }
        builder.add(identifier);
        for (CustomProperty cp : customProperties) {
            builder.add(cp.pattern, cp.property);
        }
        return builder.build();
    }

    private static class CustomProperty {
        public String pattern;
        public String property;

        public CustomProperty(String pattern, String property) {
            this.pattern = pattern;
            this.property = property;
        }
    }
}
