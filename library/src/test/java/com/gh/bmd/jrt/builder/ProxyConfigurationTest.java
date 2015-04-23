package com.gh.bmd.jrt.builder;

import org.junit.Test;

import static com.gh.bmd.jrt.builder.ProxyConfiguration.builder;
import static com.gh.bmd.jrt.builder.ProxyConfiguration.builderFrom;
import static com.gh.bmd.jrt.builder.ProxyConfiguration.withShareGroup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Share configuration unit tests.
 * <p/>
 * Created by davide on 21/04/15.
 */
public class ProxyConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ProxyConfiguration configuration =
                builder().withShareGroup("test").buildConfiguration();

        assertThat(builderFrom(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(null).buildConfiguration()).isEqualTo(
                ProxyConfiguration.EMPTY_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final ProxyConfiguration configuration =
                builder().withShareGroup("test").buildConfiguration();
        assertThat(builder().apply(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().apply(null).buildConfiguration()).isEqualTo(
                configuration);
        assertThat(builderFrom(configuration).buildConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(configuration).apply(null).buildConfiguration()).isEqualTo(
                configuration);
    }

    @Test
    public void testShareGroupEquals() {

        assertThat(withShareGroup("test").buildConfiguration()).isEqualTo(
                builder().withShareGroup("test").buildConfiguration());
        assertThat(withShareGroup("test").buildConfiguration().hashCode()).isEqualTo(
                builder().withShareGroup("test").buildConfiguration().hashCode());

        final ProxyConfiguration configuration =
                builder().withShareGroup("group").buildConfiguration();
        assertThat(configuration).isNotEqualTo(withShareGroup("test").buildConfiguration());
        assertThat(withShareGroup("group").buildConfiguration()).isNotEqualTo(
                withShareGroup("test").buildConfiguration());
    }

    @Test
    public void testToString() {

        assertThat(withShareGroup("testGroupName123").buildConfiguration().toString()).contains(
                "testGroupName123");
    }
}
