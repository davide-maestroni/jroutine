/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmd.jrt.wrapper.processor;

import com.bmd.jrt.annotation.Async;
import com.bmd.jrt.annotation.AsyncOverride;
import com.bmd.jrt.annotation.DefaultLog;
import com.bmd.jrt.annotation.DefaultRunner;
import com.bmd.jrt.builder.RoutineBuilder.ChannelDataOrder;
import com.bmd.jrt.builder.RoutineBuilder.SyncRunnerType;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.log.Log.LogLevel;
import com.bmd.jrt.wrapper.annotation.WrapAsync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * Processor implementation.
 * <p/>
 * Created by davide on 11/3/14.
 */
@SupportedAnnotationTypes("com.bmd.jrt.wrapper.annotation.WrapAsync")
@SupportedSourceVersion(SourceVersion.RELEASE_5)
public class RoutineProcessor extends AbstractProcessor {

    private static final boolean DEBUG = false;

    private static final HashMap<String, String> mBoxingMap = new HashMap<String, String>();

    static {

        final HashMap<String, String> boxingMap = mBoxingMap;

        boxingMap.put("boolean", "Boolean");
        boxingMap.put("byte", "Byte");
        boxingMap.put("char", "Character");
        boxingMap.put("double", "Double");
        boxingMap.put("float", "Float");
        boxingMap.put("int", "Integer");
        boxingMap.put("long", "Long");
        boxingMap.put("short", "Short");
        boxingMap.put("void", "Void");
    }

    private boolean mDisabled;

    private String mFooter;

    private String mHeader;

    private String mMethodAsync;

    private String mMethodResult;

    private String mMethodVoid;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {

        super.init(processingEnv);

        final byte[] buffer = new byte[2048];

        try {

            mHeader = parseTemplate("/templates/header.txt", buffer);
            mMethodAsync = parseTemplate("/templates/method_async.txt", buffer);
            mMethodResult = parseTemplate("/templates/method_result.txt", buffer);
            mMethodVoid = parseTemplate("/templates/method_void.txt", buffer);
            mFooter = parseTemplate("/templates/footer.txt", buffer);

        } catch (final IOException ex) {

            mDisabled = true;
        }
    }

    @Override
    public boolean process(final Set<? extends TypeElement> typeElements,
            final RoundEnvironment roundEnvironment) {

        if (roundEnvironment.processingOver() || mDisabled) {

            return false;
        }

        final TypeElement annotationElement = getTypeFromName(WrapAsync.class.getCanonicalName());
        final TypeMirror annotationType = annotationElement.asType();

        for (final Element element : ElementFilter.typesIn(
                roundEnvironment.getElementsAnnotatedWith(WrapAsync.class))) {

            final TypeElement classElement = (TypeElement) element;
            final List<ExecutableElement> methodElements =
                    ElementFilter.methodsIn(element.getEnclosedElements());

            for (final TypeMirror typeMirror : classElement.getInterfaces()) {

                final Element superElement = processingEnv.getTypeUtils().asElement(typeMirror);

                if (superElement != null) {

                    mergeParentMethods(methodElements,
                                       ElementFilter.methodsIn(superElement.getEnclosedElements()));
                }
            }

            final List<?> elementList = (List<?>) getElementValue(element, annotationType, "value");

            for (final Object targetElement : elementList) {

                createWrapper(classElement, getTypeFromName(targetElement.toString()),
                              methodElements);
            }
        }

        return false;
    }

    private CharSequence boxingClassName(final String s) {

        final String name = mBoxingMap.get(s);

        return (name != null) ? name : s;
    }

    private String buildGenericTypes(final TypeElement element) {

        final StringBuilder builder = new StringBuilder("<");

        for (final TypeParameterElement typeParameterElement : element.getTypeParameters()) {

            if (builder.length() > 1) {

                builder.append(", ");
            }

            builder.append(typeParameterElement.asType());
        }

        return builder.append(">").toString();
    }

    private String buildInputParams(final ExecutableElement methodElement) {

        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : methodElement.getParameters()) {

            builder.append(".pass(").append(variableElement).append(")");
        }

        return builder.toString();
    }

    private String buildParamTypes(final ExecutableElement methodElement) {

        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : methodElement.getParameters()) {

            if (builder.length() > 0) {

                builder.append(", ");
            }

            builder.append("final ")
                   .append(variableElement.asType())
                   .append(" ")
                   .append(variableElement);
        }

        return builder.toString();
    }

    private String buildParamValues(final ExecutableElement targetMethodElement) {

        int count = 0;
        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : targetMethodElement.getParameters()) {

            if (builder.length() > 0) {

                builder.append(", ");
            }

            builder.append("(")
                   .append(boxingClassName(variableElement.asType().toString()))
                   .append(") objects.get(")
                   .append(count++)
                   .append(")");
        }

        return builder.toString();
    }

    private String buildRoutineFieldsInit(final int size) {

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= size; i++) {

            builder.append("mRoutine")
                   .append(i)
                   .append(" = ")
                   .append("initRoutine")
                   .append(i)
                   .append("();");
        }

        return builder.toString();
    }

    private String buildRoutineOptions(final ExecutableElement methodElement,
            final ExecutableElement targetMethodElement) {

        final Async annotation = methodElement.getAnnotation(Async.class);
        final Async targetAnnotation = targetMethodElement.getAnnotation(Async.class);

        final TypeElement annotationElement = getTypeFromName(Async.class.getCanonicalName());
        final TypeMirror annotationType = annotationElement.asType();

        SyncRunnerType runnerType = SyncRunnerType.DEFAULT;
        int maxRunning = RoutineConfiguration.NOT_SET;
        int maxRetained = RoutineConfiguration.NOT_SET;
        LogLevel logLevel = LogLevel.DEFAULT;
        TypeElement logElement = null;
        TypeElement runnerElement = null;

        if (annotation != null) {

            runnerType = annotation.runnerType();
            maxRunning = annotation.maxRunning();
            maxRetained = annotation.maxRetained();
            logLevel = annotation.logLevel();

            final Object log = getElementValue(methodElement, annotationType, "log");

            if (log != null) {

                logElement = getTypeFromName(log.toString());
            }

            final Object runner = getElementValue(methodElement, annotationType, "runnerClass");

            if (runner != null) {

                runnerElement = getTypeFromName(runner.toString());
            }
        }

        if (targetAnnotation != null) {

            //TODO
            //            if (runnerType == null) {
            //
            //                runnerType = targetAnnotation.sequential();
            //            }

            if (maxRunning == RoutineConfiguration.NOT_SET) {

                maxRunning = targetAnnotation.maxRunning();
            }

            if (maxRetained == RoutineConfiguration.NOT_SET) {

                maxRetained = targetAnnotation.maxRetained();
            }

            if (logLevel == LogLevel.DEFAULT) {

                logLevel = targetAnnotation.logLevel();
            }

            if ((logElement == null) || logElement.equals(
                    getTypeFromName(DefaultLog.class.getCanonicalName()))) {

                final Object log = getElementValue(targetMethodElement, annotationType, "log");

                if (log != null) {

                    logElement = getTypeFromName(log.toString());
                }
            }

            if ((runnerElement == null) || runnerElement.equals(
                    getTypeFromName(DefaultRunner.class.getCanonicalName()))) {

                final Object runner =
                        getElementValue(targetMethodElement, annotationType, "runnerClass");

                if (runner != null) {

                    runnerElement = getTypeFromName(runner.toString());
                }
            }
        }

        final StringBuilder builder = new StringBuilder();

        if (runnerType != null) {

            builder.append(".syncRunner(")
                   .append(SyncRunnerType.class.getCanonicalName())
                   .append(".")
                   .append(runnerType)
                   .append(")");
        }

        if (maxRunning != RoutineConfiguration.NOT_SET) {

            builder.append(".maxRunning(").append(maxRunning).append(")");
        }

        if (maxRetained != RoutineConfiguration.NOT_SET) {

            builder.append(".maxRetained(").append(maxRetained).append(")");
        }

        if (logLevel != null) {

            builder.append(".logLevel(")
                   .append(LogLevel.class.getCanonicalName())
                   .append(".")
                   .append(logLevel)
                   .append(")");
        }

        if ((logElement != null) && !logElement.equals(
                getTypeFromName(DefaultLog.class.getCanonicalName()))) {

            builder.append(".loggedWith(new ").append(logElement).append("())");
        }

        if ((runnerElement != null) && !runnerElement.equals(
                getTypeFromName(DefaultRunner.class.getCanonicalName()))) {

            builder.append(".runBy(new ").append(runnerElement).append("())");
        }

        final TypeElement overrideAnnotationElement =
                getTypeFromName(AsyncOverride.class.getCanonicalName());
        final TypeMirror overrideAnnotationType = overrideAnnotationElement.asType();

        final List<?> values =
                (List<?>) getElementValue(methodElement, overrideAnnotationType, "value");

        if ((values != null) && !values.isEmpty()) {

            builder.append(".inputOrder(")
                   .append(ChannelDataOrder.class.getCanonicalName())
                   .append(".")
                   .append(ChannelDataOrder.INSERTION)
                   .append(")");
        }

        return builder.toString();
    }

    private void createWrapper(final TypeElement element, final TypeElement targetElement,
            final List<ExecutableElement> methodElements) {

        Writer writer = null;

        try {

            final CharSequence packageName =
                    ((PackageElement) targetElement.getEnclosingElement()).getQualifiedName();
            final CharSequence simpleName = targetElement.getSimpleName();
            final Name qualifiedName = targetElement.getQualifiedName();
            final Filer filer = processingEnv.getFiler();

            //noinspection PointlessBooleanExpression,ConstantConditions
            if (!DEBUG) {

                final JavaFileObject sourceFile = filer.createSourceFile(qualifiedName + "Async");
                writer = sourceFile.openWriter();

            } else {

                writer = new StringWriter();
            }

            String header = mHeader.replace("${packageName}", packageName);
            header = header.replace("${className}", simpleName);
            header = header.replace("${genericTypes}", buildGenericTypes(element));
            header = header.replace("${classFullName}", targetElement.asType().toString());
            header = header.replace("${interfaceFullName}", element.asType().toString());
            header = header.replace("${routineFieldsInit}",
                                    buildRoutineFieldsInit(methodElements.size()));

            writer.append(header);

            int count = 0;

            for (final ExecutableElement methodElement : methodElements) {

                final ExecutableElement targetMethod =
                        findMatchingMethod(methodElement, targetElement);
                final String returnTypeName = targetMethod.getReturnType().toString();

                String method;

                final AsyncOverride overrideAnnotation =
                        methodElement.getAnnotation(AsyncOverride.class);

                if ((overrideAnnotation != null) && overrideAnnotation.result()) {

                    method = mMethodAsync.replace("${resultClassName}",
                                                  boxingClassName(returnTypeName));

                } else if ("void".equalsIgnoreCase(returnTypeName)) {

                    method = mMethodVoid;

                } else {

                    method = mMethodResult.replace("${resultClassName}",
                                                   boxingClassName(returnTypeName));
                    method = method.replace("${resultType}", returnTypeName);
                }

                method = method.replace("${methodCount}", Integer.toString(++count));
                method = method.replace("${methodName}", methodElement.getSimpleName());
                method = method.replace("${targetMethodName}", targetMethod.getSimpleName());
                method = method.replace("${paramTypes}", buildParamTypes(methodElement));
                method = method.replace("${paramValues}", buildParamValues(targetMethod));
                method = method.replace("${inputParams}", buildInputParams(methodElement));
                method = method.replace("${routineBuilderOptions}",
                                        buildRoutineOptions(methodElement, targetMethod));

                final Async annotation = methodElement.getAnnotation(Async.class);
                final Async targetAnnotation = targetMethod.getAnnotation(Async.class);

                String lockId = Async.DEFAULT_ID;

                if (annotation != null) {

                    lockId = annotation.lockId();
                }

                if ((targetAnnotation != null) && (lockId.equals(Async.DEFAULT_ID))) {

                    lockId = targetAnnotation.lockId();
                }

                method = method.replace("${lockId}", lockId);

                writer.append(method);
            }

            writer.append(mFooter);

        } catch (final IOException ignored) {

        } finally {

            if (writer != null) {

                try {

                    writer.close();

                } catch (final IOException ignored) {

                }
            }
        }

        if (DEBUG) {

            System.out.println(writer.toString());
        }
    }

    private ExecutableElement findMatchingMethod(final ExecutableElement methodElement,
            final TypeElement targetElement) {

        ExecutableElement targetMethod = null;
        final Async asyncAnnotation = methodElement.getAnnotation(Async.class);
        final AsyncOverride overrideAnnotation = methodElement.getAnnotation(AsyncOverride.class);

        if (asyncAnnotation != null) {

            final String tag = asyncAnnotation.value();

            if ((tag != null) && (tag.length() > 0)) {

                for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                        targetElement.getEnclosedElements())) {

                    final Async targetAsyncAnnotation =
                            targetMethodElement.getAnnotation(Async.class);

                    if ((targetAsyncAnnotation != null) && tag.equals(
                            targetAsyncAnnotation.value())) {

                        targetMethod = targetMethodElement;

                        break;
                    }
                }

                if (targetMethod == null) {

                    final TypeElement annotationElement =
                            getTypeFromName(AsyncOverride.class.getCanonicalName());
                    final TypeMirror annotationType = annotationElement.asType();

                    final List<?> values =
                            (List<?>) getElementValue(methodElement, annotationType, "value");

                    if ((overrideAnnotation != null) && (values != null)) {

                        for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                                targetElement.getEnclosedElements())) {

                            if (!tag.equals(targetMethodElement.getSimpleName().toString())) {

                                continue;
                            }

                            final List<? extends VariableElement> classTypeParameters =
                                    targetMethodElement.getParameters();

                            final int length = values.size();

                            if (length == classTypeParameters.size()) {

                                boolean matches = true;

                                for (int i = 0; i < length; i++) {

                                    if (!normalizeTypeName(values.get(i).toString()).equals(
                                            classTypeParameters.get(i).asType().toString())) {

                                        matches = false;

                                        break;
                                    }
                                }

                                if (matches) {

                                    targetMethod = targetMethodElement;

                                    break;
                                }
                            }
                        }

                    } else {

                        for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                                targetElement.getEnclosedElements())) {

                            if (tag.equals(targetMethodElement.getSimpleName().toString())
                                    && haveSameParameters(methodElement, targetMethodElement)) {

                                targetMethod = targetMethodElement;

                                break;
                            }
                        }
                    }
                }
            }
        }

        if (targetMethod == null) {

            final Name methodName = methodElement.getSimpleName();

            final TypeElement annotationElement =
                    getTypeFromName(AsyncOverride.class.getCanonicalName());
            final TypeMirror annotationType = annotationElement.asType();

            final List<?> values =
                    (List<?>) getElementValue(methodElement, annotationType, "value");

            if ((overrideAnnotation != null) && (values != null)) {

                for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                        targetElement.getEnclosedElements())) {

                    if (!methodName.equals(targetMethodElement.getSimpleName())) {

                        continue;
                    }

                    final List<? extends VariableElement> classTypeParameters =
                            targetMethodElement.getParameters();

                    final int length = values.size();

                    if (length == classTypeParameters.size()) {

                        boolean matches = true;

                        for (int i = 0; i < length; i++) {

                            if (!normalizeTypeName(values.get(i).toString()).equals(
                                    classTypeParameters.get(i).asType().toString())) {

                                matches = false;

                                break;
                            }
                        }

                        if (matches) {

                            targetMethod = targetMethodElement;

                            break;
                        }
                    }
                }

            } else {

                System.out.println(">>>> method: " + methodElement);

                for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                        targetElement.getEnclosedElements())) {

                    System.out.println(">>>> target: " + targetMethodElement);

                    if (methodName.equals(targetMethodElement.getSimpleName())
                            && haveSameParameters(methodElement, targetMethodElement)) {

                        targetMethod = targetMethodElement;

                        break;
                    }
                }
            }
        }

        if (targetMethod == null) {

            throw new IllegalArgumentException("cannot find matching method in target class");
        }

        return targetMethod;
    }

    private Object getElementValue(final Element element, final TypeMirror annotationType,
            final String valueName) {

        AnnotationValue value = null;

        for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {

            if (mirror.getAnnotationType().equals(annotationType)) {

                final Set<? extends Entry<? extends ExecutableElement, ? extends AnnotationValue>>
                        entrySet = mirror.getElementValues().entrySet();

                for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                        entrySet) {

                    if (valueName.equals(entry.getKey().getSimpleName().toString())) {

                        value = entry.getValue();

                        break;
                    }
                }
            }
        }

        return (value != null) ? value.getValue() : null;
    }

    private TypeElement getTypeFromName(final String typeName) {

        return processingEnv.getElementUtils().getTypeElement(normalizeTypeName(typeName));
    }

    private boolean haveSameParameters(final ExecutableElement firstMethodElement,
            final ExecutableElement secondMethodElement) {

        final List<? extends VariableElement> firstTypeParameters =
                firstMethodElement.getParameters();
        final List<? extends VariableElement> secondTypeParameters =
                secondMethodElement.getParameters();

        final int length = firstTypeParameters.size();

        if (length != secondTypeParameters.size()) {

            return false;
        }

        for (int i = 0; i < length; i++) {

            final TypeMirror firstType = firstTypeParameters.get(i).asType();
            final TypeMirror secondType = secondTypeParameters.get(i).asType();

            if (!firstType.toString().equals(secondType.toString())) {

                return false;
            }
        }

        return true;
    }

    private void mergeParentMethods(final List<ExecutableElement> methods,
            final List<ExecutableElement> parentMethods) {

        for (final ExecutableElement parentMethod : parentMethods) {

            boolean isOverride = false;

            for (final ExecutableElement method : methods) {

                if (parentMethod.getSimpleName().equals(method.getSimpleName())
                        && parentMethod.getReturnType().equals(method.getReturnType())) {

                    if (haveSameParameters(parentMethod, method)) {

                        isOverride = true;

                        break;
                    }
                }
            }

            if (!isOverride) {

                methods.add(parentMethod);
            }
        }
    }

    private String normalizeTypeName(final String typeName) {

        if (typeName.endsWith(".class")) {

            return typeName.substring(0, typeName.length() - ".class".length());
        }

        return typeName;
    }

    private String parseTemplate(final String path, final byte[] buffer) throws IOException {

        final InputStream resourceStream = RoutineProcessor.class.getResourceAsStream(path);

        try {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;

            while ((length = resourceStream.read(buffer)) > 0) {

                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();

            return outputStream.toString("UTF-8");

        } catch (final IOException ex) {

            processingEnv.getMessager()
                         .printMessage(Kind.ERROR,
                                       "IOException while reading " + path + " template");

            throw ex;

        } finally {

            try {

                resourceStream.close();

            } catch (final IOException ignored) {

            }
        }
    }
}
