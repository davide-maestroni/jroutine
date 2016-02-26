/*
 * Copyright 2016 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.android.object.core;

import com.github.dm.jrt.android.builder.LoaderConfiguration;
import com.github.dm.jrt.android.object.annotation.CacheStrategy;
import com.github.dm.jrt.android.object.annotation.ClashResolution;
import com.github.dm.jrt.android.object.annotation.InputClashResolution;
import com.github.dm.jrt.android.object.annotation.LoaderId;
import com.github.dm.jrt.android.object.annotation.ResultStaleTime;
import com.github.dm.jrt.android.object.annotation.RoutineId;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Utility class providing helper methods used to implement a builder of routines.<br/>
 * Specifically, this class provided several utilities to manage routines used to call object
 * methods asynchronously.
 * <p/>
 * Created by davide-maestroni on 01/29/2016.
 */
public class Builders extends com.github.dm.jrt.object.core.Builders {

    /**
     * Avoid direct instantiation.
     */
    protected Builders() {

    }

    /**
     * Returns a loader configuration properly modified by taking into account the annotations added
     * to the specified method.
     *
     * @param configuration the initial configuration.
     * @param annotations   the annotations.
     * @return the modified configuration.
     * @see com.github.dm.jrt.android.object.annotation.CacheStrategy CacheStrategy
     * @see com.github.dm.jrt.android.object.annotation.ClashResolution ClashResolution
     * @see com.github.dm.jrt.android.object.annotation.InputClashResolution InputClashResolution
     * @see com.github.dm.jrt.android.object.annotation.LoaderId LoaderId
     * @see com.github.dm.jrt.android.object.annotation.ResultStaleTime ResultStaleTime
     * @see com.github.dm.jrt.android.object.annotation.RoutineId RoutineId
     */
    @NotNull
    public static LoaderConfiguration configurationWithAnnotations(
            @Nullable final LoaderConfiguration configuration,
            @Nullable final Annotation... annotations) {

        final LoaderConfiguration.Builder<LoaderConfiguration> builder =
                LoaderConfiguration.builderFrom(configuration);
        if (annotations == null) {
            return builder.getConfigured();
        }

        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == LoaderId.class) {
                builder.withLoaderId(((LoaderId) annotation).value());

            } else if (annotationType == RoutineId.class) {
                builder.withRoutineId(((RoutineId) annotation).value());

            } else if (annotationType == ClashResolution.class) {
                builder.withClashResolution(((ClashResolution) annotation).value());

            } else if (annotationType == InputClashResolution.class) {
                builder.withInputClashResolution(((InputClashResolution) annotation).value());

            } else if (annotationType == CacheStrategy.class) {
                builder.withCacheStrategy(((CacheStrategy) annotation).value());

            } else if (annotationType == ResultStaleTime.class) {
                final ResultStaleTime timeAnnotation = (ResultStaleTime) annotation;
                builder.withResultStaleTime(timeAnnotation.value(), timeAnnotation.unit());
            }
        }

        return builder.getConfigured();
    }

    /**
     * Returns a loader configuration properly modified by taking into account the annotations added
     * to the specified method.
     *
     * @param configuration the initial configuration.
     * @param method        the target method.
     * @return the modified configuration.
     * @see com.github.dm.jrt.android.object.annotation.CacheStrategy CacheStrategy
     * @see com.github.dm.jrt.android.object.annotation.ClashResolution ClashResolution
     * @see com.github.dm.jrt.android.object.annotation.InputClashResolution InputClashResolution
     * @see com.github.dm.jrt.android.object.annotation.LoaderId LoaderId
     * @see com.github.dm.jrt.android.object.annotation.ResultStaleTime ResultStaleTime
     * @see com.github.dm.jrt.android.object.annotation.RoutineId RoutineId
     */
    @NotNull
    public static LoaderConfiguration configurationWithAnnotations(
            @Nullable final LoaderConfiguration configuration, @NotNull final Method method) {

        return configurationWithAnnotations(configuration, method.getDeclaredAnnotations());
    }
}
