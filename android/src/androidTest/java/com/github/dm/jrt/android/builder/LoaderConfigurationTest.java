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
import com.github.dm.jrt.util.TimeDuration;

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
                                                           .configured();
        assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
        assertThat(builderFrom(null).configured()).isEqualTo(
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
                                                           .configured();
        assertThat(builder().with(configuration).configured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).configured()).isEqualTo(
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
                                                           .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CLEAR).configured());
        assertThat(configuration.builderFrom()
                                .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                                .configured()).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR).configured());
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
                                                           .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.ABORT_THIS).configured());
        assertThat(configuration.builderFrom()
                                .withClashResolution(ClashResolutionType.JOIN)
                                .configured()).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.JOIN).configured());
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
                                                           .configured();
        assertThat(configuration).isNotEqualTo(builder().withLoaderId(3).configured());
        assertThat(configuration.builderFrom().withLoaderId(27).configured()).isNotEqualTo(
                builder().withLoaderId(27).configured());
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
                                                           .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.ABORT_THIS).configured());
        assertThat(configuration.builderFrom()
                                .withInputClashResolution(ClashResolutionType.JOIN)
                                .configured()).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.JOIN).configured());
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
                                                           .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withResultLooper(new Handler().getLooper()).configured());
        final Looper looper = new Handler().getLooper();
        assertThat(configuration.builderFrom().withResultLooper(looper).configured()).isNotEqualTo(
                builder().withResultLooper(looper).configured());
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
                                                           .configured();
        assertThat(configuration).isNotEqualTo(builder().withRoutineId(3).configured());
        assertThat(configuration.builderFrom().withRoutineId(27).configured()).isNotEqualTo(
                builder().withRoutineId(27).configured());
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
                                                           .configured();
        assertThat(configuration).isNotEqualTo(
                builder().withResultStaleTime(TimeDuration.days(3)).configured());
        assertThat(
                configuration.builderFrom().withResultStaleTime(TimeDuration.hours(7)).configured())
                .isNotEqualTo(builder().withResultStaleTime(TimeDuration.hours(7)).configured());
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
