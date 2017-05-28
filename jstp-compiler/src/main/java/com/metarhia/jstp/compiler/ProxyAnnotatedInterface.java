package com.metarhia.jstp.compiler;

import com.metarhia.jstp.compiler.annotations.proxy.Call;
import com.metarhia.jstp.compiler.annotations.proxy.Event;
import com.metarhia.jstp.compiler.annotations.proxy.Proxy;
import com.metarhia.jstp.connection.Connection;
import com.metarhia.jstp.core.Handlers.ManualHandler;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

/**
 * Created by lundibundi on 5/25/17.
 */
public class ProxyAnnotatedInterface {

  private static final String PREFIX = "JSTP";

  private static final String METHOD_CALL_PATTERN = "$1L($2L)";
  private static final String METHOD_FIELD_CALL_PATTERN = "$1L.$2L($3L)";
  private static final String METHOD_STATIC_CALL_PATTERN = "$1T.$2L($3L)";

  private static final String VARIABLE_DEFINITION = "$1T $2L = $3L";
  private static final String VARIABLE_DECLARATION = "$1T $2L";
  private static final String VARIABLE_DECLARATION_NULL = VARIABLE_DECLARATION + " = null";
  private static final String VARIABLE_ASSIGNMENT = "$1L = $2L";
  private static final String VARIABLE_CREATION = "$1L = new $2T($3L)";

  private static final Class<?> CONNECTION_PARAMETER_TYPE = Connection.class;
  private static final ClassName CONNECTION_PARAMETER_CLASSNAME
      = ClassName.get(Connection.class);
  private static final String CONNECTION_FIELD_NAME = "connection";

  private static final String SINGLETON_INSTANCE_NAME = "instance";

  private TypeElement annotatedInterface;
  private final Messager messager;
  private String remoteInterfaceName;
  private boolean singletonClass;

  private TypeSpec.Builder classBuilder;
  private TypeUtils typeUtils;
  private TypeName interfaceClassName;
  private TypeName interfaceTypeName;

  public ProxyAnnotatedInterface(TypeElement typeElement,
                                 Elements elements, Types types,
                                 Messager messager) {
    this.messager = messager;
    annotatedInterface = typeElement;
    typeUtils = new TypeUtils(types, elements);

    interfaceClassName = ClassName.get(annotatedInterface.asType());
    interfaceTypeName = TypeName.get(annotatedInterface.asType());

    final Proxy annotation = annotatedInterface.getAnnotation(Proxy.class);
    singletonClass = annotation.singleton();
    remoteInterfaceName = annotation.interfaceName();
  }

  public void generateCode(Filer filer) throws
      ExceptionHandlerInvokeException,
      ClassCastException,
      IOException, PropertyFormatException {

    String currentClassName = PREFIX + annotatedInterface.getSimpleName();
    final ClassName currentClass = ClassName.bestGuess(currentClassName);

    classBuilder = TypeSpec.classBuilder(currentClassName)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(interfaceTypeName);

    classBuilder.addField(CONNECTION_PARAMETER_TYPE, CONNECTION_FIELD_NAME, Modifier.PRIVATE);

    final Builder mainConstructorBuilder = MethodSpec.constructorBuilder()
        .addParameter(CONNECTION_PARAMETER_CLASSNAME, CONNECTION_FIELD_NAME)
        .addStatement(VARIABLE_ASSIGNMENT, "this." + CONNECTION_FIELD_NAME,
            CONNECTION_FIELD_NAME);

    if (singletonClass) {
      mainConstructorBuilder.addModifiers(Modifier.PRIVATE);

      classBuilder.addField(currentClass, SINGLETON_INSTANCE_NAME,
          Modifier.STATIC, Modifier.PRIVATE);

      final MethodSpec singletonGetter = MethodSpec.methodBuilder("get")
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(currentClass)
          .addParameter(CONNECTION_PARAMETER_CLASSNAME, CONNECTION_FIELD_NAME)
          .beginControlFlow("if ($1L == null)", SINGLETON_INSTANCE_NAME)
          .addStatement(VARIABLE_CREATION, SINGLETON_INSTANCE_NAME,
              currentClass, CONNECTION_FIELD_NAME)
          .endControlFlow()
          .addStatement("return $1L", SINGLETON_INSTANCE_NAME)
          .build();
      classBuilder.addMethod(singletonGetter);
    } else {
      mainConstructorBuilder.addModifiers(Modifier.PUBLIC);
    }

    classBuilder.addMethod(mainConstructorBuilder.build());

    // generate methods to enclose the ones in the interface
    for (Element e : annotatedInterface.getEnclosedElements()) {
      if (e.getKind() == ElementKind.METHOD) {
        ExecutableElement method = (ExecutableElement) e;
        MethodSpec actionMethod = createActionHandler(method);
        if (actionMethod != null) {
          classBuilder.addMethod(actionMethod);
        }
      }
    }

    addDelegateMethod(CONNECTION_FIELD_NAME, Connection.class, "setCallHandler",
        Arrays.asList("interfaceName", "methodName", "handler"),
        String.class, String.class, ManualHandler.class);

    addDelegateMethod(CONNECTION_FIELD_NAME, Connection.class, "removeCallHandler",
        Arrays.asList("interfaceName", "methodName"),
        String.class, String.class);

    addDelegateMethod(CONNECTION_FIELD_NAME, Connection.class, "addEventHandler",
        Arrays.asList("interfaceName", "eventName", "handler"),
        String.class, String.class, ManualHandler.class);

    addDelegateMethod(CONNECTION_FIELD_NAME, Connection.class, "removeEventHandler",
        Arrays.asList("interfaceName", "eventName", "handler"),
        String.class, String.class, ManualHandler.class);

    // save to file
    JavaFile javaFile = JavaFile.builder(
        typeUtils.getElements()
            .getPackageOf(annotatedInterface)
            .getQualifiedName()
            .toString(),
        classBuilder.build())
        .indent("    ")
        .build();
    javaFile.writeTo(filer);
  }

  private void addDelegateMethod(String caller, Class<?> clazz, String name,
                                 List<String> parameterNames, Class<?>... parameterTypes) {
    try {
      final Method method = clazz.getDeclaredMethod(name, parameterTypes);
      final MethodSpec methodDelegate = createDelegateMethod(caller, method, parameterNames);
      classBuilder.addMethod(methodDelegate);
    } catch (NoSuchMethodException e) {
      messager.printMessage(Kind.WARNING,
          "Cannot get " + name + " method from " + clazz.getCanonicalName()
              + ". Skipping delegate creation");
    }
  }

  private MethodSpec createDelegateMethod(String caller, Method method, List<String> paramNames) {
    MethodSpec.Builder delegateBuilder = MethodSpec.methodBuilder(method.getName())
        .addModifiers(Modifier.PUBLIC)
        .returns(method.getReturnType());
    final Class<?>[] parameterTypes = method.getParameterTypes();
    for (int i = 0; i < parameterTypes.length; i++) {
      delegateBuilder.addParameter(parameterTypes[i], paramNames.get(i));
    }
    final CodeBlock originCall = composeMethodCall(caller, method.getName(), paramNames);
    delegateBuilder.addStatement("$1L", originCall);
    return delegateBuilder.build();
  }

  private MethodSpec createActionHandler(ExecutableElement method) {
    if (method.getAnnotation(Call.class) != null) {
      String[] names = getInterfaceAndMethod(method, remoteInterfaceName, Call.class, "value");
      return createCallMethod(names[0], names[1], method);
    } else if (method.getAnnotation(Event.class) != null) {
      String[] names = getInterfaceAndMethod(method, remoteInterfaceName, Event.class, "value");
      return createEventMethod(names[0], names[1], method);
    }
    return null;
  }

  private MethodSpec createCallMethod(String remoteInterface, String remoteMethod,
                                      ExecutableElement method) {
    MethodSpec.Builder callMethodBuilder = MethodSpec.overriding(method);

    String callback = "null";
    final List<? extends VariableElement> parameters = new ArrayList<>(method.getParameters());
    if (parameters.size() > 0) {
      final VariableElement lastParameter = parameters.get(parameters.size() - 1);
      if (typeUtils.isSubtype(lastParameter.asType(), ManualHandler.class)) {
        final VariableElement callbackParameter = parameters.remove(parameters.size() - 1);
        callback = callbackParameter.getSimpleName().toString();
      }
    }
    final CodeBlock argsComposedCall = composeParamMethodCall(
        "java.util.Arrays", "asList", parameters);
    final String argsName = "args";
    callMethodBuilder.addStatement(VARIABLE_DEFINITION, TypeName.get(List.class),
        argsName, argsComposedCall);

    callMethodBuilder.addStatement("$1L.$2L(\"$3L\", \"$4L\", $5L, $6L)",
        CONNECTION_FIELD_NAME, "call", remoteInterface, remoteMethod, argsName, callback);

    return callMethodBuilder.build();
  }

  private MethodSpec createEventMethod(String remoteInterface, String eventName,
                                       ExecutableElement method) {
    MethodSpec.Builder eventMethodBuilder = MethodSpec.overriding(method);

    final List<? extends VariableElement> parameters = new ArrayList<>(method.getParameters());
    final CodeBlock argsComposedCall = composeParamMethodCall(
        "java.util.Arrays", "asList", parameters);
    final String argsName = "args";
    eventMethodBuilder.addStatement(VARIABLE_DEFINITION, TypeName.get(List.class),
        argsName, argsComposedCall);

    eventMethodBuilder.addStatement("$1L.$2L(\"$3L\", \"$4L\", $5L)",
        CONNECTION_FIELD_NAME, "event", remoteInterface, eventName, argsName);

    return eventMethodBuilder.build();
  }

  private CodeBlock composeParamMethodCall(String methodName,
                                           List<? extends VariableElement> parameters) {
    return composeParamMethodCall(null, methodName, parameters);
  }

  private CodeBlock composeParamMethodCall(String name, String methodName,
                                           List<? extends VariableElement> parameters) {
    List<String> names = new ArrayList<>();
    for (VariableElement parameter : parameters) {
      names.add(parameter.getSimpleName().toString());
    }
    return composeMethodCall(name, methodName, names);
  }

  private CodeBlock composeMethodCall(String name, String methodName,
                                      Collection<String> parameters) {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    for (String param : parameters) {
      builder.append(param);
      if (++i < parameters.size()) {
        builder.append(", ");
      }
    }

    if (name == null) {
      return CodeBlock.of(METHOD_CALL_PATTERN, methodName, builder.toString());
    } else if (name.contains(".")) {
      int lastDot = name.lastIndexOf(".");
      String packageName = name.substring(0, lastDot);
      String className = name.substring(lastDot + 1);
      final ClassName clazz = ClassName.get(packageName, className);
      return CodeBlock.of(METHOD_STATIC_CALL_PATTERN, clazz, methodName, builder.toString());
    } else {
      return CodeBlock.of(METHOD_FIELD_CALL_PATTERN, name, methodName, builder.toString());
    }
  }

  private static String[] getInterfaceAndMethod(ExecutableElement element,
                                                String defaultInterface,
                                                Class<? extends Annotation> annotation,
                                                String method) {
    try {
      final Method getter = annotation.getDeclaredMethod(method);
      String[] names = (String[]) getter.invoke(element.getAnnotation(annotation));
      if (names.length == 0 || names[0].isEmpty()) {
        if (defaultInterface.isEmpty()) {
          throw new RuntimeException("Interface name is empty");
        }
        return new String[]{defaultInterface, element.getSimpleName().toString()};
      } else if (names.length == 1) {
        if (defaultInterface.isEmpty()) {
          throw new RuntimeException("Interface name is empty");
        }
        // by default if size is 1 arg is recognized as method name
        return new String[]{defaultInterface, names[0]};
      }
      return names;
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(
          "Cannot get '" + method + "'  property of " + annotation.getCanonicalName(), e);
    }
  }
}
