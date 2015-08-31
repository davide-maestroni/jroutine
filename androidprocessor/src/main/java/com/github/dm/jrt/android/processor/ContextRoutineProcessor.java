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
package com.github.dm.jrt.android.processor;

import com.github.dm.jrt.processor.RoutineProcessor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Annotation processor used to generate proxy classes enabling method asynchronous invocations,
 * by leveraging Android platform specific classes.
 * <p/>
 * Created by davide-maestroni on 06/05/15.
 */
public class ContextRoutineProcessor extends RoutineProcessor {

    private TypeMirror mCacheAnnotationType;

    private TypeMirror mClashAnnotationType;

    private TypeElement mCurrentAnnotationElement;

    private String mHeaderService;

    private String mHeaderV11;

    private String mHeaderV4;

    private TypeMirror mIdAnnotationType;

    private TypeMirror mInputClashAnnotationType;

    private String mMethodHeader;

    private String mMethodHeaderV1;

    private String mMethodInvocationFooter;

    private String mMethodInvocationHeader;

    private TypeElement mServiceProxyElement;

    private TypeMirror mStaleTimeAnnotationType;

    private TypeElement mV11ProxyElement;

    private TypeElement mV4ProxyElement;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {

        super.init(processingEnv);
        mIdAnnotationType =
                getTypeFromName("com.github.dm.jrt.android.annotation.LoaderId").asType();
        mClashAnnotationType =
                getTypeFromName("com.github.dm.jrt.android.annotation.ClashResolution").asType();
        mInputClashAnnotationType = getTypeFromName(
                "com.github.dm.jrt.android.annotation.InputClashResolution").asType();
        mCacheAnnotationType =
                getTypeFromName("com.github.dm.jrt.android.annotation.CacheStrategy").asType();
        mStaleTimeAnnotationType =
                getTypeFromName("com.github.dm.jrt.android.annotation.StaleTime").asType();
    }

    @Nonnull
    @Override
    protected String buildRoutineFieldsInit(final int size) {

        final TypeElement serviceProxyElement = mServiceProxyElement;
        final TypeElement annotationElement = mCurrentAnnotationElement;

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= size; i++) {

            builder.append("mRoutine").append(i).append(" = ").append("initRoutine").append(i);

            if (annotationElement == serviceProxyElement) {

                builder.append("(target, invocationConfiguration, proxyConfiguration, "
                                       + "serviceConfiguration);");

            } else {

                builder.append("(target, invocationConfiguration, proxyConfiguration, "
                                       + "loaderConfiguration);");
            }

            builder.append(NEW_LINE);
        }

        return builder.toString();
    }

    @Nonnull
    @Override
    @SuppressWarnings("UnusedParameters")
    protected String getHeaderTemplate() throws IOException {

        final TypeElement serviceProxyElement = mServiceProxyElement;
        final TypeElement v4ProxyElement = mV4ProxyElement;
        final TypeElement v11ProxyElement = mV11ProxyElement;
        final TypeElement annotationElement = mCurrentAnnotationElement;

        if (annotationElement == serviceProxyElement) {

            if (mHeaderService == null) {

                mHeaderService = parseTemplate("/android/templates/header.txt");
            }

            return mHeaderService;

        } else if (annotationElement == v4ProxyElement) {

            if (mHeaderV4 == null) {

                mHeaderV4 = parseTemplate("/android/v4/templates/header.txt");
            }

            return mHeaderV4;

        } else if (annotationElement == v11ProxyElement) {

            if (mHeaderV11 == null) {

                mHeaderV11 = parseTemplate("/android/v11/templates/header.txt");
            }

            return mHeaderV11;
        }

        return super.getHeaderTemplate();
    }

    @Nonnull
    @Override
    @SuppressWarnings("UnusedParameters")
    protected String getMethodHeaderTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mCurrentAnnotationElement != mServiceProxyElement) {

            if (mMethodHeaderV1 == null) {

                mMethodHeaderV1 = parseTemplate("/android/v1/templates/method_header.txt");
            }

            return mMethodHeaderV1.replace("${invocationBuilderOptions}",
                                           buildInvocationOptions(methodElement));
        }

        if (mMethodHeader == null) {

            mMethodHeader = parseTemplate("/android/templates/method_header.txt");
        }

        return mMethodHeader;
    }

    @Nonnull
    @Override
    protected String getMethodInvocationFooterTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodInvocationFooter == null) {

            mMethodInvocationFooter =
                    parseTemplate("/android/templates/method_invocation_footer.txt");
        }

        return mMethodInvocationFooter;
    }

    @Nonnull
    @Override
    protected String getMethodInvocationHeaderTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodInvocationHeader == null) {

            mMethodInvocationHeader =
                    parseTemplate("/android/templates/method_invocation_header.txt");
        }

        return mMethodInvocationHeader;
    }

    @Nonnull
    @Override
    protected String getSourceName(@Nonnull final TypeElement annotationElement,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        mCurrentAnnotationElement = annotationElement;
        return super.getSourceName(annotationElement, element, targetElement);
    }

    @Nonnull
    @Override
    protected List<TypeElement> getSupportedAnnotationElements() {

        if ((mServiceProxyElement == null) || (mV4ProxyElement == null) || (mV11ProxyElement
                == null)) {

            mServiceProxyElement =
                    getTypeFromName("com.github.dm.jrt.android.proxy.annotation.ServiceProxy");
            mV4ProxyElement = getTypeFromName("com.github.dm.jrt.android.proxy.annotation.V4Proxy");
            mV11ProxyElement =
                    getTypeFromName("com.github.dm.jrt.android.proxy.annotation.V11Proxy");
        }

        return Arrays.asList(mServiceProxyElement, mV4ProxyElement, mV11ProxyElement);
    }

    @SuppressWarnings("unchecked")
    private String buildInvocationOptions(final ExecutableElement methodElement) throws
            IOException {

        // We need to avoid explicit dependency on the android module...
        final StringBuilder builder = new StringBuilder();
        final Integer id = (Integer) getAnnotationValue(methodElement, mIdAnnotationType, "value");

        if (id != null) {

            builder.append(".withId(").append(id).append(")");
        }

        final Object resolutionType =
                getAnnotationValue(methodElement, mClashAnnotationType, "value");

        if (resolutionType != null) {

            builder.append(
                    ".withClashResolution(com.github.dm.jrt.android.builder.LoaderConfiguration"
                            + ".ClashResolutionType.").append(resolutionType).append(")");
        }

        final Object inputResolutionType =
                getAnnotationValue(methodElement, mInputClashAnnotationType, "value");

        if (inputResolutionType != null) {

            builder.append(
                    ".withInputClashResolution(com.github.dm.jrt.android.builder"
                            + ".LoaderConfiguration"
                            + ".ClashResolutionType.").append(resolutionType).append(")");
        }

        final Object strategyType =
                getAnnotationValue(methodElement, mCacheAnnotationType, "value");

        if (strategyType != null) {

            builder.append(
                    ".withCacheStrategy(com.github.dm.jrt.android.builder.LoaderConfiguration"
                            + ".CacheStrategyType.").append(strategyType).append(")");
        }

        final TypeMirror staleTimeAnnotationType = mStaleTimeAnnotationType;
        final Object staleTime =
                getAnnotationValue(methodElement, staleTimeAnnotationType, "value");

        if (staleTime != null) {

            final Object staleTimeUnit =
                    getAnnotationValue(methodElement, staleTimeAnnotationType, "unit");

            builder.append(".withResultStaleTime(")
                   .append(staleTime)
                   .append(", ")
                   .append(TimeUnit.class.getCanonicalName())
                   .append(".")
                   .append((staleTimeUnit != null) ? staleTimeUnit : TimeUnit.MILLISECONDS)
                   .append(")");
        }

        return builder.toString();
    }
}
