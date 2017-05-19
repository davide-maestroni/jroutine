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

import com.github.dm.jrt.WrapperRoutineBuilder.ProxyStrategyType;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunk;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkInputStream;
import com.github.dm.jrt.channel.io.ByteChannel.ByteChunkOutputStream;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.channel.ChannelConsumer;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.ChannelConfiguration.TimeoutActionType;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.log.Log.Level;
import com.github.dm.jrt.core.log.NullLog;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.DurationMeasure;
import com.github.dm.jrt.function.builder.FunctionalChannelConsumer;
import com.github.dm.jrt.function.util.Action;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;
import com.github.dm.jrt.operator.JRoutineOperators;
import com.github.dm.jrt.proxy.annotation.Proxy;
import com.github.dm.jrt.reflect.annotation.Alias;
import com.github.dm.jrt.reflect.annotation.AsyncOutput;
import com.github.dm.jrt.reflect.annotation.OutputTimeout;

import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.github.dm.jrt.JRoutine.onComplete;
import static com.github.dm.jrt.JRoutine.onError;
import static com.github.dm.jrt.JRoutine.onOutput;
import static com.github.dm.jrt.JRoutine.streamLifter;
import static com.github.dm.jrt.JRoutine.streamLifterOn;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.immediateExecutor;
import static com.github.dm.jrt.core.executor.ScheduledExecutors.syncExecutor;
import static com.github.dm.jrt.core.invocation.InvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.core.util.DurationMeasure.millis;
import static com.github.dm.jrt.core.util.DurationMeasure.seconds;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.function.util.ActionDecorator.wrapAction;
import static com.github.dm.jrt.function.util.ConsumerDecorator.wrapConsumer;
import static com.github.dm.jrt.operator.JRoutineOperators.average;
import static com.github.dm.jrt.operator.JRoutineOperators.filter;
import static com.github.dm.jrt.operator.JRoutineOperators.unary;
import static com.github.dm.jrt.operator.sequence.Sequence.range;
import static com.github.dm.jrt.reflect.InvocationTarget.classOfType;
import static com.github.dm.jrt.reflect.InvocationTarget.instance;
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
    final Routine<Object, Object> routine = JRoutine.wrapperOn(syncExecutor())
                                                    .withInvocation()
                                                    .withMaxInvocations(1)
                                                    .withCoreInvocations(1)
                                                    .withOutputTimeoutAction(
                                                        TimeoutActionType.CONTINUE)
                                                    .withLogLevel(Level.DEBUG)
                                                    .withLog(new NullLog())
                                                    .configuration()
                                                    .methodOf(instance(test), TestClass.GET);
    assertThat(routine.invoke().close().in(timeout).all()).containsExactly(-77L);
  }

  @Test
  public void testCallFunction() {

    final Routine<String, String> routine1 =
        JRoutine.<String, String, StringBuilder>stateful().onCreate(new Supplier<StringBuilder>() {

          public StringBuilder get() {
            return new StringBuilder();
          }
        }).onNextState(new BiFunction<StringBuilder, String, StringBuilder>() {

          public StringBuilder apply(final StringBuilder builder, final String s) {
            return builder.append(s);
          }
        }).onCompleteOutput(new Function<StringBuilder, String>() {

          public String apply(final StringBuilder builder) {
            return builder.toString();
          }
        }).routine();
    assertThat(routine1.invoke().pass("test", "1").close().in(seconds(1)).all()).containsOnly(
        "test1");

    final Routine<String, String> routine2 =
        JRoutine.<String, String, List<String>>statefulOn(syncExecutor()).onCreate(
            new Supplier<List<String>>() {

              public List<String> get() {
                return new ArrayList<String>();
              }
            }).onNextConsume(new BiConsumer<List<String>, String>() {

          public void accept(final List<String> list, final String s) {
            list.add(s);
          }
        }).onCompleteOutput(new Function<List<String>, String>() {

          public String apply(final List<String> strings) throws Exception {
            final StringBuilder builder = new StringBuilder();
            for (final String string : strings) {
              builder.append(string);
            }

            return builder.toString();
          }
        }).routine();
    assertThat(routine2.invoke().pass("test", "1").close().in(seconds(1)).all()).containsOnly(
        "test1");
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

    final Routine<Integer, Integer> sumRoutine =
        JRoutine.routineOn(syncExecutor()).of(factoryOf(execSum, this));
    final Routine<Integer, Integer> squareRoutine =
        JRoutine.routine().of(unary(new Function<Integer, Integer>() {

          public Integer apply(final Integer integer) {
            final int i = integer;
            return i * i;
          }
        }));

    assertThat(sumRoutine.invoke()
                         .pass(squareRoutine.invoke().pass(1, 2, 3, 4).close())
                         .close()
                         .in(timeout)
                         .all()).containsExactly(30);
  }

  @Test
  public void testChannelHandler() {
    assertThat(JRoutine.channelHandlerOn(immediateExecutor()).channelOf(new Callable<String>() {

      public String call() {
        return "test";
      }
    }).next()).isEqualTo("test");
    assertThat(JRoutine.channelHandler().channelOf(new Callable<String>() {

      public String call() {
        return "test";
      }
    }).in(seconds(1)).next()).isEqualTo("test");
  }

  @Test
  public void testClassStaticMethod() {

    final TestStatic testStatic = JRoutine.wrapperOn(ScheduledExecutors.poolExecutor())
                                          .withInvocation()
                                          .withLogLevel(Level.DEBUG)
                                          .withLog(new NullLog())
                                          .configuration()
                                          .proxyOf(classOfType(TestClass.class), TestStatic.class);
    try {
      assertThat(testStatic.getOne().all()).containsExactly(1);
      fail();

    } catch (final InvocationException ignored) {

    }

    assertThat(testStatic.getTwo().all()).containsExactly(2);
  }

  @Test
  public void testCommandInvocation() {

    final Routine<Void, String> routine = JRoutine.routine().of(new GetString());
    assertThat(routine.invoke().close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testConcatReadOutput() throws IOException {

    final Channel<ByteChunk, ByteChunk> channel = JRoutine.channel().ofType();
    final ByteChunkOutputStream stream =
        JRoutine.outputStream().withStream().withChunkSize(3).configuration().of(channel);
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ByteChunkInputStream inputStream = JRoutine.inputStream(channel.next(), channel.next());
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

    final Channel<ByteChunk, ByteChunk> channel = JRoutine.channelOn(syncExecutor()).ofType();
    final ByteChunkOutputStream stream =
        JRoutine.outputStream().withStream().withChunkSize(3).configuration().of(channel);
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ByteChunkInputStream inputStream =
        JRoutine.inputStream(channel.eventuallyContinue().all());
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
        JRoutine.<Void, String>stateless().onComplete(new Consumer<Channel<String, ?>>() {

          public void accept(final Channel<String, ?> result) {

            result.pass("test", "1");
          }
        }).routine();
    assertThat(routine.invoke().close().in(seconds(1)).all()).containsOnly("test", "1");
  }

  @Test
  public void testConsumerMapping() {

    final Routine<Object, String> routine =
        JRoutine.<Object, String>statelessOn(syncExecutor()).onNext(
            new BiConsumer<Object, Channel<String, ?>>() {

              public void accept(final Object o, final Channel<String, ?> result) {

                result.pass(o.toString());
              }
            }).routine();
    assertThat(routine.invoke().pass("test", 1).close().in(seconds(1)).all()).containsOnly("test",
        "1");
  }

  @Test
  public void testFlatten() {
    final Channel<String, String> outputChannel = JRoutine.channel().ofType();
    final Channel<Integer, Integer> inputChannel = JRoutine.channel().ofType();
    inputChannel.consume(new ChannelConsumer<Integer>() {

      public void onComplete() {
        outputChannel.close();
      }

      public void onError(@NotNull final RoutineException error) {
        outputChannel.abort(error);
      }

      public void onOutput(final Integer output) {
        outputChannel.pass(output.toString());
      }
    });
    assertThat(JRoutine.flatten(inputChannel, outputChannel)
                       .pass(1, 2, 3)
                       .close()
                       .in(seconds(1))
                       .all()).containsExactly("1", "2", "3");
  }

  @Test
  public void testInstance() {
    assertThat(JRoutine.wrapper()
                       .methodOf(instance("test"), "toString")
                       .invoke()
                       .close()
                       .in(seconds(1))
                       .all()).containsExactly("test");
  }

  @Test
  public void testInvocation() {

    final Routine<String, String> routine = JRoutine.routine().ofSingleton(new ToCase());
    assertThat(routine.invoke().pass("TEST").close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testInvocationAndArgs() {

    final Routine<String, String> routine = JRoutine.routine().of(factoryOf(new ToCase(), true));
    assertThat(routine.invoke().pass("test").close().in(seconds(1)).all()).containsOnly("TEST");
  }

  @Test
  public void testInvocationClass() {

    final Routine<String, String> routine = JRoutine.routine().of(factoryOf(ToCase.class));
    assertThat(routine.invoke().pass("TEST").close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testInvocationClassAndArgs() {

    final Routine<String, String> routine = JRoutine.routine().of(factoryOf(ToCase.class, true));
    assertThat(routine.invoke().pass("test").close().in(seconds(1)).all()).containsOnly("TEST");
  }

  @Test
  public void testInvocationToken() {

    final Routine<String, String> routine = JRoutine.routine().of(factoryOf(tokenOf(ToCase.class)));
    assertThat(routine.invoke().pass("TEST").close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testInvocationTokenAndArgs() {

    final Routine<String, String> routine =
        JRoutine.routine().of(factoryOf(tokenOf(ToCase.class), true));
    assertThat(routine.invoke().pass("test").close().in(seconds(1)).all()).containsOnly("TEST");
  }

  @Test
  public void testMappingInvocation() {

    final Routine<String, String> routine = JRoutine.routine().of(new ToCase());
    assertThat(routine.invoke().pass("TEST").close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testObjectStaticMethod() {

    final TestClass test = new TestClass();
    final TestStatic testStatic = JRoutine.wrapperOn(ScheduledExecutors.poolExecutor())
                                          .withStrategy(ProxyStrategyType.REFLECTION)
                                          .withInvocation()
                                          .withLogLevel(Level.DEBUG)
                                          .withLog(new NullLog())
                                          .configuration()
                                          .proxyOf(instance(test), TestStatic.class);
    assertThat(testStatic.getOne().all()).containsExactly(1);
    assertThat(testStatic.getTwo().all()).containsExactly(2);
  }

  @Test
  public void testObjectWrapAlias() {

    final TestClass test = new TestClass();
    final Routine<Object, Object> routine = JRoutine.wrapperOn(syncExecutor())
                                                    .withInvocation()
                                                    .withLogLevel(Level.DEBUG)
                                                    .withLog(new NullLog())
                                                    .configuration()
                                                    .methodOf(instance(test), TestClass.GET);
    assertThat(routine.invoke().close().all()).containsExactly(-77L);
  }

  @Test
  public void testObjectWrapGeneratedProxy() {

    final TestClass test = new TestClass();
    final TestStatic proxy = JRoutine.wrapperOn(ScheduledExecutors.poolExecutor())
                                     .withStrategy(ProxyStrategyType.CODE_GENERATION)
                                     .withInvocation()
                                     .withLogLevel(Level.DEBUG)
                                     .withLog(new NullLog())
                                     .configuration()
                                     .proxyOf(instance(test), TestStatic.class);
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testObjectWrapGeneratedProxyToken() {

    final TestClass test = new TestClass();
    final TestStatic proxy = JRoutine.wrapperOn(ScheduledExecutors.poolExecutor())
                                     .withInvocation()
                                     .withLogLevel(Level.DEBUG)
                                     .withLog(new NullLog())
                                     .configuration()
                                     .proxyOf(instance(test), tokenOf(TestStatic.class));
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testObjectWrapMethod() throws NoSuchMethodException {

    final TestClass test = new TestClass();
    final Routine<Object, Object> routine = JRoutine.wrapperOn(syncExecutor())
                                                    .withWrapper()
                                                    .withSharedFields()
                                                    .configuration()
                                                    .methodOf(instance(test),
                                                        TestClass.class.getMethod("getLong"));
    assertThat(routine.invoke().close().all()).containsExactly(-77L);
  }

  @Test
  public void testObjectWrapMethodName() {

    final TestClass test = new TestClass();
    final Routine<Object, Object> routine = JRoutine.wrapperOn(syncExecutor())
                                                    .withInvocation()
                                                    .withLogLevel(Level.DEBUG)
                                                    .withLog(new NullLog())
                                                    .configuration()
                                                    .methodOf(instance(test), "getLong");
    assertThat(routine.invoke().close().all()).containsExactly(-77L);
  }

  @Test
  public void testObjectWrapProxy() {

    final TestClass test = new TestClass();
    final TestItf proxy = JRoutine.wrapperOn(ScheduledExecutors.poolExecutor())
                                  .withInvocation()
                                  .withLogLevel(Level.DEBUG)
                                  .withLog(new NullLog())
                                  .configuration()
                                  .proxyOf(instance(test), TestItf.class);
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testObjectWrapProxyToken() {

    final TestClass test = new TestClass();
    final TestItf proxy = JRoutine.wrapperOn(ScheduledExecutors.poolExecutor())
                                  .withInvocation()
                                  .withLogLevel(Level.DEBUG)
                                  .withLog(new NullLog())
                                  .configuration()
                                  .proxyOf(instance(test), tokenOf(TestItf.class));
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testOf() {
    final Channel<?, Integer> channel = JRoutine.channel().of(2);
    assertThat(channel.isOpen()).isFalse();
    assertThat(channel.in(seconds(1)).all()).containsExactly(2);
    assertThat(JRoutine.channel().of().in(seconds(1)).all()).isEmpty();
    assertThat(JRoutine.channel().of(-11, 73).in(seconds(1)).all()).containsExactly(-11, 73);
    assertThat(
        JRoutine.channel().of(Arrays.asList(3, 12, -7)).in(seconds(1)).all()).containsExactly(3, 12,
        -7);
    assertThat(JRoutine.channel().of((Object[]) null).all()).isEmpty();
    assertThat(JRoutine.channel().of((List<Object>) null).all()).isEmpty();
  }

  @Test
  public void testOnComplete() throws Exception {
    final TestAction action1 = new TestAction();
    final TestAction action2 = new TestAction();
    final TestAction action3 = new TestAction();
    FunctionalChannelConsumer<Object> channelConsumer = onComplete(action1);
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
    channelConsumer = onComplete(action1).andOnComplete(wrapAction(action2).andThen(action3));
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
  public void testOnError() throws Exception {
    final TestConsumer<RoutineException> consumer1 = new TestConsumer<RoutineException>();
    final TestConsumer<RoutineException> consumer2 = new TestConsumer<RoutineException>();
    final TestConsumer<RoutineException> consumer3 = new TestConsumer<RoutineException>();
    FunctionalChannelConsumer<Object> channelConsumer = onError(consumer1);
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
    channelConsumer = onError(consumer1).andOnError(wrapConsumer(consumer2).andThen(consumer3));
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
  public void testOnOutput() throws Exception {
    final TestConsumer<Object> consumer1 = new TestConsumer<Object>();
    final TestConsumer<Object> consumer2 = new TestConsumer<Object>();
    final TestConsumer<Object> consumer3 = new TestConsumer<Object>();
    FunctionalChannelConsumer<Object> channelConsumer = onOutput(consumer1);
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
    channelConsumer = onOutput(consumer1).andOnOutput(wrapConsumer(consumer2).andThen(consumer3));
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
  public void testPendingInputs() {

    final Channel<Object, Object> channel =
        JRoutine.routine().of(JRoutineOperators.identity()).invoke();
    assertThat(channel.isOpen()).isTrue();
    channel.pass("test");
    assertThat(channel.isOpen()).isTrue();
    channel.after(millis(500)).pass("test");
    assertThat(channel.isOpen()).isTrue();
    final Channel<Object, Object> outputChannel = JRoutine.channelOn(syncExecutor()).ofType();
    channel.afterNoDelay().pass(outputChannel);
    assertThat(channel.isOpen()).isTrue();
    channel.close();
    assertThat(channel.isOpen()).isFalse();
    outputChannel.close();
    assertThat(channel.isOpen()).isFalse();
  }

  @Test
  public void testPredicateFilter() {

    final Routine<String, String> routine = JRoutine.routine().of(filter(new Predicate<String>() {

      public boolean test(final String s) {

        return s.length() > 1;
      }
    }));
    assertThat(routine.invoke().pass("test", "1").close().in(seconds(1)).all()).containsOnly(
        "test");
  }

  @Test
  public void testProxyConfiguration() {

    final TestClass test = new TestClass();
    final TestItf proxy = JRoutine.wrapperOn(ScheduledExecutors.poolExecutor())
                                  .withInvocation()
                                  .withLogLevel(Level.DEBUG)
                                  .withLog(new NullLog())
                                  .configuration()
                                  .withWrapper()
                                  .withSharedFields()
                                  .configuration()
                                  .proxyOf(instance(test), TestItf.class);
    assertThat(proxy.getOne().all()).containsExactly(1);
  }

  @Test
  public void testProxyError() {

    try {
      JRoutine.wrapper().proxyOf(classOfType(TestItf.class), TestItf.class);
      fail();

    } catch (final IllegalArgumentException ignored) {

    }
  }

  @Test
  public void testReadAll() throws IOException {

    final Channel<ByteChunk, ByteChunk> channel = JRoutine.channel().ofType();
    final ByteChunkOutputStream stream = JRoutine.outputStream().of(channel);
    stream.write(new byte[]{31, 17, (byte) 155, 13});
    stream.flush();
    final ByteChunkInputStream inputStream = JRoutine.inputStream(channel.next());
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
  public void testReadOnly() {
    final Channel<String, String> wrapped =
        JRoutine.channel().<String>ofType().after(millis(200)).pass("test");
    final Channel<String, String> channel = JRoutine.readOnly(wrapped);
    assertThat(channel.isOpen()).isTrue();
    assertThat(channel.close().isOpen()).isTrue();
    channel.after(10, TimeUnit.MILLISECONDS).close();
    assertThat(channel.in(millis(100)).getComplete()).isFalse();
    assertThat(channel.isOpen()).isTrue();
  }

  @Test
  public void testStream() {
    assertThat(
        JRoutine.streamOf(JRoutine.routine().of(JRoutineOperators.appendOutputsOf(range(1, 1000))))
                .map(JRoutine.routine().of(unary(new Function<Number, Double>() {

                  public Double apply(final Number number) {
                    return Math.sqrt(number.doubleValue());
                  }
                })))
                .map(JRoutine.routineOn(syncExecutor()).of(average(Double.class)))
                .invoke()
                .close()
                .in(seconds(3))
                .next()).isCloseTo(21, Offset.offset(0.1));
  }

  @Test
  public void testSupplierCommand() {

    final Routine<Void, String> routine =
        JRoutine.<Void, String>stateless().onCompleteOutput(new Supplier<String>() {

          public String get() {
            return "test";
          }
        }).routine();
    assertThat(routine.invoke().close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testSupplierFactory() {

    final Routine<String, String> routine =
        JRoutine.routine().of(SupplierDecorator.factoryOf(new Supplier<ToCase>() {

          public ToCase get() {
            return new ToCase();
          }
        }));
    assertThat(routine.invoke().pass("TEST").close().in(seconds(1)).all()).containsOnly("test");
  }

  @Test
  public void testTryCatch() {
    assertThat(JRoutine.streamOf(
        JRoutine.routineOn(syncExecutor()).of(unary(new Function<Object, Object>() {

          public Object apply(final Object o) {
            throw new NullPointerException();
          }
        }))).lift(streamLifter().tryCatch(new Function<RoutineException, Object>() {

      public Object apply(final RoutineException e) {
        return "exception";
      }
    })).invoke().pass("test").close().next()).isEqualTo("exception");
    assertThat(
        JRoutine.streamOf(JRoutine.routineOn(syncExecutor()).of(JRoutineOperators.identity()))
                .lift(streamLifterOn(immediateExecutor()).tryCatch(
                    new Function<RoutineException, Object>() {

                      public Object apply(final RoutineException e) {
                        return "exception";
                      }
                    }))
                .invoke()
                .pass("test")
                .close()
                .next()).isEqualTo("test");
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

    public boolean isCalled() {
      return mIsCalled;
    }

    public void reset() {
      mIsCalled = false;
    }

    public void accept(final OUT out) {
      mIsCalled = true;
    }

  }
}
