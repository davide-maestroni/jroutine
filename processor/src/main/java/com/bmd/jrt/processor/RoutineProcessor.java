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
import com.bmd.jrt.builder.RoutineBuilder.TimeoutAction;
import com.bmd.jrt.annotation.AsyncName;
import com.bmd.jrt.annotation.AsyncType;
import com.bmd.jrt.annotation.AsyncWrap;
import com.bmd.jrt.annotation.ParallelType;
import com.bmd.jrt.builder.RoutineBuilder;
import com.bmd.jrt.builder.RoutineChannelBuilder.DataOrder;
import com.bmd.jrt.builder.RoutineConfiguration;
import com.bmd.jrt.channel.OutputChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * Annotation processor used to generate wrapper classes enable method asynchronous invocations.
 * <p/>
 * Created by davide on 11/3/14.
 */
@SupportedAnnotationTypes("com.bmd.jrt.annotation.AsyncWrap")
public class RoutineProcessor extends AbstractProcessor {

    private static final boolean DEBUG = false;

    private static final String NEW_LINE = System.getProperty("line.separator");

    private boolean mDisabled;

    private String mFooter;

    private String mHeader;

    private TypeElement mListElement;

    private String mMethodArray;

    private String mMethodAsync;

    private String mMethodFooter;

    private String mMethodFooterVoid;

    private String mMethodHeader;

    private String mMethodList;

    private String mMethodParallelArray;

    private String mMethodParallelAsync;

    private String mMethodParallelList;

    private String mMethodParallelResult;

    private String mMethodParallelVoid;

    private String mMethodResult;

    private String mMethodVoid;

    private TypeMirror mObjectType;

    private TypeElement mOutputChannelElement;

    @Override
    public SourceVersion getSupportedSourceVersion() {

        // Let's return the latest version
        final SourceVersion[] values = SourceVersion.values();
        return values[values.length - 1];
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {

        super.init(processingEnv);

        final byte[] buffer = new byte[2048];

        try {

            mHeader = parseTemplate("/templates/header.txt", buffer);
            mMethodHeader = parseTemplate("/templates/method_header.txt", buffer);
            mMethodArray = parseTemplate("/templates/method_array.txt", buffer);
            mMethodAsync = parseTemplate("/templates/method_async.txt", buffer);
            mMethodList = parseTemplate("/templates/method_list.txt", buffer);
            mMethodResult = parseTemplate("/templates/method_result.txt", buffer);
            mMethodVoid = parseTemplate("/templates/method_void.txt", buffer);
            mMethodParallelArray = parseTemplate("/templates/method_parallel_array.txt", buffer);
            mMethodParallelAsync = parseTemplate("/templates/method_parallel_async.txt", buffer);
            mMethodParallelList = parseTemplate("/templates/method_parallel_list.txt", buffer);
            mMethodParallelResult = parseTemplate("/templates/method_parallel_result.txt", buffer);
            mMethodParallelVoid = parseTemplate("/templates/method_parallel_void.txt", buffer);
            mMethodFooter = parseTemplate("/templates/method_footer.txt", buffer);
            mMethodFooterVoid = parseTemplate("/templates/method_footer_void.txt", buffer);
            mFooter = parseTemplate("/templates/footer.txt", buffer);

            mOutputChannelElement = getTypeFromName(OutputChannel.class.getCanonicalName());
            mListElement = getTypeFromName(List.class.getCanonicalName());
            mObjectType = getTypeFromName(Object.class.getCanonicalName()).asType();


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

        final TypeElement annotationElement = getTypeFromName(AsyncWrap.class.getCanonicalName());
        final TypeMirror annotationType = annotationElement.asType();

        for (final Element element : ElementFilter.typesIn(
                roundEnvironment.getElementsAnnotatedWith(AsyncWrap.class))) {

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

            final Object targetElement = getElementValue(element, annotationType, "value");

            if (targetElement != null) {

                createWrapper(classElement, getTypeFromName(targetElement.toString()),
                              methodElements);
            }
        }

        return false;
    }

    @Nonnull
    private String buildGenericTypes(@Nonnull final TypeElement element) {

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

    @Nonnull
    private String buildInputParams(@Nonnull final ExecutableElement methodElement) {

        final Types typeUtils = processingEnv.getTypeUtils();
        final TypeElement outputChannelElement = mOutputChannelElement;
        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : methodElement.getParameters()) {

            builder.append(".pass(");

            if (typeUtils.isAssignable(outputChannelElement.asType(),
                                       typeUtils.erasure(variableElement.asType()))) {

                if (variableElement.getAnnotation(AsyncType.class) != null) {

                    builder.append("(com.bmd.jrt.channel.OutputChannel)");

                } else {

                    builder.append("(Object)");
                }
            }

            builder.append(variableElement).append(")");
        }

        return builder.toString();
    }

    private String buildOutputOptions(@Nonnull final TypeElement element,
            final ExecutableElement methodElement) {

        final Async classAnnotation = element.getAnnotation(Async.class);
        final Async methodAnnotation = methodElement.getAnnotation(Async.class);

        long resultTimeout = RoutineConfiguration.DEFAULT;
        TimeUnit resultTimeUnit = null;
        TimeoutAction timeoutAction = RoutineBuilder.TimeoutAction.DEFAULT;

        if (methodAnnotation != null) {

            resultTimeout = methodAnnotation.resultTimeout();
            resultTimeUnit = methodAnnotation.resultTimeUnit();
            timeoutAction = methodAnnotation.eventually();
        }

        if ((classAnnotation != null) && (resultTimeout == RoutineConfiguration.DEFAULT)) {

            resultTimeout = classAnnotation.resultTimeout();
            resultTimeUnit = classAnnotation.resultTimeUnit();
            timeoutAction = classAnnotation.eventually();
        }

        final StringBuilder builder = new StringBuilder();

        if (resultTimeout != RoutineConfiguration.DEFAULT) {

            builder.append(".afterMax(")
                   .append(resultTimeout)
                   .append(", ")
                   .append(TimeUnit.class.getCanonicalName())
                   .append(".")
                   .append(resultTimeUnit)
                   .append(")");
        }

        if (timeoutAction == RoutineBuilder.TimeoutAction.EXIT) {

            builder.append(".eventuallyExit()");

        } else if (timeoutAction == RoutineBuilder.TimeoutAction.DEADLOCK) {

            builder.append(".eventuallyDeadlock()");
        }

        return builder.toString();
    }

    @Nonnull
    private String buildParamTypes(@Nonnull final ExecutableElement methodElement) {

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

    @Nonnull
    private String buildParamValues(@Nonnull final ExecutableElement targetMethodElement) {

        int count = 0;
        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : targetMethodElement.getParameters()) {

            if (builder.length() > 0) {

                builder.append(", ");
            }

            builder.append("(")
                   .append(getBoxedType(variableElement.asType()))
                   .append(") objects.get(")
                   .append(count++)
                   .append(")");
        }

        return builder.toString();
    }

    @Nonnull
    private CharSequence buildParams(@Nonnull final ExecutableElement methodElement) {

        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : methodElement.getParameters()) {

            if (builder.length() > 0) {

                builder.append(", ");
            }

            builder.append(variableElement);
        }

        return builder.toString();
    }

    @Nonnull
    private String buildRoutineFieldsInit(final int size) {

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= size; i++) {

            builder.append("mRoutine")
                   .append(i)
                   .append(" = ")
                   .append("initRoutine")
                   .append(i)
                   .append("(configuration);")
                   .append(NEW_LINE);
        }

        return builder.toString();
    }

    @Nonnull
    private String buildRoutineOptions(@Nonnull final ExecutableElement methodElement) {

        final StringBuilder builder = new StringBuilder();

        boolean isOverrideParameters = false;

        for (final VariableElement parameterElement : methodElement.getParameters()) {

            if (parameterElement.getAnnotation(AsyncType.class) != null) {

                isOverrideParameters = true;
                break;
            }
        }

        if (isOverrideParameters) {

            builder.append(".inputOrder(")
                   .append(DataOrder.class.getCanonicalName())
                   .append(".")
                   .append(DataOrder.INSERTION)
                   .append(")");
        }

        return builder.toString();
    }

    private void createWrapper(@Nonnull final TypeElement element,
            @Nonnull final TypeElement targetElement,
            @Nonnull final List<ExecutableElement> methodElements) {

        Writer writer = null;

        try {

            final String packageName = getPackage(element).getQualifiedName().toString();
            final String interfaceName = element.getSimpleName().toString();
            final Filer filer = processingEnv.getFiler();

            //noinspection PointlessBooleanExpression,ConstantConditions
            if (!DEBUG) {

                final JavaFileObject sourceFile =
                        filer.createSourceFile(packageName + "." + interfaceName + "$$Wrapper");
                writer = sourceFile.openWriter();

            } else {

                writer = new StringWriter();
            }

            String header;
            header = mHeader.replace("${packageName}", packageName);
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
                writeMethod(writer, element, targetElement, methodElement, count);
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

    @Nonnull
    private ExecutableElement findMatchingMethod(@Nonnull final ExecutableElement methodElement,
            @Nonnull final TypeElement targetElement) {

        String methodName = methodElement.getSimpleName().toString();
        ExecutableElement targetMethod = null;
        final AsyncName asyncAnnotation = methodElement.getAnnotation(AsyncName.class);

        if (asyncAnnotation != null) {

            methodName = asyncAnnotation.value();

            for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                    targetElement.getEnclosedElements())) {

                final AsyncName targetAsyncAnnotation =
                        targetMethodElement.getAnnotation(AsyncName.class);

                if ((targetAsyncAnnotation != null) && methodName.equals(
                        targetAsyncAnnotation.value())) {

                    targetMethod = targetMethodElement;

                    break;
                }
            }
        }

        if (targetMethod == null) {

            final Types typeUtils = processingEnv.getTypeUtils();
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

                            if (!typeUtils.isSameType((TypeMirror) value, typeUtils.erasure(
                                    classTypeParameters.get(i).asType()))) {

                                matches = false;
                                break;
                            }

                        } else {

                            if (!typeUtils.isSameType(variableElement.asType(), typeUtils.erasure(
                                    classTypeParameters.get(i).asType()))) {

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

    private TypeMirror getBoxedType(final TypeMirror type) {

        if ((type != null) && type.getKind().isPrimitive()) {

            return processingEnv.getTypeUtils().boxedClass((PrimitiveType) type).asType();
        }

        return type;
    }

    @Nullable
    private Object getElementValue(@Nonnull final Element element,
            @Nonnull final TypeMirror annotationType, @Nonnull final String valueName) {

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

    @Nonnull
    private PackageElement getPackage(@Nonnull final TypeElement element) {

        return processingEnv.getElementUtils().getPackageOf(element);
    }

    @Nonnull
    private TypeElement getTypeFromName(@Nonnull final String typeName) {

        return processingEnv.getElementUtils().getTypeElement(normalizeTypeName(typeName));
    }

    private boolean haveSameParameters(@Nonnull final ExecutableElement firstMethodElement,
            @Nonnull final ExecutableElement secondMethodElement) {

        final List<? extends VariableElement> firstTypeParameters =
                firstMethodElement.getParameters();
        final List<? extends VariableElement> secondTypeParameters =
                secondMethodElement.getParameters();

        final int length = firstTypeParameters.size();

        if (length != secondTypeParameters.size()) {

            return false;
        }

        final Types typeUtils = processingEnv.getTypeUtils();

        for (int i = 0; i < length; i++) {

            final TypeMirror firstType = firstTypeParameters.get(i).asType();
            final TypeMirror secondType = secondTypeParameters.get(i).asType();

            if (!typeUtils.isSameType(firstType, secondType)) {

                return false;
            }
        }

        return true;
    }

    private void mergeParentMethods(@Nonnull final List<ExecutableElement> methods,
            @Nonnull final List<ExecutableElement> parentMethods) {

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

    @Nonnull
    private String normalizeTypeName(@Nonnull final String typeName) {

        if (typeName.endsWith(".class")) {

            return typeName.substring(0, typeName.length() - ".class".length());
        }

        return typeName;
    }

    @Nonnull
    private String parseTemplate(@Nonnull final String path, @Nonnull final byte[] buffer) throws
            IOException {

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

    private void writeMethod(@Nonnull final Writer writer, @Nonnull final TypeElement element,
            @Nonnull final TypeElement targetElement,
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        final Types typeUtils = processingEnv.getTypeUtils();
        final TypeElement outputChannelElement = mOutputChannelElement;
        final TypeElement listElement = mListElement;
        final TypeMirror objectType = mObjectType;
        final ExecutableElement targetMethod = findMatchingMethod(methodElement, targetElement);
        TypeMirror targetReturnType = targetMethod.getReturnType();

        boolean isParallel = false;
        final boolean isVoid = (targetReturnType.getKind() == TypeKind.VOID);
        final AsyncType overrideAnnotation = methodElement.getAnnotation(AsyncType.class);
        final List<? extends VariableElement> parameters = methodElement.getParameters();

        if ((parameters.size() == 1) && (parameters.get(0).getAnnotation(ParallelType.class)
                != null)) {

            isParallel = true;

        } else {

            for (final VariableElement parameter : parameters) {

                if (parameter.getAnnotation(ParallelType.class) != null) {

                    throw new IllegalArgumentException(
                            "Invalid annotations for method: " + methodElement);
                }
            }
        }

        String method;

        if (overrideAnnotation != null) {

            final TypeMirror returnType = methodElement.getReturnType();
            final TypeMirror returnTypeErasure = typeUtils.erasure(returnType);

            if (returnType.getKind() == TypeKind.ARRAY) {

                targetReturnType = ((ArrayType) returnType).getComponentType();
                method = (isParallel) ? mMethodParallelArray : mMethodArray;

            } else if (typeUtils.isAssignable(listElement.asType(), returnTypeErasure)) {

                final List<? extends TypeMirror> typeArguments =
                        ((DeclaredType) returnType).getTypeArguments();

                if (typeArguments.isEmpty()) {

                    targetReturnType = objectType;

                } else {

                    targetReturnType = typeArguments.get(0);
                }

                method = (isParallel) ? mMethodParallelList : mMethodList;

            } else if (typeUtils.isAssignable(outputChannelElement.asType(), returnTypeErasure)) {

                final List<? extends TypeMirror> typeArguments =
                        ((DeclaredType) returnType).getTypeArguments();

                if (typeArguments.isEmpty()) {

                    targetReturnType = objectType;

                } else {

                    targetReturnType = typeArguments.get(0);
                }

                method = (isParallel) ? mMethodParallelAsync : mMethodAsync;

            } else {

                throw new IllegalArgumentException(
                        "Invalid return type for method: " + methodElement);
            }

        } else if (isVoid) {

            method = (isParallel) ? mMethodParallelVoid : mMethodVoid;

        } else {

            targetReturnType = methodElement.getReturnType();

            method = (isParallel) ? mMethodParallelResult : mMethodResult;
        }

        final String resultClassName = getBoxedType(targetReturnType).toString();

        String methodHeader;
        methodHeader = mMethodHeader.replace("${resultClassName}", resultClassName);
        methodHeader = methodHeader.replace("${methodCount}", Integer.toString(count));
        methodHeader = methodHeader.replace("${routineBuilderOptions}",
                                            buildRoutineOptions(methodElement));

        writer.append(methodHeader);

        method = method.replace("${resultClassName}", resultClassName);
        method = method.replace("${resultRawClass}", targetReturnType.toString());
        method = method.replace("${resultType}", methodElement.getReturnType().toString());
        method = method.replace("${methodCount}", Integer.toString(count));
        method = method.replace("${methodName}", methodElement.getSimpleName());
        method = method.replace("${params}", buildParams(methodElement));
        method = method.replace("${paramTypes}", buildParamTypes(methodElement));
        method = method.replace("${paramValues}", buildParamValues(targetMethod));
        method = method.replace("${inputParams}", buildInputParams(methodElement));
        method = method.replace("${outputOptions}", buildOutputOptions(element, methodElement));

        writer.append(method);

        String methodFooter;
        methodFooter = (isVoid) ? mMethodFooterVoid : mMethodFooter;
        methodFooter = methodFooter.replace("${classFullName}", targetElement.asType().toString());
        methodFooter = methodFooter.replace("${resultClassName}", resultClassName);
        methodFooter = methodFooter.replace("${methodCount}", Integer.toString(count));
        methodFooter = methodFooter.replace("${methodName}", methodElement.getSimpleName());
        methodFooter = methodFooter.replace("${targetMethodName}", targetMethod.getSimpleName());
        methodFooter = methodFooter.replace("${paramValues}", buildParamValues(targetMethod));

        final Async classAnnotation = element.getAnnotation(Async.class);
        final Async methodAnnotation = methodElement.getAnnotation(Async.class);
        String lockName = Async.DEFAULT_LOCK;

        if (methodAnnotation != null) {

            lockName = methodAnnotation.lockName();
        }

        if ((classAnnotation != null) && (lockName.equals(Async.DEFAULT_LOCK))) {

            lockName = classAnnotation.lockName();
        }

        methodFooter = methodFooter.replace("${lockName}", lockName);

        writer.append(methodFooter);
    }
}
