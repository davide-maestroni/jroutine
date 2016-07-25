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

package com.github.dm.jrt.android.object.builder;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.object.annotation.CacheStrategy;
import com.github.dm.jrt.android.object.annotation.ClashResolution;
import com.github.dm.jrt.android.object.annotation.FactoryId;
import com.github.dm.jrt.android.object.annotation.LoaderId;
import com.github.dm.jrt.android.object.annotation.LogClass;
import com.github.dm.jrt.android.object.annotation.MatchResolution;
import com.github.dm.jrt.android.object.annotation.ResultStaleTime;
import com.github.dm.jrt.android.object.annotation.RunnerClass;
import com.github.dm.jrt.core.util.ConstantConditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Utility class providing helper methods used to implement a builder of routines.
 * <br>
 * Specifically, this class provided several utilities to manage routines used to call object
 * methods asynchronously.
 * <p>
 * Created by davide-maestroni on 01/29/2016.
 */
public class AndroidBuilders {

    /**
     * Avoid explicit instantiation.
     */
    protected AndroidBuilders() {
        ConstantConditions.avoid();
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
     * @see com.github.dm.jrt.android.object.annotation.FactoryId FactoryId
     * @see com.github.dm.jrt.android.object.annotation.MatchResolution MatchResolution
     * @see com.github.dm.jrt.android.object.annotation.LoaderId LoaderId
     * @see com.github.dm.jrt.android.object.annotation.ResultStaleTime ResultStaleTime
     */
    @NotNull
    public static LoaderConfiguration withAnnotations(
            @Nullable final LoaderConfiguration configuration,
            @Nullable final Annotation... annotations) {
        final LoaderConfiguration.Builder<LoaderConfiguration> builder =
                LoaderConfiguration.builderFrom(configuration);
        if (annotations == null) {
            return builder.configured();
        }

        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == LoaderId.class) {
                builder.withLoaderId(((LoaderId) annotation).value());

            } else if (annotationType == FactoryId.class) {
                builder.withFactoryId(((FactoryId) annotation).value());

            } else if (annotationType == ClashResolution.class) {
                builder.withClashResolution(((ClashResolution) annotation).value());

            } else if (annotationType == MatchResolution.class) {
                builder.withMatchResolution(((MatchResolution) annotation).value());

            } else if (annotationType == CacheStrategy.class) {
                builder.withCacheStrategy(((CacheStrategy) annotation).value());

            } else if (annotationType == ResultStaleTime.class) {
                final ResultStaleTime timeAnnotation = (ResultStaleTime) annotation;
                builder.withResultStaleTime(timeAnnotation.value(), timeAnnotation.unit());
            }
        }

        return builder.configured();
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
     * @see com.github.dm.jrt.android.object.annotation.FactoryId FactoryId
     * @see com.github.dm.jrt.android.object.annotation.MatchResolution MatchResolution
     * @see com.github.dm.jrt.android.object.annotation.LoaderId LoaderId
     * @see com.github.dm.jrt.android.object.annotation.ResultStaleTime ResultStaleTime
     */
    @NotNull
    public static LoaderConfiguration withAnnotations(
            @Nullable final LoaderConfiguration configuration, @NotNull final Method method) {
        return withAnnotations(configuration, method.getDeclaredAnnotations());
    }

    /**
     * Returns a service configuration properly modified by taking into account the annotations
     * added to the specified method.
     *
     * @param configuration the initial configuration.
     * @param annotations   the annotations.
     * @return the modified configuration.
     * @see com.github.dm.jrt.android.object.annotation.LogClass LogClass
     * @see com.github.dm.jrt.android.object.annotation.RunnerClass RunnerClass
     */
    @NotNull
    public static ServiceConfiguration withAnnotations(
            @Nullable final ServiceConfiguration configuration,
            @Nullable final Annotation... annotations) {
        final ServiceConfiguration.Builder<ServiceConfiguration> builder =
                ServiceConfiguration.builderFrom(configuration);
        if (annotations == null) {
            return builder.configured();
        }

        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == LogClass.class) {
                builder.withLogClass(((LogClass) annotation).value()).withLogArgs((Object[]) null);

            } else if (annotationType == RunnerClass.class) {
                builder.withRunnerClass(((RunnerClass) annotation).value())
                       .withRunnerArgs((Object[]) null);
            }
        }

        return builder.configured();
    }

    /**
     * Returns a service configuration properly modified by taking into account the annotations
     * added to the specified method.
     *
     * @param configuration the initial configuration.
     * @param method        the target method.
     * @return the modified configuration.
     * @see com.github.dm.jrt.android.object.annotation.LogClass LogClass
     * @see com.github.dm.jrt.android.object.annotation.RunnerClass RunnerClass
     */
    @NotNull
    public static ServiceConfiguration withAnnotations(
            @Nullable final ServiceConfiguration configuration, @NotNull final Method method) {
        return withAnnotations(configuration, method.getDeclaredAnnotations());
    }
}
