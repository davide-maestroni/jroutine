package com.gh.bmd.jrt.builder;

import org.junit.Test;

import static com.gh.bmd.jrt.builder.ShareConfiguration.builder;
import static com.gh.bmd.jrt.builder.ShareConfiguration.builderFrom;
import static com.gh.bmd.jrt.builder.ShareConfiguration.withGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Share configuration unit tests.
 * <p/>
 * Created by davide on 21/04/15.
 */
public class ShareConfigurationTest {

    @Test
    public void tesShareGroupEquals() {

        assertThat(withGroup("test").buildConfiguration()).isEqualTo(
                builder().withGroup("test").buildConfiguration());
        assertThat(withGroup("test").buildConfiguration().hashCode()).isEqualTo(
                builder().withGroup("test").buildConfiguration().hashCode());

        final ShareConfiguration configuration = builder().withGroup("group").buildConfiguration();
        assertThat(configuration).isNotEqualTo(withGroup("test").buildConfiguration());
        assertThat(withGroup("group").buildConfiguration()).isNotEqualTo(
                withGroup("test").buildConfiguration());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testBuildFromError() {

        try {

            builderFrom(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final ShareConfiguration configuration = builder().withGroup("test").buildConfiguration();
        assertThat(builder().configure(configuration).buildConfiguration()).isEqualTo(
                configuration);
        assertThat(configuration.builderFrom().buildConfiguration()).isEqualTo(configuration);
        assertThat(builderFrom(configuration).buildConfiguration()).isEqualTo(configuration);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConfigurationError() {

        try {

            builder().configure(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testToString() {

        assertThat(withGroup("testGroupName123").buildConfiguration().toString()).contains(
                "testGroupName123");
    }
}
