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

package com.github.dm.jrt.retrofit;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutor;
import com.github.dm.jrt.core.invocation.MappingInvocation;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.reflect.util.InvocationReflection;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.routine.StreamRoutine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.CallAdapter.Factory;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a Call adapter factory supporting {@code Routine} and
 * {@code StreamRoutine} return types.
 * <br>
 * Note that the generated routines must be invoked and the returned channel closed before any
 * result is produced.
 * <p>
 * Created by davide-maestroni on 05/19/2016.
 */
public abstract class AbstractAdapterFactory extends CallAdapter.Factory {

  private static final MappingInvocation<Call<Object>, Object> sCallInvocation =
      new MappingInvocation<Call<Object>, Object>(null) {

        public void onInput(final Call<Object> input,
            @NotNull final Channel<Object, ?> result) throws IOException {
          final Response<Object> response = input.execute();
          if (response.isSuccessful()) {
            result.pass(response.body());

          } else {
            result.abort(new ErrorResponseException(response));
          }
        }
      };

  private final InvocationConfiguration mConfiguration;

  private final CallAdapter.Factory mDelegateFactory;

  private final ScheduledExecutor mExecutor;

  /**
   * Constructor.
   *
   * @param executor        the executor instance.
   * @param configuration   the invocation configuration.
   * @param delegateFactory the delegate factory.
   */
  protected AbstractAdapterFactory(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration,
      @Nullable final Factory delegateFactory) {
    mExecutor = ConstantConditions.notNull("executor instance", executor);
    mConfiguration = ConstantConditions.notNull("invocation configuration", configuration);
    mDelegateFactory = delegateFactory;
  }

  /**
   * Returns a type representing a channel with the specified output data type.
   *
   * @param outputType the output data type.
   * @return the parameterized type.
   */
  @NotNull
  protected static ParameterizedType getChannelType(@NotNull final Type outputType) {
    return new RoutineType(ConstantConditions.notNull("outputs type", outputType));
  }

  @Override
  public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
      final Retrofit retrofit) {
    if (returnType instanceof ParameterizedType) {
      final ParameterizedType parameterizedType = (ParameterizedType) returnType;
      final Type responseType = extractResponseType(parameterizedType);
      if (responseType != null) {
        return get(mExecutor, mConfiguration, parameterizedType.getRawType(), responseType,
            annotations, retrofit);
      }

    } else if (returnType instanceof Class) {
      return get(mExecutor, mConfiguration, returnType, Object.class, annotations, retrofit);
    }

    return null;
  }

  /**
   * Builds and returns a routine instance handling Retrofit calls.
   *
   * @param executor      the executor instance.
   * @param configuration the invocation configuration.
   * @param returnRawType the return raw type to be adapted.
   * @param responseType  the type of the call response.
   * @param annotations   the method annotations.
   * @param retrofit      the Retrofit instance.
   * @return the routine instance.
   */
  @NotNull
  @SuppressWarnings("UnusedParameters")
  protected Routine<? extends Call<?>, ?> buildRoutine(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration, @NotNull final Type returnRawType,
      @NotNull final Type responseType, @NotNull final Annotation[] annotations,
      @NotNull final Retrofit retrofit) {
    // Use annotations to configure the routine
    final InvocationConfiguration invocationConfiguration =
        InvocationReflection.withAnnotations(configuration, annotations);
    final MappingInvocation<Call<Object>, Object> factory =
        getFactory(configuration, responseType, annotations, retrofit);
    return JRoutineCore.routineOn(executor).withConfiguration(invocationConfiguration).of(factory);
  }

  /**
   * Extracts the type of response from the method return type.
   *
   * @param returnType the return type to be adapted.
   * @return the response type or null.
   */
  @Nullable
  protected Type extractResponseType(@NotNull final ParameterizedType returnType) {
    final Type rawType = returnType.getRawType();
    if ((Routine.class == rawType) || (StreamRoutine.class == rawType)) {
      return returnType.getActualTypeArguments()[1];
    }

    return null;
  }

  /**
   * Gets the adapter used to convert a Retrofit call into the method return type.
   *
   * @param executor      the executor instance.
   * @param configuration the invocation configuration.
   * @param returnRawType the return raw type to be adapted.
   * @param responseType  the type of the call response.
   * @param annotations   the method annotations.
   * @param retrofit      the Retrofit instance.
   * @return the call adapter or null.
   */
  @Nullable
  protected CallAdapter<?> get(@NotNull final ScheduledExecutor executor,
      @NotNull final InvocationConfiguration configuration, @NotNull final Type returnRawType,
      @NotNull final Type responseType, @NotNull final Annotation[] annotations,
      @NotNull final Retrofit retrofit) {
    if ((Routine.class == returnRawType) || (StreamRoutine.class == returnRawType)) {
      return new StreamRoutineAdapter(
          buildRoutine(executor, configuration, returnRawType, responseType, annotations, retrofit),
          responseType);
    }

    return null;
  }

  @NotNull
  private MappingInvocation<Call<Object>, Object> getFactory(
      @NotNull final InvocationConfiguration configuration, @NotNull final Type responseType,
      @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {
    final CallAdapter.Factory delegateFactory = mDelegateFactory;
    if (delegateFactory == null) {
      return sCallInvocation;
    }

    @SuppressWarnings("unchecked") final CallAdapter<Routine<?, ?>> routineAdapter =
        (CallAdapter<Routine<?, ?>>) delegateFactory.get(new RoutineType(responseType), annotations,
            retrofit);
    if (routineAdapter != null) {
      return new RoutineAdapterInvocation(
          asArgs(delegateFactory, configuration, responseType, annotations, retrofit),
          routineAdapter);
    }

    @SuppressWarnings("unchecked") final CallAdapter<StreamRoutine<?, ?>> streamRoutineAdapter =
        (CallAdapter<StreamRoutine<?, ?>>) delegateFactory.get(new StreamRoutineType(responseType),
            annotations, retrofit);
    if (streamRoutineAdapter != null) {
      return new RoutineAdapterInvocation(
          asArgs(delegateFactory, configuration, responseType, annotations, retrofit),
          streamRoutineAdapter);
    }

    final CallAdapter<?> bodyAdapter = delegateFactory.get(responseType, annotations, retrofit);
    if (bodyAdapter != null) {
      return new BodyAdapterInvocation(
          asArgs(delegateFactory, configuration, responseType, annotations, retrofit), bodyAdapter);
    }

    throw new IllegalArgumentException(
        "The delegate factory does not support any of the required return types: " + delegateFactory
            .getClass()
            .getName());
  }

  /**
   * Base adapter implementation.
   */
  protected static abstract class BaseAdapter<T> implements CallAdapter<T> {

    private final Type mResponseType;

    private final Routine<Call<?>, ?> mRoutine;

    /**
     * Constructor.
     *
     * @param routine      the routine instance.
     * @param responseType the response type.
     */
    @SuppressWarnings("unchecked")
    protected BaseAdapter(@NotNull final Routine<? extends Call<?>, ?> routine,
        @NotNull final Type responseType) {
      mResponseType = ConstantConditions.notNull("response type", responseType);
      mRoutine = ConstantConditions.notNull("routine instance", (Routine<Call<?>, ?>) routine);
    }

    public Type responseType() {
      return mResponseType;
    }

    /**
     * Gets the adapter routine.
     *
     * @return the routine instance.
     */
    @NotNull
    protected Routine<Call<?>, ?> getRoutine() {
      return mRoutine;
    }
  }

  /**
   * Mapping invocation employing a call adapter.
   */
  private static class BodyAdapterInvocation extends MappingInvocation<Call<Object>, Object> {

    private final CallAdapter<?> mCallAdapter;

    /**
     * Constructor.
     *
     * @param args        the factory arguments.
     * @param callAdapter the call adapter instance.
     */
    private BodyAdapterInvocation(@Nullable final Object[] args,
        @NotNull final CallAdapter<?> callAdapter) {
      super(args);
      mCallAdapter = callAdapter;
    }

    public void onInput(final Call<Object> input, @NotNull final Channel<Object, ?> result) {
      result.pass(mCallAdapter.adapt(input));
    }
  }

  /**
   * Invocation allowing a single Call object as input.
   */
  private static class InputCallInvocation extends TemplateInvocation<Call<?>, Call<?>> {

    private final Call<?> mCall;

    /**
     * Constructor.
     *
     * @param call the input Call.
     */
    private InputCallInvocation(@Nullable final Call<?> call) {
      mCall = call;
    }

    @Override
    public void onComplete(@NotNull final Channel<Call<?>, ?> result) {
      result.pass(mCall);
    }

    @Override
    public void onInput(final Call<?> input, @NotNull final Channel<Call<?>, ?> result) {
      result.abort(new IllegalArgumentException("no input is allowed"));
    }
  }

  /**
   * Mapping invocation employing a call adapter.
   */
  private static class RoutineAdapterInvocation extends MappingInvocation<Call<Object>, Object> {

    private final CallAdapter<? extends Routine<?, ?>> mCallAdapter;

    /**
     * Constructor.
     *
     * @param args        the factory arguments.
     * @param callAdapter the Call adapter instance.
     */
    private RoutineAdapterInvocation(@Nullable final Object[] args,
        @NotNull final CallAdapter<? extends Routine<?, ?>> callAdapter) {
      super(args);
      mCallAdapter = callAdapter;
    }

    public void onInput(final Call<Object> input, @NotNull final Channel<Object, ?> result) {
      final Channel<?, ?> channel = mCallAdapter.adapt(input).invoke();
      result.pass(channel);
      channel.close();
    }
  }

  /**
   * Parameterized type implementation mimicking a routine type.
   */
  private static class RoutineType implements ParameterizedType {

    private final Type[] mTypeArguments;

    /**
     * Constructor.
     *
     * @param outputType the output data type.
     */
    private RoutineType(@NotNull final Type outputType) {
      mTypeArguments = new Type[]{Object.class, outputType};
    }

    public Type[] getActualTypeArguments() {
      return mTypeArguments.clone();
    }

    public Type getRawType() {
      return Routine.class;
    }

    public Type getOwnerType() {
      return null;
    }
  }

  /**
   * Stream routine adapter implementation.
   */
  private static class StreamRoutineAdapter extends BaseAdapter<Routine> {

    /**
     * Constructor.
     *
     * @param routine      the routine instance.
     * @param responseType the response type.
     */
    private StreamRoutineAdapter(@NotNull final Routine<? extends Call<?>, ?> routine,
        @NotNull final Type responseType) {
      super(routine, responseType);
    }

    public <OUT> Routine adapt(final Call<OUT> call) {
      return JRoutineStream.streamOfSingleton(new InputCallInvocation(call)).map(getRoutine());
    }
  }

  /**
   * Parameterized type implementation mimicking a stream routine type.
   */
  private static class StreamRoutineType implements ParameterizedType {

    private final Type[] mTypeArguments;

    /**
     * Constructor.
     *
     * @param outputType the output data type.
     */
    private StreamRoutineType(@NotNull final Type outputType) {
      mTypeArguments = new Type[]{Object.class, outputType};
    }

    public Type[] getActualTypeArguments() {
      return mTypeArguments.clone();
    }

    public Type getRawType() {
      return StreamRoutine.class;
    }

    public Type getOwnerType() {
      return null;
    }
  }
}
