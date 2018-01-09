package com.metarhia.jstp.compiler;

import com.google.auto.service.AutoService;
import com.metarhia.jstp.compiler.annotations.proxy.Proxy;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
public class ProxyAnnotationProcessor extends AnnotationProcessor {

  public ProxyAnnotationProcessor() {
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
    ProxyAnnotatedInterface jstpReceiver = new ProxyAnnotatedInterface(
        typeElement, elementUtils, typeUtils, messager);

    jstpReceiver.generateCode(filer);
  }

  @Override
  protected List<Class<? extends Annotation>> getSupportedAnnotations() {
    return Collections.<Class<? extends Annotation>>singletonList(Proxy.class);
  }

  @Override
  protected List<String> getAvailableCompletions() {
    return Arrays.asList("Proxy", "Call", "Event");
  }
}
