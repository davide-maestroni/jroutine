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

        final ObjectConfiguration configuration = builder().withSharedFields("test").configured();
        assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().configured().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(null).configured()).isEqualTo(
                ObjectConfiguration.defaultConfiguration());
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

        final ObjectConfiguration configuration = builder().withSharedFields("test").configured();
        assertThat(builder().with(configuration).configured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().configured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).configured()).isEqualTo(
                ObjectConfiguration.defaultConfiguration());
    }

    @Test
    public void testConstructor() {

        boolean failed = false;
        try {
            new Builders();
            failed = true;

        } catch (final Throwable ignored) {

        }

        assertThat(failed).isFalse();
    }

    @Test
    public void testSharedFieldsEquals() {

        final ObjectConfiguration configuration = builder().withSharedFields("group").configured();
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").configured());
        assertThat(configuration.builderFrom().withSharedFields("test").configured()).isEqualTo(
                builder().withSharedFields("test").configured());
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").configured());
    }

    @Test
    public void testToString() {

        assertThat(builder().withSharedFields("testGroupName123").configured().toString()).contains(
                "testGroupName123");
    }
}
