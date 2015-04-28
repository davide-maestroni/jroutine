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

        final ProxyConfiguration configuration = builder().withShareGroup("test").applied();

        assertThat(configuration.builderFrom().applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().applied().hashCode()).isEqualTo(
                configuration.hashCode());
        assertThat(builderFrom(null).applied()).isEqualTo(ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("test").applied();
        assertThat(builder().with(configuration).applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().applied()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).applied()).isEqualTo(
                ProxyConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testShareGroupEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("group").applied();
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").applied());
        assertThat(configuration.builderFrom().withShareGroup("test").applied()).isEqualTo(
                builder().withShareGroup("test").applied());
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").applied());
    }

    @Test
    public void testToString() {

        assertThat(builder().withShareGroup("testGroupName123").applied().toString()).contains(
                "testGroupName123");
    }
}
