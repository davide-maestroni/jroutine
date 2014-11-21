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
package com.bmd.jrt.processor;

import com.bmd.jrt.annotation.Async;
import com.bmd.jrt.annotation.AsyncClass;
import com.bmd.jrt.annotation.AsyncType;
import com.bmd.jrt.annotation.DefaultLog;
import com.bmd.jrt.annotation.DefaultRunner;
import com.bmd.jrt.annotation.ParallelType;
import com.bmd.jrt.builder.RoutineBuilder.ChannelDataOrder;
import com.bmd.jrt.builder.RoutineBuilder.SyncRunnerType;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.log.Log.LogLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * Annotation processor used to generate wrapper classes enable method asynchronous invocations.
 * <p/>
 * Created by davide on 11/3/14.
 */
@SupportedAnnotationTypes("com.bmd.jrt.annotation.AsyncClass")
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

    private String mMethodFooter;

    private String mMethodFooterVoid;

    private String mMethodHeader;

    private String mMethodParallelAsync;

    private String mMethodParallelResult;

    private String mMethodParallelVoid;

    private String mMethodResult;

    private String mMethodVoid;

    @Override
    public SourceVersion getSupportedSourceVersion() {

        // Let's return the latest version
        return SourceVersion.values()[SourceVersion.values().length - 1];
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {

        super.init(processingEnv);

        final byte[] buffer = new byte[2048];

        try {

            mHeader = parseTemplate("/templates/header.txt", buffer);
            mMethodHeader = parseTemplate("/templates/method_header.txt", buffer);
            mMethodAsync = parseTemplate("/templates/method_async.txt", buffer);
            mMethodResult = parseTemplate("/templates/method_result.txt", buffer);
            mMethodVoid = parseTemplate("/templates/method_void.txt", buffer);
            mMethodParallelAsync = parseTemplate("/templates/method_parallel_async.txt", buffer);
            mMethodParallelResult = parseTemplate("/templates/method_parallel_result.txt", buffer);
            mMethodParallelVoid = parseTemplate("/templates/method_parallel_void.txt", buffer);
            mMethodFooter = parseTemplate("/templates/method_footer.txt", buffer);
            mMethodFooterVoid = parseTemplate("/templates/method_footer_void.txt", buffer);
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

        final TypeElement annotationElement = getTypeFromName(AsyncClass.class.getCanonicalName());
        final TypeMirror annotationType = annotationElement.asType();

        for (final Element element : ElementFilter.typesIn(
                roundEnvironment.getElementsAnnotatedWith(AsyncClass.class))) {

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

        final List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();

        if (typeParameters.isEmpty()) {

            return "";
        }

        final StringBuilder builder = new StringBuilder("<");

        for (final TypeParameterElement typeParameterElement : typeParameters) {

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

    private CharSequence buildParams(final ExecutableElement methodElement) {

        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : methodElement.getParameters()) {

            if (builder.length() > 0) {

                builder.append(", ");
            }

            builder.append(variableElement);
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
                   .append("(configuration);");
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
        long availTimeout = RoutineConfiguration.NOT_SET;
        TimeUnit availTimeUnit = null;
        int maxInput = RoutineConfiguration.NOT_SET;
        long inputTimeout = RoutineConfiguration.NOT_SET;
        TimeUnit inputTimeUnit = null;
        ChannelDataOrder inputOrder = ChannelDataOrder.DEFAULT;
        int maxOutput = RoutineConfiguration.NOT_SET;
        long outputTimeout = RoutineConfiguration.NOT_SET;
        TimeUnit outputTimeUnit = null;
        ChannelDataOrder outputOrder = ChannelDataOrder.DEFAULT;
        LogLevel logLevel = LogLevel.DEFAULT;
        TypeElement logElement = null;
        TypeElement runnerElement = null;

        if (annotation != null) {

            final Object runner = getElementValue(methodElement, annotationType, "runnerClass");

            if (runner != null) {

                runnerElement = getTypeFromName(runner.toString());
            }

            runnerType = annotation.runnerType();
            maxRunning = annotation.maxRunning();
            maxRetained = annotation.maxRetained();
            availTimeout = annotation.availTimeout();
            availTimeUnit = annotation.availTimeUnit();
            maxInput = annotation.maxInput();
            inputTimeout = annotation.inputTimeout();
            inputTimeUnit = annotation.inputTimeUnit();
            inputOrder = annotation.inputOrder();
            maxOutput = annotation.maxOutput();
            outputTimeout = annotation.outputTimeout();
            outputTimeUnit = annotation.outputTimeUnit();
            outputOrder = annotation.outputOrder();

            final Object log = getElementValue(methodElement, annotationType, "log");

            if (log != null) {

                logElement = getTypeFromName(log.toString());
            }

            logLevel = annotation.logLevel();
        }

        if (targetAnnotation != null) {

            if ((runnerElement == null) || runnerElement.equals(
                    getTypeFromName(DefaultRunner.class.getCanonicalName()))) {

                final Object runner =
                        getElementValue(targetMethodElement, annotationType, "runnerClass");

                if (runner != null) {

                    runnerElement = getTypeFromName(runner.toString());
                }
            }

            if (runnerType == SyncRunnerType.DEFAULT) {

                runnerType = targetAnnotation.runnerType();
            }

            if (maxRunning == RoutineConfiguration.NOT_SET) {

                maxRunning = targetAnnotation.maxRunning();
            }

            if (maxRetained == RoutineConfiguration.NOT_SET) {

                maxRetained = targetAnnotation.maxRetained();
            }

            if (availTimeout == RoutineConfiguration.NOT_SET) {

                availTimeout = targetAnnotation.availTimeout();
                availTimeUnit = targetAnnotation.availTimeUnit();
            }

            if (maxInput == RoutineConfiguration.NOT_SET) {

                maxInput = targetAnnotation.maxInput();
            }

            if (inputTimeout == RoutineConfiguration.NOT_SET) {

                inputTimeout = targetAnnotation.inputTimeout();
                inputTimeUnit = targetAnnotation.inputTimeUnit();
            }

            if (inputOrder == ChannelDataOrder.DEFAULT) {

                inputOrder = targetAnnotation.inputOrder();
            }

            if (maxOutput == RoutineConfiguration.NOT_SET) {

                maxOutput = targetAnnotation.maxOutput();
            }

            if (outputTimeout == RoutineConfiguration.NOT_SET) {

                outputTimeout = targetAnnotation.outputTimeout();
                outputTimeUnit = targetAnnotation.outputTimeUnit();
            }

            if (outputOrder == ChannelDataOrder.DEFAULT) {

                outputOrder = targetAnnotation.outputOrder();
            }

            if ((logElement == null) || logElement.equals(
                    getTypeFromName(DefaultLog.class.getCanonicalName()))) {

                final Object log = getElementValue(targetMethodElement, annotationType, "log");

                if (log != null) {

                    logElement = getTypeFromName(log.toString());
                }
            }

            if (logLevel == LogLevel.DEFAULT) {

                logLevel = targetAnnotation.logLevel();
            }
        }

        final StringBuilder builder = new StringBuilder();

        if ((runnerElement != null) && !runnerElement.equals(
                getTypeFromName(DefaultRunner.class.getCanonicalName()))) {

            builder.append(".runBy(new ").append(runnerElement).append("())");
        }

        if (runnerType != SyncRunnerType.DEFAULT) {

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

        if (availTimeout != RoutineConfiguration.NOT_SET) {

            builder.append(".availTimeout(")
                   .append(availTimeout)
                   .append(", ")
                   .append(TimeUnit.class.getCanonicalName())
                   .append(".")
                   .append(availTimeUnit)
                   .append(")");
        }

        if (maxInput != RoutineConfiguration.NOT_SET) {

            builder.append(".inputMaxSize(").append(maxInput).append(")");
        }

        if (inputTimeout != RoutineConfiguration.NOT_SET) {

            builder.append(".inputTimeout(")
                   .append(inputTimeout)
                   .append(", ")
                   .append(TimeUnit.class.getCanonicalName())
                   .append(".")
                   .append(inputTimeUnit)
                   .append(")");
        }

        if (inputOrder != ChannelDataOrder.DEFAULT) {

            builder.append(".inputOrder(")
                   .append(ChannelDataOrder.class.getCanonicalName())
                   .append(".")
                   .append(inputOrder)
                   .append(")");
        }

        if (maxOutput != RoutineConfiguration.NOT_SET) {

            builder.append(".outputMaxSize(").append(maxOutput).append(")");
        }

        if (outputTimeout != RoutineConfiguration.NOT_SET) {

            builder.append(".outputTimeout(")
                   .append(outputTimeout)
                   .append(", ")
                   .append(TimeUnit.class.getCanonicalName())
                   .append(".")
                   .append(outputTimeUnit)
                   .append(")");
        }

        if (outputOrder != ChannelDataOrder.DEFAULT) {

            builder.append(".outputOrder(")
                   .append(ChannelDataOrder.class.getCanonicalName())
                   .append(".")
                   .append(outputOrder)
                   .append(")");
        }

        if ((logElement != null) && !logElement.equals(
                getTypeFromName(DefaultLog.class.getCanonicalName()))) {

            builder.append(".loggedWith(new ").append(logElement).append("())");
        }

        if (logLevel != LogLevel.DEFAULT) {

            builder.append(".logLevel(")
                   .append(LogLevel.class.getCanonicalName())
                   .append(".")
                   .append(logLevel)
                   .append(")");
        }

        boolean isOverrideParameters = false;

        for (final VariableElement parameterElement : methodElement.getParameters()) {

            if (parameterElement.getAnnotation(AsyncType.class) != null) {

                isOverrideParameters = true;

                break;
            }
        }

        if (isOverrideParameters) {

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

            final String packageName = getPackage(element).getQualifiedName().toString();
            final String className = targetElement.getSimpleName().toString();
            final String interfaceName = element.getSimpleName().toString();
            final Filer filer = processingEnv.getFiler();

            //noinspection PointlessBooleanExpression,ConstantConditions
            if (!DEBUG) {

                final JavaFileObject sourceFile =
                        filer.createSourceFile(packageName + "." + interfaceName + className);
                writer = sourceFile.openWriter();

            } else {

                writer = new StringWriter();
            }

            String header;

            header = mHeader.replace("${packageName}", packageName);
            header = header.replace("${className}", className);
            header = header.replace("${genericTypes}", buildGenericTypes(element));
            header = header.replace("${classFullName}", targetElement.asType().toString());
            header = header.replace("${interfaceName}", interfaceName);
            header = header.replace("${interfaceFullName}", element.asType().toString());
            header = header.replace("${routineFieldsInit}",
                                    buildRoutineFieldsInit(methodElements.size()));

            writer.append(header);

            int count = 0;

            for (final ExecutableElement methodElement : methodElements) {

                ++count;

                final ExecutableElement targetMethod =
                        findMatchingMethod(methodElement, targetElement);
                final String returnTypeName = targetMethod.getReturnType().toString();

                String methodHeader;

                methodHeader = mMethodHeader.replace("${resultClassName}",
                                                     boxingClassName(returnTypeName));
                methodHeader = methodHeader.replace("${methodCount}", Integer.toString(count));
                methodHeader = methodHeader.replace("${routineBuilderOptions}",
                                                    buildRoutineOptions(methodElement,
                                                                        targetMethod));

                writer.append(methodHeader);

                String method = null;

                final boolean isVoid = "void".equalsIgnoreCase(returnTypeName);
                final AsyncType overrideAnnotation = methodElement.getAnnotation(AsyncType.class);
                final List<? extends VariableElement> parameters = methodElement.getParameters();

                if ((parameters.size() == 1) && (parameters.get(0).getAnnotation(ParallelType.class)
                        != null)) {

                    if (overrideAnnotation != null) {

                        method = mMethodParallelAsync;

                    } else if (isVoid) {

                        method = mMethodParallelVoid;

                    } else {

                        method = mMethodParallelResult;
                    }

                } else if (overrideAnnotation != null) {

                    method = mMethodAsync;
                }

                if (method == null) {

                    if (isVoid) {

                        method = mMethodVoid;

                    } else {

                        method = mMethodResult;
                    }
                }

                method = method.replace("${resultClassName}", boxingClassName(returnTypeName));
                method = method.replace("${resultType}", returnTypeName);
                method = method.replace("${methodCount}", Integer.toString(count));
                method = method.replace("${methodName}", methodElement.getSimpleName());
                method = method.replace("${params}", buildParams(methodElement));
                method = method.replace("${paramTypes}", buildParamTypes(methodElement));
                method = method.replace("${paramValues}", buildParamValues(targetMethod));
                method = method.replace("${inputParams}", buildInputParams(methodElement));

                writer.append(method);

                String methodFooter;

                methodFooter = (isVoid) ? mMethodFooterVoid : mMethodFooter;

                methodFooter =
                        methodFooter.replace("${resultClassName}", boxingClassName(returnTypeName));
                methodFooter = methodFooter.replace("${methodCount}", Integer.toString(count));
                methodFooter = methodFooter.replace("${methodName}", methodElement.getSimpleName());
                methodFooter =
                        methodFooter.replace("${targetMethodName}", targetMethod.getSimpleName());
                methodFooter =
                        methodFooter.replace("${paramValues}", buildParamValues(targetMethod));

                final Async annotation = methodElement.getAnnotation(Async.class);
                final Async targetAnnotation = targetMethod.getAnnotation(Async.class);

                String lockId = Async.DEFAULT_ID;

                if (annotation != null) {

                    lockId = annotation.lockId();
                }

                if ((targetAnnotation != null) && (lockId.equals(Async.DEFAULT_ID))) {

                    lockId = targetAnnotation.lockId();
                }

                methodFooter = methodFooter.replace("${lockId}", lockId);

                writer.append(methodFooter);
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

        String methodName = methodElement.getSimpleName().toString();
        ExecutableElement targetMethod = null;
        final Async asyncAnnotation = methodElement.getAnnotation(Async.class);

        if (asyncAnnotation != null) {

            final String tag = asyncAnnotation.value();

            if ((tag != null) && (tag.length() > 0)) {

                methodName = tag;

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
            }
        }

        if (targetMethod == null) {

            final TypeElement asyncAnnotationElement =
                    getTypeFromName(AsyncType.class.getCanonicalName());
            final TypeMirror asyncAnnotationType = asyncAnnotationElement.asType();
            final TypeElement parallelAnnotationElement =
                    getTypeFromName(ParallelType.class.getCanonicalName());
            final TypeMirror parallelAnnotationType = parallelAnnotationElement.asType();

            final List<? extends VariableElement> interfaceTypeParameters =
                    methodElement.getParameters();
            final int length = interfaceTypeParameters.size();

            for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                    targetElement.getEnclosedElements())) {

                if (!methodName.equals(targetMethodElement.getSimpleName().toString())) {

                    continue;
                }

                final List<? extends VariableElement> classTypeParameters =
                        targetMethodElement.getParameters();

                if (length == classTypeParameters.size()) {

                    boolean matches = true;

                    for (int i = 0; i < length; i++) {

                        Object value = null;
                        final VariableElement variableElement = interfaceTypeParameters.get(i);

                        if (variableElement.getAnnotation(AsyncType.class) != null) {

                            value = getElementValue(variableElement, asyncAnnotationType, "value");
                        }

                        if (variableElement.getAnnotation(ParallelType.class) != null) {

                            value = getElementValue(variableElement, parallelAnnotationType,
                                                    "value");
                        }

                        if (value != null) {

                            if (!normalizeTypeName(value.toString()).equals(
                                    classTypeParameters.get(i).asType().toString())) {

                                matches = false;

                                break;
                            }

                        } else {

                            if (!variableElement.asType()
                                                .toString()
                                                .equals(classTypeParameters.get(i)
                                                                           .asType()
                                                                           .toString())) {

                                matches = false;

                                break;
                            }
                        }
                    }

                    if (matches) {

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

            if (!mirror.getAnnotationType().equals(annotationType)) {

                continue;
            }

            final Set<? extends Entry<? extends ExecutableElement, ? extends AnnotationValue>> set =
                    mirror.getElementValues().entrySet();

            for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : set) {

                if (valueName.equals(entry.getKey().getSimpleName().toString())) {

                    value = entry.getValue();

                    break;
                }
            }
        }

        return (value != null) ? value.getValue() : null;
    }

    private PackageElement getPackage(final TypeElement element) {

        Element enclosingElement = element.getEnclosingElement();

        while (!(enclosingElement instanceof PackageElement)) {

            enclosingElement = enclosingElement.getEnclosingElement();
        }

        return (PackageElement) enclosingElement;
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