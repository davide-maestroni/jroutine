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
package com.github.dm.jrt.builder;

import com.github.dm.jrt.builder.ProxyConfiguration.Builder;

import org.junit.Test;

import static com.github.dm.jrt.builder.ProxyConfiguration.builder;
import static com.github.dm.jrt.builder.ProxyConfiguration.builderFrom;
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

        final ProxyConfiguration configuration = builder().withSharedFields("test").set();
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(null).set()).isEqualTo(ProxyConfiguration.DEFAULT_CONFIGURATION);
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

        final ProxyConfiguration configuration = builder().withSharedFields("test").set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testSharedFieldsEquals() {

        final ProxyConfiguration configuration = builder().withSharedFields("group").set();
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").set());
        assertThat(configuration.builderFrom().withSharedFields("test").set()).isEqualTo(
                builder().withSharedFields("test").set());
        assertThat(configuration).isNotEqualTo(builder().withSharedFields("test").set());
    }

    @Test
    public void testToString() {

        assertThat(builder().withSharedFields("testGroupName123").set().toString()).contains(
                "testGroupName123");
    }
}
