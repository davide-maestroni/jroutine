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

package com.github.dm.jrt.object.builder;

import com.github.dm.jrt.object.config.ObjectConfiguration;
import com.github.dm.jrt.object.config.ObjectConfiguration.Builder;

import org.junit.Test;

import static com.github.dm.jrt.object.config.ObjectConfiguration.builder;
import static com.github.dm.jrt.object.config.ObjectConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Object configuration unit tests.
 * <p>
 * Created by davide-maestroni on 04/21/2015.
 */
public class ObjectConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ObjectConfiguration configuration = builder().withSharedFields("test").apply();
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(null).apply()).isEqualTo(ObjectConfiguration.defaultConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuildNullPointerError() {

        try {

            new Builder<Object>(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new Builder<Object>(null, ObjectConfiguration.defaultConfiguration());

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final ObjectConfiguration configuration = builder().withSharedFields("test").apply();
        assertThat(builder().with(configuration).apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).apply()).isEqualTo(
                ObjectConfiguration.defaultConfiguration());
    }

    @Test
    public void testSharedFieldsEquals() {

        final ObjectConfiguration configuration = builder().withSharedFields("group").apply();
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").apply());
        assertThat(configuration.builderFrom().withSharedFields("test").apply()).isEqualTo(
                builder().withSharedFields("test").apply());
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").apply());
    }

    @Test
    public void testToString() {

        assertThat(builder().withSharedFields("testGroupName123").apply().toString()).contains(
                "testGroupName123");
    }
}