/*
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
package com.gh.bmd.jrt.processor;

import com.gh.bmd.jrt.annotation.Alias;
import com.gh.bmd.jrt.annotation.Input;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Inputs;
import com.gh.bmd.jrt.annotation.Output;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.Priority;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.channel.InvocationChannel;
import com.gh.bmd.jrt.channel.OutputChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Annotation processor used to generate proxy classes enabling method asynchronous invocations.
 * <p/>
 * Created by davide-maestroni on 11/3/14.
 */
public class RoutineProcessor extends AbstractProcessor {

    protected static final String NEW_LINE = System.getProperty("line.separator");

    private static final boolean DEBUG = false;

    private final byte[] mByteBuffer = new byte[2048];

    protected TypeMirror invocationChannelType;

    protected TypeMirror iterableType;

    protected TypeMirror listType;

    protected TypeMirror objectType;

    protected TypeMirror outputChannelType;

    private String mFooter;

    private String mHeader;

    private String mMethodArray;

    private String mMethodArrayInvocation;

    private String mMethodArrayInvocationCollection;

    private String mMethodArrayInvocationVoid;

    private String mMethodAsync;

    private String mMethodHeader;

    private String mMethodInputs;

    private String mMethodInvocation;

    private String mMethodInvocationCollection;

    private String mMethodInvocationVoid;

    private String mMethodList;

    private String mMethodParallelArray;

    private String mMethodParallelAsync;

    private String mMethodParallelList;

    private String mMethodParallelResult;

    private String mMethodParallelVoid;

    private String mMethodResult;

    private String mMethodVoid;

    private TypeElement mProxyElement;

    /**
     * Prints the stacktrace of the specified throwable into a string.
     *
     * @param throwable the throwable instance.
     * @return the printed stacktrace.
     */
    @Nonnull
    protected static String printStackTrace(@Nonnull final Throwable throwable) {

        final StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {

        final List<TypeElement> annotationElements = getSupportedAnnotationElements();
        final HashSet<String> annotationTypes = new HashSet<String>(annotationElements.size());

        for (final TypeElement annotationElement : annotationElements) {

            if (annotationElement != null) {

                annotationTypes.add(annotationElement.getQualifiedName().toString());
            }
        }

        return annotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {

        // Let's return the latest version
        final SourceVersion[] values = SourceVersion.values();
        return values[values.length - 1];
    }

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {

        super.init(processingEnv);
        invocationChannelType =
                getTypeFromName(InvocationChannel.class.getCanonicalName()).asType();
        outputChannelType = getTypeFromName(OutputChannel.class.getCanonicalName()).asType();
        iterableType = getTypeFromName(Iterable.class.getCanonicalName()).asType();
        listType = getTypeFromName(List.class.getCanonicalName()).asType();
        objectType = getTypeFromName(Object.class.getCanonicalName()).asType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean process(final Set<? extends TypeElement> typeElements,
            final RoundEnvironment roundEnvironment) {

        if (roundEnvironment.processingOver()) {

            return false;
        }

        for (final TypeElement annotationElement : getSupportedAnnotationElements()) {

            final TypeMirror annotationType = annotationElement.asType();

            for (final Element element : ElementFilter.typesIn(
                    roundEnvironment.getElementsAnnotatedWith(annotationElement))) {

                final TypeElement classElement = (TypeElement) element;
                final List<ExecutableElement> methodElements =
                        ElementFilter.methodsIn(element.getEnclosedElements());

                for (final TypeMirror typeMirror : classElement.getInterfaces()) {

                    final Element superElement = processingEnv.getTypeUtils().asElement(typeMirror);

                    if (superElement != null) {

                        mergeParentMethods(methodElements, ElementFilter.methodsIn(
                                superElement.getEnclosedElements()));
                    }
                }

                final Object targetElement = getAnnotationValue(element, annotationType, "value");

                if (targetElement != null) {

                    createProxy(annotationElement, classElement,
                                getTypeFromName(targetElement.toString()), methodElements);
                }
            }
        }

        return false;
    }

    /**
     * Builds the string used to replace "${paramValues}" in the template.
     *
     * @param targetMethodElement the target method element.
     * @return the string.
     */
    @Nonnull
    protected String buildCollectionParamValues(
            @Nonnull final ExecutableElement targetMethodElement) {

        final VariableElement targetParameter = targetMethodElement.getParameters().get(0);
        return "(" + targetParameter.asType() + ") objects";
    }

    /**
     * Builds the string used to replace "${genericTypes}" in the template.
     *
     * @param element the annotated element.
     * @return the string.
     */
    @Nonnull
    protected String buildGenericTypes(@Nonnull final TypeElement element) {

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

    /**
     * Builds the string used to replace "${inputParams}" in the template.
     *
     * @param methodElement the method element.
     * @return the string.
     */
    @Nonnull
    protected String buildInputParams(@Nonnull final ExecutableElement methodElement) {

        final Types typeUtils = processingEnv.getTypeUtils();
        final TypeMirror outputChannelType = this.outputChannelType;
        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : methodElement.getParameters()) {

            builder.append(".pass(");

            if (typeUtils.isAssignable(outputChannelType,
                                       typeUtils.erasure(variableElement.asType())) && (
                    variableElement.getAnnotation(Input.class) != null)) {

                builder.append("(").append(typeUtils.erasure(outputChannelType)).append("<?>)");

            } else {

                builder.append("(Object)");
            }

            builder.append(variableElement).append(")");
        }

        return builder.toString();
    }

    /**
     * Builds the string used to replace "${outputOptions}" in the template.
     *
     * @param methodElement the method element.
     * @return the string.
     */
    @Nonnull
    protected String buildOutputOptions(@Nonnull final ExecutableElement methodElement) {

        final StringBuilder builder = new StringBuilder();
        final Timeout timeoutAnnotation = methodElement.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.append(".afterMax(")
                   .append(timeoutAnnotation.value())
                   .append(", ")
                   .append(TimeUnit.class.getCanonicalName())
                   .append(".")
                   .append(timeoutAnnotation.unit())
                   .append(")");
        }

        final TimeoutAction actionAnnotation = methodElement.getAnnotation(TimeoutAction.class);

        if (actionAnnotation != null) {

            final TimeoutActionType timeoutActionType = actionAnnotation.value();

            if (timeoutActionType == TimeoutActionType.DEADLOCK) {

                builder.append(".eventuallyDeadlock()");

            } else if (timeoutActionType == TimeoutActionType.EXIT) {

                builder.append(".eventuallyExit()");

            } else if (timeoutActionType == TimeoutActionType.ABORT) {

                builder.append(".eventuallyAbort()");
            }
        }

        return builder.toString();
    }

    /**
     * Builds the string used to replace "${paramTypes}" in the template.
     *
     * @param methodElement the method element.
     * @return the string.
     */
    @Nonnull
    protected String buildParamTypes(@Nonnull final ExecutableElement methodElement) {

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

    /**
     * Builds the string used to replace "${paramValues}" in the template.
     *
     * @param targetMethodElement the target method element.
     * @return the string.
     */
    @Nonnull
    protected String buildParamValues(@Nonnull final ExecutableElement targetMethodElement) {

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

    /**
     * Builds the string used to replace "${params}" in the template.
     *
     * @param methodElement the method element.
     * @return the string.
     */
    @Nonnull
    protected CharSequence buildParams(@Nonnull final ExecutableElement methodElement) {

        final StringBuilder builder = new StringBuilder();

        for (final VariableElement variableElement : methodElement.getParameters()) {

            if (builder.length() > 0) {

                builder.append(", ");
            }

            builder.append(variableElement);
        }

        return builder.toString();
    }

    /**
     * Builds the string used to replace "${routineFieldsInit}" in the template.
     *
     * @param size the number of method elements.
     * @return the string.
     */
    @Nonnull
    protected String buildRoutineFieldsInit(final int size) {

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= size; i++) {

            builder.append("mRoutine")
                   .append(i)
                   .append(" = ")
                   .append("initRoutine")
                   .append(i)
                   .append("(invocationConfiguration);")
                   .append(NEW_LINE);
        }

        return builder.toString();
    }

    /**
     * Builds the string used to replace "${routineBuilderOptions}" in the template.
     *
     * @param methodElement the method element.
     * @param inputMode     the input transfer mode.
     * @param outputMode    the output transfer mode.
     * @return the string.
     */
    @Nonnull
    protected String buildRoutineOptions(@Nonnull final ExecutableElement methodElement,
            @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

        final StringBuilder builder = new StringBuilder();
        final Priority priorityAnnotation = methodElement.getAnnotation(Priority.class);

        if (priorityAnnotation != null) {

            builder.append(".withPriority(").append(priorityAnnotation.value()).append(")");
        }

        return builder.append(".withInputOrder(")
                      .append(OrderType.class.getCanonicalName())
                      .append(".")
                      .append(((inputMode == InputMode.ELEMENT) ? OrderType.NONE
                              : OrderType.PASS_ORDER))
                      .append(").withOutputOrder(")
                      .append(OrderType.class.getCanonicalName())
                      .append(".")
                      .append(((outputMode == OutputMode.ELEMENT) ? OrderType.PASS_ORDER
                              : OrderType.NONE))
                      .append(")")
                      .toString();
    }

    /**
     * Builds the string used to replace "${resultRawSizedArray}" in the template.
     *
     * @param returnType the target method return type.
     * @return the string.
     */
    @Nonnull
    protected String buildSizedArray(@Nonnull final TypeMirror returnType) {

        final StringBuilder builder = new StringBuilder();

        if (returnType.getKind() == TypeKind.ARRAY) {

            final String typeString = returnType.toString();
            final int firstBracket = typeString.indexOf('[');
            builder.append(typeString.substring(0, firstBracket))
                   .append("[$$size]")
                   .append(typeString.substring(firstBracket));

        } else {

            builder.append(returnType).append("[$$size]");
        }

        return builder.toString();
    }

    /**
     * Returns the element value of the specified annotation attribute.
     *
     * @param element        the annotated element.
     * @param annotationType the annotation type.
     * @param attributeName  the attribute name.
     * @return the value.
     */
    @Nullable
    protected Object getAnnotationValue(@Nonnull final Element element,
            @Nonnull final TypeMirror annotationType, @Nonnull final String attributeName) {

        AnnotationValue value = null;

        for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {

            if (!mirror.getAnnotationType().equals(annotationType)) {

                continue;
            }

            final Set<? extends Entry<? extends ExecutableElement, ? extends AnnotationValue>> set =
                    mirror.getElementValues().entrySet();

            for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : set) {

                if (attributeName.equals(entry.getKey().getSimpleName().toString())) {

                    value = entry.getValue();
                    break;
                }
            }
        }

        return (value != null) ? value.getValue() : null;
    }

    /**
     * Returned the boxed type.
     *
     * @param type the type to be boxed.
     * @return the boxed type.
     */
    protected TypeMirror getBoxedType(@Nullable final TypeMirror type) {

        if ((type != null) && (type.getKind().isPrimitive() || (type.getKind() == TypeKind.VOID))) {

            return processingEnv.getTypeUtils().boxedClass((PrimitiveType) type).asType();
        }

        return type;
    }

    /**
     * Gets the default class name prefix.
     *
     * @param annotationElement the annotation element.
     * @param element           the annotated element.
     * @param targetElement     the target element.
     * @return the name prefix.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getDefaultClassPrefix(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        String defaultPrefix = null;

        for (final Element valueElement : annotationElement.getEnclosedElements()) {

            if (valueElement.getSimpleName().toString().equals("DEFAULT_CLASS_PREFIX")) {

                defaultPrefix = (String) ((VariableElement) valueElement).getConstantValue();
                break;
            }
        }

        return (defaultPrefix == null) ? "" : defaultPrefix;
    }

    /**
     * Gets the default class name suffix.
     *
     * @param annotationElement the annotation element.
     * @param element           the annotated element.
     * @param targetElement     the target element.
     * @return the name suffix.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getDefaultClassSuffix(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        String defaultSuffix = null;

        for (final Element valueElement : annotationElement.getEnclosedElements()) {

            if (valueElement.getSimpleName().toString().equals("DEFAULT_CLASS_SUFFIX")) {

                defaultSuffix = (String) ((VariableElement) valueElement).getConstantValue();
                break;
            }
        }

        return (defaultSuffix == null) ? "" : defaultSuffix;
    }

    /**
     * Returns the specified template as a string.
     *
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    protected String getFooterTemplate() throws IOException {

        if (mFooter == null) {

            mFooter = parseTemplate("/templates/footer.txt");
        }

        return mFooter;
    }

    /**
     * Returns the name of the generated class.
     *
     * @param annotationElement the annotation element.
     * @param element           the annotated element.
     * @param targetElement     the target element.
     * @return the class name.
     */
    @Nonnull
    @SuppressWarnings({"ConstantConditions", "UnusedParameters"})
    protected String getGeneratedClassName(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        String className =
                (String) getAnnotationValue(element, annotationElement.asType(), "className");

        if ((className == null) || className.equals("*")) {

            className = element.getSimpleName().toString();
            Element enclosingElement = element.getEnclosingElement();

            while ((enclosingElement != null) && !(enclosingElement instanceof PackageElement)) {

                className = enclosingElement.getSimpleName().toString() + "_" + className;
                enclosingElement = enclosingElement.getEnclosingElement();
            }
        }

        return className;
    }

    /**
     * Returns the package of the generated class.
     *
     * @param annotationElement the annotation element.
     * @param element           the annotated element.
     * @param targetElement     the target element.
     * @return the class package.
     */
    @Nonnull
    @SuppressWarnings({"ConstantConditions", "UnusedParameters"})
    protected String getGeneratedClassPackage(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        String classPackage =
                (String) getAnnotationValue(element, annotationElement.asType(), "classPackage");

        if ((classPackage == null) || classPackage.equals("*")) {

            classPackage = getPackage(element).getQualifiedName().toString();
        }

        return classPackage;
    }

    /**
     * Returns the prefix of the generated class name.
     *
     * @param annotationElement the annotation element.
     * @param element           the annotated element.
     * @param targetElement     the target element.
     * @return the name prefix.
     */
    @Nonnull
    @SuppressWarnings({"ConstantConditions", "UnusedParameters"})
    protected String getGeneratedClassPrefix(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        final String classPrefix =
                (String) getAnnotationValue(element, annotationElement.asType(), "classPrefix");
        return (classPrefix == null) ? getDefaultClassPrefix(annotationElement, element,
                                                             targetElement) : classPrefix;
    }

    /**
     * Returns the suffix of the generated class name.
     *
     * @param annotationElement the annotation element.
     * @param element           the annotated element.
     * @param targetElement     the target element.
     * @return the name suffix.
     */
    @Nonnull
    @SuppressWarnings({"ConstantConditions", "UnusedParameters"})
    protected String getGeneratedClassSuffix(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        final String classSuffix =
                (String) getAnnotationValue(element, annotationElement.asType(), "classSuffix");
        return (classSuffix == null) ? getDefaultClassSuffix(annotationElement, element,
                                                             targetElement) : classSuffix;
    }

    /**
     * Returns the specified template as a string.
     *
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    protected String getHeaderTemplate() throws IOException {

        if (mHeader == null) {

            mHeader = parseTemplate("/templates/header.txt");
        }

        return mHeader;
    }

    /**
     * Gets the input transfer mode.
     *
     * @param methodElement   the method element.
     * @param annotation      the parameter annotation.
     * @param targetParameter the target parameter.
     * @param length          the total number of parameters.
     * @return the input mode.
     */
    @Nonnull
    protected InputMode getInputMode(@Nonnull final ExecutableElement methodElement,
            @Nonnull final Input annotation, @Nonnull final VariableElement targetParameter,
            final int length) {

        final Types typeUtils = processingEnv.getTypeUtils();
        final TypeMirror outputChannelType = this.outputChannelType;
        final TypeMirror iterableType = this.iterableType;
        final TypeMirror listType = this.listType;
        final TypeMirror targetType = targetParameter.asType();
        final TypeMirror targetTypeErasure = typeUtils.erasure(targetType);
        final TypeElement annotationElement = getTypeFromName(Input.class.getCanonicalName());
        final TypeMirror annotationType = annotationElement.asType();
        final TypeMirror targetMirror =
                (TypeMirror) getAnnotationValue(targetParameter, annotationType, "value");
        InputMode inputMode = annotation.mode();

        if (inputMode == InputMode.AUTO) {

            if (typeUtils.isAssignable(targetTypeErasure, outputChannelType)) {

                if ((length == 1) && (targetMirror != null) && (
                        (targetMirror.getKind() == TypeKind.ARRAY) || typeUtils.isAssignable(
                                listType, targetMirror))) {

                    inputMode = InputMode.COLLECTION;

                } else {

                    inputMode = InputMode.VALUE;
                }

            } else if ((targetType.getKind() == TypeKind.ARRAY) || typeUtils.isAssignable(
                    targetTypeErasure, iterableType)) {

                if ((targetType.getKind() == TypeKind.ARRAY) && !typeUtils.isAssignable(
                        getBoxedType(((ArrayType) targetType).getComponentType()),
                        getBoxedType(targetMirror))) {

                    throw new IllegalArgumentException(
                            "[" + methodElement + "] the async input array with mode "
                                    + InputMode.ELEMENT + " does not match the bound type: "
                                    + targetMirror);
                }

                if (length > 1) {

                    throw new IllegalArgumentException(
                            "[" + methodElement + "] an async input with mode " + InputMode.ELEMENT
                                    + " cannot be applied to a method taking " + length
                                    + " input parameters");
                }

                inputMode = InputMode.ELEMENT;

            } else {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] cannot automatically choose an "
                                + "input mode for an output of type: " + targetParameter);
            }

        } else if (inputMode == InputMode.VALUE) {

            if (!typeUtils.isAssignable(targetTypeErasure, outputChannelType)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.VALUE
                                + " must implement an " + outputChannelType);
            }

        } else if (inputMode == InputMode.COLLECTION) {

            if (!typeUtils.isAssignable(targetTypeErasure, outputChannelType)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.COLLECTION
                                + " must implement an " + outputChannelType);
            }

            if ((targetMirror != null) && (targetMirror.getKind() != TypeKind.ARRAY)
                    && !typeUtils.isAssignable(listType, targetMirror)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.COLLECTION
                                + " must be bound to an array or a superclass of " + listType);
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.COLLECTION
                                + " cannot be applied to a method taking " + length
                                + " input parameters");
            }

        } else { // InputMode.ELEMENT

            if ((targetType.getKind() != TypeKind.ARRAY) && !typeUtils.isAssignable(
                    targetTypeErasure, iterableType)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.ELEMENT
                                + " must be an array or implement an " + iterableType);
            }

            if ((targetType.getKind() == TypeKind.ARRAY) && !typeUtils.isAssignable(
                    getBoxedType(((ArrayType) targetType).getComponentType()),
                    getBoxedType(targetMirror))) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] the async input array with mode "
                                + InputMode.ELEMENT + " does not match the bound type: "
                                + targetMirror);
            }

            if (length > 1) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.ELEMENT
                                + " cannot be applied to a method taking " + length
                                + " input parameters");
            }
        }

        return inputMode;
    }

    /**
     * Gets the input transfer mode.
     *
     * @param methodElement the method element.
     * @param annotation    the method annotation.
     * @return the input mode.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected InputMode getInputsMode(@Nonnull final ExecutableElement methodElement,
            @Nonnull final Inputs annotation) {

        if (!methodElement.getParameters().isEmpty()) {

            throw new IllegalArgumentException(
                    "methods annotated with " + Inputs.class.getSimpleName()
                            + " must have no input parameters: " + methodElement);
        }

        final Types typeUtils = processingEnv.getTypeUtils();

        if (!typeUtils.isAssignable(invocationChannelType,
                                    typeUtils.erasure(methodElement.getReturnType()))) {

            throw new IllegalArgumentException(
                    "the proxy method has incompatible return type: " + methodElement);
        }

        final TypeMirror listType = this.listType;
        final TypeElement annotationElement = getTypeFromName(Inputs.class.getCanonicalName());
        final TypeMirror annotationType = annotationElement.asType();
        final List<AnnotationValue> targetMirrors =
                (List<AnnotationValue>) getAnnotationValue(methodElement, annotationType, "value");

        if (targetMirrors == null) {

            // should never happen
            throw new NullPointerException(
                    Inputs.class.getSimpleName() + " annotation value must not be null");
        }

        InputMode inputMode = annotation.mode();

        if (inputMode == InputMode.AUTO) {

            if (targetMirrors.size() == 1) {

                final TypeMirror targetType = (TypeMirror) targetMirrors.get(0).getValue();

                if ((targetType != null) && ((targetType.getKind() == TypeKind.ARRAY)
                        || typeUtils.isAssignable(listType, typeUtils.erasure(targetType)))) {

                    inputMode = InputMode.COLLECTION;

                } else {

                    inputMode = InputMode.ELEMENT;
                }

            } else {

                inputMode = InputMode.VALUE;
            }

        } else if (inputMode == InputMode.COLLECTION) {

            final TypeMirror targetType = (TypeMirror) targetMirrors.get(0).getValue();

            if ((targetType != null) && ((targetType.getKind() != TypeKind.ARRAY)
                    && !typeUtils.isAssignable(listType, typeUtils.erasure(targetType)))) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.COLLECTION
                                + " must be bound to an array or a superclass of " + listType);
            }

            if (targetMirrors.size() > 1) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.COLLECTION +
                                " cannot be applied to a method taking " + targetMirrors.size()
                                + " input parameters");
            }

        } else if (inputMode == InputMode.ELEMENT) {

            if (targetMirrors.size() > 1) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async input with mode " + InputMode.ELEMENT +
                                " cannot be applied to a method taking " + targetMirrors.size()
                                + " input parameters");
            }
        }

        return inputMode;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodArrayInvocationCollectionTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodArrayInvocationCollection == null) {

            mMethodArrayInvocationCollection =
                    parseTemplate("/templates/method_array_invocation_collection.txt");
        }

        return mMethodArrayInvocationCollection;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodArrayInvocationTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodArrayInvocation == null) {

            mMethodArrayInvocation = parseTemplate("/templates/method_array_invocation.txt");
        }

        return mMethodArrayInvocation;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodArrayInvocationVoidTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodArrayInvocationVoid == null) {

            mMethodArrayInvocationVoid =
                    parseTemplate("/templates/method_array_invocation_void.txt");
        }

        return mMethodArrayInvocationVoid;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodArrayTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodArray == null) {

            mMethodArray = parseTemplate("/templates/method_array.txt");
        }

        return mMethodArray;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodAsyncTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodAsync == null) {

            mMethodAsync = parseTemplate("/templates/method_async.txt");
        }

        return mMethodAsync;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodHeaderTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodHeader == null) {

            mMethodHeader = parseTemplate("/templates/method_header.txt");
        }

        return mMethodHeader;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInputsTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodInputs == null) {

            mMethodInputs = parseTemplate("/templates/method_inputs.txt");
        }

        return mMethodInputs;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInvocationCollectionTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodInvocationCollection == null) {

            mMethodInvocationCollection =
                    parseTemplate("/templates/method_invocation_collection.txt");
        }

        return mMethodInvocationCollection;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInvocationTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodInvocation == null) {

            mMethodInvocation = parseTemplate("/templates/method_invocation.txt");
        }

        return mMethodInvocation;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInvocationVoidTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodInvocationVoid == null) {

            mMethodInvocationVoid = parseTemplate("/templates/method_invocation_void.txt");
        }

        return mMethodInvocationVoid;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodListTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodList == null) {

            mMethodList = parseTemplate("/templates/method_list.txt");
        }

        return mMethodList;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodParallelArrayTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodParallelArray == null) {

            mMethodParallelArray = parseTemplate("/templates/method_parallel_array.txt");
        }

        return mMethodParallelArray;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodParallelAsyncTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodParallelAsync == null) {

            mMethodParallelAsync = parseTemplate("/templates/method_parallel_async.txt");
        }

        return mMethodParallelAsync;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodParallelListTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodParallelList == null) {

            mMethodParallelList = parseTemplate("/templates/method_parallel_list.txt");
        }

        return mMethodParallelList;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodParallelResultTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodParallelResult == null) {

            mMethodParallelResult = parseTemplate("/templates/method_parallel_result.txt");
        }

        return mMethodParallelResult;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodParallelVoidTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodParallelVoid == null) {

            mMethodParallelVoid = parseTemplate("/templates/method_parallel_void.txt");
        }

        return mMethodParallelVoid;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodResultTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodResult == null) {

            mMethodResult = parseTemplate("/templates/method_result.txt");
        }

        return mMethodResult;
    }

    /**
     * Returns the specified template as a string.
     *
     * @param methodElement the method element.
     * @param count         the method count.
     * @return the template.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getMethodVoidTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodVoid == null) {

            mMethodVoid = parseTemplate("/templates/method_void.txt");
        }

        return mMethodVoid;
    }

    /**
     * Gets the return output transfer mode.
     *
     * @param methodElement       the method element.
     * @param targetMethodElement the target method element.
     * @return the output mode.
     */
    @Nonnull
    protected OutputMode getOutputMode(@Nonnull final ExecutableElement methodElement,
            @Nonnull final ExecutableElement targetMethodElement) {

        final Types typeUtils = processingEnv.getTypeUtils();
        final TypeMirror outputChannelType = this.outputChannelType;
        final TypeMirror iterableType = this.iterableType;
        final TypeMirror listType = this.listType;
        final TypeMirror returnType = methodElement.getReturnType();
        final TypeMirror erasure = typeUtils.erasure(returnType);
        final TypeMirror targetMirror = typeUtils.erasure(targetMethodElement.getReturnType());
        OutputMode outputMode = methodElement.getAnnotation(Output.class).value();

        if (outputMode == OutputMode.AUTO) {

            if ((returnType.getKind() == TypeKind.ARRAY) || typeUtils.isAssignable(listType,
                                                                                   erasure)) {

                if ((returnType.getKind() == TypeKind.ARRAY) && !typeUtils.isAssignable(
                        getBoxedType(targetMirror),
                        getBoxedType(((ArrayType) returnType).getComponentType()))) {

                    throw new IllegalArgumentException(
                            "[" + methodElement + "] the async output array with mode "
                                    + OutputMode.COLLECTION + " does not match the bound type: "
                                    + targetMirror);
                }

                outputMode = OutputMode.COLLECTION;

            } else if (typeUtils.isAssignable(outputChannelType, erasure)) {

                if ((targetMirror != null) && ((targetMirror.getKind() == TypeKind.ARRAY)
                        || typeUtils.isAssignable(targetMirror, iterableType))) {

                    outputMode = OutputMode.ELEMENT;

                } else {

                    outputMode = OutputMode.VALUE;
                }

            } else {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] cannot automatically choose an "
                                + "output mode for an input of type: " + returnType);
            }

        } else if (outputMode == OutputMode.VALUE) {

            if (!typeUtils.isAssignable(outputChannelType, erasure)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async output with mode " + OutputMode.VALUE
                                + " must be a superclass of " + outputChannelType);
            }

        } else if (outputMode == OutputMode.ELEMENT) {

            if (!typeUtils.isAssignable(outputChannelType, erasure)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async output with mode " + OutputMode.VALUE
                                + " must be a superclass of " + outputChannelType);
            }

            if ((targetMirror != null) && (targetMirror.getKind() != TypeKind.ARRAY)
                    && !typeUtils.isAssignable(targetMirror, iterableType)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async output with mode " + OutputMode.ELEMENT
                                + " must be bound to an array or a type implementing an "
                                + iterableType);
            }

        } else { // OutputMode.COLLECTION

            if ((returnType.getKind() != TypeKind.ARRAY) && !typeUtils.isAssignable(listType,
                                                                                    erasure)) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] an async output with mode " + OutputMode.COLLECTION
                                + " must be an array or a superclass of " + listType);
            }

            if ((returnType.getKind() == TypeKind.ARRAY) && !typeUtils.isAssignable(
                    getBoxedType(targetMirror),
                    getBoxedType(((ArrayType) returnType).getComponentType()))) {

                throw new IllegalArgumentException(
                        "[" + methodElement + "] the async output array with mode "
                                + OutputMode.COLLECTION + " does not match the bound type: "
                                + targetMirror);
            }
        }

        return outputMode;
    }

    /**
     * Returns the package of the specified element..
     *
     * @param element the element.
     * @return the package.
     */
    @Nonnull
    protected PackageElement getPackage(@Nonnull final TypeElement element) {

        return processingEnv.getElementUtils().getPackageOf(element);
    }

    /**
     * Gets the generated source name.
     *
     * @param annotationElement the annotation element.
     * @param element           the annotated element.
     * @param targetElement     the target element.
     * @return the source name.
     */
    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getSourceName(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        return getGeneratedClassPackage(annotationElement, element, targetElement) + "."
                + getGeneratedClassPrefix(annotationElement, element, targetElement)
                + getGeneratedClassName(annotationElement, element, targetElement)
                + getGeneratedClassSuffix(annotationElement, element, targetElement);
    }

    /**
     * Returns the list of supported annotation elements.
     *
     * @return the elements of the supported annotations.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected List<TypeElement> getSupportedAnnotationElements() {

        if (mProxyElement == null) {

            mProxyElement = getTypeFromName("com.gh.bmd.jrt.proxy.annotation.Proxy");
        }

        return Collections.singletonList(mProxyElement);
    }

    /**
     * Gets the type element with the specified name.
     *
     * @param typeName the type name.
     * @return the type element.
     */
    protected TypeElement getTypeFromName(@Nonnull final String typeName) {

        return processingEnv.getElementUtils().getTypeElement(normalizeTypeName(typeName));
    }

    /**
     * Checks if the specified methods have the same parameters.
     *
     * @param firstMethodElement  the first method element.
     * @param secondMethodElement the second method element.
     * @return whether the methods have the same parameters.
     */
    protected boolean haveSameParameters(@Nonnull final ExecutableElement firstMethodElement,
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

    /**
     * Returns the normalized version of the specified type name.
     *
     * @param typeName the type name.
     * @return the normalized type name.
     */
    @Nonnull
    protected String normalizeTypeName(@Nonnull final String typeName) {

        if (typeName.endsWith(".class")) {

            return typeName.substring(0, typeName.length() - ".class".length());
        }

        return typeName;
    }

    /**
     * Parses the template resource with the specified path and returns it into a string.
     *
     * @param path the resource path.
     * @return the template as a string.
     * @throws java.io.IOException if an I/O error occurred.
     */
    @Nonnull
    @SuppressFBWarnings(value = "UI_INHERITANCE_UNSAFE_GETRESOURCE",
            justification = "inheritance of the processor class must be supported")
    protected String parseTemplate(@Nonnull final String path) throws IOException {

        final byte[] buffer = mByteBuffer;
        final InputStream resourceStream = getClass().getResourceAsStream(path);

        try {

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int length;

            while ((length = resourceStream.read(buffer)) > 0) {

                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            return outputStream.toString("UTF-8");

        } finally {

            try {

                resourceStream.close();

            } catch (final IOException ignored) {

            }
        }
    }

    @SuppressWarnings("PointlessBooleanExpression")
    private void createProxy(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement,
            @Nonnull final List<ExecutableElement> methodElements) {

        Writer writer = null;

        try {

            final Filer filer = processingEnv.getFiler();

            if (!DEBUG) {

                final JavaFileObject sourceFile = filer.createSourceFile(
                        getSourceName(annotationElement, element, targetElement));
                writer = sourceFile.openWriter();

            } else {

                writer = new StringWriter();
            }

            String header;
            header = getHeaderTemplate().replace("${packageName}",
                                                 getGeneratedClassPackage(annotationElement,
                                                                          element, targetElement));
            header = header.replace("${generatedClassName}",
                                    getGeneratedClassPrefix(annotationElement, element,
                                                            targetElement) +
                                            getGeneratedClassName(annotationElement, element,
                                                                  targetElement) +
                                            getGeneratedClassSuffix(annotationElement, element,
                                                                    targetElement));
            header = header.replace("${genericTypes}", buildGenericTypes(element));
            header = header.replace("${classFullName}", targetElement.asType().toString());
            header = header.replace("${interfaceFullName}", element.asType().toString());
            header = header.replace("${routineFieldsInit}",
                                    buildRoutineFieldsInit(methodElements.size()));
            writer.append(header);
            int count = 0;

            for (final ExecutableElement methodElement : methodElements) {

                ++count;
                writeMethod(writer, element, targetElement, methodElement, count);
            }

            writer.append(getFooterTemplate());

        } catch (final IOException e) {

            processingEnv.getMessager()
                         .printMessage(Kind.ERROR,
                                       "IOException while writing template; " + printStackTrace(e));
            throw new RuntimeException(e);

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
        final Alias asyncAnnotation = methodElement.getAnnotation(Alias.class);

        if (asyncAnnotation != null) {

            methodName = asyncAnnotation.value();

            for (final ExecutableElement targetMethodElement : ElementFilter.methodsIn(
                    targetElement.getEnclosedElements())) {

                final Alias targetAsyncAnnotation = targetMethodElement.getAnnotation(Alias.class);

                if ((targetAsyncAnnotation != null) && methodName.equals(
                        targetAsyncAnnotation.value())) {

                    targetMethod = targetMethodElement;

                    break;
                }
            }
        }

        if (targetMethod == null) {

            final Types typeUtils = processingEnv.getTypeUtils();
            final TypeMirror asyncAnnotationType =
                    getTypeFromName(Input.class.getCanonicalName()).asType();
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

                        if (variableElement.getAnnotation(Input.class) != null) {

                            value = getAnnotationValue(variableElement, asyncAnnotationType,
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

            throw new IllegalArgumentException(
                    "[" + methodElement + "] cannot find matching method in target class");
        }

        return targetMethod;
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

    private void writeMethod(@Nonnull final Writer writer, @Nonnull final TypeElement element,
            @Nonnull final TypeElement targetElement,
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        final Types typeUtils = processingEnv.getTypeUtils();
        final TypeMirror outputChannelType = this.outputChannelType;
        final TypeMirror listType = this.listType;
        final TypeMirror objectType = this.objectType;
        final ExecutableElement targetMethod = findMatchingMethod(methodElement, targetElement);
        TypeMirror targetReturnType = targetMethod.getReturnType();
        final boolean isVoid = (targetReturnType.getKind() == TypeKind.VOID);
        final Inputs inputsAnnotation = methodElement.getAnnotation(Inputs.class);
        final Output outputAnnotation = methodElement.getAnnotation(Output.class);
        InputMode inputMode = null;
        OutputMode outputMode = null;

        final List<? extends VariableElement> parameters = methodElement.getParameters();

        for (final VariableElement parameter : parameters) {

            final Input annotation = parameter.getAnnotation(Input.class);

            if (annotation == null) {

                continue;
            }

            inputMode = getInputMode(methodElement, annotation, parameter, parameters.size());
        }

        String method;

        if (inputsAnnotation != null) {

            inputMode = getInputsMode(methodElement, inputsAnnotation);
            outputMode = OutputMode.VALUE;
            method = getMethodInputsTemplate(methodElement, count);

        } else if (outputAnnotation != null) {

            outputMode = getOutputMode(methodElement, targetMethod);

            final TypeMirror returnType = methodElement.getReturnType();
            final TypeMirror returnTypeErasure = typeUtils.erasure(returnType);

            if (returnType.getKind() == TypeKind.ARRAY) {

                targetReturnType = ((ArrayType) returnType).getComponentType();
                method = ((inputMode == InputMode.ELEMENT) && !typeUtils.isAssignable(
                        methodElement.getParameters().get(0).asType(), outputChannelType))
                        ? getMethodParallelArrayTemplate(methodElement, count)
                        : getMethodArrayTemplate(methodElement, count);

            } else if (typeUtils.isAssignable(listType, returnTypeErasure)) {

                final List<? extends TypeMirror> typeArguments =
                        ((DeclaredType) returnType).getTypeArguments();

                if (typeArguments.isEmpty()) {

                    targetReturnType = objectType;

                } else {

                    targetReturnType = typeArguments.get(0);
                }

                method = ((inputMode == InputMode.ELEMENT) && !typeUtils.isAssignable(
                        methodElement.getParameters().get(0).asType(), outputChannelType))
                        ? getMethodParallelListTemplate(methodElement, count)
                        : getMethodListTemplate(methodElement, count);

            } else if (typeUtils.isAssignable(outputChannelType, returnTypeErasure)) {

                final List<? extends TypeMirror> typeArguments =
                        ((DeclaredType) returnType).getTypeArguments();

                if (typeArguments.isEmpty()) {

                    targetReturnType = objectType;

                } else {

                    targetReturnType = typeArguments.get(0);
                }

                method = ((inputMode == InputMode.ELEMENT) && !typeUtils.isAssignable(
                        methodElement.getParameters().get(0).asType(), outputChannelType))
                        ? getMethodParallelAsyncTemplate(methodElement, count)
                        : getMethodAsyncTemplate(methodElement, count);

            } else {

                throw new IllegalArgumentException("[" + methodElement + "] invalid return type");
            }

        } else if (isVoid) {

            method = ((inputMode == InputMode.ELEMENT) && !typeUtils.isAssignable(
                    methodElement.getParameters().get(0).asType(), outputChannelType))
                    ? getMethodParallelVoidTemplate(methodElement, count)
                    : getMethodVoidTemplate(methodElement, count);

        } else {

            targetReturnType = methodElement.getReturnType();
            method = ((inputMode == InputMode.ELEMENT) && !typeUtils.isAssignable(
                    methodElement.getParameters().get(0).asType(), outputChannelType))
                    ? getMethodParallelResultTemplate(methodElement, count)
                    : getMethodResultTemplate(methodElement, count);
        }

        final String resultClassName = getBoxedType(targetReturnType).toString();
        String methodHeader;
        methodHeader = getMethodHeaderTemplate(methodElement, count).replace("${resultClassName}",
                                                                             resultClassName);
        methodHeader = methodHeader.replace("${classFullName}", targetElement.asType().toString());
        methodHeader = methodHeader.replace("${methodCount}", Integer.toString(count));
        methodHeader = methodHeader.replace("${genericTypes}", buildGenericTypes(element));
        methodHeader = methodHeader.replace("${routineBuilderOptions}",
                                            buildRoutineOptions(methodElement, inputMode,
                                                                outputMode));

        final ShareGroup shareGroupAnnotation = methodElement.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            methodHeader = methodHeader.replace("${shareGroup}",
                                                "\"" + shareGroupAnnotation.value() + "\"");

        } else {

            methodHeader = methodHeader.replace("${shareGroup}", "mShareGroup");
        }

        writer.append(methodHeader);
        method = method.replace("${resultClassName}", resultClassName);
        method = method.replace("${resultRawClass}", targetReturnType.toString());
        method = method.replace("${resultRawSizedArray}", buildSizedArray(targetReturnType));
        method = method.replace("${resultType}", methodElement.getReturnType().toString());
        method = method.replace("${methodCount}", Integer.toString(count));
        method = method.replace("${methodName}", methodElement.getSimpleName());
        method = method.replace("${params}", buildParams(methodElement));
        method = method.replace("${paramTypes}", buildParamTypes(methodElement));
        method = method.replace("${inputParams}", buildInputParams(methodElement));
        method = method.replace("${outputOptions}", buildOutputOptions(methodElement));
        method = method.replace("${invokeMethod}",
                                (inputMode == InputMode.ELEMENT) ? "invokeParallel"
                                        : "invokeAsync");
        writer.append(method);
        String methodInvocation;

        if ((inputMode == InputMode.COLLECTION) && (
                targetMethod.getParameters().get(0).asType().getKind() == TypeKind.ARRAY)) {

            final ArrayType arrayType = (ArrayType) targetMethod.getParameters().get(0).asType();
            methodInvocation = (isVoid) ? getMethodArrayInvocationVoidTemplate(methodElement, count)
                    : (outputMode == OutputMode.ELEMENT)
                            ? getMethodArrayInvocationCollectionTemplate(methodElement, count)
                            : getMethodArrayInvocationTemplate(methodElement, count);
            methodInvocation = methodInvocation.replace("${componentType}",
                                                        arrayType.getComponentType().toString());
            methodInvocation = methodInvocation.replace("${boxedType}", getBoxedType(
                    arrayType.getComponentType()).toString());

        } else {

            methodInvocation = (isVoid) ? getMethodInvocationVoidTemplate(methodElement, count)
                    : (outputMode == OutputMode.ELEMENT) ? getMethodInvocationCollectionTemplate(
                            methodElement, count)
                            : getMethodInvocationTemplate(methodElement, count);
        }

        methodInvocation =
                methodInvocation.replace("${classFullName}", targetElement.asType().toString());
        methodInvocation = methodInvocation.replace("${resultClassName}", resultClassName);
        methodInvocation = methodInvocation.replace("${methodCount}", Integer.toString(count));
        methodInvocation = methodInvocation.replace("${genericTypes}", buildGenericTypes(element));
        methodInvocation = methodInvocation.replace("${methodName}", methodElement.getSimpleName());
        methodInvocation =
                methodInvocation.replace("${targetMethodName}", targetMethod.getSimpleName());

        if (inputMode == InputMode.COLLECTION) {

            methodInvocation = methodInvocation.replace("${maxParamSize}",
                                                        Integer.toString(Integer.MAX_VALUE));
            methodInvocation = methodInvocation.replace("${paramValues}",
                                                        buildCollectionParamValues(targetMethod));

        } else {

            methodInvocation = methodInvocation.replace("${maxParamSize}", Integer.toString(
                    targetMethod.getParameters().size()));
            methodInvocation =
                    methodInvocation.replace("${paramValues}", buildParamValues(targetMethod));
        }

        writer.append(methodInvocation);
    }
}
