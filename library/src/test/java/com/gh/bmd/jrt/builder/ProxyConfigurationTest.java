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

        final ProxyConfiguration configuration = builder().withShareGroup("test").build();

        assertThat(configuration.builderFrom().build()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().build().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(null).build()).isEqualTo(ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("test").build();
        assertThat(builder().with(configuration).build()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().build()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).build()).isEqualTo(
                ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testShareGroupEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("group").build();
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").build());
        assertThat(configuration.builderFrom().withShareGroup("test").build()).isEqualTo(
                builder().withShareGroup("test").build());
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").build());
    }

    @Test
    public void testToString() {

        assertThat(builder().withShareGroup("testGroupName123").build().toString()).contains(
                "testGroupName123");
    }
}
