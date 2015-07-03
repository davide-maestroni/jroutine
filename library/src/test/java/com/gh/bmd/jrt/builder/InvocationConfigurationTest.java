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

import com.gh.bmd.jrt.builder.InvocationConfiguration.Builder;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.log.Log.LogLevel;
import com.gh.bmd.jrt.log.Logs;
import com.gh.bmd.jrt.log.NullLog;
import com.gh.bmd.jrt.runner.Runners;
import com.gh.bmd.jrt.util.TimeDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.gh.bmd.jrt.builder.InvocationConfiguration.builder;
import static com.gh.bmd.jrt.builder.InvocationConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocation configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 11/22/14.
 */
public class InvocationConfigurationTest {

    @Test
    public void testAsyncRunnerEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withPriority(11)
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withAsyncRunner(Runners.sharedRunner()).set());
        assertThat(configuration.builderFrom()
                                .withAsyncRunner(Runners.queuedRunner())
                                .set()).isNotEqualTo(
                builder().withAsyncRunner(Runners.queuedRunner()).set());
    }

    @Test
    public void testAvailableTimeoutEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withPriority(11)
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withAvailInstanceTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withAvailInstanceTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withAvailInstanceTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withAvailInstanceTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    public void testBuildFrom() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withPriority(11)
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(builderFrom(configuration).set().hashCode()).isEqualTo(configuration.hashCode());
        assertThat(builderFrom(configuration).set()).isEqualTo(configuration);
        assertThat(builderFrom(null).set().hashCode()).isEqualTo(
                InvocationConfiguration.DEFAULT_CONFIGURATION.hashCode());
        assertThat(builderFrom(null).set()).isEqualTo(
                InvocationConfiguration.DEFAULT_CONFIGURATION);
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

            new Builder<Object>(null, InvocationConfiguration.DEFAULT_CONFIGURATION);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testBuilderFromEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withPriority(11)
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(builder().with(configuration).set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().set()).isEqualTo(configuration);
        assertThat(configuration.builderFrom().with(null).set()).isEqualTo(
                InvocationConfiguration.DEFAULT_CONFIGURATION);
    }

    @Test
    public void testCoreInvocationsEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withCoreInstances(27)
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withCoreInstances(3).set());
        assertThat(configuration.builderFrom().withCoreInstances(27).set()).isNotEqualTo(
                builder().withCoreInstances(27).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testCoreInvocationsError() {

        try {

            builder().withCoreInstances(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputOrderEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withInputOrder(OrderType.BY_CHANCE).set());
        assertThat(
                configuration.builderFrom().withInputOrder(OrderType.BY_CALL).set()).isNotEqualTo(
                builder().withInputOrder(OrderType.BY_CALL).set());
    }

    @Test
    public void testInputSizeEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withInputMaxSize(10).set());
        assertThat(configuration.builderFrom().withInputMaxSize(31).set()).isNotEqualTo(
                builder().withInputMaxSize(31).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputSizeError() {

        try {

            builder().withInputMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputTimeoutEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withInputTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withInputTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withInputTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputTimeoutError() {

        try {

            builder().withInputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withInputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testLogEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).set());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).set()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).set());
    }

    @Test
    public void testLogLevelEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(LogLevel.DEBUG).set());
        assertThat(configuration.builderFrom().withLogLevel(LogLevel.WARNING).set()).isNotEqualTo(
                builder().withLogLevel(LogLevel.WARNING).set());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withMaxInstances(4).set());
        assertThat(configuration.builderFrom().withMaxInstances(41).set()).isNotEqualTo(
                builder().withMaxInstances(41).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testMaxInvocationsError() {

        try {

            builder().withMaxInstances(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputOrderEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputOrder(OrderType.BY_CHANCE).set());
        assertThat(
                configuration.builderFrom().withOutputOrder(OrderType.BY_CALL).set()).isNotEqualTo(
                builder().withOutputOrder(OrderType.BY_CALL).set());
    }

    @Test
    public void testOutputSizeEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withOutputMaxSize(10).set());
        assertThat(configuration.builderFrom().withOutputMaxSize(1).set()).isNotEqualTo(
                builder().withOutputMaxSize(1).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputSizeError() {

        try {

            builder().withOutputMaxSize(0);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputTimeoutEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withOutputTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withOutputTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputTimeoutError() {

        try {

            builder().withOutputTimeout(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withOutputTimeout(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testPriorityEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withPriority(17)
                         .withCoreInstances(27)
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withPriority(3).set());
        assertThat(configuration.builderFrom().withPriority(17).set()).isNotEqualTo(
                builder().withPriority(17).set());
    }

    @Test
    public void testReadTimeoutActionEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
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

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(builder().withReadTimeout(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withReadTimeout(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withReadTimeout(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    public void testSyncRunnerEquals() {

        final InvocationConfiguration configuration =
                builder().withAvailInstanceTimeout(TimeDuration.millis(100))
                         .withInputOrder(OrderType.BY_CALL)
                         .withAsyncRunner(Runners.queuedRunner())
                         .withLog(new NullLog())
                         .withOutputMaxSize(100)
                         .set();
        assertThat(configuration).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).set());
        assertThat(configuration.builderFrom()
                                .withSyncRunner(Runners.queuedRunner())
                                .set()).isNotEqualTo(
                builder().withSyncRunner(Runners.queuedRunner()).set());
    }
}
