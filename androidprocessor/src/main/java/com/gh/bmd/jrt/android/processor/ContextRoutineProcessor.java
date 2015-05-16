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
package com.gh.bmd.jrt.android.processor;

import com.gh.bmd.jrt.processor.RoutineProcessor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Annotation processor used to generate proxy classes enabling method asynchronous invocations,
 * bound to a context lifecycle.
 * <p/>
 * Created by davide on 06/05/15.
 */
public class ContextRoutineProcessor extends RoutineProcessor {

    private TypeElement mCurrentAnnotationElement;

    private String mHeaderService;

    private String mHeaderV11;

    private String mHeaderV4;

    private String mMethodArrayInvocation;

    private String mMethodArrayInvocationCollection;

    private String mMethodArrayInvocationVoid;

    private String mMethodHeader;

    private String mMethodHeaderV1;

    private String mMethodInvocation;

    private String mMethodInvocationCollection;

    private String mMethodInvocationVoid;

    private TypeElement mServiceProxyElement;

    private TypeElement mV11ProxyElement;

    private TypeElement mV4ProxyElement;

    @Nonnull
    @Override
    protected String buildRoutineFieldsInit(final int size) {

        final TypeElement serviceProxyElement = mServiceProxyElement;
        final TypeElement annotationElement = mCurrentAnnotationElement;

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= size; i++) {

            builder.append("mRoutine").append(i).append(" = ").append("initRoutine").append(i);

            if (annotationElement == serviceProxyElement) {

                builder.append("(routineConfiguration, serviceConfiguration);");

            } else {

                builder.append("(routineConfiguration, loaderConfiguration);");
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
    protected String getMethodArrayInvocationCollectionTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodArrayInvocationCollection == null) {

            mMethodArrayInvocationCollection =
                    parseTemplate("/android/templates/method_array_invocation_collection.txt");
        }

        return mMethodArrayInvocationCollection;
    }

    @Nonnull
    @Override
    protected String getMethodArrayInvocationTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodArrayInvocation == null) {

            mMethodArrayInvocation =
                    parseTemplate("/android/templates/method_array_invocation.txt");
        }

        return mMethodArrayInvocation;
    }

    @Nonnull
    @Override
    @SuppressWarnings("UnusedParameters")
    protected String getMethodArrayInvocationVoidTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodArrayInvocationVoid == null) {

            mMethodArrayInvocationVoid =
                    parseTemplate("/android/templates/method_array_invocation_void.txt");
        }

        return mMethodArrayInvocationVoid;
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
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInvocationCollectionTemplate(
            @Nonnull final ExecutableElement methodElement, final int count) throws IOException {

        if (mMethodInvocationCollection == null) {

            mMethodInvocationCollection =
                    parseTemplate("/android/templates/method_invocation_collection.txt");
        }

        return mMethodInvocationCollection;
    }

    @Nonnull
    @Override
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInvocationTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodInvocation == null) {

            mMethodInvocation = parseTemplate("/android/templates/method_invocation.txt");
        }

        return mMethodInvocation;
    }

    @Nonnull
    @Override
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInvocationVoidTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodInvocationVoid == null) {

            mMethodInvocationVoid = parseTemplate("/android/templates/method_invocation_void.txt");
        }

        return mMethodInvocationVoid;
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
                    getTypeFromName("com.gh.bmd.jrt.android.proxy.annotation.ServiceProxy");
            mV4ProxyElement = getTypeFromName("com.gh.bmd.jrt.android.proxy.annotation.V4Proxy");
            mV11ProxyElement = getTypeFromName("com.gh.bmd.jrt.android.proxy.annotation.V11Proxy");
        }

        return Arrays.asList(mServiceProxyElement, mV4ProxyElement, mV11ProxyElement);
    }

    @SuppressWarnings("unchecked")
    private String buildInvocationOptions(final ExecutableElement methodElement) throws
            IOException {

        // We need to avoid explicit dependency on the android module...
        final StringBuilder builder = new StringBuilder();
        final TypeElement idAnnotationElement =
                getTypeFromName("com.gh.bmd.jrt.android.annotation.LoaderId");
        final Integer id =
                (Integer) getAnnotationValue(methodElement, idAnnotationElement.asType(), "value");

        if (id != null) {

            builder.append(".withId(").append(id).append(")");
        }

        final TypeElement clashAnnotationElement =
                getTypeFromName("com.gh.bmd.jrt.android.annotation.ClashResolution");
        final Object resolutionType =
                getAnnotationValue(methodElement, clashAnnotationElement.asType(), "value");

        if (resolutionType != null) {

            builder.append(".withClashResolution(com.gh.bmd.jrt.android.builder.LoaderConfiguration"
                                   + ".ClashResolutionType.").append(resolutionType).append(")");
        }

        final TypeElement cacheAnnotationElement =
                getTypeFromName("com.gh.bmd.jrt.android.annotation.CacheStrategy");
        final Object strategyType =
                getAnnotationValue(methodElement, cacheAnnotationElement.asType(), "value");

        if (strategyType != null) {

            builder.append(".withCacheStrategy(com.gh.bmd.jrt.android.builder.LoaderConfiguration"
                                   + ".CacheStrategyType.").append(strategyType).append(")");
        }

        return builder.toString();
    }
}
