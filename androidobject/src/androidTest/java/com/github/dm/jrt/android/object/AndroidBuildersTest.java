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

package com.github.dm.jrt.android.object;

import android.test.AndroidTestCase;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.CacheStrategyType;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.ClashResolutionType;
import com.github.dm.jrt.android.object.annotation.CacheStrategy;
import com.github.dm.jrt.android.object.annotation.ClashResolution;
import com.github.dm.jrt.android.object.annotation.InputClashResolution;
import com.github.dm.jrt.android.object.annotation.LoaderId;
import com.github.dm.jrt.android.object.annotation.ResultStaleTime;
import com.github.dm.jrt.android.object.annotation.RoutineId;
import com.github.dm.jrt.core.util.TimeDuration;

import static com.github.dm.jrt.android.core.config.LoaderConfiguration.builder;
import static com.github.dm.jrt.android.object.AndroidBuilders.configurationWithAnnotations;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Android builder unit tests.
 * <p/>
 * Created by davide-maestroni on 03/08/2016.
 */
public class AndroidBuildersTest extends AndroidTestCase {

    public void testBuilderConfigurationThroughAnnotations() throws NoSuchMethodException {

        assertThat(configurationWithAnnotations(LoaderConfiguration.DEFAULT_CONFIGURATION,
                AnnotationItf.class.getMethod("toString"))).isEqualTo(
                builder().withCacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
                         .withClashResolution(ClashResolutionType.ABORT)
                         .withInputClashResolution(ClashResolutionType.ABORT_THIS)
                         .withLoaderId(-77)
                         .withResultStaleTime(TimeDuration.millis(333))
                         .withRoutineId(13)
                         .getConfigured());
    }

    public interface AnnotationItf {

        @CacheStrategy(CacheStrategyType.CACHE_IF_ERROR)
        @ClashResolution(ClashResolutionType.ABORT)
        @InputClashResolution(ClashResolutionType.ABORT_THIS)
        @LoaderId(-77)
        @ResultStaleTime(333)
        @RoutineId(13)
        String toString();
    }
}
