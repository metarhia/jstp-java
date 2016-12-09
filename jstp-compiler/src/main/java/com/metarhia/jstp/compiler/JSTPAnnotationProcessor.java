package com.metarhia.jstp.compiler;

import com.google.auto.service.AutoService;
import com.metarhia.jstp.compiler.annotations.JSTPReceiver;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static javax.annotation.processing.Completions.of;

@AutoService(Processor.class)
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
            try {
                if (annotatedElement.getKind() != ElementKind.INTERFACE) {
                    error(annotatedElement, "Only interfaces can be annotated with @%s",
                            JSTPReceiver.class.getSimpleName());
                    return true;
                }

                TypeElement typeElement = (TypeElement) annotatedElement;
                JSTPReceiverAnnotatedInterface jstpReceiver = new JSTPReceiverAnnotatedInterface(
                        typeElement, elementUtils, typeUtils);

                jstpReceiver.generateCode(filer);
            } catch (PropertyFormatException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            } catch (ExceptionHandlerInvokeException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            } catch (ClassCastException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Cannot write class to file: " + e.getMessage());
            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Unexpected error: " + e.toString());
            }
        }
        return true;
    }


    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotationMirror,
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
