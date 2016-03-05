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

package com.github.dm.jrt.android.builder;

import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.github.dm.jrt.android.builder.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.builder.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.core.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.builder.LoaderConfiguration.builder;
import static com.github.dm.jrt.android.builder.LoaderConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader invocation configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 04/22/2015.
 */
public class LoaderConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withRoutineId(71)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration.builderFrom().getConfigured()).isEqualTo(configuration);
        assertThat(builderFrom(null).getConfigured()).isEqualTo(
                LoaderConfiguration.DEFAULT_CONFIGURATION);
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuildNullPointerError() {

        try {

            new Builder<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new Builder<Object>(null, LoaderConfiguration.DEFAULT_CONFIGURATION);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testBuilderFromEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withRoutineId(71)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(builder().with(configuration).getConfigured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().getConfigured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).getConfigured()).isEqualTo(
                LoaderConfiguration.DEFAULT_CONFIGURATION);
    }

    public void testCacheStrategyEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CLEAR).getConfigured());
        assertThat(configuration.builderFrom()
                                .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                                .getConfigured()).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR).getConfigured());
    }

    public void testClashResolutionEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.ABORT_THIS).getConfigured());
        assertThat(configuration.builderFrom()
                                .withClashResolution(ClashResolutionType.JOIN)
                                .getConfigured()).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.JOIN).getConfigured());
    }

    public void testIdEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration).isNotEqualTo(builder().withLoaderId(3).getConfigured());
        assertThat(configuration.builderFrom().withLoaderId(27).getConfigured()).isNotEqualTo(
                builder().withLoaderId(27).getConfigured());
    }

    public void testInputClashResolutionEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.ABORT_THIS).getConfigured());
        assertThat(configuration.builderFrom()
                                .withInputClashResolution(ClashResolutionType.JOIN)
                                .getConfigured()).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.JOIN).getConfigured());
    }

    public void testLooperEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration).isNotEqualTo(
                builder().withResultLooper(new Handler().getLooper()).getConfigured());
        final Looper looper = new Handler().getLooper();
        assertThat(
                configuration.builderFrom().withResultLooper(looper).getConfigured()).isNotEqualTo(
                builder().withResultLooper(looper).getConfigured());
    }

    public void testRoutineIdEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withRoutineId(71)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration).isNotEqualTo(builder().withRoutineId(3).getConfigured());
        assertThat(configuration.builderFrom().withRoutineId(27).getConfigured()).isNotEqualTo(
                builder().withRoutineId(27).getConfigured());
    }

    public void testStaleTimeEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .getConfigured();
        assertThat(configuration).isNotEqualTo(
                builder().withResultStaleTime(TimeDuration.days(3)).getConfigured());
        assertThat(configuration.builderFrom()
                                .withResultStaleTime(TimeDuration.hours(7))
                                .getConfigured()).isNotEqualTo(
                builder().withResultStaleTime(TimeDuration.hours(7)).getConfigured());
    }

    @SuppressWarnings("ConstantConditions")
    public void testStaleTimeErrors() {

        try {

            builder().withResultStaleTime(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withResultStaleTime(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }
}
