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

package com.github.dm.jrt;

import com.github.dm.jrt.ObjectProxyRoutineBuilder.BuilderType;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ChunkOutputStream;
import com.github.dm.jrt.core.channel.AbortException;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.TemplateChannelConsumer;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.IdentityInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.runner.Runners;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.object.annotation.Alias;
import com.github.dm.jrt.object.annotation.AsyncOutput;
import com.github.dm.jrt.object.annotation.OutputTimeout;
import com.github.dm.jrt.operator.Operators;
import com.github.dm.jrt.proxy.annotation.Proxy;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.Functions.constant;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.object.InvocationTarget.instance;
import static com.github.dm.jrt.operator.Operators.appendAccept;
import static com.github.dm.jrt.operator.sequence.Sequences.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * JRoutine unit tests.
 * <p>
 * Created by davide-maestroni on 02/29/2016.
 */
public class JRoutineTest {

  @Test
  public void testAliasMethod() throws NoSuchMethodException {

    final DurationMeasure timeout = seconds(1);
    final TestClass test = new TestClass();
    final Routine<Object, Object> routine = JRoutine.with(instance(test))
                                                    .applyInvocationConfiguration()
                                                    .withRunner(Runners.syncRunner())
                                                    .withMaxInstances(1)
                                                    .withCoreInstances(1)
                                                    .withOutputTimeoutAction(
                                                        TimeoutActionType.CONTINUE)
                                                    .withLogLevel(Level.DEBUG)
                                                    .withLog(new NullLog())
                                                    .configured()
                                                    .method(TestClass.GET);
    assertThat(routine.close().in(timeout).all()).containsExactly(-77L);
  }

  @Test
  public void testCallFunction() {

    final Routine<String, String> routine = JRoutine.withCall(new Function<List<String>, String>() {

      public String apply(final List<String> strings) {

        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
          builder.append(string);
        }

        return builder.toString();
      }
    }).buildRoutine();
    assertThat(routine.call("test", "1").in(seconds(1)).all()).containsOnly("test1");
  }

  @Test
  public void testChainedRoutine() {

    final DurationMeasure timeout = seconds(1);
    final CallInvocation<Integer, Integer> execSum = new CallInvocation<Integer, Integer>() {

      @Override
      protected void onCall(@NotNull final List<? extends Integer> integers,
          @NotNull final Channel<Integer, ?> result) {
        int sum = 0;
        for (final Integer integer : integers) {
          sum += integer;
        }

        result.pass(sum);
      }
    };

    final Routine<Integer, Integer> sumRoutine = JRoutine.with(factoryOf(execSum, this))
                                                         .applyInvocationConfiguration()
                                                         .withRunner(Runners.syncRunner())
                                                         .configured()
                                                         .buildRoutine();
    final Routine<Integer, Integer> squareRoutine =
        JRoutine.with(functionMapping(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {
            final int i = integer;
            return i * i;
          }
        })).buildRoutine();

    assertThat(sumRoutine.call(squareRoutine.call(1, 2, 3, 4)).in(timeout).all()).containsExactly(
        30);
  }

  @Test
  public void testClassStaticMethod() {

    final TestStatic testStatic = JRoutine.withClassOfType(TestClass.class)
                                          .applyInvocationConfiguration()
                                          .withRunner(Runners.poolRunner())
                                          .withLogLevel(Level.DEBUG)
                                          .withLog(new NullLog())
                                          .configured()
                                          .buildProxy(TestStatic.class);
    try {
      assertThat(testStatic.getOne().all()).containsExactly(1);
      fail();

    } catch (final InvocationException ignored) {

    }

    assertThat(testStatic.getTwo().all()).containsExactly(2);
  }

  @Test
  public void testCommandInvocation() {

    final Routine<Void, String> routine = JRoutine.with(new GetString()).buildRoutine();
    assertThat(routine.close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testConcatReadOutput() throws IOException {

    final Channel<ByteChunk, ByteChunk> channel = JRoutine.<ByteChunk>ofInputs().buildChannel();
    final ChunkOutputStream stream = JRoutine.withOutput(channel)
                                             .applyChunkStreamConfiguration()
                                             .withChunkSize(3)
                                             .configured()
                                             .buildOutputStream();
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ChunkInputStream inputStream = JRoutine.getInputStream(channel.next(), channel.next());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThat(inputStream.read(outputStream)).isEqualTo(3);
    assertThat(outputStream.size()).isEqualTo(3);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155);
    assertThat(inputStream.read(outputStream)).isEqualTo(1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
  }

  @Test
  public void testConcatReadOutput2() throws IOException {

    final Channel<ByteChunk, ByteChunk> channel = JRoutine.<ByteChunk>ofInputs().buildChannel();
    final ChunkOutputStream stream = JRoutine.withOutput(channel)
                                             .applyChunkStreamConfiguration()
                                             .withChunkSize(3)
                                             .configured()
                                             .buildOutputStream();
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ChunkInputStream inputStream =
        JRoutine.getInputStream(channel.eventuallyContinue().all());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThat(inputStream.read(outputStream)).isEqualTo(3);
    assertThat(outputStream.size()).isEqualTo(3);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155);
    assertThat(inputStream.read(outputStream)).isEqualTo(1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
  }

  @Test
  public void testConstructor() {

    boolean failed = false;
    try {
      new JRoutine();
      failed = true;

    } catch (final Throwable ignored) {

    }

    assertThat(failed).isFalse();
  }

  @Test
  public void testConsumerCommand() {

    final Routine<Void, String> routine =
        JRoutine.withCommandConsumer(new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> result) {

            result.pass("test", "1");
          }
        }).buildRoutine();
    assertThat(routine.close().in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testConsumerFunction() {

    final Routine<String, String> routine =
        JRoutine.withCallConsumer(new BiConsumer<List<String>, Channel<String, ?>>() {

          public void accept(final List<String> strings, final Channel<String, ?> result) {

            final StringBuilder builder = new StringBuilder();
            for (final String string : strings) {
              builder.append(string);
            }

            result.pass(builder.toString());
          }
        }).buildRoutine();
    assertThat(routine.call("test", "1").in(seconds(1)).all()).containsOnly("test1");
  }

  @Test
  public void testConsumerMapping() {

    final Routine<Object, String> routine =
        JRoutine.withMappingConsumer(new BiConsumer<Object, Channel<String, ?>>() {

          public void accept(final Object o, final Channel<String, ?> result) {

            result.pass(o.toString());
          }
        }).buildRoutine();
    assertThat(routine.call("test", 1).in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testFunctionMapping() {

    final Routine<Object, String> routine = JRoutine.withMapping(new Function<Object, String>() {

      public String apply(final Object o) {

        return o.toString();
      }
    }).buildRoutine();
    assertThat(routine.call("test", 1).in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testInstance() {
    assertThat(JRoutine.withInstance("test")
                       .method("toString")
                       .close()
                       .in(seconds(1))
                       .all()).containsExactly("test");
  }

  @Test
  public void testInvocation() {

    final Routine<String, String> routine =
        JRoutine.with((Invocation<String, String>) new ToCase()).buildRoutine();
    assertThat(routine.call("TEST").in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testInvocationAndArgs() {

    final Routine<String, String> routine = JRoutine.with(new ToCase(), true).buildRoutine();
    assertThat(routine.call("test").in(seconds(1)).all()).containsOnly("TEST");
  }

  @Test
  public void testInvocationClass() {

    final Routine<String, String> routine = JRoutine.with(ToCase.class).buildRoutine();
    assertThat(routine.call("TEST").in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testInvocationClassAndArgs() {

    final Routine<String, String> routine = JRoutine.with(ToCase.class, true).buildRoutine();
    assertThat(routine.call("test").in(seconds(1)).all()).containsOnly("TEST");
  }

  @Test
  public void testInvocationFactory() {

    final Routine<String, String> routine =
        JRoutine.with((InvocationFactory<String, String>) new ToCase()).buildRoutine();
    assertThat(routine.call("TEST").in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testInvocationToken() {

    final Routine<String, String> routine = JRoutine.with(tokenOf(ToCase.class)).buildRoutine();
    assertThat(routine.call("TEST").in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testInvocationTokenAndArgs() {

    final Routine<String, String> routine =
        JRoutine.with(tokenOf(ToCase.class), true).buildRoutine();
    assertThat(routine.call("test").in(seconds(1)).all()).containsOnly("TEST");
  }

  @Test
  public void testMappingInvocation() {

    final Routine<String, String> routine = JRoutine.with(new ToCase()).buildRoutine();
    assertThat(routine.call("TEST").in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testObjectStaticMethod() {

    final TestClass test = new TestClass();
    final TestStatic testStatic = JRoutine.with(instance(test))
                                          .withType(BuilderType.OBJECT)
                                          .applyInvocationConfiguration()
                                          .withRunner(Runners.poolRunner())
                                          .withLogLevel(Level.DEBUG)
                                          .withLog(new NullLog())
                                          .configured()
                                          .buildProxy(TestStatic.class);
    assertThat(testStatic.getOne().all()).containsExactly(1);
    assertThat(testStatic.getTwo().all()).containsExactly(2);
  }

  @Test
  public void testObjectWrapAlias() {

    final TestClass test = new TestClass();
    final Routine<Object, Object> routine = JRoutine.with(test)
                                                    .applyInvocationConfiguration()
                                                    .withRunner(Runners.syncRunner())
                                                    .withLogLevel(Level.DEBUG)
                                                    .withLog(new NullLog())
                                                    .configured()
                                                    .method(TestClass.GET);
    assertThat(routine.close().all()).containsExactly(-77L);
  }

  @Test
  public void testObjectWrapGeneratedProxy() {

    final TestClass test = new TestClass();
    final TestStatic proxy = JRoutine.with(test)
                                     .withType(BuilderType.PROXY)
                                     .applyInvocationConfiguration()
                                     .withRunner(Runners.poolRunner())
                                     .withLogLevel(Level.DEBUG)
                                     .withLog(new NullLog())
                                     .configured()
                                     .buildProxy(TestStatic.class);
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testObjectWrapGeneratedProxyToken() {

    final TestClass test = new TestClass();
    final TestStatic proxy = JRoutine.with(test)
                                     .applyInvocationConfiguration()
                                     .withRunner(Runners.poolRunner())
                                     .withLogLevel(Level.DEBUG)
                                     .withLog(new NullLog())
                                     .configured()
                                     .buildProxy(tokenOf(TestStatic.class));
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testObjectWrapMethod() throws NoSuchMethodException {

    final TestClass test = new TestClass();
    final Routine<Object, Object> routine = JRoutine.with(test)
                                                    .applyInvocationConfiguration()
                                                    .withRunner(Runners.syncRunner())
                                                    .configured()
                                                    .applyObjectConfiguration()
                                                    .withSharedFields()
                                                    .configured()
                                                    .method(TestClass.class.getMethod("getLong"));
    assertThat(routine.close().all()).containsExactly(-77L);
  }

  @Test
  public void testObjectWrapMethodName() {

    final TestClass test = new TestClass();
    final Routine<Object, Object> routine = JRoutine.with(test)
                                                    .applyInvocationConfiguration()
                                                    .withRunner(Runners.syncRunner())
                                                    .withLogLevel(Level.DEBUG)
                                                    .withLog(new NullLog())
                                                    .configured()
                                                    .method("getLong");
    assertThat(routine.close().all()).containsExactly(-77L);
  }

  @Test
  public void testObjectWrapProxy() {

    final TestClass test = new TestClass();
    final TestItf proxy = JRoutine.with(test)
                                  .applyInvocationConfiguration()
                                  .withRunner(Runners.poolRunner())
                                  .withLogLevel(Level.DEBUG)
                                  .withLog(new NullLog())
                                  .configured()
                                  .buildProxy(TestItf.class);
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testObjectWrapProxyToken() {

    final TestClass test = new TestClass();
    final TestItf proxy = JRoutine.with(test)
                                  .applyInvocationConfiguration()
                                  .withRunner(Runners.poolRunner())
                                  .withLogLevel(Level.DEBUG)
                                  .withLog(new NullLog())
                                  .configured()
                                  .buildProxy(tokenOf(TestItf.class));
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testOf() {
    final Channel<?, Integer> channel = JRoutine.of(2).buildChannel();
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.in(seconds(1)).all()).containsExactly(2);
    assertThat(JRoutine.of().buildChannel().in(seconds(1)).all()).isEmpty();
    assertThat(JRoutine.of(-11, 73).buildChannel().in(seconds(1)).all()).containsExactly(-11, 73);
    assertThat(
        JRoutine.of(Arrays.asList(3, 12, -7)).buildChannel().in(seconds(1)).all()).containsExactly(
        3, 12, -7);
    assertThat(JRoutine.of((Object[]) null).buildChannel().all()).isEmpty();
    assertThat(JRoutine.of((List<Object>) null).buildChannel().all()).isEmpty();
  }

  @Test
  public void testPendingInputs() {

    final Channel<Object, Object> channel = JRoutine.with(IdentityInvocation.factoryOf()).call();
    assertThat(channel.isOpen()).isTrue();
    channel.pass("test");
    assertThat(channel.isOpen()).isTrue();
    channel.after(millis(500)).pass("test");
    assertThat(channel.isOpen()).isTrue();
    final Channel<Object, Object> outputChannel = JRoutine.ofInputs().buildChannel();
    channel.afterNoDelay().pass(outputChannel);
    assertThat(channel.isOpen()).isTrue();
    channel.close();
    assertThat(channel.isOpen()).isFalse();
    outputChannel.close();
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testPredicateFilter() {

    final Routine<String, String> routine = JRoutine.withFilter(new Predicate<String>() {

      public boolean test(final String s) {

        return s.length() > 1;
      }
    }).buildRoutine();
    assertThat(routine.call("test", "1").in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testProxyConfiguration() {

    final TestClass test = new TestClass();
    final TestItf proxy = JRoutine.with(test)
                                  .applyInvocationConfiguration()
                                  .withRunner(Runners.poolRunner())
                                  .withLogLevel(Level.DEBUG)
                                  .withLog(new NullLog())
                                  .configured()
                                  .applyObjectConfiguration()
                                  .withSharedFields()
                                  .configured()
                                  .buildProxy(TestItf.class);
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testProxyError() {

    try {
      JRoutine.with(TestItf.class);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testReadAll() throws IOException {

    final Channel<ByteChunk, ByteChunk> channel = JRoutine.<ByteChunk>ofInputs().buildChannel();
    final ChunkOutputStream stream = JRoutine.withOutput(channel).buildOutputStream();
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ChunkInputStream inputStream = JRoutine.getInputStream(channel.next());
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    assertThat(inputStream.readAll(outputStream)).isEqualTo(4);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
    assertThat(inputStream.read(outputStream)).isEqualTo(-1);
    assertThat(outputStream.size()).isEqualTo(4);
    assertThat(outputStream.toByteArray()).containsExactly((byte) 31, (byte) 17, (byte) 155,
        (byte) 13);
  }

  @Test
  public void testStream() {
    assertThat(JRoutine.<Integer>withStream().map(appendAccept(range(1, 1000)))
                                             .map(new Function<Number, Double>() {

                                               public Double apply(final Number number) {
                                                 return Math.sqrt(number.doubleValue());
                                               }
                                             })
                                             .sync()
                                             .map(Operators.<Double>averageDouble())
                                             .close()
                                             .in(seconds(3))
                                             .next()).isCloseTo(21, Offset.offset(0.1));
  }

  @Test
  public void testStreamAccept() {
    assertThat(JRoutine.withStreamAccept(range(0, 3)).immediate().close().all()).containsExactly(0,
        1, 2, 3);
    assertThat(JRoutine.withStreamAccept(2, range(1, 0)).immediate().close().all()).containsExactly(
        1, 0, 1, 0);
  }

  @Test
  public void testStreamAcceptAbort() {
    Channel<Integer, Integer> channel = JRoutine.withStreamAccept(range(0, 3)).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutine.withStreamAccept(2, range(1, 0)).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamAcceptError() {
    assertThat(JRoutine.withStreamAccept(range(0, 3))
                       .immediate()
                       .call(31)
                       .getError()
                       .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutine.withStreamAccept(2, range(1, 0))
                       .immediate()
                       .call(-17)
                       .getError()
                       .getCause()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testStreamGet() {
    assertThat(JRoutine.withStreamGet(constant("test")).immediate().close().all()).containsExactly(
        "test");
    assertThat(
        JRoutine.withStreamGet(2, constant("test2")).immediate().close().all()).containsExactly(
        "test2", "test2");
  }

  @Test
  public void testStreamGetAbort() {
    Channel<String, String> channel =
        JRoutine.withStreamGet(constant("test")).immediate().immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutine.withStreamGet(2, constant("test2")).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamGetError() {
    assertThat(JRoutine.withStreamGet(constant("test"))
                       .immediate()
                       .call("test")
                       .getError()
                       .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(
        JRoutine.withStreamGet(2, constant("test2")).immediate().call("test").getError().getCause())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testStreamOf() {
    assertThat(JRoutine.withStreamOf("test").immediate().close().all()).containsExactly("test");
    assertThat(
        JRoutine.withStreamOf("test1", "test2", "test3").immediate().close().all()).containsExactly(
        "test1", "test2", "test3");
    assertThat(JRoutine.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                       .immediate()
                       .close()
                       .all()).containsExactly("test1", "test2", "test3");
    assertThat(JRoutine.withStreamOf(JRoutine.of("test1", "test2", "test3").buildChannel())
                       .immediate()
                       .close()
                       .all()).containsExactly("test1", "test2", "test3");
  }

  @Test
  public void testStreamOfAbort() {
    Channel<String, String> channel = JRoutine.withStreamOf("test").immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutine.withStreamOf("test1", "test2", "test3").immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutine.withStreamOf(Arrays.asList("test1", "test2", "test3")).immediate().call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
    channel = JRoutine.withStreamOf(JRoutine.of("test1", "test2", "test3").buildChannel())
                      .immediate()
                      .call();
    assertThat(channel.abort()).isTrue();
    assertThat(channel.getError()).isInstanceOf(AbortException.class);
  }

  @Test
  @SuppressWarnings({"ConstantConditions", "ThrowableResultOfMethodCallIgnored"})
  public void testStreamOfError() {
    assertThat(
        JRoutine.withStreamOf("test").immediate().call("test").getError().getCause()).isInstanceOf(
        IllegalStateException.class);
    assertThat(JRoutine.withStreamOf("test1", "test2", "test3")
                       .immediate()
                       .call("test")
                       .getError()
                       .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutine.withStreamOf(Arrays.asList("test1", "test2", "test3"))
                       .immediate()
                       .call("test")
                       .getError()
                       .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutine.withStreamOf(JRoutine.of("test1", "test2", "test3").buildChannel())
                       .immediate()
                       .call("test")
                       .getError()
                       .getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(JRoutine.withStreamOf(
        JRoutine.ofInputs().buildChannel().bind(new TemplateChannelConsumer<Object>() {}))
                       .immediate()
                       .close()
                       .getError()
                       .getCause()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testSupplierCommand() {

    final Routine<Void, String> routine = JRoutine.withCommand(new Supplier<String>() {

      public String get() {

        return "test";
      }
    }).buildRoutine();
    assertThat(routine.close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testSupplierFactory() {

    final Routine<String, String> routine = JRoutine.withFactory(new Supplier<ToCase>() {

      public ToCase get() {

        return new ToCase();
      }
    }).buildRoutine();
    assertThat(routine.call("TEST").in(seconds(1)).all()).containsOnly("test");
  }

  public interface TestItf {

    @OutputTimeout(300)
    @AsyncOutput
    Channel<?, Integer> getOne();
  }

  @Proxy(TestClass.class)
  public interface TestStatic {

    @OutputTimeout(300)
    @AsyncOutput
    Channel<?, Integer> getOne();

    @OutputTimeout(300)
    @AsyncOutput
    Channel<?, Integer> getTwo();
  }

  public static class GetString extends CommandInvocation<String> {

    /**
     * Constructor.
     */
    protected GetString() {

      super(null);
    }

    public void onComplete(@NotNull final Channel<String, ?> result) {

      result.pass("test");
    }
  }

  @SuppressWarnings("unused")
  public static class TestClass {

    public static final String GET = "get";

    public static final String THROW = "throw";

    public static int getTwo() {

      return 2;
    }

    @Alias(GET)
    public long getLong() {

      return -77;
    }

    public int getOne() {

      return 1;
    }

    @Alias(THROW)
    public void throwException(final RuntimeException ex) {

      throw ex;
    }
  }

  public static class ToCase extends MappingInvocation<String, String> {

    private final boolean mIsUpper;

    public ToCase() {

      this(false);
    }

    public ToCase(final boolean isUpper) {

      super(asArgs(isUpper));
      mIsUpper = isUpper;
    }

    public void onInput(final String input, @NotNull final Channel<String, ?> result) {

      result.pass(mIsUpper ? input.toUpperCase() : input.toLowerCase());
    }
  }
}
