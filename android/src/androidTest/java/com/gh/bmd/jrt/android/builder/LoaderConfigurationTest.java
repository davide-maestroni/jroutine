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
package com.gh.bmd.jrt.android.builder;

import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;

import com.gh.bmd.jrt.android.builder.LoaderConfiguration.Builder;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration.ClashResolutionType;

import static com.gh.bmd.jrt.android.builder.LoaderConfiguration.builder;
import static com.gh.bmd.jrt.android.builder.LoaderConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loader invocation configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 22/04/15.
 */
public class LoaderConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .set();
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(builderFrom(null).set()).isEqualTo(LoaderConfiguration.DEFAULT_CONFIGURATION);
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
        final LoaderConfiguration configuration = builder().withId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                LoaderConfiguration.DEFAULT_CONFIGURATION);
    }

    public void testCacheStrategyEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .set();
        assertThat(configuration).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CLEAR).set());
        assertThat(configuration.builderFrom()
                                .withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                                .set()).isNotEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR).set());
    }

    public void testClashResolutionEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .set();
        assertThat(configuration).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.ABORT_THIS).set());
        assertThat(configuration.builderFrom()
                                .withClashResolution(ClashResolutionType.KEEP_THAT)
                                .set()).isNotEqualTo(
                builder().withClashResolution(ClashResolutionType.KEEP_THAT).set());
    }

    public void testIdEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .set();
        assertThat(configuration).isNotEqualTo(builder().withId(3).set());
        assertThat(configuration.builderFrom().withId(27).set()).isNotEqualTo(
                builder().withId(27).set());
    }

    public void testLooperEquals() {

        final ClashResolutionType resolutionType = ClashResolutionType.ABORT_THAT;
        final CacheStrategyType strategyType = CacheStrategyType.CACHE;
        final LoaderConfiguration configuration = builder().withId(-1)
                                                           .withClashResolution(resolutionType)
                                                           .withCacheStrategy(strategyType)
                                                           .withResultLooper(Looper.getMainLooper())
                                                           .set();
        assertThat(configuration).isNotEqualTo(
                builder().withResultLooper(new Handler().getLooper()).set());
        final Looper looper = new Handler().getLooper();
        assertThat(configuration.builderFrom().withResultLooper(looper).set()).isNotEqualTo(
                builder().withResultLooper(looper).set());
    }
}
