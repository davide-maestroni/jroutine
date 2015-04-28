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

import android.test.AndroidTestCase;

import com.gh.bmd.jrt.android.builder.InvocationConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.InvocationConfiguration.ClashResolutionType;

import static com.gh.bmd.jrt.android.builder.InvocationConfiguration.builder;
import static com.gh.bmd.jrt.android.builder.InvocationConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation configuration unit tests.
 * <p/>
 * Created by davide on 22/04/15.
 */
public class InvocationConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final InvocationConfiguration configuration = builder().withId(-1)
                                                               .onClash(
                                                                       ClashResolutionType
                                                                               .ABORT_THAT)
                                                               .onComplete(CacheStrategyType.CACHE)
                                                               .apply();

        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(builderFrom(null).apply()).isEqualTo(
                InvocationConfiguration.DEFAULT_CONFIGURATION);
    }

    public void testBuilderFromEquals() {

        final InvocationConfiguration configuration = builder().withId(-1)
                                                               .onClash(
                                                                       ClashResolutionType
                                                                               .ABORT_THAT)
                                                               .onComplete(CacheStrategyType.CACHE)
                                                               .apply();
        assertThat(builder().with(configuration).apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).apply()).isEqualTo(
                InvocationConfiguration.DEFAULT_CONFIGURATION);
    }

    public void testCacheStrategyEquals() {

        final InvocationConfiguration configuration = builder().withId(-1)
                                                               .onClash(
                                                                       ClashResolutionType
                                                                               .ABORT_THAT)
                                                               .onComplete(CacheStrategyType.CACHE)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(
                builder().onComplete(CacheStrategyType.CLEAR).apply());
        assertThat(configuration.builderFrom().onComplete(CacheStrategyType.CACHE_IF_ERROR).apply())
                .isNotEqualTo(builder().onComplete(CacheStrategyType.CACHE_IF_ERROR).apply());
    }

    public void testClashResolutionEquals() {

        final InvocationConfiguration configuration = builder().withId(-1)
                                                               .onClash(
                                                                       ClashResolutionType
                                                                               .ABORT_THAT)
                                                               .onComplete(CacheStrategyType.CACHE)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(
                builder().onClash(ClashResolutionType.ABORT_THIS).apply());
        assertThat(configuration.builderFrom()
                                .onClash(ClashResolutionType.KEEP_THAT)
                                .apply()).isNotEqualTo(
                builder().onClash(ClashResolutionType.KEEP_THAT).apply());
    }

    public void testIdEquals() {

        final InvocationConfiguration configuration = builder().withId(-1)
                                                               .onClash(
                                                                       ClashResolutionType
                                                                               .ABORT_THAT)
                                                               .onComplete(CacheStrategyType.CACHE)
                                                               .apply();
        assertThat(configuration).isNotEqualTo(builder().withId(3).apply());
        assertThat(configuration.builderFrom().withId(27).apply()).isNotEqualTo(
                builder().withId(27).apply());
    }
}
