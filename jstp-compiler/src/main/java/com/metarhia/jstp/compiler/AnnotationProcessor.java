package com.metarhia.jstp.compiler;

import static javax.annotation.processing.Completions.of;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public abstract class AnnotationProcessor extends AbstractProcessor {

  protected Types typeUtils;
  protected Elements elementUtils;
  protected Filer filer;
  protected Messager messager;

  public AnnotationProcessor() {
  }

  protected abstract void handleAnnotation(Element annotatedElement, Class<?> annotation)
      throws ExceptionHandlerInvokeException, IOException, PropertyFormatException;

  protected abstract List<Class<? extends Annotation>> getSupportedAnnotations();

  protected abstract List<String> getAvailableCompletions();

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
    for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
      for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(annotation)) {
        processInterfaceAnnotation(annotatedElement, annotation);
      }
    }
    return true;
  }

  public void processInterfaceAnnotation(Element annotatedElement,
                                         Class<?> annotation) {
    try {
      handleAnnotation(annotatedElement, annotation);
    } catch (RuntimeException | IOException e) {
      errorWithStacktrace(e, "Cannot write class to file: " + e.getMessage());
    } catch (Exception e) {
      errorWithStacktrace(e, "Unexpected error: " + e.toString());
    }
  }

  @Override
  public Iterable<? extends Completion> getCompletions(Element element,
                                                       AnnotationMirror annotationMirror,
                                                       ExecutableElement executableElement,
                                                       String s) {
    final Set<String> completions = new HashSet<>(getAvailableCompletions());
    final ArrayList<Completion> matching = new ArrayList<>();
    for (String completion : completions) {
      if (completion.startsWith(s)) {
        matching.add(of(completion));
      }
    }
    return matching;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    final HashSet<String> annotations = new HashSet<>();
    for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
      annotations.add(annotation.getCanonicalName());
    }
    return annotations;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_7;
  }

  protected void errorWithStacktrace(Exception e, String msg, Object... args) {
    StringWriter out = new StringWriter();
    e.printStackTrace(new PrintWriter(out));

    String format = String.format(msg, args);
    format = format + " \n stacktrace: " + out.toString();
    messager.printMessage(
        Diagnostic.Kind.ERROR,
        format);
  }

  protected void error(Element e, String msg, Object... args) {
    messager.printMessage(
        Diagnostic.Kind.ERROR,
        String.format(msg, args),
        e);
  }
}
