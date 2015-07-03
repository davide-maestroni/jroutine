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
package com.gh.bmd.jrt.builder;

import com.gh.bmd.jrt.builder.ChannelConfiguration.Builder;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logs;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.util.TimeDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.gh.bmd.jrt.builder.ChannelConfiguration.builder;
import static com.gh.bmd.jrt.builder.ChannelConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Channel configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 03/07/15.
 */
public class ChannelConfigurationTest {

    @Test
    public void testAsyncRunnerEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).set());
        assertThat(configuration.builderFrom()
                                .withAsyncRunner(Runners.queuedRunner())
                                .set()).isNotEqualTo(
                builder().withAsyncRunner(Runners.queuedRunner()).set());
    }

    @Test
    public void testBuildFrom() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(builderFrom(configuration).set()).isEqualTo(configuration);
        assertThat(builderFrom(null).set()).isEqualTo(ChannelConfiguration.DEFAULT_CONFIGURATION);
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

            new Builder<Object>(null, ChannelConfiguration.DEFAULT_CONFIGURATION);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                ChannelConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testChannelOrderEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_CHANCE).set());
        assertThat(
                configuration.builderFrom().withChannelOrder(OrderType.BY_CALL).set()).isNotEqualTo(
                builder().withChannelOrder(OrderType.BY_CALL).set());
    }

    @Test
    public void testChannelSizeEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(builder().withChannelMaxSize(10).set());
        assertThat(configuration.builderFrom().withChannelMaxSize(1).set()).isNotEqualTo(
                builder().withChannelMaxSize(1).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testChannelSizeError() {

        try {

            builder().withChannelMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testChannelTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withChannelTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withChannelTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withChannelTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testChannelTimeoutError() {

        try {

            builder().withChannelTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withChannelTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testLogEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).set());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).set()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).set());
    }

    @Test
    public void testLogLevelEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(LogLevel.DEBUG).set());
        assertThat(configuration.builderFrom().withLogLevel(LogLevel.WARNING).set()).isNotEqualTo(
                builder().withLogLevel(LogLevel.WARNING).set());
    }

    @Test
    public void testReadTimeoutActionEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.ABORT).set());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.EXIT).set());
        assertThat(
                configuration.builderFrom().withReadTimeoutAction(TimeoutActionType.DEADLOCK).set())
                .isNotEqualTo(builder().withReadTimeoutAction(TimeoutActionType.DEADLOCK).set());
    }

    @Test
    public void testReadTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.queuedRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(builder().withReadTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).set());
    }
}
