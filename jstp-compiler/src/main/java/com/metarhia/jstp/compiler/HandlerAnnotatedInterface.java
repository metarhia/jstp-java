package com.metarhia.jstp.compiler;

import com.metarhia.jstp.compiler.annotations.handlers.ErrorHandler;
import com.metarhia.jstp.compiler.annotations.handlers.Handler;
import com.metarhia.jstp.compiler.annotations.handlers.NoDefaultGet;
import com.metarhia.jstp.compiler.annotations.handlers.NotNull;
import com.metarhia.jstp.compiler.annotations.handlers.Receiver;
import com.metarhia.jstp.compiler.annotations.handlers.Typed;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.metarhia.jstp.core.JSInterfaces.JSObject;
import com.metarhia.jstp.handlers.ExecutableHandler;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
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
  private static final String MESSAGE_PARAMETER_NAME = "internalMessage";
  private static final String HANDLER_METHOD = "handle";
  private static final String VARIABLE_DECLARATION = "$1T $2L";
  private static final String VARIABLE_DECLARATION_NULL = VARIABLE_DECLARATION + " = null";
  private static final String VARIABLE_DEFINITION = VARIABLE_DECLARATION + " = ($1T) $3L";
  private static final String VARIABLE_ASSIGNMENT = "$2L = ($1T) $3L";

  public static final String NUMBER_TO_LONG_VALUE = "(($1T) $2L).longValue()";

  private static final Class JSTP_VALUE_TYPE = Object.class;
  private static final TypeName JSTP_VALUE_TYPENAME = TypeName.get(JSObject.class);

  private final Class<?> annotation;
  private TypeElement annotatedInterface;

  private List<ExecutableElement> errorHandlers;
  private List<com.squareup.javapoet.MethodSpec> messageHandlers;

  private MethodSpec.Builder mainInvokeBuilder;
  private TypeSpec.Builder jstpClassBuilder;
  private TypeUtils typeUtils;
  private String handlersName;
  private String messageParameterName;
  private TypeName interfaceClassName;
  private TypeName interfaceTypeName;
  private Class<?> handlerClass;

  public HandlerAnnotatedInterface(Class<?> annotation, TypeElement typeElement,
                                   Elements elements, Types types) {
    this.annotation = annotation;
    annotatedInterface = typeElement;
    typeUtils = new TypeUtils(types, elements);
    errorHandlers = new LinkedList<>();
    messageHandlers = new LinkedList<>();

    interfaceClassName = ClassName.get(annotatedInterface.asType());
    interfaceTypeName = TypeName.get(annotatedInterface.asType());

    handlerClass = ManualHandler.class;

    TypeMirror jstpHandlerClass = null;
    handlersName = null;
    if (annotation == Receiver.class) {
      handlerClass = com.metarhia.jstp.core.Handlers.Handler.class;
      handlersName = "handlers";
    } else {
      try {
        annotatedInterface.getAnnotation(Handler.class).value();
      } catch (MirroredTypeException e) {
        // intended ...
        jstpHandlerClass = e.getTypeMirror();
      }
    }

    if (jstpHandlerClass != null &&
        typeUtils.isSubtype(jstpHandlerClass, ExecutableHandler.class)) {
      handlerClass = ExecutableHandler.class;
      mainInvokeBuilder = MethodSpec.methodBuilder("run")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC);
      messageParameterName = "message";
    } else {
      messageParameterName = MESSAGE_PARAMETER_NAME;
      mainInvokeBuilder = MethodSpec.methodBuilder(HANDLER_METHOD)
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .addParameter(JSTP_VALUE_TYPENAME, messageParameterName);
    }
  }

  public void generateCode(Filer filer) throws
      ExceptionHandlerInvokeException,
      ClassCastException,
      IOException, PropertyFormatException {
    String implementationClassName = PREFIX + annotatedInterface.getSimpleName();

    if (handlersName == null) {
      jstpClassBuilder = TypeSpec.classBuilder(implementationClassName)
          .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
          .addSuperinterface(interfaceTypeName);

      if (handlerClass != ExecutableHandler.class) {
        jstpClassBuilder.addSuperinterface(handlerClass);
      } else {
        jstpClassBuilder.superclass(handlerClass);

        final MethodSpec mainConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassName.get(Executor.class), "executor")
            .addStatement("super($N)", "executor")
            .build();

        jstpClassBuilder.addMethod(mainConstructor);
      }
    } else {
      final ParameterizedTypeName handlerSuperclass =
          ParameterizedTypeName.get(ClassName.get(handlerClass), interfaceTypeName);

      jstpClassBuilder = TypeSpec.classBuilder(implementationClassName)
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
    }

    // generate methods to enclose the ones in the interface
    for (Element e : annotatedInterface.getEnclosedElements()) {
      if (e.getKind() == ElementKind.METHOD) {
        // check for error handler
        ExecutableElement method = (ExecutableElement) e;
        if (method.getAnnotation(ErrorHandler.class) != null) {
          errorHandlers.add(method);
        } else {
          // create new invoke wrapper
          MethodSpec invokeMethod = createInvokeMethod(method, handlersName);
          messageHandlers.add(invokeMethod);
        }
      }
    }

    // generate main invocation function
    if (errorHandlers.size() > 0) {
      mainInvokeBuilder.beginControlFlow("try ");
    }
    for (MethodSpec ms : messageHandlers) {
      mainInvokeBuilder.addStatement("$L($L)", ms.name, messageParameterName);
      jstpClassBuilder.addMethod(ms);
    }

    if (errorHandlers.size() > 0) {
      composeCatchClauses(mainInvokeBuilder, errorHandlers, handlersName);
    }

    jstpClassBuilder.addMethod(mainInvokeBuilder.build());

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

  private MethodSpec createInvokeMethod(ExecutableElement method, String handlersName)
      throws ClassCastException, PropertyFormatException {
    final String name = HANDLER_METHOD + capitalize(method.getSimpleName().toString());
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PRIVATE)
        .addParameter(JSTP_VALUE_TYPENAME, MESSAGE_PARAMETER_NAME);

    String payloadName = MESSAGE_PARAMETER_NAME;

    TypeMirror payloadType = getClassFromTyped(method, true);
    CodeBlock payloadData = PropertyGetterUtils
        .composeGetterFromAnnotations(MESSAGE_PARAMETER_NAME, method);
    if (payloadData == null && method.getAnnotation(NoDefaultGet.class) == null) {
      // no method annotation and no explicit denial of default getter
      // so by default payload is second argument
      payloadData = PropertyGetterUtils.composeCustomGetter(MESSAGE_PARAMETER_NAME, "{1}");
    }

    if (payloadData != null) {
      payloadName = "internalPayload";
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

    composeArgumentGetters(method, methodBuilder, payloadType, payloadName);

    Class notNullAnnotation = method.getAnnotation(NotNull.class) != null ? null : NotNull.class;
    final String controlFlow = composeCondition(" || ", " == null", parameters, notNullAnnotation);
    if (controlFlow != null) {
      methodBuilder.beginControlFlow(controlFlow)
          .addStatement("return")
          .endControlFlow();
    }

    String methodCall = composeMethodCall(method.getSimpleName().toString(), parameters);

    if (handlersName != null) {
      final String handlersForLoop = String.format("for ($T h : %s)", handlersName);
      methodBuilder.beginControlFlow(handlersForLoop, interfaceTypeName)
          .addStatement("h.$L", methodCall)
          .endControlFlow();
    } else {
      methodBuilder.addStatement(methodCall);
    }

    return methodBuilder.build();
  }

  private String capitalize(String name) {
    if (name != null && name.length() > 0) {
      return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    return name;
  }

  private void composeArgumentGetters(ExecutableElement method, MethodSpec.Builder methodBuilder,
                                      TypeMirror payloadType, String payloadName)
      throws PropertyFormatException {
    int argCounter = 0;
    CodeBlock payloadNameBlock = CodeBlock.of(payloadName);

    final List<? extends VariableElement> parameters = method.getParameters();
    for (VariableElement parameter : parameters) {
      CodeBlock getter = PropertyGetterUtils
          .composeGetterFromAnnotations(payloadNameBlock, parameter);
      // if there are more then one parameter then split payload sequentially
      if (parameters.size() > 1 && getter == null) {
        if (typeUtils.isSameType(payloadType, JSObject.class)) {
          getter = PropertyGetterUtils
              .composeCustomGetter(payloadNameBlock, String.format("{%d}", argCounter));
          composeArgumentGetter(methodBuilder, parameter, getter, false);
        } else if (typeUtils.isSameType(payloadType, List.class)) {
          getter = PropertyGetterUtils.composeArrayGetter(payloadNameBlock, argCounter);
          composeArgumentGetter(methodBuilder, parameter, getter, false);
        } else {
          // don't know type, so check both (JSObject and JSArray)

          // predeclare
          methodBuilder.addStatement(VARIABLE_DECLARATION_NULL, parameter.asType(),
              parameter.getSimpleName());

          final ClassName objectClassName = ClassName.get(JSObject.class);
          methodBuilder.beginControlFlow("if ($L instanceof $T)", payloadName, objectClassName);
          getter = PropertyGetterUtils
              .composeCustomGetter(payloadName, String.format("{%d}", argCounter));
          composeArgumentGetter(methodBuilder, parameter, getter, true);

          final ClassName arrayClassName = ClassName.get(List.class);
          methodBuilder.nextControlFlow("else if ($L instanceof $T)", payloadName, arrayClassName);
          getter = PropertyGetterUtils.composeArrayGetter(payloadName, argCounter);
          composeArgumentGetter(methodBuilder, parameter, getter, true);

          methodBuilder.endControlFlow();
        }
      } else {
        if (getter == null) {
          getter = CodeBlock.of(payloadName);
        }
        composeArgumentGetter(methodBuilder, parameter, getter, false);
      }
      ++argCounter;
    }
  }

  private void composeArgumentGetter(MethodSpec.Builder methodBuilder, VariableElement parameter,
                                     CodeBlock getter, boolean declared) {
    TypeMirror parameterType = parameter.asType();
    String pattern = declared ? VARIABLE_ASSIGNMENT : VARIABLE_DEFINITION;
    if (typeUtils.isSameType(parameterType, Long.class)) {
      getter = CodeBlock.of(NUMBER_TO_LONG_VALUE, TypeName.get(Number.class), getter);
    }
    methodBuilder.addStatement(pattern, parameterType, parameter.getSimpleName(), getter);
  }

  private TypeMirror getClassFromTyped(ExecutableElement method, boolean strictJSType)
      throws ClassCastException {
    TypeMirror payloadType = null;
    if (method.getAnnotation(Typed.class) != null) {
      try {
        method.getAnnotation(Typed.class).value();
      } catch (MirroredTypeException e) {
        // intended ...
        payloadType = e.getTypeMirror();
      }
      if (strictJSType && !typeUtils.isSubtype(payloadType, JSTP_VALUE_TYPE)) {
        throw new ClassCastException(
            "Cannot cast jstp message data to " + String.valueOf(payloadType));
      }
    } else {
      payloadType = typeUtils.getTypeElement(JSTP_VALUE_TYPE.getCanonicalName());
    }
    return typeUtils.erasure(payloadType);
  }

  private String composeCondition(String logic, String condition,
                                  List<? extends VariableElement> parameters,
                                  Class annotationClass) {
    StringBuilder builder = new StringBuilder("if (");
    int i = 0;
    int j = 0;
    for (VariableElement parameter : parameters) {
      ++i;
      if (annotationClass == null || parameter.getAnnotation(annotationClass) != null) {
        ++j;
        builder.append(parameter.getSimpleName())
            .append(condition);
        if (i < parameters.size()) {
          builder.append(logic);
        }
      }
    }
    builder.append(')');
    return j > 0 ? builder.toString() : null;
  }

  private String composeMethodCall(String methodName, List<? extends VariableElement> parameters) {
    StringBuilder builder = new StringBuilder(methodName);
    builder.append('(');
    int i = 0;
    for (VariableElement parameter : parameters) {
      builder.append(parameter.getSimpleName());
      if (++i < parameters.size()) {
        builder.append(", ");
      }
    }
    builder.append(')');
    return builder.toString();
  }

  private void composeCatchClauses(MethodSpec.Builder builder,
                                   List<ExecutableElement> errorHandlers, String handlersName)
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
        ee.getAnnotation(ErrorHandler.class).value();
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
      addCatchClause(builder, me.getKey(), me.getValue(), handlersName);
    }

    builder.endControlFlow();
  }

  private void addCatchClause(MethodSpec.Builder builder, TypeMirror type,
                              List<ExecutableElement> funcs, String handlersName)
      throws ExceptionHandlerInvokeException {
    builder.nextControlFlow("catch ($T e)", ClassName.get(type));

    String callPattern = "$L(e)";
    if (handlersName != null) {
      final String handlersForLoop = String.format("for ($T h : %s)", handlersName);
      builder.beginControlFlow(handlersForLoop, interfaceClassName);
      callPattern = "h.$L(e)";
    }

    for (ExecutableElement func : funcs) {
      if (!checkFirstCast(func.getParameters(), type)) {
        throw new ExceptionHandlerInvokeException(
            func.getSimpleName() + " cannot be called with " + type.toString());
      }
      builder.addStatement(callPattern, func.getSimpleName().toString());
    }

    if (handlersName != null) {
      // finish cycle
      builder.endControlFlow();
    }
  }

  private boolean checkFirstCast(List<? extends VariableElement> parameters, TypeMirror type) {
    return parameters.size() == 1
        && typeUtils.isAssignable(parameters.get(0).asType(), type);
  }
}
