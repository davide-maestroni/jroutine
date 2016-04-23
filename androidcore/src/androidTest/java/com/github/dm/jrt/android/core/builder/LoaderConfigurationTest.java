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

import android.test.AndroidTestCase;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.core.util.UnitDuration;

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
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(builderFrom(null).apply()).isEqualTo(LoaderConfiguration.defaultConfiguration());
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
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(builder().with(configuration).apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).apply()).isEqualTo(
                LoaderConfiguration.defaultConfiguration());
    }

    public void testCacheStrategyEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CLEAR).apply());
        assertThat(configuration.builderFrom()
                                .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                                .apply()).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR).apply());
    }

    public void testClashResolutionEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.ABORT_THIS).apply());
        assertThat(configuration.builderFrom()
                                .withClashResolution(ClashResolutionType.JOIN)
                                .apply()).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.JOIN).apply());
    }

    public void testFactoryIdEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withFactoryId(71)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(configuration).isNotEqualTo(builder().withFactoryId(3).apply());
        assertThat(configuration.builderFrom().withFactoryId(27).apply()).isNotEqualTo(
                builder().withFactoryId(27).apply());
    }

    public void testIdEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(configuration).isNotEqualTo(builder().withLoaderId(3).apply());
        assertThat(configuration.builderFrom().withLoaderId(27).apply()).isNotEqualTo(
                builder().withLoaderId(27).apply());
    }

    public void testInputClashResolutionEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.ABORT_THIS).apply());
        assertThat(configuration.builderFrom()
                                .withInputClashResolution(ClashResolutionType.JOIN)
                                .apply()).isNotEqualTo(
                builder().withInputClashResolution(ClashResolutionType.JOIN).apply());
    }

    public void testStaleTimeEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withLoaderId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withInputClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultStaleTime(1, TimeUnit.SECONDS)
                                                           .apply();
        assertThat(configuration).isNotEqualTo(
                builder().withResultStaleTime(UnitDuration.days(3)).apply());
        assertThat(configuration.builderFrom()
                                .withResultStaleTime(UnitDuration.hours(7))
                                .apply()).isNotEqualTo(
                builder().withResultStaleTime(UnitDuration.hours(7)).apply());
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
