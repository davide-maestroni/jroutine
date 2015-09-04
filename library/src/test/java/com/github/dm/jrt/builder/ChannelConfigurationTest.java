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

import com.github.dm.jrt.builder.ChannelConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.log.Log.LogLevel;
import com.github.dm.jrt.log.Logs;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.TimeDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.builder.ChannelConfiguration.builder;
import static com.github.dm.jrt.builder.ChannelConfiguration.builderFrom;
import static com.github.dm.jrt.util.TimeDuration.millis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Channel configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 07/03/2015.
 */
public class ChannelConfigurationTest {

    @Test
    public void testAsyncRunnerEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).set());
        assertThat(configuration.builderFrom()
                                .withAsyncRunner(Runners.syncRunner())
                                .set()).isNotEqualTo(
                builder().withAsyncRunner(Runners.syncRunner()).set());
    }

    @Test
    public void testBuildFrom() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(builderFrom(configuration).set().hashCode()).isEqualTo(configuration.hashCode());
        assertThat(builderFrom(configuration).set()).isEqualTo(configuration);
        assertThat(builderFrom(null).set().hashCode()).isEqualTo(
                ChannelConfiguration.DEFAULT_CONFIGURATION.hashCode());
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
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .withLogLevel(LogLevel.SILENT)
                                                            .withChannelTimeout(
                                                                    TimeDuration.seconds(1))
                                                            .withPassTimeout(
                                                                    TimeDuration.seconds(10))
                                                            .withPassTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                ChannelConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testChannelOrderEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.syncRunner())
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
                                                            .withAsyncRunner(Runners.syncRunner())
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
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(
                builder().withChannelTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withChannelTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom().withChannelTimeout(millis(1)).set()).isNotEqualTo(
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
                                                            .withAsyncRunner(Runners.syncRunner())
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
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(LogLevel.DEBUG).set());
        assertThat(configuration.builderFrom().withLogLevel(LogLevel.WARNING).set()).isNotEqualTo(
                builder().withLogLevel(LogLevel.WARNING).set());
    }

    @Test
    public void testPassTimeoutActionEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(
                builder().withPassTimeoutAction(TimeoutActionType.ABORT).set());
        assertThat(configuration).isNotEqualTo(
                builder().withPassTimeoutAction(TimeoutActionType.EXIT).set());
        assertThat(configuration.builderFrom().withPassTimeoutAction(TimeoutActionType.THROW).set())
                .isNotEqualTo(builder().withPassTimeoutAction(TimeoutActionType.THROW).set());
    }

    @Test
    public void testPassTimeoutEquals() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withLog(new NullLog())
                                                            .withChannelMaxSize(100)
                                                            .set();
        assertThat(configuration).isNotEqualTo(builder().withPassTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withPassTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom().withPassTimeout(millis(1)).set()).isNotEqualTo(
                builder().withPassTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    public void testToInputChannelConfiguration() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelMaxSize(100)
                                                            .withChannelTimeout(millis(33))
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withPassTimeout(millis(100))
                                                            .withPassTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(LogLevel.SILENT)
                                                            .set();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withExecutionTimeout(millis(100))
                       .withExecutionTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(LogLevel.SILENT)
                       .withInputOrder(OrderType.BY_CALL)
                       .withInputMaxSize(100)
                       .withInputTimeout(millis(33))
                       .set();
        assertThat(configuration.toInputChannelConfiguration()).isEqualTo(invocationConfiguration);
    }

    @Test
    public void testToInvocationConfiguration() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelMaxSize(100)
                                                            .withChannelTimeout(millis(33))
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withPassTimeout(millis(100))
                                                            .withPassTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(LogLevel.SILENT)
                                                            .set();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withExecutionTimeout(millis(100))
                       .withExecutionTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(LogLevel.SILENT)
                       .set();
        assertThat(configuration.toInvocationConfiguration()).isEqualTo(invocationConfiguration);
    }

    @Test
    public void testToOutputChannelConfiguration() {

        final ChannelConfiguration configuration = builder().withChannelOrder(OrderType.BY_CALL)
                                                            .withChannelMaxSize(100)
                                                            .withChannelTimeout(millis(33))
                                                            .withAsyncRunner(Runners.syncRunner())
                                                            .withPassTimeout(millis(100))
                                                            .withPassTimeoutAction(
                                                                    TimeoutActionType.ABORT)
                                                            .withLog(Logs.nullLog())
                                                            .withLogLevel(LogLevel.SILENT)
                                                            .set();
        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builder();
        final InvocationConfiguration invocationConfiguration =
                builder.withRunner(Runners.syncRunner())
                       .withExecutionTimeout(millis(100))
                       .withExecutionTimeoutAction(TimeoutActionType.ABORT)
                       .withLog(Logs.nullLog())
                       .withLogLevel(LogLevel.SILENT)
                       .withOutputOrder(OrderType.BY_CALL)
                       .withOutputMaxSize(100)
                       .withOutputTimeout(millis(33))
                       .set();
        assertThat(configuration.toOutputChannelConfiguration()).isEqualTo(invocationConfiguration);
    }
}
