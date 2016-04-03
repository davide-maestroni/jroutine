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

package com.github.dm.jrt.android.core.builder;

import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.core.util.TimeDuration;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.android.core.config.LoaderConfiguration.builder;
import static com.github.dm.jrt.android.core.config.LoaderConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader invocation configuration unit tests.
 * <p>
 * Created by davide-maestroni on 04/22/2015.
 */
public class LoaderConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withFactoryId(71)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .setConfiguration();
        assertThat(configuration.builderFrom().setConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).setConfiguration()).isEqualTo(
                LoaderConfiguration.defaultConfiguration());
    }

    @SuppressWarnings("ConstantConditions")
    public void testBuildNullPointerError() {

        try {

            new Builder<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new Builder<Object>(null, LoaderConfiguration.defaultConfiguration());

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    public void testBuilderFromEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withFactoryId(71)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .setConfiguration();
        assertThat(builder().with(configuration).setConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().setConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).setConfiguration()).isEqualTo(
                LoaderConfiguration.defaultConfiguration());
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
                                                           .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CLEAR).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                                .setConfiguration()).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR).setConfiguration());
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
                                                           .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.ABORT_THIS).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withClashResolution(ClashResolutionType.JOIN)
                                .setConfiguration()).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.JOIN).setConfiguration());
    }

    public void testFactoryIdEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withFactoryId(71)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .setConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withFactoryId(3).setConfiguration());
        assertThat(configuration.builderFrom().withFactoryId(27).setConfiguration()).isNotEqualTo(
                builder().withFactoryId(27).setConfiguration());
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
                                                           .setConfiguration();
        assertThat(configuration).isNotEqualTo(builder().withLoaderId(3).setConfiguration());
        assertThat(configuration.builderFrom().withLoaderId(27).setConfiguration()).isNotEqualTo(
                builder().withLoaderId(27).setConfiguration());
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
                                                           .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.ABORT_THIS)
                         .setConfiguration());
        assertThat(configuration.builderFrom()
                                .withInputClashResolution(ClashResolutionType.JOIN)
                                .setConfiguration()).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.JOIN).setConfiguration());
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
                                                           .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withResultLooper(new Handler().getLooper()).setConfiguration());
        final Looper looper = new Handler().getLooper();
        assertThat(configuration.builderFrom()
                                .withResultLooper(looper)
                                .setConfiguration()).isNotEqualTo(
                builder().withResultLooper(looper).setConfiguration());
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
                                                           .setConfiguration();
        assertThat(configuration).isNotEqualTo(
                builder().withResultStaleTime(TimeDuration.days(3)).setConfiguration());
        assertThat(configuration.builderFrom()
                                .withResultStaleTime(TimeDuration.hours(7))
                                .setConfiguration()).isNotEqualTo(
                builder().withResultStaleTime(TimeDuration.hours(7)).setConfiguration());
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
