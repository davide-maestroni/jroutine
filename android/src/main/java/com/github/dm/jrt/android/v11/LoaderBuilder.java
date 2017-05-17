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

package com.github.dm.jrt.android.v11;

import com.github.dm.jrt.android.LoaderWrapperRoutineBuilder;
import com.github.dm.jrt.android.core.builder.LoaderChannelBuilder;
import com.github.dm.jrt.android.core.builder.LoaderRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderSource;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.invocation.CommandInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiConsumer;
import com.github.dm.jrt.function.util.Consumer;
import com.github.dm.jrt.function.util.Decorator;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Predicate;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.function.util.SupplierDecorator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryFrom;
import static com.github.dm.jrt.android.core.invocation.ContextInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.reflect.ContextInvocationTarget.instanceOf;
import static com.github.dm.jrt.core.util.ClassToken.tokenOf;
import static com.github.dm.jrt.function.Functions.consumerCall;
import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.consumerMapping;
import static com.github.dm.jrt.function.Functions.decorate;
import static com.github.dm.jrt.function.Functions.functionCall;
import static com.github.dm.jrt.function.Functions.functionMapping;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.supplierCommand;
import static com.github.dm.jrt.function.Functions.supplierFactory;

/**
 * Context based builder of Loader routine builders.
 * <p>
 * Created by davide-maestroni on 03/06/2016.
 */
@SuppressWarnings("WeakerAccess")
public class LoaderBuilder {

  private final LoaderSource mContext;

  /**
   * Constructor.
   *
   * @param context the Loader context.
   */
  LoaderBuilder(@NotNull final LoaderSource context) {
    mContext = ConstantConditions.notNull("Loader context", context);
  }

  private static void checkStatic(@NotNull final Decorator decorator,
      @NotNull final Object function) {
    if (!decorator.hasStaticScope()) {
      throw new IllegalArgumentException(
          "the function instance does not have a static scope: " + function.getClass().getName());
    }
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class.
   * <br>
   * The method accepts also classes implementing
   * {@link com.github.dm.jrt.android.core.invocation.ContextInvocation ContextInvocation}.
   * <p>
   * Note that inner and anonymous classes can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct arguments
   * to guarantee proper instantiation.
   *
   * @param invocationClass the invocation class.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope or no default construct is found.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass) {
    return with(factoryOf(invocationClass));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class by passing the specified arguments to the class constructor.
   * <br>
   * The method accepts also classes implementing
   * {@link com.github.dm.jrt.android.core.invocation.ContextInvocation ContextInvocation}.
   * <p>
   * Note that inner and anonymous classes can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct arguments
   * to guarantee proper instantiation.
   *
   * @param invocationClass the invocation class.
   * @param args            the invocation constructor arguments.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope or no construct constructor taking
   *                                            the specified objects as parameters is found.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
      @Nullable final Object... args) {
    return with(factoryOf(invocationClass, args));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class token.
   * <br>
   * The method accepts also classes implementing
   * {@link com.github.dm.jrt.android.core.invocation.ContextInvocation ContextInvocation}.
   * <p>
   * Note that inner and anonymous classes can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct arguments
   * to guarantee proper instantiation.
   *
   * @param invocationToken the invocation class token.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope or no default construct is found.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken) {
    return with(factoryOf(invocationToken));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * class token by passing the specified arguments to the class constructor.
   * <br>
   * The method accepts also classes implementing
   * {@link com.github.dm.jrt.android.core.invocation.ContextInvocation ContextInvocation}.
   * <p>
   * Note that inner and anonymous classes can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct
   * arguments to guarantee proper instantiation.
   *
   * @param invocationToken the invocation class token.
   * @param args            the invocation constructor arguments.
   * @param <IN>            the input data type.
   * @param <OUT>           the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope or no construct constructor taking
   *                                            the specified objects as parameters is found.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken,
      @Nullable final Object... args) {
    return with(factoryOf(invocationToken, args));
  }

  /**
   * Returns a routine builder based on the specified command invocation.
   *
   * @param invocation the command invocation instance.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope.
   */
  @NotNull
  public <OUT> LoaderRoutineBuilder<Void, OUT> with(
      @NotNull final CommandInvocation<OUT> invocation) {
    return with((InvocationFactory<Void, OUT>) invocation);
  }

  /**
   * Returns a routine builder based on the specified mapping invocation.
   *
   * @param invocation the mapping invocation instance.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final MappingInvocation<IN, OUT> invocation) {
    return with((InvocationFactory<IN, OUT>) invocation);
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * object.
   * <br>
   * The method accepts also instances implementing
   * {@link com.github.dm.jrt.android.core.invocation.ContextInvocation ContextInvocation}.
   * <p>
   * Note that inner and anonymous objects can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct arguments
   * to guarantee proper instantiation.
   *
   * @param invocation the invocation instance.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope or no default construct is found.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final Invocation<IN, OUT> invocation) {
    return with(tokenOf(invocation));
  }

  /**
   * Returns a routine builder based on an invocation factory creating instances of the specified
   * object.
   * <br>
   * The method accepts also instances implementing
   * {@link com.github.dm.jrt.android.core.invocation.ContextInvocation ContextInvocation}.
   * <p>
   * Note that inner and anonymous objects can be passed as well. Remember however that Java
   * creates synthetic constructors for such classes, so be sure to specify the correct arguments
   * to guarantee proper instantiation.
   *
   * @param invocation the invocation instance.
   * @param args       the invocation constructor arguments.
   * @param <IN>       the input data type.
   * @param <OUT>      the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
   *                                            static scope or no default construct is found.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(@NotNull final Invocation<IN, OUT> invocation,
      @Nullable final Object... args) {
    return with(tokenOf(invocation), args);
  }

  /**
   * Returns a builder of routines bound to the builder context.
   * <br>
   * In order to prevent undesired leaks, the class of the specified factory must have a static
   * scope.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper thread,
   * thus waiting for the outputs immediately after its invocation may result in a deadlock.
   * <br>
   * Note also that the input data passed to the invocation channel will be cached, and the
   * results will be produced only after the invocation channel is closed, so be sure to avoid
   * streaming inputs in order to prevent starvation or out of memory errors.
   *
   * @param factory the invocation factory.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified factory has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final ContextInvocationFactory<IN, OUT> factory) {
    return JRoutineLoader.on(mContext).with(factory);
  }

  /**
   * Returns a builder of routines bound to the builder context, wrapping the specified target
   * object.
   * <br>
   * In order to customize the object creation, the caller must employ an implementation of a
   * {@link com.github.dm.jrt.android.reflect.builder.FactoryContext FactoryContext} as the
   * application Context.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper thread,
   * thus waiting for the outputs immediately after its invocation may result in a deadlock.
   *
   * @param target the invocation target.
   * @return the routine builder instance.
   */
  @NotNull
  public LoaderWrapperRoutineBuilder with(@NotNull final ContextInvocationTarget<?> target) {
    return new DefaultLoaderWrapperRoutineBuilder(mContext, target);
  }

  /**
   * Returns a builder of routines bound to the builder context.
   * <br>
   * In order to prevent undesired leaks, the class of the specified factory must have a static
   * scope.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper thread,
   * thus waiting for the outputs immediately after its invocation may result in a deadlock.
   * <br>
   * Note also that the input data passed to the invocation channel will be cached, and the
   * results will be produced only after the invocation channel is closed, so be sure to avoid
   * streaming inputs in order to prevent starvation or out of memory errors.
   *
   * @param factory the invocation factory.
   * @param <IN>    the input data type.
   * @param <OUT>   the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified factory has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> with(
      @NotNull final InvocationFactory<IN, OUT> factory) {
    return with(factoryFrom(factory));
  }

  /**
   * Returns a routine builder based on a call invocation factory backed by the specified function.
   *
   * @param function the function instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified consumer has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> withCall(
      @NotNull final Function<? super List<IN>, ? extends OUT> function) {
    checkStatic(decorate(function), function);
    return with(functionCall(function));
  }

  /**
   * Returns a routine builder based on a call invocation factory backed by the specified consumer.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified consumer has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> withCallConsumer(
      @NotNull final BiConsumer<? super List<IN>, ? super Channel<OUT, ?>> consumer) {
    checkStatic(decorate(consumer), consumer);
    return with(consumerCall(consumer));
  }

  /**
   * Returns a builder of routines bound to the builder context, wrapping the specified target
   * class.
   * <br>
   * In order to customize the object creation, the caller must employ an implementation of a
   * {@link com.github.dm.jrt.android.reflect.builder.FactoryContext FactoryContext} as the
   * application Context.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper thread,
   * thus waiting for the outputs immediately after its invocation may result in a deadlock.
   *
   * @param targetClass the invocation target class.
   * @return the routine builder instance.
   */
  @NotNull
  public LoaderWrapperRoutineBuilder withClassOfType(@NotNull final Class<?> targetClass) {
    return with(classOfType(targetClass));
  }

  /**
   * Returns a routine builder based on a command invocation backed by the specified supplier.
   *
   * @param supplier the supplier instance.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified supplier has not a
   *                                            static scope.
   */
  @NotNull
  public <OUT> LoaderRoutineBuilder<Void, OUT> withCommand(
      @NotNull final Supplier<? extends OUT> supplier) {
    checkStatic(decorate(supplier), supplier);
    return with(supplierCommand(supplier));
  }

  /**
   * Returns a routine builder based on a command invocation backed by the specified consumer.
   *
   * @param consumer the consumer instance.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified consumer has not a
   *                                            static scope.
   */
  @NotNull
  public <OUT> LoaderRoutineBuilder<Void, OUT> withCommandConsumer(
      @NotNull final Consumer<? super Channel<OUT, ?>> consumer) {
    checkStatic(decorate(consumer), consumer);
    return with(consumerCommand(consumer));
  }

  /**
   * Returns a routine builder based on an invocation factory backed by the specified supplier.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified supplier has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> withContextFactory(
      @NotNull final Supplier<? extends ContextInvocation<? super IN, ? extends OUT>> supplier) {
    final SupplierDecorator<? extends ContextInvocation<? super IN, ? extends OUT>> wrapper =
        decorate(supplier);
    checkStatic(wrapper, supplier);
    return with(new SupplierContextInvocationFactory<IN, OUT>(wrapper));
  }

  /**
   * Returns a routine builder based on an invocation factory backed by the specified supplier.
   *
   * @param supplier the supplier instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified supplier has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> withFactory(
      @NotNull final Supplier<? extends Invocation<? super IN, ? extends OUT>> supplier) {
    checkStatic(decorate(supplier), supplier);
    return with(supplierFactory(supplier));
  }

  /**
   * Returns a routine builder based on a operation invocation backed by the specified predicate.
   *
   * @param predicate the predicate instance.
   * @param <IN>      the input data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified predicate has not a
   *                                            static scope.
   */
  @NotNull
  public <IN> LoaderRoutineBuilder<IN, IN> withFilter(
      @NotNull final Predicate<? super IN> predicate) {
    checkStatic(decorate(predicate), predicate);
    return with(predicateFilter(predicate));
  }

  /**
   * Returns a builder of channels bound to the Loader identified by the specified ID.
   * <br>
   * If no invocation with the specified ID is running at the time of the channel creation, the
   * output will be aborted with a
   * {@link com.github.dm.jrt.android.core.invocation.MissingLoaderException
   * MissingLoaderException}.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper thread,
   * thus waiting for the outputs immediately after its invocation may result in a deadlock.
   *
   * @param loaderId the Loader ID.
   * @return the channel builder instance.
   */
  @NotNull
  public LoaderChannelBuilder withId(final int loaderId) {
    return JRoutineLoader.on(mContext).withId(loaderId);
  }

  /**
   * Returns a builder of routines bound to the builder context, wrapping the specified target
   * object.
   * <br>
   * In order to customize the object creation, the caller must employ an implementation of a
   * {@link com.github.dm.jrt.android.reflect.builder.FactoryContext FactoryContext} as the
   * application Context.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper thread,
   * thus waiting for the outputs immediately after its invocation may result in a deadlock.
   *
   * @param targetClass the class of the invocation target.
   * @return the routine builder instance.
   */
  @NotNull
  public LoaderWrapperRoutineBuilder withInstanceOf(@NotNull final Class<?> targetClass) {
    return with(instanceOf(targetClass));
  }

  /**
   * Returns a builder of routines bound to the builder context, wrapping the specified target
   * object.
   * <br>
   * In order to customize the object creation, the caller must employ an implementation of a
   * {@link com.github.dm.jrt.android.reflect.builder.FactoryContext FactoryContext} as the
   * application Context.
   * <p>
   * Note that the built routine results will be always dispatched on the configured Looper thread,
   * thus waiting for the outputs immediately after its invocation may result in a deadlock.
   *
   * @param targetClass the class of the invocation target.
   * @param factoryArgs the object factory arguments.
   * @return the routine builder instance.
   */
  @NotNull
  public LoaderWrapperRoutineBuilder withInstanceOf(@NotNull final Class<?> targetClass,
      @Nullable final Object... factoryArgs) {
    return with(instanceOf(targetClass, factoryArgs));
  }

  /**
   * Returns a routine builder based on a mapping invocation backed by the specified function.
   *
   * @param function the function instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified function has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> withMapping(
      @NotNull final Function<? super IN, ? extends OUT> function) {
    checkStatic(decorate(function), function);
    return with(functionMapping(function));
  }

  /**
   * Returns a routine builder based on a mapping invocation backed by the specified consumer.
   *
   * @param consumer the consumer instance.
   * @param <IN>     the input data type.
   * @param <OUT>    the output data type.
   * @return the routine builder instance.
   * @throws java.lang.IllegalArgumentException if the class of the specified consumer has not a
   *                                            static scope.
   */
  @NotNull
  public <IN, OUT> LoaderRoutineBuilder<IN, OUT> withMappingConsumer(
      @NotNull final BiConsumer<? super IN, ? super Channel<OUT, ?>> consumer) {
    checkStatic(decorate(consumer), consumer);
    return with(consumerMapping(consumer));
  }
}
