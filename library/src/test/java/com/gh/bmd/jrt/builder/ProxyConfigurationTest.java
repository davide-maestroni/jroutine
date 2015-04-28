package com.gh.bmd.jrt.builder;

import org.junit.Test;

import static com.gh.bmd.jrt.builder.ProxyConfiguration.builder;
import static com.gh.bmd.jrt.builder.ProxyConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proxy configuration unit tests.
 * <p/>
 * Created by davide on 21/04/15.
 */
public class ProxyConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ProxyConfiguration configuration = builder().withShareGroup("test").apply();

        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(null).apply()).isEqualTo(ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("test").apply();
        assertThat(builder().with(configuration).apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).apply()).isEqualTo(
                ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testShareGroupEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("group").apply();
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").apply());
        assertThat(configuration.builderFrom().withShareGroup("test").apply()).isEqualTo(
                builder().withShareGroup("test").apply());
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").apply());
    }

    @Test
    public void testToString() {

        assertThat(builder().withShareGroup("testGroupName123").apply().toString()).contains(
                "testGroupName123");
    }
}
