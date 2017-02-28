/*
 * Copyright 2017 Davide Maestroni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dm.jrt.function.builder;

import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.lambda.Action;
import com.github.dm.jrt.function.lambda.Consumer;

import org.junit.Test;

import static com.github.dm.jrt.function.Functions.sink;
import static com.github.dm.jrt.function.JRoutineFunction.onComplete;
import static com.github.dm.jrt.function.JRoutineFunction.onError;
import static com.github.dm.jrt.function.JRoutineFunction.onOutput;
import static com.github.dm.jrt.function.lambda.ActionDecorator.decorate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Channel consumer builder unit tests.
 * <p>
 * Created by davide-maestroni on 09/24/2015.
 */
public class ChannelConsumerBuilderTest {

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNullPointer() {

    try {

      onOutput(null, Functions.<RoutineException>sink(), Functions.noOp());

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onOutput(sink(), null, Functions.noOp());

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onOutput(sink(), Functions.<RoutineException>sink(), null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testOnComplete() throws Exception {

    final TestAction action1 = new TestAction();
    final TestAction action2 = new TestAction();
    final TestAction action3 = new TestAction();
    ChannelConsumerBuilder<Object> channelConsumer = onComplete(action1);
    channelConsumer.onOutput("test");
    assertThat(action1.isCalled()).isFalse();
    channelConsumer.onError(new RoutineException());
    assertThat(action1.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(action1.isCalled()).isTrue();
    action1.reset();
    channelConsumer = channelConsumer.andOnComplete(action2);
    channelConsumer.onOutput("test");
    assertThat(action1.isCalled()).isFalse();
    assertThat(action2.isCalled()).isFalse();
    channelConsumer.onError(new RoutineException());
    assertThat(action1.isCalled()).isFalse();
    assertThat(action2.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(action1.isCalled()).isTrue();
    assertThat(action2.isCalled()).isTrue();
    action1.reset();
    action2.reset();
    channelConsumer = onComplete(action1).andOnComplete(decorate(action2).andThen(action3));
    channelConsumer.onOutput("test");
    assertThat(action1.isCalled()).isFalse();
    assertThat(action2.isCalled()).isFalse();
    assertThat(action3.isCalled()).isFalse();
    channelConsumer.onError(new RoutineException());
    assertThat(action1.isCalled()).isFalse();
    assertThat(action2.isCalled()).isFalse();
    assertThat(action3.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(action1.isCalled()).isTrue();
    assertThat(action2.isCalled()).isTrue();
    assertThat(action3.isCalled()).isTrue();
    action1.reset();
    final TestConsumer<Object> outConsumer = new TestConsumer<Object>();
    final TestConsumer<RoutineException> errorConsumer = new TestConsumer<RoutineException>();
    channelConsumer = onComplete(action1).andOnOutput(outConsumer).andOnError(errorConsumer);
    channelConsumer.onOutput("test");
    assertThat(action1.isCalled()).isFalse();
    assertThat(outConsumer.isCalled()).isTrue();
    assertThat(errorConsumer.isCalled()).isFalse();
    channelConsumer.onError(new RoutineException());
    assertThat(action1.isCalled()).isFalse();
    assertThat(errorConsumer.isCalled()).isTrue();
    channelConsumer.onComplete();
    assertThat(action1.isCalled()).isTrue();
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

      onComplete(Functions.noOp()).andOnComplete(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onComplete(Functions.noOp()).andOnError(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onComplete(Functions.noOp()).andOnOutput(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testOnError() throws Exception {

    final TestConsumer<RoutineException> consumer1 = new TestConsumer<RoutineException>();
    final TestConsumer<RoutineException> consumer2 = new TestConsumer<RoutineException>();
    final TestConsumer<RoutineException> consumer3 = new TestConsumer<RoutineException>();
    ChannelConsumerBuilder<Object> channelConsumer = onError(consumer1);
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isTrue();
    consumer1.reset();
    channelConsumer = channelConsumer.andOnError(consumer2);
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(consumer2.isCalled()).isTrue();
    consumer1.reset();
    consumer2.reset();
    channelConsumer =
        onError(consumer1).andOnError(Functions.decorate(consumer2).andThen(consumer3));
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    assertThat(consumer3.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    assertThat(consumer3.isCalled()).isFalse();
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(consumer2.isCalled()).isTrue();
    assertThat(consumer3.isCalled()).isTrue();
    consumer1.reset();
    final TestConsumer<Object> outConsumer = new TestConsumer<Object>();
    final TestAction completeAction = new TestAction();
    channelConsumer = onError(consumer1).andOnOutput(outConsumer).andOnComplete(completeAction);
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(outConsumer.isCalled()).isTrue();
    assertThat(completeAction.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(completeAction.isCalled()).isTrue();
    channelConsumer.onError(new RoutineException());
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

      onError(Functions.<RoutineException>sink()).andOnComplete(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onError(Functions.<RoutineException>sink()).andOnError(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onError(Functions.<RoutineException>sink()).andOnOutput(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  @Test
  public void testOnOutput() throws Exception {

    final TestConsumer<Object> consumer1 = new TestConsumer<Object>();
    final TestConsumer<Object> consumer2 = new TestConsumer<Object>();
    final TestConsumer<Object> consumer3 = new TestConsumer<Object>();
    ChannelConsumerBuilder<Object> channelConsumer = onOutput(consumer1);
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isTrue();
    consumer1.reset();
    channelConsumer = channelConsumer.andOnOutput(consumer2);
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(consumer2.isCalled()).isTrue();
    consumer1.reset();
    consumer2.reset();
    channelConsumer =
        onOutput(consumer1).andOnOutput(Functions.decorate(consumer2).andThen(consumer3));
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    assertThat(consumer3.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(consumer2.isCalled()).isFalse();
    assertThat(consumer3.isCalled()).isFalse();
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(consumer2.isCalled()).isTrue();
    assertThat(consumer3.isCalled()).isTrue();
    consumer1.reset();
    final TestConsumer<RoutineException> errorConsumer = new TestConsumer<RoutineException>();
    final TestAction completeAction = new TestAction();
    channelConsumer = onOutput(consumer1).andOnError(errorConsumer).andOnComplete(completeAction);
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(errorConsumer.isCalled()).isTrue();
    assertThat(completeAction.isCalled()).isFalse();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(completeAction.isCalled()).isTrue();
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isTrue();
    consumer1.reset();
    errorConsumer.reset();
    completeAction.reset();
    channelConsumer = onOutput(consumer1, errorConsumer);
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(errorConsumer.isCalled()).isFalse();
    assertThat(completeAction.isCalled()).isFalse();
    consumer1.reset();
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(errorConsumer.isCalled()).isTrue();
    assertThat(completeAction.isCalled()).isFalse();
    errorConsumer.reset();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(errorConsumer.isCalled()).isFalse();
    assertThat(completeAction.isCalled()).isFalse();
    channelConsumer = onOutput(consumer1, errorConsumer, completeAction);
    channelConsumer.onOutput("test");
    assertThat(consumer1.isCalled()).isTrue();
    assertThat(errorConsumer.isCalled()).isFalse();
    assertThat(completeAction.isCalled()).isFalse();
    consumer1.reset();
    channelConsumer.onError(new RoutineException());
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(errorConsumer.isCalled()).isTrue();
    assertThat(completeAction.isCalled()).isFalse();
    errorConsumer.reset();
    channelConsumer.onComplete();
    assertThat(consumer1.isCalled()).isFalse();
    assertThat(errorConsumer.isCalled()).isFalse();
    assertThat(completeAction.isCalled()).isTrue();
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

      onOutput(sink()).andOnComplete(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onOutput(sink()).andOnError(null);

      fail();

    } catch (final NullPointerException ignored) {

    }

    try {

      onOutput(sink()).andOnOutput(null);

      fail();

    } catch (final NullPointerException ignored) {

    }
  }

  private static class TestAction implements Action {

    private boolean mIsCalled;

    public boolean isCalled() {

      return mIsCalled;
    }

    public void perform() {

      mIsCalled = true;
    }

    public void reset() {

      mIsCalled = false;
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
