package com.metarhia.jstp.compiler;

import com.metarhia.jstp.compiler.annotations.handlers.JSTPHandler;
import com.metarhia.jstp.compiler.annotations.handlers.JSTPReceiver;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

public class HandlerAnnotationProcessor extends JSTPAnnotationProcessor {

  public HandlerAnnotationProcessor() {
  }

  @Override
  protected void handleAnnotation(Element annotatedElement, Class<?> annotation)
      throws ExceptionHandlerInvokeException, IOException, PropertyFormatException {
    if (annotatedElement.getKind() != ElementKind.INTERFACE) {
      error(annotatedElement, "Only interfaces can be annotated with @%s",
          annotation.getSimpleName());
      return;
    }

    TypeElement typeElement = (TypeElement) annotatedElement;
    HandlerAnnotatedInterface jstpReceiver = new HandlerAnnotatedInterface(annotation,
        typeElement, elementUtils, typeUtils);

    jstpReceiver.generateCode(filer);
  }

  @Override
  protected List<Class<? extends Annotation>> getSupportedAnnotations() {
    return Arrays.asList(JSTPReceiver.class, JSTPHandler.class);
  }

  @Override
  protected List<String> getAvailableCompletions() {
    return Arrays.asList(
        "Array", "Object", "JSTPReceiver", "JSTPHandler",
        "Mixed", "ErrorHandler", "NotNull", "Typed", "NoDefaultGet");
  }
}
