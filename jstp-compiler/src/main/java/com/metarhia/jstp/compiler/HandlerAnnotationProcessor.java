package com.metarhia.jstp.compiler;

import com.google.auto.service.AutoService;
import com.metarhia.jstp.compiler.annotations.handlers.Handler;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
public class HandlerAnnotationProcessor extends AnnotationProcessor {

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
    return Arrays.<Class<? extends Annotation>>asList(Handler.class);
  }

  @Override
  protected List<String> getAvailableCompletions() {
    return Arrays.asList(
        "Array", "Object", "Handler",
        "Mixed", "ExceptionHandler", "NotNull", "Typed", "NoDefaultGet");
  }
}
