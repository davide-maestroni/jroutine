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

import com.github.dm.jrt.object.builder.ProxyConfiguration.Builder;

import org.junit.Test;

import static com.github.dm.jrt.object.builder.ProxyConfiguration.builder;
import static com.github.dm.jrt.object.builder.ProxyConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Proxy configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 04/21/2015.
 */
public class ProxyConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ProxyConfiguration configuration = builder().withSharedFields("test").getConfigured();
        assertThat(configuration.builderFrom().getConfigured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().getConfigured().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(null).getConfigured()).isEqualTo(
                ProxyConfiguration.DEFAULT_CONFIGURATION);
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

            new Builder<Object>(null, ProxyConfiguration.DEFAULT_CONFIGURATION);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final ProxyConfiguration configuration = builder().withSharedFields("test").getConfigured();
        assertThat(builder().with(configuration).getConfigured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().getConfigured()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).getConfigured()).isEqualTo(
                ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testSharedFieldsEquals() {

        final ProxyConfiguration configuration =
                builder().withSharedFields("group").getConfigured();
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").getConfigured());
        assertThat(configuration.builderFrom().withSharedFields("test").getConfigured()).isEqualTo(
                builder().withSharedFields("test").getConfigured());
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").getConfigured());
    }

    @Test
    public void testToString() {

        assertThat(
                builder().withSharedFields("testGroupName123").getConfigured().toString()).contains(
                "testGroupName123");
    }
}
