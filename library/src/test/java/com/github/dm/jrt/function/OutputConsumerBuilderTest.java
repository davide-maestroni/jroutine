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
package com.github.dm.jrt.function;

import com.github.dm.jrt.channel.RoutineException;

import org.junit.Test;

import static com.github.dm.jrt.function.Functions.onComplete;
import static com.github.dm.jrt.function.Functions.onError;
import static com.github.dm.jrt.function.Functions.onOutput;
import static com.github.dm.jrt.function.Functions.sink;
import static com.github.dm.jrt.function.Functions.wrapConsumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Output consumer builder unit tests.
 * <p/>
 * Created by davide-maestroni on 09/24/2015.
 */
public class OutputConsumerBuilderTest {

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testNullPointer() {

        try {

            new OutputConsumerBuilder<Object>(Functions.<Void>sink(),
                                              Functions.<RoutineException>sink(), null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new OutputConsumerBuilder<Object>(Functions.<Void>sink(), null, sink());

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            new OutputConsumerBuilder<Object>(null, Functions.<RoutineException>sink(), sink());

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testOnComplete() {

        final TestConsumer<Void> consumer1 = new TestConsumer<Void>();
        final TestConsumer<Void> consumer2 = new TestConsumer<Void>();
        final TestConsumer<Void> consumer3 = new TestConsumer<Void>();
        OutputConsumerBuilder<Object> outputConsumer = onComplete(consumer1);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        outputConsumer = outputConsumer.andThenComplete(consumer2);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        consumer1.reset();
        consumer2.reset();
        outputConsumer =
                onComplete(consumer1).andThenComplete(wrapConsumer(consumer2).andThen(consumer3));
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer<Object> outConsumer = new TestConsumer<Object>();
        final TestConsumer<RoutineException> errorConsumer = new TestConsumer<RoutineException>();
        outputConsumer =
                onComplete(consumer1).andThenOutput(outConsumer).andThenError(errorConsumer);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(outConsumer.isCalled()).isTrue();
        assertThat(errorConsumer.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(errorConsumer.isCalled()).isTrue();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOnCompleteError() {

        try {

            onComplete(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onComplete(Functions.<Void>sink()).andThenComplete(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onComplete(Functions.<Void>sink()).andThenError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onComplete(Functions.<Void>sink()).andThenOutput(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testOnError() {

        final TestConsumer<RoutineException> consumer1 = new TestConsumer<RoutineException>();
        final TestConsumer<RoutineException> consumer2 = new TestConsumer<RoutineException>();
        final TestConsumer<RoutineException> consumer3 = new TestConsumer<RoutineException>();
        OutputConsumerBuilder<Object> outputConsumer = onError(consumer1);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        outputConsumer = outputConsumer.andThenError(consumer2);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        consumer1.reset();
        consumer2.reset();
        outputConsumer =
                onError(consumer1).andThenError(wrapConsumer(consumer2).andThen(consumer3));
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer<Object> outConsumer = new TestConsumer<Object>();
        final TestConsumer<Void> completeConsumer = new TestConsumer<Void>();
        outputConsumer =
                onError(consumer1).andThenOutput(outConsumer).andThenComplete(completeConsumer);
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(outConsumer.isCalled()).isTrue();
        assertThat(completeConsumer.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(completeConsumer.isCalled()).isTrue();
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOnErrorError() {

        try {

            onError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onError(Functions.<RoutineException>sink()).andThenComplete(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onError(Functions.<RoutineException>sink()).andThenError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onError(Functions.<RoutineException>sink()).andThenOutput(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    @Test
    public void testOnOutput() {

        final TestConsumer<Object> consumer1 = new TestConsumer<Object>();
        final TestConsumer<Object> consumer2 = new TestConsumer<Object>();
        final TestConsumer<Object> consumer3 = new TestConsumer<Object>();
        OutputConsumerBuilder<Object> outputConsumer = onOutput(consumer1);
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
        consumer1.reset();
        outputConsumer = outputConsumer.andThenOutput(consumer2);
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        consumer1.reset();
        consumer2.reset();
        outputConsumer =
                onOutput(consumer1).andThenOutput(wrapConsumer(consumer2).andThen(consumer3));
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(consumer2.isCalled()).isFalse();
        assertThat(consumer3.isCalled()).isFalse();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
        assertThat(consumer2.isCalled()).isTrue();
        assertThat(consumer3.isCalled()).isTrue();
        consumer1.reset();
        final TestConsumer<RoutineException> errorConsumer = new TestConsumer<RoutineException>();
        final TestConsumer<Void> completeConsumer = new TestConsumer<Void>();
        outputConsumer =
                onOutput(consumer1).andThenError(errorConsumer).andThenComplete(completeConsumer);
        outputConsumer.onError(new RoutineException());
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(errorConsumer.isCalled()).isTrue();
        assertThat(completeConsumer.isCalled()).isFalse();
        outputConsumer.onComplete();
        assertThat(consumer1.isCalled()).isFalse();
        assertThat(completeConsumer.isCalled()).isTrue();
        outputConsumer.onOutput("test");
        assertThat(consumer1.isCalled()).isTrue();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOnOutputError() {

        try {

            onOutput(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onOutput(Functions.sink()).andThenComplete(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onOutput(Functions.sink()).andThenError(null);

            fail();

        } catch (final NullPointerException ignored) {

        }

        try {

            onOutput(Functions.sink()).andThenOutput(null);

            fail();

        } catch (final NullPointerException ignored) {

        }
    }

    private static class TestConsumer<OUT> implements Consumer<OUT> {

        private boolean mIsCalled;

        public void accept(final OUT out) {

            mIsCalled = true;
        }

        public boolean isCalled() {

            return mIsCalled;
        }

        public void reset() {

            mIsCalled = false;
        }
    }
}
