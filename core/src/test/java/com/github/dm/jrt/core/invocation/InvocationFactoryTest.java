/*
 * Copyright 2016 Davide Maestroni
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

package com.github.dm.jrt.core.invocation;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.routine.Routine;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocation factories unit test.
 * <p>
 * Created by davide-maestroni on 08/19/2015.
 */
public class InvocationFactoryTest {

  @Test
  public void testDecoratingInvocationFactory() throws Exception {
    final InvocationFactory<String, String> factory = IdentityInvocation.factory();
    assertThat(factory.newInvocation()).isExactlyInstanceOf(IdentityInvocation.class);
    final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
    assertThat(decoratedFactory.newInvocation()).isExactlyInstanceOf(TestInvocationDecorator.class);
  }

  @Test
  public void testDecoratingInvocationFactoryEquals() {
    final InvocationFactory<String, String> factory = IdentityInvocation.factory();
    final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
    assertThat(decoratedFactory).isEqualTo(decoratedFactory);
    assertThat(decoratedFactory).isNotEqualTo(null);
    assertThat(decoratedFactory).isNotEqualTo("test");
    assertThat(decoratedFactory).isNotEqualTo(new TestInvocationFactory(
        factoryOf(TestInvocationDecorator.class, (Invocation<?, ?>) null)));
    assertThat(decoratedFactory).isEqualTo(new TestInvocationFactory(factory));
    assertThat(decoratedFactory.hashCode()).isEqualTo(
        new TestInvocationFactory(factory).hashCode());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testDecoratingInvocationFactoryError() {
    try {
      new TestInvocationFactory(null);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  public void testInvocationDecoratorAbort() {
    final InvocationFactory<String, String> factory = IdentityInvocation.factory();
    final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
    final Routine<String, String> routine = JRoutineCore.routine().of(decoratedFactory);
    assertThat(routine.invoke().after(millis(100)).pass("test").afterNoDelay().abort()).isTrue();
  }

  @Test
  public void testInvocationDecoratorLifecycle() {
    final InvocationFactory<String, String> factory = IdentityInvocation.factory();
    final TestInvocationFactory decoratedFactory = new TestInvocationFactory(factory);
    final Routine<String, String> routine = JRoutineCore.routine().of(decoratedFactory);
    assertThat(routine.invoke().pass("test").close().in(seconds(1)).all()).containsExactly("test");
  }

  private static class TestInvocationDecorator extends InvocationDecorator<String, String> {

    /**
     * Constructor.
     *
     * @param wrapped the wrapped invocation instance.
     */
    public TestInvocationDecorator(@NotNull final Invocation<String, String> wrapped) {
      super(wrapped);
    }
  }

  private static class TestInvocationFactory extends DecoratingInvocationFactory<String, String> {

    /**
     * Constructor.
     *
     * @param wrapped the wrapped factory instance.
     */
    public TestInvocationFactory(@NotNull final InvocationFactory<String, String> wrapped) {
      super(wrapped);
    }

    @NotNull
    @Override
    protected Invocation<String, String> decorate(
        @NotNull final Invocation<String, String> invocation) {
      return new TestInvocationDecorator(invocation);
    }
  }
}
