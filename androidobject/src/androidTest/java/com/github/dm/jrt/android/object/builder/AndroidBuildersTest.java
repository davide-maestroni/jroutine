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

import android.test.AndroidTestCase;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.object.annotation.CacheStrategy;
import com.github.dm.jrt.android.object.annotation.ClashResolution;
import com.github.dm.jrt.android.object.annotation.FactoryId;
import com.github.dm.jrt.android.object.annotation.LoaderId;
import com.github.dm.jrt.android.object.annotation.MatchResolution;
import com.github.dm.jrt.android.object.annotation.ResultStaleTime;
import com.github.dm.jrt.core.util.UnitDuration;

import static com.github.dm.jrt.android.core.config.LoaderConfiguration.builder;
import static com.github.dm.jrt.android.object.builder.AndroidBuilders.withAnnotations;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android builder unit tests.
 * <p>
 * Created by davide-maestroni on 03/08/2016.
 */
public class AndroidBuildersTest extends AndroidTestCase {

    public void testBuilderConfigurationThroughAnnotations() throws NoSuchMethodException {

        assertThat(withAnnotations(LoaderConfiguration.defaultConfiguration(),
                AnnotationItf.class.getMethod("toString"))).isEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                         .withClashResolution(ClashResolutionType.ABORT_BOTH)
                         .withFactoryId(13)
                         .withMatchResolution(ClashResolutionType.ABORT_THIS)
                         .withLoaderId(-77)
                         .withResultStaleTime(UnitDuration.millis(333))
                         .configured());
    }

    public void testConstructor() {

        boolean failed = false;
        try {
            new AndroidBuilders();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    public interface AnnotationItf {

        @CacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
        @ClashResolution(ClashResolutionType.ABORT_BOTH)
        @FactoryId(13)
        @MatchResolution(ClashResolutionType.ABORT_THIS)
        @LoaderId(-77)
        @ResultStaleTime(333)
        String toString();
    }
}
