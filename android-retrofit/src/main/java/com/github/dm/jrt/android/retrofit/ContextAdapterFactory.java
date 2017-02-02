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

package com.github.dm.jrt.android.retrofit;

import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.invocation.TemplateContextInvocation;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.retrofit.AbstractAdapterFactory;
import com.github.dm.jrt.retrofit.ErrorResponseException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.CallAdapter.Factory;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract implementation of a call adapter factory, providing an automatic way to create Context
 * invocation factories to be used to handle Retrofit calls.
 * <p>
 * Created by davide-maestroni on 05/27/2016.
 */
public abstract class ContextAdapterFactory extends AbstractAdapterFactory {

  private static final TemplateContextInvocation<Call<Object>, Object> sCallInvocation =
      new TemplateContextInvocation<Call<Object>, Object>() {

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

  private static final ContextInvocationFactory<Call<Object>, Object> sCallInvocationFactory =
      new ContextInvocationFactory<Call<Object>, Object>(null) {

        @NotNull
        @Override
        public ContextInvocation<Call<Object>, Object> newInvocation() {
          return sCallInvocation;
        }
      };

  private final CallAdapter.Factory mDelegateFactory;

  /**
   * Constructor.
   *
   * @param delegateFactory the delegate factory.
   * @param configuration   the invocation configuration.
   */
  protected ContextAdapterFactory(@Nullable final Factory delegateFactory,
      @NotNull final InvocationConfiguration configuration) {
    super(delegateFactory, configuration);
    mDelegateFactory = delegateFactory;
  }

  /**
   * Gets the Context invocation factory to handle the call execution.
   *
   * @param configuration the invocation configuration.
   * @param responseType  the response type.
   * @param annotations   the method annotations.
   * @param retrofit      the Retrofit instance.
   * @return the invocation factory.
   */
  @NotNull
  @SuppressWarnings("UnusedParameters")
  protected ContextInvocationFactory<Call<Object>, Object> getFactory(
      @NotNull final InvocationConfiguration configuration, @NotNull final Type responseType,
      @NotNull final Annotation[] annotations, @NotNull final Retrofit retrofit) {
    final CallAdapter.Factory delegateFactory = mDelegateFactory;
    if (delegateFactory == null) {
      return sCallInvocationFactory;
    }

    @SuppressWarnings("unchecked") final CallAdapter<Channel<?, ?>> channelAdapter =
        (CallAdapter<Channel<?, ?>>) delegateFactory.get(getChannelType(responseType), annotations,
            retrofit);
    if (channelAdapter != null) {
      return new ChannelAdapterInvocationFactory(
          asArgs(delegateFactory, responseType, annotations, retrofit), channelAdapter);
    }

    final CallAdapter<?> bodyAdapter = delegateFactory.get(responseType, annotations, retrofit);
    if (bodyAdapter != null) {
      return new BodyAdapterInvocationFactory(
          asArgs(delegateFactory, responseType, annotations, retrofit), bodyAdapter);
    }

    throw new IllegalArgumentException(
        "The delegate factory does not support any of the required return types: " + delegateFactory
            .getClass()
            .getName());
  }

  /**
   * Context invocation employing a call adapter.
   */
  private static class BodyAdapterInvocation
      extends TemplateContextInvocation<Call<Object>, Object> {

    private final CallAdapter<?> mCallAdapter;

    /**
     * Constructor.
     *
     * @param callAdapter the call adapter instance.
     */
    private BodyAdapterInvocation(@NotNull final CallAdapter<?> callAdapter) {
      mCallAdapter = callAdapter;
    }

    @Override
    public void onInput(final Call<Object> input, @NotNull final Channel<Object, ?> result) {
      result.pass(mCallAdapter.adapt(input));
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }
  }

  /**
   * Context invocation factory employing a call adapter.
   */
  private static class BodyAdapterInvocationFactory
      extends ContextInvocationFactory<Call<Object>, Object> {

    private final BodyAdapterInvocation mInvocation;

    /**
     * Constructor.
     *
     * @param args        the factory arguments.
     * @param callAdapter the call adapter instance.
     */
    private BodyAdapterInvocationFactory(@Nullable final Object[] args,
        @NotNull final CallAdapter<?> callAdapter) {
      super(args);
      mInvocation = new BodyAdapterInvocation(callAdapter);
    }

    @NotNull
    @Override
    public ContextInvocation<Call<Object>, Object> newInvocation() {
      return mInvocation;
    }
  }

  /**
   * Context invocation employing a call adapter.
   */
  private static class ChannelAdapterInvocation
      extends TemplateContextInvocation<Call<Object>, Object> {

    private final CallAdapter<Channel<?, ?>> mCallAdapter;

    /**
     * Constructor.
     *
     * @param callAdapter the call adapter instance.
     */
    private ChannelAdapterInvocation(@NotNull final CallAdapter<Channel<?, ?>> callAdapter) {
      mCallAdapter = callAdapter;
    }

    @Override
    public void onInput(final Call<Object> input, @NotNull final Channel<Object, ?> result) {
      result.pass(mCallAdapter.adapt(input));
    }

    @Override
    public boolean onRecycle(final boolean isReused) {
      return true;
    }
  }

  /**
   * Context invocation factory employing a call adapter.
   */
  private static class ChannelAdapterInvocationFactory
      extends ContextInvocationFactory<Call<Object>, Object> {

    private final ChannelAdapterInvocation mInvocation;

    /**
     * Constructor.
     *
     * @param args        the factory arguments.
     * @param callAdapter the call adapter instance.
     */
    private ChannelAdapterInvocationFactory(@Nullable final Object[] args,
        @NotNull final CallAdapter<Channel<?, ?>> callAdapter) {
      super(args);
      mInvocation = new ChannelAdapterInvocation(callAdapter);
    }

    @NotNull
    @Override
    public ContextInvocation<Call<Object>, Object> newInvocation() {
      return mInvocation;
    }
  }
}
