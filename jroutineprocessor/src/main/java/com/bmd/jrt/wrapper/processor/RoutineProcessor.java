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
import com.bmd.jrt.annotation.AsyncParameters;
import com.bmd.jrt.annotation.AsyncResult;
import com.bmd.jrt.annotation.DefaultLog;
import com.bmd.jrt.annotation.DefaultRunner;
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

        final InputStream headerStream =
                RoutineProcessor.class.getResourceAsStream("/templates/header.txt");

        try {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;

            while ((length = headerStream.read(buffer)) > 0) {

                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();

            mHeader = outputStream.toString("UTF-8");

        } catch (final IOException ex) {

            processingEnv.getMessager()
                         .printMessage(Kind.ERROR, "IOException while reading header template");

            mDisabled = true;

            return;

        } finally {

            try {

                headerStream.close();

            } catch (final IOException ignored) {

            }
        }

        final InputStream methodAsyncStream =
                RoutineProcessor.class.getResourceAsStream("/templates/method_async.txt");

        try {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;

            while ((length = methodAsyncStream.read(buffer)) > 0) {

                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();

            mMethodAsync = outputStream.toString("UTF-8");

        } catch (final IOException ex) {

            processingEnv.getMessager()
                         .printMessage(Kind.ERROR,
                                       "IOException while reading async method template");

            mDisabled = true;

            return;

        } finally {

            try {

                methodAsyncStream.close();

            } catch (final IOException ignored) {

            }
        }

        final InputStream methodResultStream =
                RoutineProcessor.class.getResourceAsStream("/templates/method_result.txt");

        try {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;

            while ((length = methodResultStream.read(buffer)) > 0) {

                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();

            mMethodResult = outputStream.toString("UTF-8");

        } catch (final IOException ex) {

            processingEnv.getMessager()
                         .printMessage(Kind.ERROR,
                                       "IOException while reading result method template");

            mDisabled = true;

            return;

        } finally {

            try {

                methodResultStream.close();

            } catch (final IOException ignored) {

            }
        }

        final InputStream methodVoidStream =
                RoutineProcessor.class.getResourceAsStream("/templates/method_void.txt");

        try {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;

            while ((length = methodVoidStream.read(buffer)) > 0) {

                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();

            mMethodVoid = outputStream.toString("UTF-8");

        } catch (final IOException ex) {

            processingEnv.getMessager()
                         .printMessage(Kind.ERROR,
                                       "IOException while reading void method template");

            mDisabled = true;

            return;

        } finally {

            try {

                methodVoidStream.close();

            } catch (final IOException ignored) {

            }
        }

        final InputStream footerStream =
                RoutineProcessor.class.getResourceAsStream("/templates/footer.txt");

        try {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;

            while ((length = footerStream.read(buffer)) > 0) {

                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();

            mFooter = outputStream.toString("UTF-8");

        } catch (final IOException ex) {

            processingEnv.getMessager()
                         .printMessage(Kind.ERROR, "IOException while reading footer template");

            mDisabled = true;

        } finally {

            try {

                footerStream.close();

            } catch (final IOException ignored) {

            }
        }
    }

    @Override
    public boolean process(final Set<? extends TypeElement> typeElements,
            final RoundEnvironment roundEnvironment) {

        if (roundEnvironment.processingOver() || mDisabled) {

            return false;
        }

        final TypeElement annotationElement = getTypeFromName(WrapAsync.class.getName());
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

        final TypeElement annotationElement = getTypeFromName(Async.class.getName());
        final TypeMirror annotationType = annotationElement.asType();

        Boolean isSequential = null;
        LogLevel logLevel = null;
        TypeElement logType = null;
        TypeElement runnerType = null;

        if (annotation != null) {

            isSequential = annotation.sequential();
            logLevel = annotation.logLevel();

            final Object log = getElementValue(methodElement, annotationType, "log");

            if (log != null) {

                logType = getTypeFromName(log.toString());
            }

            final Object runner = getElementValue(methodElement, annotationType, "runner");

            if (runner != null) {

                runnerType = getTypeFromName(runner.toString());
            }
        }

        if (targetAnnotation != null) {

            if (isSequential == null) {

                isSequential = targetAnnotation.sequential();
            }

            if (logLevel == null) {

                logLevel = targetAnnotation.logLevel();
            }

            if ((logType == null) || logType.equals(getTypeFromName(DefaultLog.class.getName()))) {

                final Object log = getElementValue(targetMethodElement, annotationType, "log");

                if (log != null) {

                    logType = getTypeFromName(log.toString());
                }
            }

            if ((runnerType == null) || runnerType.equals(
                    getTypeFromName(DefaultRunner.class.getName()))) {

                final Object runner =
                        getElementValue(targetMethodElement, annotationType, "runner");

                if (runner != null) {

                    runnerType = getTypeFromName(runner.toString());
                }
            }
        }

        final StringBuilder builder = new StringBuilder();

        if (isSequential != null) {

            if (isSequential) {

                builder.append(".sequential()");

            } else {

                builder.append(".queued()");
            }
        }

        if (logLevel != null) {

            builder.append(".logLevel(")
                   .append(LogLevel.class.getCanonicalName())
                   .append(".")
                   .append(logLevel)
                   .append(")");
        }

        if ((logType != null) && !logType.equals(getTypeFromName(DefaultLog.class.getName()))) {

            builder.append(".loggedWith(new ").append(logType).append("())");
        }

        if ((runnerType != null) && !runnerType.equals(
                getTypeFromName(DefaultRunner.class.getName()))) {

            builder.append(".runBy(new ").append(runnerType).append("())");
        }

        if (methodElement.getAnnotation(AsyncParameters.class) != null) {

            builder.append(".orderedInput()");
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
            header = header.replace("${classFullName}", qualifiedName);
            header = header.replace("${interfaceFullName}", element.getQualifiedName());
            header = header.replace("${routineFieldsInit}",
                                    buildRoutineFieldsInit(methodElements.size()));

            writer.append(header);

            int count = 0;

            for (final ExecutableElement methodElement : methodElements) {

                final ExecutableElement targetMethod =
                        findMatchingMethod(methodElement, targetElement);
                final String returnTypeName = targetMethod.getReturnType().toString();

                String method;

                if (methodElement.getAnnotation(AsyncResult.class) != null) {

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
                method = method.replace("${className}", targetElement.getSimpleName());
                method = method.replace("${classFullName}", qualifiedName);
                method = method.replace("${methodName}", methodElement.getSimpleName());
                method = method.replace("${targetMethodName}", targetMethod.getSimpleName());
                method = method.replace("${paramTypes}", buildParamTypes(methodElement));
                method = method.replace("${paramValues}", buildParamValues(targetMethod));
                method = method.replace("${inputParams}", buildInputParams(methodElement));
                method = method.replace("${routineBuilderOptions}",
                                        buildRoutineOptions(methodElement, targetMethod));

                final Async annotation = methodElement.getAnnotation(Async.class);
                final Async targetAnnotation = targetMethod.getAnnotation(Async.class);

                String parallelId = Async.DEFAULT_ID;

                if (annotation != null) {

                    parallelId = annotation.parallelId();
                }

                if ((targetAnnotation != null) && (parallelId.equals(Async.DEFAULT_ID))) {

                    parallelId = targetAnnotation.parallelId();
                }

                method = method.replace("${parallelId}", parallelId);

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
        final AsyncParameters asyncParametersAnnotation =
                methodElement.getAnnotation(AsyncParameters.class);

        if (asyncAnnotation != null) {

            final String tag = asyncAnnotation.tag();

            if ((tag != null) && (tag.length() > 0)) {

                for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                        targetElement.getEnclosedElements())) {

                    final Async targetAsyncAnnotation =
                            targetMethodElement.getAnnotation(Async.class);

                    if ((targetAsyncAnnotation != null) && tag.equals(
                            targetAsyncAnnotation.tag())) {

                        targetMethod = targetMethodElement;

                        break;
                    }
                }

                if (targetMethod == null) {

                    if (asyncParametersAnnotation != null) {

                        final TypeElement annotationElement =
                                getTypeFromName(AsyncParameters.class.getName());
                        final TypeMirror annotationType = annotationElement.asType();

                        final List<?> values =
                                (List<?>) getElementValue(methodElement, annotationType, "value");

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

            if (asyncParametersAnnotation != null) {

                final TypeElement annotationElement =
                        getTypeFromName(AsyncParameters.class.getName());
                final TypeMirror annotationType = annotationElement.asType();

                final List<?> values =
                        (List<?>) getElementValue(methodElement, annotationType, "value");

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

                for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                        targetElement.getEnclosedElements())) {

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

                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
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

    private boolean haveSameParameters(final ExecutableElement interfaceMethod,
            final ExecutableElement classMethod) {

        final List<? extends VariableElement> interfaceTypeParameters =
                interfaceMethod.getParameters();
        final List<? extends VariableElement> classTypeParameters = classMethod.getParameters();

        final int length = interfaceTypeParameters.size();

        if (length != classTypeParameters.size()) {

            return false;
        }

        for (int i = 0; i < length; i++) {

            if (!interfaceTypeParameters.get(i)
                                        .asType()
                                        .equals(classTypeParameters.get(i).asType())) {

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

                    final List<? extends VariableElement> parentParameters =
                            parentMethod.getParameters();
                    final List<? extends VariableElement> parameters = method.getParameters();

                    final int size = parentParameters.size();

                    if (size != parameters.size()) {

                        continue;
                    }

                    isOverride = true;

                    for (int i = 0; i < size; i++) {

                        //TODO: what about generics
                        if (!parentParameters.get(i).asType().equals(parameters.get(i).asType())) {

                            isOverride = false;

                            break;
                        }
                    }

                    if (isOverride) {

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
}
