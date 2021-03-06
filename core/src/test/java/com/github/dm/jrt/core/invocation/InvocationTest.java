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

import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Invocations unit tests.
 * <p>
 * Created by davide-maestroni on 02/16/2015.
 */
public class InvocationTest {

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testCommandInvocation() throws Exception {
    final CommandInvocation<Object> invocation = new CommandInvocation<Object>(null) {

      public void onComplete(@NotNull final Channel<Object, ?> result) throws Exception {
      }
    };
    invocation.onRestart();
    invocation.onInput(null, null);
    invocation.onComplete(null);
    invocation.onAbort(null);
  }

  @Test
  public void testComparableCommandInvocation() {
    final TestComparableCommandInvocation factory = new TestComparableCommandInvocation(asArgs(1));
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo(new InvocationFactory<Object, Object>(null) {

      @NotNull
      @Override
      public Invocation<Object, Object> newInvocation() {
        return new TemplateInvocation<Object, Object>() {};
      }
    });
    assertThat(factory).isNotEqualTo(new TestComparableCommandInvocation(asArgs(2)));
    assertThat(factory.hashCode()).isEqualTo(
        new TestComparableCommandInvocation(asArgs(1)).hashCode());
    assertThat(factory).isEqualTo(new TestComparableCommandInvocation(asArgs(1)));
  }

  @Test
  public void testComparableInvocationFactory() {
    final TestComparableInvocationFactory factory = new TestComparableInvocationFactory(asArgs(1));
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo(new InvocationFactory<Object, Object>(null) {

      @NotNull
      @Override
      public Invocation<Object, Object> newInvocation() {
        return new TemplateInvocation<Object, Object>() {};
      }
    });
    assertThat(factory).isNotEqualTo(new TestComparableInvocationFactory(asArgs(2)));
    assertThat(factory.hashCode()).isEqualTo(
        new TestComparableInvocationFactory(asArgs(1)).hashCode());
    assertThat(factory).isEqualTo(new TestComparableInvocationFactory(asArgs(1)));
  }

  @Test
  public void testComparableMappingInvocation() {
    final TestComparableMappingInvocation factory = new TestComparableMappingInvocation(asArgs(1));
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(null);
    assertThat(factory).isNotEqualTo(new InvocationFactory<Object, Object>(null) {

      @NotNull
      @Override
      public Invocation<Object, Object> newInvocation() {
        return new TemplateInvocation<Object, Object>() {};
      }
    });
    assertThat(factory).isNotEqualTo(new TestComparableMappingInvocation(asArgs(2)));
    assertThat(factory.hashCode()).isEqualTo(
        new TestComparableMappingInvocation(asArgs(1)).hashCode());
    assertThat(factory).isEqualTo(new TestComparableMappingInvocation(asArgs(1)));
  }

  @Test
  @SuppressWarnings("NullArgumentToVariableArgMethod")
  public void testInvocationFactory() throws Exception {
    assertThat(factoryOf(TestInvocation.class).newInvocation()).isExactlyInstanceOf(
        TestInvocation.class);
    assertThat(
        factoryOf(ClassToken.tokenOf(TestInvocation.class)).newInvocation()).isExactlyInstanceOf(
        TestInvocation.class);
    assertThat(factoryOf(new TestInvocation()).newInvocation()).isExactlyInstanceOf(
        TestInvocation.class);
  }

  @Test
  public void testInvocationFactoryEquals() {
    final InvocationFactory<Object, Object> factory = factoryOf(TestInvocation.class);
    assertThat(factory).isEqualTo(factory);
    assertThat(factory).isNotEqualTo(new InvocationFactory<Object, Object>(null) {

      @NotNull
      @Override
      public Invocation<Object, Object> newInvocation() {
        return new TemplateInvocation<Object, Object>() {};
      }
    });
    assertThat(factoryOf(TestInvocation.class).hashCode()).isEqualTo(
        factoryOf(TestInvocation.class).hashCode());
    assertThat(factoryOf(TestInvocation.class)).isEqualTo(factoryOf(TestInvocation.class));
    assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode()).isEqualTo(
        factoryOf(TestInvocation.class).hashCode());
    assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class))).isEqualTo(
        factoryOf(TestInvocation.class));
    assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode()).isEqualTo(
        factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode());
    assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class))).isEqualTo(
        factoryOf(ClassToken.tokenOf(TestInvocation.class)));
    assertThat(factoryOf(TestInvocation.class).hashCode()).isNotEqualTo(
        factoryOf(new TemplateInvocation<Object, Object>() {}, this).hashCode());
    assertThat(factoryOf(TestInvocation.class)).isNotEqualTo(
        factoryOf(new TemplateInvocation<Object, Object>() {}, this));
    assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class)).hashCode()).isNotEqualTo(
        factoryOf(new TemplateInvocation<Object, Object>() {}, this).hashCode());
    assertThat(factoryOf(ClassToken.tokenOf(TestInvocation.class))).isNotEqualTo(
        factoryOf(new TemplateInvocation<Object, Object>() {}, this));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNullClassError() {
    try {
      factoryOf((Class<TestInvocation>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      factoryOf((Class<TestInvocation>) null, Reflection.NO_ARGS);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNullInvocationError() {
    try {
      factoryOf((TestInvocation) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      factoryOf((TestInvocation) null, Reflection.NO_ARGS);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testNullTokenError() {
    try {
      factoryOf((ClassToken<TestInvocation>) null);
      fail();

    } catch (final NullPointerException ignored) {
    }

    try {
      factoryOf((ClassToken<TestInvocation>) null, Reflection.NO_ARGS);
      fail();

    } catch (final NullPointerException ignored) {
    }
  }

  private static class TestComparableCommandInvocation extends CommandInvocation<Object> {

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected TestComparableCommandInvocation(@Nullable final Object[] args) {
      super(args);
    }

    public void onComplete(@NotNull final Channel<Object, ?> result) {
    }
  }

  private static class TestComparableInvocationFactory extends InvocationFactory<Object, Object> {

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected TestComparableInvocationFactory(@Nullable final Object[] args) {
      super(args);
    }

    @NotNull
    @Override
    public Invocation<Object, Object> newInvocation() {
      return new TemplateInvocation<Object, Object>() {};
    }
  }

  private static class TestComparableMappingInvocation extends MappingInvocation<Object, Object> {

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected TestComparableMappingInvocation(@Nullable final Object[] args) {
      super(args);
    }

    public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
    }
  }

  private static class TestInvocation extends MappingInvocation<Object, Object> {

    /**
     * Constructor.
     */
    protected TestInvocation() {
      super(null);
    }

    public void onInput(final Object o, @NotNull final Channel<Object, ?> result) {
    }
  }
}
