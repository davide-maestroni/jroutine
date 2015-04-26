package com.gh.bmd.jrt.builder;

import org.junit.Test;

import static com.gh.bmd.jrt.builder.ProxyConfiguration.builder;
import static com.gh.bmd.jrt.builder.ProxyConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Share configuration unit tests.
 * <p/>
 * Created by davide on 21/04/15.
 */
public class ProxyConfigurationTest {

    @Test
    public void testBuildFrom() {

        final ProxyConfiguration configuration = builder().withShareGroup("test").then();

        assertThat(configuration.builderFrom().then()).isEqualTo(configuration);
        assertThat(builderFrom(null, null).then()).isEqualTo(
                ProxyConfiguration.EMPTY_CONFIGURATION);
    }

    @Test
    public void testBuilderFromEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("test").then();
        assertThat(builder().with(configuration).then()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().then()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with((ProxyConfiguration) null).then()).isEqualTo(
                configuration);
        assertThat(configuration.builderFrom().then()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with((ProxyConfiguration) null).then()).isEqualTo(
                configuration);
    }

    @Test
    public void testShareGroupEquals() {

        final ProxyConfiguration configuration = builder().withShareGroup("group").then();
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").then());
        assertThat(configuration.builderFrom().withShareGroup("test").then()).isEqualTo(
                builder().withShareGroup("test").then());
        assertThat(configuration).isNotEqualTo(builder().withShareGroup("test").then());
    }

    @Test
    public void testToString() {

        assertThat(builder().withShareGroup("testGroupName123").then().toString()).contains(
                "testGroupName123");
    }
}
