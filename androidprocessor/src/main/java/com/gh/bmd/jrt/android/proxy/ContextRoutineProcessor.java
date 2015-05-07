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
package com.gh.bmd.jrt.android.proxy;

import com.gh.bmd.jrt.processor.RoutineProcessor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Created by davide on 06/05/15.
 */
public class ContextRoutineProcessor extends RoutineProcessor {

    private static final HashSet<String> SUPPORTED_ANNOTATIONS = new HashSet<String>(
            Arrays.asList(com.gh.bmd.jrt.android.proxy.v4.annotation.ContextProxy.class.getName(),
                          com.gh.bmd.jrt.android.proxy.v11.annotation.ContextProxy.class.getName
                                  ()));

    private Class<? extends Annotation> mCurrentAnnotationClass;

    private String mHeaderV11;

    private String mHeaderV4;

    private String mMethodArrayInvocation;

    private String mMethodArrayInvocationCollection;

    private String mMethodArrayInvocationVoid;

    private String mMethodHeader;

    private String mMethodInvocation;

    private String mMethodInvocationCollection;

    private String mMethodInvocationVoid;

    @Override
    public Set<String> getSupportedAnnotationTypes() {

        return Collections.unmodifiableSet(SUPPORTED_ANNOTATIONS);
    }

    @Nonnull
    protected String buildRoutineFieldsInit(final int size) {

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= size; i++) {

            builder.append("mRoutine")
                   .append(i)
                   .append(" = ")
                   .append("initRoutine")
                   .append(i)
                   .append("(routineConfiguration, invocationConfiguration);")
                   .append(NEW_LINE);
        }

        return builder.toString();
    }

    @Nonnull
    @SuppressWarnings("UnusedParameters")
    protected String getHeaderTemplate() throws IOException {

        final Class<? extends Annotation> annotationClass = mCurrentAnnotationClass;

        if (annotationClass == com.gh.bmd.jrt.android.proxy.v4.annotation.ContextProxy.class) {

            if (mHeaderV4 == null) {

                mHeaderV4 = parseTemplate("/android/v4/templates/header.txt");
            }

            return mHeaderV4;

        } else if (annotationClass
                == com.gh.bmd.jrt.android.proxy.v11.annotation.ContextProxy.class) {

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
    @SuppressWarnings("UnusedParameters")
    protected String getMethodHeaderTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodHeader == null) {

            mMethodHeader = parseTemplate("/android/templates/method_header.txt");
        }

        return mMethodHeader;
    }

    @Nonnull
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
    @SuppressWarnings("UnusedParameters")
    protected String getMethodInvocationTemplate(@Nonnull final ExecutableElement methodElement,
            final int count) throws IOException {

        if (mMethodInvocation == null) {

            mMethodInvocation = parseTemplate("/android/templates/method_invocation.txt");
        }

        return mMethodInvocation;
    }

    @Nonnull
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
    protected String getSourceName(@Nonnull final Class<? extends Annotation> annotationClass,
            @Nonnull final TypeElement element, @Nonnull final TypeElement targetElement) {

        mCurrentAnnotationClass = annotationClass;

        final String packageName = getPackage(element).getQualifiedName().toString();
        final String interfaceName = element.getSimpleName().toString();
        String prefix = "";

        if (annotationClass == com.gh.bmd.jrt.android.proxy.v4.annotation.ContextProxy.class) {

            prefix = ".JRoutineProxyV4_";

        } else if (annotationClass
                == com.gh.bmd.jrt.android.proxy.v11.annotation.ContextProxy.class) {

            prefix = ".JRoutineProxyV11_";
        }

        return packageName + prefix + interfaceName;
    }
}
