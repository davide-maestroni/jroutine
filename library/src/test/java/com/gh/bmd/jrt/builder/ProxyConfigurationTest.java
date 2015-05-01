package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.builder.ProxyConfiguration.Builder;

import org.junit.Test;

import static com.gh.bmd.jrt.builder.ProxyConfiguration.builder;
import static com.gh.bmd.jrt.builder.ProxyConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Proxy configuration unit tests.
 * <p/>
 * Created by davide on 21/04/15.
 */
public class ProxyConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ProxyConfiguration configuration = builder().withShareGroup("test").set();

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

        final ProxyConfiguration configuration = builder().withShareGroup("test").set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testShareGroupEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("group").set();
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").set());
        assertThat(configuration.builderFrom().withShareGroup("test").set()).isEqualTo(
                builder().withShareGroup("test").set());
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").set());
    }

    @Test
    public void testToString() {

        assertThat(builder().withShareGroup("testGroupName123").set().toString()).contains(
                "testGroupName123");
    }
}
