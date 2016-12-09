package com.metarhia.jstp.compiler;

import com.metarhia.jstp.compiler.annotations.*;
import com.metarhia.jstp.core.Handlers.Handler;
import com.metarhia.jstp.core.JSTypes.JSArray;
import com.metarhia.jstp.core.JSTypes.JSObject;
import com.metarhia.jstp.core.JSTypes.JSValue;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.*;


/**
 * Created by lundibundi on 8/7/16.
 */
public class JSTPReceiverAnnotatedInterface {

    public static final String PACKET_PARAMETER_NAME = "packet";
    public static final String INVOKE_PREFIX = "invoke";
    public static final String VARIABLE_DECLARATION = "$1T $2L";
    public static final String VARIABLE_DECLARATION_NULL = VARIABLE_DECLARATION + " = null";
    public static final String VARIABLE_DEFINITION = VARIABLE_DECLARATION + " = ($1T) $3B";
    public static final String VARIABLE_DEFINITION_JAVA_TYPE = VARIABLE_DECLARATION + " = ($1T) ($3B).getGeneralizedValue()";
    public static final String VARIABLE_ASSIGNMENT = "$2L = ($1T) $3B";
    public static final String VARIABLE_ASSIGNMENT_JAVA_TYPE = "$2L = ($1T) ($3B).getGeneralizedValue()";
    private static final String PREFIX = "JSTP";
    private static final TypeName jstpValueType = TypeName.get(JSValue.class);
    private static Class handlerSuperClass = Handler.class;
    private TypeElement annotatedInterface;
    private Types typeUtils;

    private List<ExecutableElement> errorHandlers;
    private List<com.squareup.javapoet.MethodSpec> packetHandlers;

    private MethodSpec.Builder mainInvokeBuilder;
    private TypeSpec.Builder jstpClassBuilder;
    private Elements elementUtils;

    public JSTPReceiverAnnotatedInterface(TypeElement typeElement, Elements elementUtils, Types typeUtils) {
        annotatedInterface = typeElement;
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
        errorHandlers = new LinkedList<>();
        packetHandlers = new LinkedList<>();

        mainInvokeBuilder = MethodSpec.methodBuilder(INVOKE_PREFIX)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(jstpValueType, PACKET_PARAMETER_NAME);
    }

    private static CodeBlock composeGetterFromAnnotations(String name, Element element) throws PropertyFormatException {
        return composeGetterFromAnnotations(CodeBlock.of(name), element);
    }

    private static CodeBlock composeGetterFromAnnotations(CodeBlock name, Element element) throws PropertyFormatException {
        if (element.getAnnotation(CustomNamed.class) != null) {
            CustomNamed annotation = element.getAnnotation(CustomNamed.class);
            return PropertyGetterUtils.composeCustomGetter(name, annotation.value());
        } else if (element.getAnnotation(Named.class) != null) {
            Named annotation = element.getAnnotation(Named.class);
            return PropertyGetterUtils.composeObjectGetter(name, annotation.value());
        } else if (element.getAnnotation(Indexed.class) != null) {
            Indexed annotation = element.getAnnotation(Indexed.class);
            return PropertyGetterUtils.composeArrayGetter(name, annotation.value());
        }
        return null;
    }

    public void generateCode(Filer filer) throws
            ExceptionHandlerInvokeException,
            ClassCastException,
            IOException, PropertyFormatException {
        String jstpReceiverClassName = PREFIX + annotatedInterface.getSimpleName();

        for (Element e : annotatedInterface.getEnclosedElements()) {
            // generate methods to enclose the ones in the interface
            if (e.getKind() == ElementKind.METHOD) {
                // check for error handler
                ExecutableElement method = (ExecutableElement) e;

                if (method.getAnnotation(ErrorHandler.class) != null) {
                    errorHandlers.add(method);
                } else {
                    // create new invoke wrapper
                    MethodSpec invokeMethod = createInvokeMethod(method);
                    packetHandlers.add(invokeMethod);
                }
            }
        }

        final TypeName interfaceTypeName = TypeName.get(annotatedInterface.asType());

        final ParameterizedTypeName handlerSuperclass =
                ParameterizedTypeName.get(ClassName.get(Handler.class), interfaceTypeName);

        jstpClassBuilder = TypeSpec.classBuilder(jstpReceiverClassName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(handlerSuperclass);


        final MethodSpec defaultConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()").build();

        final MethodSpec mainConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(interfaceTypeName, "handler")
                .addStatement("super($N)", "handler")
                .build();

        jstpClassBuilder.addMethod(defaultConstructor);
        jstpClassBuilder.addMethod(mainConstructor);

        mainInvokeBuilder.beginControlFlow("try ");
        for (MethodSpec ms : packetHandlers) {
            mainInvokeBuilder.addStatement("$L($L)", ms.name, PACKET_PARAMETER_NAME);
            jstpClassBuilder.addMethod(ms);
        }
        composeCatchClauses(mainInvokeBuilder, errorHandlers);

        jstpClassBuilder.addMethod(mainInvokeBuilder.build());

        JavaFile javaFile = JavaFile.builder(
                elementUtils.getPackageOf(annotatedInterface).getQualifiedName().toString(),
                jstpClassBuilder.build())
                .indent("    ")
                .build();

        javaFile.writeTo(filer);
    }

    private MethodSpec createInvokeMethod(ExecutableElement method) throws ClassCastException, PropertyFormatException {
        final String name = INVOKE_PREFIX + capitalize(method.getSimpleName().toString());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PRIVATE)
                .addParameter(jstpValueType, PACKET_PARAMETER_NAME);

        String payloadName = "internalPayload";

        TypeMirror payloadType = getClassFromTyped(method, true);
        CodeBlock payloadData = composeGetterFromAnnotations(PACKET_PARAMETER_NAME, method);
        if (payloadData == null) {
            // nothing changed so by default payload is second argument
            payloadData = PropertyGetterUtils.composeCustomGetter(PACKET_PARAMETER_NAME, "{1}");
        }

        methodBuilder.addStatement(VARIABLE_DEFINITION, payloadType, payloadName, payloadData);

        final List<? extends VariableElement> parameters = method.getParameters();

        if (parameters.size() == 1) {
            VariableElement parameter = parameters.get(0);
            payloadType = parameter.asType();
        }

        composeArgumentGetters(method, methodBuilder, payloadType, payloadName);

        TypeName interfaceType = ClassName.get(annotatedInterface.asType());
        String methodCall = composeMethodCall(method.getSimpleName().toString(), parameters);
        methodBuilder.beginControlFlow("for ($T h : handlers)", interfaceType)
                .addStatement("h.$L", methodCall)
                .endControlFlow();

        return methodBuilder.build();
    }

    private TypeMirror getTypeMirror(Class clazz) {
        return typeUtils.erasure(elementUtils.getTypeElement(clazz.getCanonicalName()).asType());
    }

    private String capitalize(String name) {
        if (name != null && name.length() > 0) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name;
    }

    private void composeArgumentGetters(ExecutableElement method, MethodSpec.Builder methodBuilder,
                                        TypeMirror payloadType, String payloadName) throws PropertyFormatException {
        int argCounter = 0;
        CodeBlock payloadNameBlock = CodeBlock.of(payloadName);

        final List<? extends VariableElement> parameters = method.getParameters();
        for (VariableElement parameter : parameters) {
            CodeBlock getter = composeGetterFromAnnotations(payloadNameBlock, parameter);
            // if there are more then one parameter then split payload sequentially
            if (parameters.size() > 1 && getter == null) {
                if (isSameType(payloadType, JSObject.class)) {
                    getter = PropertyGetterUtils.composeCustomGetter(payloadNameBlock, String.format("{%d}", argCounter));
                    composeArgumentGetter(methodBuilder, parameter, getter, false);
                } else if (isSameType(payloadType, JSArray.class)) {
                    getter = PropertyGetterUtils.composeArrayGetter(payloadNameBlock, argCounter);
                    composeArgumentGetter(methodBuilder, parameter, getter, false);
                } else {
                    // don't know type, so check both (JSObject and JSArray)

                    // predeclare
                    methodBuilder.addStatement(VARIABLE_DECLARATION_NULL, parameter.asType(), parameter.getSimpleName());

                    final ClassName objectClassName = ClassName.get(JSObject.class);
                    methodBuilder.beginControlFlow("if ($L instanceof $T)", payloadName, objectClassName);
                    getter = PropertyGetterUtils.composeCustomGetter(payloadName, String.format("{%d}", argCounter));
                    composeArgumentGetter(methodBuilder, parameter, getter, true);

                    final ClassName arrayClassName = ClassName.get(JSArray.class);
                    methodBuilder.nextControlFlow("else if ($L instanceof $T)", payloadName, arrayClassName);
                    getter = PropertyGetterUtils.composeArrayGetter(payloadName, argCounter);
                    composeArgumentGetter(methodBuilder, parameter, getter, true);

                    methodBuilder.endControlFlow();
                }
            } else {
                if (getter == null) getter = CodeBlock.of(payloadName);
                composeArgumentGetter(methodBuilder, parameter, getter, false);
            }
            ++argCounter;
        }
    }

    private void composeArgumentGetter(MethodSpec.Builder methodBuilder, VariableElement parameter, CodeBlock getter, boolean declared) {
        TypeMirror parameterType = parameter.asType();
        String pattern;
        if (isSubtype(parameterType, JSValue.class)) {
            pattern = declared ? VARIABLE_ASSIGNMENT : VARIABLE_DEFINITION;
        } else {
            pattern = declared ? VARIABLE_ASSIGNMENT_JAVA_TYPE : VARIABLE_DEFINITION_JAVA_TYPE;
        }
        methodBuilder.addStatement(pattern, parameterType, parameter.getSimpleName(), getter);
    }

    private TypeMirror getClassFromTyped(ExecutableElement method, boolean strictJSType) throws ClassCastException {
        TypeMirror payloadType = null;
        if (method.getAnnotation(Typed.class) != null) {
            try {
                method.getAnnotation(Typed.class).value();
            } catch (MirroredTypeException e) {
                // intended ...
                payloadType = e.getTypeMirror();
            }
            if (strictJSType && !isSubtype(payloadType, JSValue.class)) {
                throw new ClassCastException(
                        "Cannot cast jstp packet data to " + payloadType.toString());
            }
        } else {
            payloadType = elementUtils.getTypeElement(JSValue.class.getCanonicalName()).asType();
        }
        return typeUtils.erasure(payloadType);
    }

    private String composeMethodCall(String methodName, List<? extends VariableElement> parameters) {
        StringBuilder builder = new StringBuilder(methodName + "(");
        int i = 0;
        for (VariableElement parameter : parameters) {
            builder.append(parameter.getSimpleName());
            if (++i < parameters.size()) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    private void composeCatchClauses(MethodSpec.Builder builder, List<ExecutableElement> errorHandlers) throws ExceptionHandlerInvokeException {
        Map<TypeMirror, List<ExecutableElement>> exceptionHandlers = new TreeMap<>(new Comparator<TypeMirror>() {
            @Override
            public int compare(TypeMirror tm1, TypeMirror tm2) {
                if (typeUtils.isSameType(tm1, tm2)) return 0;
                else if (typeUtils.isSubtype(tm1, tm2)) return -1;
                else if (typeUtils.isSubtype(tm2, tm1)) return 1;
                return 0;
            }
        });

        for (ExecutableElement ee : errorHandlers) {
            List<? extends TypeMirror> exceptions = null;
            try {
                ee.getAnnotation(ErrorHandler.class).exceptionTypes();
            } catch (MirroredTypesException e) {
                // intended ...
                exceptions = e.getTypeMirrors();
            }
            for (TypeMirror e : exceptions) {
                List<ExecutableElement> functions = exceptionHandlers.get(e);
                if (functions == null) {
                    exceptionHandlers.put(e, new LinkedList<ExecutableElement>());
                    functions = exceptionHandlers.get(e);
                }
                functions.add(ee);
            }
        }

        final TypeMirror exceptionMirror = getTypeMirror(Exception.class);
        boolean addedGeneralClause = false;

        for (Map.Entry<TypeMirror, List<ExecutableElement>> me : exceptionHandlers.entrySet()) {
            addCatchClause(builder, me.getKey(), me.getValue());
            if (typeUtils.isAssignable(me.getKey(), exceptionMirror)) addedGeneralClause = true;
        }

        if (!addedGeneralClause) {
            addCatchClause(builder, exceptionMirror, new LinkedList<ExecutableElement>());
        }

        builder.endControlFlow();
    }

    private void addCatchClause(MethodSpec.Builder builder, TypeMirror type, List<ExecutableElement> funcs) throws ExceptionHandlerInvokeException {
        builder.nextControlFlow("catch ($T e)", ClassName.get(type));
        for (ExecutableElement func : funcs) {
            if (!checkFirstCast(func.getParameters(), type)) {
                throw new ExceptionHandlerInvokeException(
                        func.getSimpleName() + " cannot be called with " + type.toString());
            }
            builder.beginControlFlow("for ($T h : handlers)", ClassName.get(annotatedInterface.asType()))
                    .addStatement("h.$L(e)", func.getSimpleName().toString())
                    .endControlFlow();
        }
    }

    private boolean checkFirstCast(List<? extends VariableElement> parameters, TypeMirror type) {
        return parameters.size() == 1
                && typeUtils.isAssignable(parameters.get(0).asType(), type);
    }

    public boolean isSubtype(TypeMirror type, Class<?> clazz) {
        return typeUtils.isSubtype(type, getTypeMirror(clazz));
    }

    public boolean isSameType(TypeMirror type, Class<?> clazz) {
        return isSameType(type, clazz.getCanonicalName());
    }

    public boolean isSameType(TypeMirror type, String canonicalName) {
        TypeMirror otherType = elementUtils.getTypeElement(canonicalName).asType();
        return typeUtils.isSameType(type, typeUtils.erasure(otherType));
    }
}
