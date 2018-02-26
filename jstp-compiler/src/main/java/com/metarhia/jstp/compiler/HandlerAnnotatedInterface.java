package com.metarhia.jstp.compiler;

import com.metarhia.jstp.compiler.annotations.handlers.Error;
import com.metarhia.jstp.compiler.annotations.handlers.ExceptionHandler;
import com.metarhia.jstp.compiler.annotations.handlers.Handler;
import com.metarhia.jstp.compiler.annotations.handlers.Has;
import com.metarhia.jstp.compiler.annotations.handlers.NoDefaultGet;
import com.metarhia.jstp.compiler.annotations.handlers.NotNull;
import com.metarhia.jstp.compiler.annotations.handlers.Typed;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.handlers.OkErrorHandler;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;


/**
 * Created by lundibundi on 8/7/16.
 */
public class HandlerAnnotatedInterface {

  private static final String PREFIX = "JSTP";

  private static final String HANDLER_METHOD = "onMessage";
  private static final String ERROR_METHOD = "onError";

  private static final String VARIABLE_DECLARATION = "$1T $2L";
  private static final String VARIABLE_DECLARATION_NULL = VARIABLE_DECLARATION + " = null";
  private static final String VARIABLE_DEFINITION = VARIABLE_DECLARATION + " = ($1T) $3L";
  private static final String VARIABLE_ASSIGNMENT = "$2L = ($1T) $3L";

  public static final String NUMBER_TO_LONG_VALUE = "(($1T) $2L).longValue()";

  private static final Class JSTP_VALUE_TYPE = Object.class;
  private static final TypeName JSTP_VALUE_TYPENAME = TypeName.get(JSObject.class);

  private final Class<?> annotation;
  private TypeElement annotatedInterface;

  private TypeSpec.Builder jstpClassBuilder;
  private TypeUtils typeUtils;
  private TypeName interfaceClassName;
  private TypeName interfaceTypeName;
  private Class<?> handlerClass;

  public HandlerAnnotatedInterface(Class<?> annotation, TypeElement typeElement,
                                   Elements elements, Types types) {
    this.annotation = annotation;
    annotatedInterface = typeElement;
    typeUtils = new TypeUtils(types, elements);

    interfaceClassName = ClassName.get(annotatedInterface.asType());
    interfaceTypeName = TypeName.get(annotatedInterface.asType());

    handlerClass = ManualHandler.class;

    TypeMirror jstpHandlerClass = null;
    try {
      annotatedInterface.getAnnotation(Handler.class).value();
    } catch (MirroredTypeException e) {
      // intended ...
      jstpHandlerClass = e.getTypeMirror();
    }

    if (jstpHandlerClass != null) {
      if (typeUtils.isSameType(jstpHandlerClass, OkErrorHandler.class)) {
        handlerClass = OkErrorHandler.class;
      } else if (!typeUtils.isSameType(jstpHandlerClass, ManualHandler.class)) {
        throw new HandlerProcessorException(
            "Not supported ManualHandler subtype: " + jstpHandlerClass.toString());
      }
    }
  }

  public void generateCode(Filer filer) throws
      ExceptionHandlerInvokeException,
      ClassCastException,
      IOException, PropertyFormatException {
    String implementationClassName = PREFIX + annotatedInterface.getSimpleName();

    jstpClassBuilder = TypeSpec.classBuilder(implementationClassName)
        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
        .addSuperinterface(interfaceTypeName);

    if (handlerClass == OkErrorHandler.class) {
      generateOkErrorHandler();
    } else {
      generateManualHandler();
    }

    // save to file
    JavaFile javaFile = JavaFile.builder(
        typeUtils.getElements()
            .getPackageOf(annotatedInterface)
            .getQualifiedName()
            .toString(),
        jstpClassBuilder.build())
        .indent("    ")
        .build();
    javaFile.writeTo(filer);
  }

  private void generateManualHandler()
      throws PropertyFormatException, ExceptionHandlerInvokeException {
    jstpClassBuilder.addSuperinterface(handlerClass);

    String messageParameterName = "internalMessageParameter";

    List<ExecutableElement> exceptionHandlers = new LinkedList<>();
    List<MessageHandler> messageHandlers = new LinkedList<>();
    List<ExecutableElement> errorMethods = new LinkedList<>();
    MethodSpec.Builder mainInvokeBuilder = MethodSpec.methodBuilder(HANDLER_METHOD)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(JSTP_VALUE_TYPENAME, messageParameterName);

    // generate methods to enclose the ones in the interface
    for (Element e : annotatedInterface.getEnclosedElements()) {
      if (e.getKind() == ElementKind.METHOD) {
        // check for exception handler
        ExecutableElement method = (ExecutableElement) e;
        if (method.getAnnotation(ExceptionHandler.class) != null) {
          exceptionHandlers.add(method);
        } else if (method.getAnnotation(Error.class) != null) {
          errorMethods.add(method);
        } else {
          // create new handler wrapper
          MethodSpec invokeMethod = createHandlerWrapper(method);
          messageHandlers.add(new MessageHandler(method, invokeMethod));
        }
      }
    }

    // generate main invocation function
    if (exceptionHandlers.size() > 0) {
      mainInvokeBuilder.beginControlFlow("try ");
    }

    addHandlerInvocations(mainInvokeBuilder, messageHandlers, messageParameterName);

    if (exceptionHandlers.size() > 0) {
      composeCatchClauses(mainInvokeBuilder, exceptionHandlers);
      mainInvokeBuilder.endControlFlow();
    }

    // generate onError function
    String errorCodeParameter = "errorCode";
    MethodSpec.Builder errorMethodBuilder = MethodSpec.methodBuilder(ERROR_METHOD)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(TypeName.INT, errorCodeParameter);
    for (ExecutableElement method : errorMethods) {
      int[] errors = method.getAnnotation(Error.class).errors();
      if (errors.length > 0) {
        addErrorCodeCheck(errorMethodBuilder, errorCodeParameter, errors);
      }

      errorMethodBuilder.addStatement("$L($L)",
          method.getSimpleName().toString(), errorCodeParameter);

      if (errors.length > 0) {
        errorMethodBuilder.endControlFlow();
      }
    }

    jstpClassBuilder.addMethod(mainInvokeBuilder.build());
    jstpClassBuilder.addMethod(errorMethodBuilder.build());
  }

  private void addErrorCodeCheck(Builder errorMethodBuilder, String errorCodeParameter,
                                 int[] errors) {
    List<String> errorElements = new ArrayList<>();
    for (int e : errors) {
      errorElements.add(String.valueOf(e));
    }
    errorMethodBuilder.beginControlFlow(composeCondition(
        " || ", " == " + errorCodeParameter, errorElements));
  }

  private void generateOkErrorHandler()
      throws PropertyFormatException, ExceptionHandlerInvokeException {
    jstpClassBuilder.superclass(OkErrorHandler.class);

    String messageParameterName = "data";
    List<MessageHandler> okHandlers = new LinkedList<>();
    List<MessageHandler> errorHandlers = new LinkedList<>();
    List<ExecutableElement> exceptionHandlers = new LinkedList<>();
    MethodSpec.Builder okHandlerBuilder = MethodSpec.methodBuilder("handleOk")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(List.class),
                WildcardTypeName.subtypeOf(Object.class)),
            messageParameterName);

    String errorCodeParameterName = "errorCode";
    MethodSpec.Builder errorHandlerBuilder = MethodSpec.methodBuilder("handleError")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(TypeName.get(Integer.class), errorCodeParameterName)
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(List.class),
                WildcardTypeName.subtypeOf(Object.class)),
            messageParameterName);

    // generate methods to enclose the ones in the interface
    for (Element e : annotatedInterface.getEnclosedElements()) {
      if (e.getKind() != ElementKind.METHOD) {
        continue;
      }
      // check for exception handler
      ExecutableElement method = (ExecutableElement) e;
      if (method.getAnnotation(ExceptionHandler.class) != null) {
        exceptionHandlers.add(method);
      } else if (method.getAnnotation(Error.class) != null) {
        // create new error wrapper
        MethodSpec errorMethod = createErrorWrapper(method);
        errorHandlers.add(new MessageHandler(method, errorMethod));
      } else {
        // create new invoke wrapper
        MethodSpec invokeMethod = createOkWrapper(method);
        okHandlers.add(new MessageHandler(method, invokeMethod));
      }
    }

    if (exceptionHandlers.size() > 0) {
      MethodSpec.Builder handlerMethod = MethodSpec.methodBuilder(HANDLER_METHOD)
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .addParameter(JSTP_VALUE_TYPENAME, "message");
      handlerMethod.beginControlFlow("try ");
      handlerMethod.addStatement("super.$L($L)", HANDLER_METHOD, "message");
      composeCatchClauses(handlerMethod, exceptionHandlers);
      handlerMethod.endControlFlow();
      jstpClassBuilder.addMethod(handlerMethod.build());
    }

    addHandlerInvocations(okHandlerBuilder, okHandlers, messageParameterName);

    for (MessageHandler mh : errorHandlers) {
      if (mh.method.getAnnotation(Has.class) != null) {
        throw new HandlerProcessorException("'Has' annotation is not supported for error handlers");
      }
      int[] errors = mh.method.getAnnotation(Error.class).errors();
      if (errors.length > 0) {
        addErrorCodeCheck(errorHandlerBuilder, errorCodeParameterName, errors);
      }

      errorHandlerBuilder.addStatement("$L($L, $L)", mh.handler.name,
          errorCodeParameterName, messageParameterName);
      jstpClassBuilder.addMethod(mh.handler);

      if (errors.length > 0) {
        errorHandlerBuilder.endControlFlow();
      }
    }

    jstpClassBuilder.addMethod(okHandlerBuilder.build());
    jstpClassBuilder.addMethod(errorHandlerBuilder.build());
  }

  private void addHandlerInvocations(MethodSpec.Builder methodBuilder,
                                     List<MessageHandler> handlers,
                                     String parameterName) throws PropertyFormatException {
    for (MessageHandler mh : handlers) {
      Has hasAnnotation = mh.method.getAnnotation(Has.class);
      if (hasAnnotation != null) {
        CodeBlock getter = PropertyGetterUtils.composeCustomGetter(
            parameterName, hasAnnotation.value());
        methodBuilder.beginControlFlow("if ($L != null)", getter);
      }

      methodBuilder.addStatement("$L($L)", mh.handler.name, parameterName);
      jstpClassBuilder.addMethod(mh.handler);

      if (hasAnnotation != null) {
        methodBuilder.endControlFlow();
      }
    }
  }

  private MethodSpec createOkWrapper(ExecutableElement method)
      throws ClassCastException, PropertyFormatException {
    final String methodName = HANDLER_METHOD + "Ok" + capitalize(method.getSimpleName().toString());
    String dataParameterName = "dataParameter";

    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PRIVATE)
        .addParameter(ParameterizedTypeName.get(ClassName.get(List.class),
            WildcardTypeName.subtypeOf(Object.class)), dataParameterName);

    String payloadName = dataParameterName;

    TypeMirror payloadType = getClassFromTyped(method);
    if (typeUtils.isSameType(payloadType, Object.class)) {
      payloadType = typeUtils.getTypeMirror(List.class);
    }

    CodeBlock payloadData = PropertyGetterUtils
        .composeGetterFromAnnotations(dataParameterName, method);
    if (payloadData != null) {
      payloadName = "internalPayloadParameter";
      methodBuilder.addStatement(VARIABLE_DEFINITION, payloadType, payloadName, payloadData);
      methodBuilder.beginControlFlow("if ($L == null)", payloadName)
          .addStatement("return")
          .endControlFlow();
    }

    final List<? extends VariableElement> parameters = method.getParameters();

    if (parameters.size() == 1) {
      VariableElement parameter = parameters.get(0);
      payloadType = parameter.asType();
    }

    composeArgumentGetters(method, methodBuilder, method.getParameters(), payloadType, payloadName);

    addNotNullChecks(method, methodBuilder, parameters);

    String methodCall = composeMethodCall(method.getSimpleName().toString(), parameters);

    methodBuilder.addStatement(methodCall);

    return methodBuilder.build();
  }

  private void addNotNullChecks(ExecutableElement method, Builder methodBuilder,
                                List<? extends VariableElement> parameters) {
    Class notNullAnnotation = method.getAnnotation(NotNull.class) != null ? null : NotNull.class;

    // filter elements with NotNull
    List<String> notNullParameters = new ArrayList<>();
    for (VariableElement ve : parameters) {
      if (notNullAnnotation == null || ve.getAnnotation(notNullAnnotation) != null) {
        notNullParameters.add(ve.getSimpleName().toString());
      }
    }

    if (!notNullParameters.isEmpty()) {
      methodBuilder.beginControlFlow(composeCondition(
          " || ", " == null", notNullParameters))
          .addStatement("return")
          .endControlFlow();
    }
  }

  private MethodSpec createErrorWrapper(ExecutableElement method)
      throws ClassCastException, PropertyFormatException {
    final String methodName =
        HANDLER_METHOD + "Error" + capitalize(method.getSimpleName().toString());
    String dataParameterName = "dataParameter";
    String errorParameterName = "errorCodeParameter";
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PRIVATE)
        .addParameter(TypeName.get(Integer.class), errorParameterName)
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(List.class),
                WildcardTypeName.subtypeOf(Object.class)),
            dataParameterName);

    String payloadName = dataParameterName;

    TypeMirror payloadType = getClassFromTyped(method);
    if (typeUtils.isSameType(payloadType, Object.class)) {
      payloadType = typeUtils.getTypeMirror(List.class);
    }

    CodeBlock payloadData = PropertyGetterUtils
        .composeGetterFromAnnotations(dataParameterName, method);
    if (payloadData != null) {
      payloadName = "internalPayloadParameter";
      methodBuilder.addStatement(VARIABLE_DEFINITION, payloadType, payloadName, payloadData);
      methodBuilder.beginControlFlow("if ($L == null)", payloadName)
          .addStatement("return")
          .endControlFlow();
    }

    final List<? extends VariableElement> parameters = new ArrayList<>(method.getParameters());
    // remove first error code parameter
    parameters.remove(0);

    List<String> parameterNames = new ArrayList<>();
    parameterNames.add(errorParameterName);
    for (VariableElement ve : parameters) {
      parameterNames.add(ve.getSimpleName().toString());
    }

    // only add getters for more than one parameter because first is always error code parameter
    // >0 because error code parameter was removed beforehand
    if (parameters.size() > 0) {
      composeArgumentGetters(method, methodBuilder, parameters, payloadType, payloadName);

      addNotNullChecks(method, methodBuilder, parameters);
    }

    methodBuilder.addStatement(composeMethodCallBase(
        method.getSimpleName().toString(), parameterNames));

    return methodBuilder.build();
  }

  private MethodSpec createHandlerWrapper(ExecutableElement method)
      throws ClassCastException, PropertyFormatException {
    String messageParameterName = "messageParameter";
    final String name = "wrap" + capitalize(method.getSimpleName().toString());
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PRIVATE)
        .addParameter(JSTP_VALUE_TYPENAME, messageParameterName);

    String payloadName = messageParameterName;

    TypeMirror payloadType = getClassFromTyped(method);
    if (typeUtils.isSameType(payloadType, Object.class)) {
      // use List type by default
      payloadType = typeUtils.getTypeMirror(List.class);
    }
    CodeBlock payloadData = PropertyGetterUtils
        .composeGetterFromAnnotations(messageParameterName, method);
    if (payloadData == null && method.getAnnotation(NoDefaultGet.class) == null) {
      // no method annotation and no explicit denial of default getter
      // so by default payload is second argument
      payloadData = PropertyGetterUtils.composeCustomGetter(messageParameterName, "{1}");
    }

    if (payloadData != null) {
      payloadName = "internalPayloadParameter";
      methodBuilder.addStatement(VARIABLE_DEFINITION, payloadType, payloadName, payloadData);
      methodBuilder.beginControlFlow("if ($L == null)", payloadName)
          .addStatement("return")
          .endControlFlow();
    }

    final List<? extends VariableElement> parameters = method.getParameters();

    if (parameters.size() == 1) {
      VariableElement parameter = parameters.get(0);
      payloadType = parameter.asType();
    }

    composeArgumentGetters(method, methodBuilder, method.getParameters(), payloadType, payloadName);

    addNotNullChecks(method, methodBuilder, parameters);

    String methodCall = composeMethodCall(method.getSimpleName().toString(), parameters);

    methodBuilder.addStatement(methodCall);

    return methodBuilder.build();
  }

  private String capitalize(String name) {
    if (name != null && name.length() > 0) {
      return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    return name;
  }

  private void composeArgumentGetters(ExecutableElement method, MethodSpec.Builder methodBuilder,
                                      List<? extends VariableElement> parameters,
                                      TypeMirror payloadType, String payloadName)
      throws PropertyFormatException {
    int argCounter = 0;
    CodeBlock payloadNameBlock = CodeBlock.of(payloadName);

    for (VariableElement parameter : parameters) {
      CodeBlock getter = PropertyGetterUtils
          .composeGetterFromAnnotations(payloadNameBlock, parameter);
      // if there are more then one parameter then split payload sequentially
      if (parameters.size() > 1 && getter == null) {
        if (typeUtils.isSameType(payloadType, JSObject.class)) {
          getter = PropertyGetterUtils
              .composeCustomGetter(payloadNameBlock, String.format("{%d}", argCounter));
          putArgumentGetter(methodBuilder, parameter, getter, false);
        } else if (typeUtils.isSameType(payloadType, List.class)) {
          // use List by default
          getter = PropertyGetterUtils.composeArrayGetter(payloadNameBlock, argCounter);
          putArgumentGetter(methodBuilder, parameter, getter, false);
        } else {
          throw new HandlerProcessorException(
              "Unsupported payload type. Only JSObject and List are supported");
        }
      } else {
        if (getter == null) {
          getter = CodeBlock.of(payloadName);
        }
        putArgumentGetter(methodBuilder, parameter, getter, false);
      }
      ++argCounter;
    }
  }

  private void putArgumentGetter(MethodSpec.Builder methodBuilder, VariableElement parameter,
                                 CodeBlock getter, boolean declared) {
    TypeMirror parameterType = parameter.asType();
    String pattern = declared ? VARIABLE_ASSIGNMENT : VARIABLE_DEFINITION;
    if (typeUtils.isSameType(parameterType, Long.class)) {
      getter = CodeBlock.of(NUMBER_TO_LONG_VALUE, TypeName.get(Number.class), getter);
    }
    methodBuilder.addStatement(pattern, parameterType, parameter.getSimpleName(), getter);
  }

  private TypeMirror getClassFromTyped(ExecutableElement method)
      throws ClassCastException {
    TypeMirror payloadType = null;
    if (method.getAnnotation(Typed.class) != null) {
      try {
        method.getAnnotation(Typed.class).value();
      } catch (MirroredTypeException e) {
        // intended ...
        payloadType = e.getTypeMirror();
      }
    } else {
      payloadType = typeUtils.getTypeElement(JSTP_VALUE_TYPE.getCanonicalName());
    }
    return typeUtils.erasure(payloadType);
  }

  private String composeCondition(String logic, String condition,
                                  List<String> parameters) {
    StringBuilder builder = new StringBuilder("if (");
    int i = 0;
    for (String parameter : parameters) {
      ++i;
      builder.append(parameter)
          .append(condition);
      if (i < parameters.size()) {
        builder.append(logic);
      }
    }
    builder.append(')');
    return builder.toString();
  }

  private String composeMethodCall(String methodName, List<? extends VariableElement> parameters) {
    List<String> params = new ArrayList<>();
    for (VariableElement ve : parameters) {
      params.add(ve.getSimpleName().toString());
    }
    return composeMethodCallBase(methodName, params);
  }

  private String composeMethodCallBase(String methodName, List<String> parameters) {
    StringBuilder builder = new StringBuilder(methodName);
    builder.append('(');
    int i = 0;
    for (String parameter : parameters) {
      builder.append(parameter);
      if (++i < parameters.size()) {
        builder.append(", ");
      }
    }
    builder.append(')');
    return builder.toString();
  }

  private void composeCatchClauses(MethodSpec.Builder builder,
                                   List<ExecutableElement> errorHandlers)
      throws ExceptionHandlerInvokeException {
    Map<TypeMirror, List<ExecutableElement>> exceptionHandlers = new TreeMap<>(
        new Comparator<TypeMirror>() {
          @Override
          public int compare(TypeMirror tm1, TypeMirror tm2) {
            if (typeUtils.isSameType(tm1, tm2)) {
              return 0;
            } else if (typeUtils.isSubtype(tm1, tm2)) {
              return -1;
            } else if (typeUtils.isSubtype(tm2, tm1)) {
              return 1;
            }
            return 0;
          }
        });

    for (ExecutableElement ee : errorHandlers) {
      List<? extends TypeMirror> exceptions = null;
      try {
        ee.getAnnotation(ExceptionHandler.class).value();
      } catch (MirroredTypesException e) {
        // intended ...
        exceptions = e.getTypeMirrors();
      }
      if (exceptions == null) {
        return;
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

    for (Map.Entry<TypeMirror, List<ExecutableElement>> me : exceptionHandlers.entrySet()) {
      addCatchClause(builder, me.getKey(), me.getValue());
    }
  }

  private void addCatchClause(MethodSpec.Builder builder, TypeMirror type,
                              List<ExecutableElement> funcs)
      throws ExceptionHandlerInvokeException {
    builder.nextControlFlow("catch ($T e)", ClassName.get(type));

    String callPattern = "$L(e)";

    for (ExecutableElement func : funcs) {
      if (!checkFirstCast(func.getParameters(), type)) {
        throw new ExceptionHandlerInvokeException(
            func.getSimpleName() + " cannot be called with " + type.toString());
      }
      builder.addStatement(callPattern, func.getSimpleName().toString());
    }
  }

  private boolean checkFirstCast(List<? extends VariableElement> parameters, TypeMirror type) {
    return parameters.size() == 1
        && typeUtils.isAssignable(parameters.get(0).asType(), type);
  }

  private static class MessageHandler {

    public final ExecutableElement method;
    public final MethodSpec handler;

    public MessageHandler(ExecutableElement method, MethodSpec handler) {
      this.method = method;
      this.handler = handler;
    }
  }
}
