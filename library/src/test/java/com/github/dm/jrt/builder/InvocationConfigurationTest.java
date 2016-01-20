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

import com.github.dm.jrt.builder.InvocationConfiguration.Builder;
import com.github.dm.jrt.builder.InvocationConfiguration.OrderType;
import com.github.dm.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.github.dm.jrt.log.Log.Level;
import com.github.dm.jrt.log.Logs;
import com.github.dm.jrt.log.NullLog;
import com.github.dm.jrt.runner.Runners;
import com.github.dm.jrt.util.TimeDuration;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.builder.InvocationConfiguration.builder;
import static com.github.dm.jrt.builder.InvocationConfiguration.builderFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocation configuration unit tests.
 * <p/>
 * Created by davide-maestroni on 11/22/2014.
 */
public class InvocationConfigurationTest {

    @Test
    public void testBuildFrom() {

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
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

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
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

        final InvocationConfiguration configuration = builder().withCoreInstances(27)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
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
    public void testExecutionTimeoutActionEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.ABORT).set());
        assertThat(configuration).isNotEqualTo(
                builder().withReadTimeoutAction(TimeoutActionType.EXIT).set());
        assertThat(configuration.builderFrom().withReadTimeoutAction(TimeoutActionType.THROW).set())
                .isNotEqualTo(builder().withReadTimeoutAction(TimeoutActionType.THROW).set());
    }

    @Test
    public void testExecutionTimeoutEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
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
    public void testInputLimitEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withInputLimit(10).set());
        assertThat(configuration.builderFrom().withInputLimit(31).set()).isNotEqualTo(
                builder().withInputLimit(31).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputLimitError() {

        try {

            builder().withInputLimit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputMaxDelayEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(
                builder().withInputMaxDelay(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withInputMaxDelay(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withInputMaxDelay(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withInputMaxDelay(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testInputMaxDelayError() {

        try {

            builder().withInputMaxDelay(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withInputMaxDelay(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testInputOrderEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withInputOrder(OrderType.BY_DELAY).set());
        assertThat(
                configuration.builderFrom().withInputOrder(OrderType.BY_CALL).set()).isNotEqualTo(
                builder().withInputOrder(OrderType.BY_CALL).set());
    }

    @Test
    public void testInputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
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
    public void testLogEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withLog(Logs.nullLog()).set());
        assertThat(configuration.builderFrom().withLog(Logs.systemLog()).set()).isNotEqualTo(
                builder().withLog(Logs.systemLog()).set());
    }

    @Test
    public void testLogLevelEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withLogLevel(Level.DEBUG).set());
        assertThat(configuration.builderFrom().withLogLevel(Level.WARNING).set()).isNotEqualTo(
                builder().withLogLevel(Level.WARNING).set());
    }

    @Test
    public void testMaxInvocationsEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
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
    public void testOutputLimitEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withOutputLimit(10).set());
        assertThat(configuration.builderFrom().withOutputLimit(31).set()).isNotEqualTo(
                builder().withOutputLimit(31).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputLimitError() {

        try {

            builder().withOutputLimit(-1);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputMaxDelayEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(
                builder().withOutputMaxDelay(TimeDuration.ZERO).set());
        assertThat(configuration).isNotEqualTo(
                builder().withOutputMaxDelay(1, TimeUnit.MILLISECONDS).set());
        assertThat(configuration.builderFrom()
                                .withOutputMaxDelay(TimeDuration.millis(1))
                                .set()).isNotEqualTo(
                builder().withOutputMaxDelay(1, TimeUnit.MILLISECONDS).set());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOutputMaxDelayError() {

        try {

            builder().withOutputMaxDelay(1, null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            builder().withOutputMaxDelay(-1, TimeUnit.MILLISECONDS);

            fail();

        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testOutputOrderEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withOutputOrder(OrderType.BY_DELAY).set());
        assertThat(
                configuration.builderFrom().withOutputOrder(OrderType.BY_CALL).set()).isNotEqualTo(
                builder().withOutputOrder(OrderType.BY_CALL).set());
    }

    @Test
    public void testOutputSizeEquals() {

        final InvocationConfiguration configuration = builder().withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
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
    public void testPriorityEquals() {

        final InvocationConfiguration configuration = builder().withPriority(17)
                                                               .withCoreInstances(27)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withPriority(3).set());
        assertThat(configuration.builderFrom().withPriority(17).set()).isNotEqualTo(
                builder().withPriority(17).set());
    }

    @Test
    public void testRunnerEquals() {

        final InvocationConfiguration configuration = builder().withPriority(11)
                                                               .withInputOrder(OrderType.BY_CALL)
                                                               .withRunner(Runners.syncRunner())
                                                               .withLog(new NullLog())
                                                               .withOutputMaxSize(100)
                                                               .set();
        assertThat(configuration).isNotEqualTo(builder().withRunner(Runners.sharedRunner()).set());
        assertThat(configuration.builderFrom().withRunner(Runners.syncRunner()).set()).isNotEqualTo(
                builder().withRunner(Runners.syncRunner()).set());
    }
}
