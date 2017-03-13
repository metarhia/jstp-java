package com.metarhia.jstp.compiler;

import static javax.annotation.processing.Completions.of;

import com.metarhia.jstp.compiler.annotations.JSTPHandler;
import com.metarhia.jstp.compiler.annotations.JSTPReceiver;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public class JSTPAnnotationProcessor extends AbstractProcessor {

  private Types typeUtils;
  private Elements elementUtils;
  private Filer filer;
  private Messager messager;

  public JSTPAnnotationProcessor() {
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(JSTPReceiver.class)) {
      processInterfaceAnnotation(annotatedElement, JSTPReceiver.class);
    }
    for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(JSTPHandler.class)) {
      processInterfaceAnnotation(annotatedElement, JSTPHandler.class);
    }
    return true;
  }

  public void processInterfaceAnnotation(Element annotatedElement,
      Class<?> annotation) {
    try {
      if (annotatedElement.getKind() != ElementKind.INTERFACE) {
        error(annotatedElement, "Only interfaces can be annotated with @%s",
            annotation.getSimpleName());
        return;
      }

      TypeElement typeElement = (TypeElement) annotatedElement;
      JSTPAnnotatedInterface jstpReceiver = new JSTPAnnotatedInterface(annotation,
          typeElement, elementUtils, typeUtils);

      jstpReceiver.generateCode(filer);
    } catch (PropertyFormatException e) {
      messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    } catch (ExceptionHandlerInvokeException e) {
      messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    } catch (ClassCastException e) {
      messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    } catch (IOException e) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, "Cannot write class to file: " + e.getMessage());
    } catch (Exception e) {
      messager.printMessage(
          Diagnostic.Kind.ERROR, "Unexpected error: " + e.toString());
    }
  }


  @Override
  public Iterable<? extends Completion> getCompletions(Element element,
      AnnotationMirror annotationMirror,
      ExecutableElement executableElement, String s) {
    if (s.startsWith("N")) {
      return Arrays.asList(of("Number"));
    } else if (s.startsWith("I")) {
      return Arrays.asList(of("Indexed"));
    } else if (s.startsWith("E")) {
      return Arrays.asList(of("ErrorHandler"));
    } else {
      return Arrays.asList(of("JSTPReceiver"));
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList(
        JSTPReceiver.class.getCanonicalName()
    ));
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_7;
  }

  private void error(Element e, String msg, Object... args) {
    messager.printMessage(
        Diagnostic.Kind.ERROR,
        String.format(msg, args),
        e);
  }
}
