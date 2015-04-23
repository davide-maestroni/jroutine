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

import com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.CacheStrategyType;
import com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.ClashResolutionType;

import static com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.builder;
import static com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.builderFrom;
import static com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.onClash;
import static com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.onComplete;
import static com.gh.bmd.jrt.android.builder.ContextInvocationConfiguration.withId;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context invocation configuration unit tests.
 * <p/>
 * Created by davide on 22/04/15.
 */
public class ContextInvocationConfigurationTest extends AndroidTestCase {

    public void testBuildFrom() {

        final ContextInvocationConfiguration configuration =
                withId(-1).onClash(ClashResolutionType.ABORT_THAT)
                          .onComplete(CacheStrategyType.CACHE)
                          .buildConfiguration();

        assertThat(builderFrom(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).buildConfiguration()).isEqualTo(
                ContextInvocationConfiguration.EMPTY_CONFIGURATION);
    }

    public void testBuilderFromEquals() {

        final ContextInvocationConfiguration configuration =
                withId(-1).onClash(ClashResolutionType.ABORT_THAT)
                          .onComplete(CacheStrategyType.CACHE)
                          .buildConfiguration();
        assertThat(builder().apply(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply(null).buildConfiguration()).isEqualTo(
                configuration);
    }

    public void testCacheStrategyEquals() {

        assertThat(onComplete(CacheStrategyType.CLEAR).buildConfiguration()).isEqualTo(
                builder().onComplete(CacheStrategyType.CLEAR).buildConfiguration());

        final ContextInvocationConfiguration configuration =
                withId(-1).onClash(ClashResolutionType.ABORT_THAT)
                          .onComplete(CacheStrategyType.CACHE)
                          .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                onComplete(CacheStrategyType.CLEAR).buildConfiguration());
        assertThat(onComplete(CacheStrategyType.CLEAR).buildConfiguration()).isNotEqualTo(
                onComplete(CacheStrategyType.CACHE_IF_ERROR).buildConfiguration());
    }

    public void testClashResolutionEquals() {

        assertThat(onClash(ClashResolutionType.ABORT_THIS).buildConfiguration()).isEqualTo(
                builder().onClash(ClashResolutionType.ABORT_THIS).buildConfiguration());

        final ContextInvocationConfiguration configuration =
                withId(-1).onClash(ClashResolutionType.ABORT_THAT)
                          .onComplete(CacheStrategyType.CACHE)
                          .buildConfiguration();
        assertThat(configuration).isNotEqualTo(
                onClash(ClashResolutionType.ABORT_THIS).buildConfiguration());
        assertThat(onClash(ClashResolutionType.ABORT_THIS).buildConfiguration()).isNotEqualTo(
                onClash(ClashResolutionType.KEEP_THAT).buildConfiguration());
    }

    public void testIdEquals() {

        assertThat(withId(3).buildConfiguration()).isEqualTo(
                builder().withId(3).buildConfiguration());

        final ContextInvocationConfiguration configuration =
                withId(-1).onClash(ClashResolutionType.ABORT_THAT)
                          .onComplete(CacheStrategyType.CACHE)
                          .buildConfiguration();
        assertThat(configuration).isNotEqualTo(withId(3).buildConfiguration());
        assertThat(withId(3).buildConfiguration()).isNotEqualTo(withId(27).buildConfiguration());
    }
}
