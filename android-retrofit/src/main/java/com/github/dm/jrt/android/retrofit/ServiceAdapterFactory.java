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

import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceSource;
import com.github.dm.jrt.android.core.config.ServiceConfigurable;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.reflect.builder.AndroidReflectionRoutineBuilders;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.BiFunction;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.reflect.util.InvocationReflection;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.builder.StreamBuilder;
import com.github.dm.jrt.stream.config.StreamConfiguration;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.android.core.invocation.InvocationFactoryReference.factoryOf;
import static com.github.dm.jrt.function.Functions.decorate;

/**
 * Implementation of a call adapter factory supporting {@code Channel} and {@code StreamBuilder}
 * return types.
 * <br>
 * Note that the routines generated through stream builders must be invoked and the returned channel
 * closed before any result is produced.
 * <p>
 * If properly configured, the routine invocations will run in a dedicated Android Service.
 * <p>
 * Note, however, that a different {@code OkHttpClient} instance will be created by the Service. In
 * order to properly configure it, the target Service class should implement
 * {@link com.github.dm.jrt.android.reflect.builder.FactoryContext}, and return the configured
 * instance when requested. Like, for example:
 * <pre><code>
 * public RetrofitService extends InvocationService implements FactoryContext {
 *
 *   private final OkHttpClient mClient;
 *
 *   public RetrofitService() {
 *     mClient = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).build();
 *   }
 *
 *   &#64;Nullable
 *   &lt;TYPE&gt; TYPE geInstance(&#64;NotNull final Class&lt;? extends TYPE&gt; type,
 *       &#64;NotNull final Object... args) {
 *     if (type == OkHttpClient.class) {
 *       return type.cast(mClient);
 *     }
 *
 *     return null;
 *   }
 * }
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 05/16/2016.
 */
public class ServiceAdapterFactory extends CallAdapter.Factory {

  private static final CallMappingInvocation sInvocation = new CallMappingInvocation();

  private final InvocationConfiguration mInvocationConfiguration;

  private final ServiceConfiguration mServiceConfiguration;

  private final ServiceSource mServiceSource;

  /**
   * Constructor.
   *
   * @param serviceSource           the Service context.
   * @param invocationConfiguration the invocation configuration.
   * @param serviceConfiguration    the Service configuration.
   */
  private ServiceAdapterFactory(@NotNull final ServiceSource serviceSource,
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final ServiceConfiguration serviceConfiguration) {
    mServiceSource = serviceSource;
    mInvocationConfiguration = invocationConfiguration;
    mServiceConfiguration = serviceConfiguration;
  }

  /**
   * Returns an adapter factory builder.
   *
   * @param context the Service context.
   * @return the builder instance.
   */
  @NotNull
  public static Builder on(@NotNull final ServiceSource context) {
    return new Builder(context);
  }

  @Override
  public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
      final Retrofit retrofit) {
    Type rawType = null;
    Type responseType = Object.class;
    if (returnType instanceof ParameterizedType) {
      final ParameterizedType parameterizedType = (ParameterizedType) returnType;
      rawType = parameterizedType.getRawType();
      if ((Channel.class == rawType) || (StreamBuilder.class == rawType)) {
        responseType = parameterizedType.getActualTypeArguments()[1];
      }

    } else if (returnType instanceof Class) {
      rawType = returnType;
    }

    if (rawType != null) {
      // Use annotations to configure the routine
      final InvocationConfiguration invocationConfiguration =
          InvocationReflection.withAnnotations(mInvocationConfiguration, annotations);
      final ServiceConfiguration serviceConfiguration =
          AndroidReflectionRoutineBuilders.withAnnotations(mServiceConfiguration, annotations);
      if (Channel.class == rawType) {
        return new OutputChannelAdapter(invocationConfiguration,
            retrofit.responseBodyConverter(responseType, annotations),
            buildRoutine(invocationConfiguration, serviceConfiguration), responseType);

      } else if (StreamBuilder.class == rawType) {
        return new StreamBuilderAdapter(invocationConfiguration,
            retrofit.responseBodyConverter(responseType, annotations),
            buildRoutine(invocationConfiguration, serviceConfiguration), responseType);
      }
    }

    return null;
  }

  @NotNull
  private Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> buildRoutine(
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final ServiceConfiguration serviceConfiguration) {
    return JRoutineService.on(mServiceSource)
                          .with(factoryOf(ServiceCallInvocation.class))
                          .withConfiguration(invocationConfiguration)
                          .apply(serviceConfiguration)
                          .buildRoutine();
  }

  /**
   * Builder of Service routine adapter factory instances.
   * <p>
   * The options set through the builder configuration will be applied to all the routine handling
   * the Retrofit calls, unless they are overwritten by specific annotations.
   *
   * @see InvocationReflection#withAnnotations(InvocationConfiguration, Annotation...)
   * @see AndroidReflectionRoutineBuilders#withAnnotations(ServiceConfiguration, Annotation...)
   */
  public static class Builder
      implements ServiceConfigurable<Builder>, InvocationConfigurable<Builder> {

    private final ServiceSource mServiceSource;

    private InvocationConfiguration mInvocationConfiguration =
        InvocationConfiguration.defaultConfiguration();

    private ServiceConfiguration mServiceConfiguration =
        ServiceConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the Service context.
     */
    private Builder(@NotNull final ServiceSource context) {
      mServiceSource = ConstantConditions.notNull("Service context", context);
    }

    /**
     * Builds and return a new factory instance.
     *
     * @return the factory instance.
     */
    @NotNull
    public ServiceAdapterFactory buildFactory() {
      return new ServiceAdapterFactory(mServiceSource, mInvocationConfiguration,
          mServiceConfiguration);
    }

    @NotNull
    @Override
    public Builder withConfiguration(@NotNull final ServiceConfiguration configuration) {
      mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public Builder withConfiguration(@NotNull final InvocationConfiguration configuration) {
      mInvocationConfiguration =
          ConstantConditions.notNull("invocation configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public InvocationConfiguration.Builder<? extends Builder> withInvocation() {
      return new InvocationConfiguration.Builder<Builder>(this, mInvocationConfiguration);
    }

    @NotNull
    @Override
    public ServiceConfiguration.Builder<? extends Builder> withService() {
      return new ServiceConfiguration.Builder<Builder>(this, mServiceConfiguration);
    }
  }

  /**
   * Base adapter implementation.
   */
  private static abstract class BaseAdapter<T> implements CallAdapter<T> {

    private final Type mResponseType;

    private final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> mRoutine;

    /**
     * Constructor.
     *
     * @param routine      the routine instance.
     * @param responseType the response type.
     */
    private BaseAdapter(
        @NotNull final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine,
        @NotNull final Type responseType) {
      mResponseType = responseType;
      mRoutine = routine;
    }

    @Override
    public Type responseType() {
      return mResponseType;
    }

    /**
     * Gets the adapter routine.
     *
     * @return the routine instance.
     */
    @NotNull
    Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> getRoutine() {
      return mRoutine;
    }
  }

  /**
   * Bind function used to create the stream channel instances.
   */
  private static class BindService
      implements Function<Channel<?, ParcelableFlow<Object>>, Channel<?, Object>> {

    private final ChannelConfiguration mConfiguration;

    private final Converter<ResponseBody, ?> mConverter;

    private final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> mRoutine;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param converter     the body converter.
     * @param routine       the routine instance.
     */
    private BindService(@NotNull final ChannelConfiguration configuration,
        @NotNull final Converter<ResponseBody, ?> converter,
        @NotNull final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine) {
      mConfiguration = configuration;
      mConverter = converter;
      mRoutine = routine;
    }

    @Override
    public Channel<?, Object> apply(final Channel<?, ParcelableFlow<Object>> channel) {
      final Channel<Object, Object> outputChannel =
          JRoutineCore.ofData().apply(mConfiguration).buildChannel();
      mRoutine.invoke()
              .consume(new ConverterChannelConsumer(mConverter, outputChannel))
              .pass(channel)
              .close();
      return outputChannel;
    }
  }

  /**
   * Output channel adapter implementation.
   */
  private static class OutputChannelAdapter extends BaseAdapter<Channel> {

    private final ChannelConfiguration mChannelConfiguration;

    private final Converter<ResponseBody, ?> mConverter;

    private final InvocationConfiguration mInvocationConfiguration;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param converter     the body converter.
     * @param routine       the routine instance.
     * @param responseType  the response type.
     */
    private OutputChannelAdapter(@NotNull final InvocationConfiguration configuration,
        @NotNull final Converter<ResponseBody, ?> converter,
        @NotNull final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine,
        @NotNull final Type responseType) {
      super(routine, responseType);
      mInvocationConfiguration = configuration;
      mChannelConfiguration = configuration.outputConfigurationBuilder().configuration();
      mConverter = converter;
    }

    @NotNull
    private Channel<?, ParcelableFlow<Object>> invokeCall(final Call<?> call) {
      return JRoutineCore.with(sInvocation)
                         .apply(mInvocationConfiguration)
                         .invoke()
                         .pass(call)
                         .close();
    }

    @Override
    public <OUT> Channel adapt(final Call<OUT> call) {
      final Channel<Object, Object> outputChannel =
          JRoutineCore.ofData().apply(mChannelConfiguration).buildChannel();
      getRoutine().invoke()
                  .consume(new ConverterChannelConsumer(mConverter, outputChannel))
                  .pass(invokeCall(call))
                  .close();
      return outputChannel;
    }
  }

  /**
   * Stream routine builder adapter implementation.
   */
  private static class StreamBuilderAdapter extends BaseAdapter<StreamBuilder> implements
      BiFunction<StreamConfiguration, Function<Channel<?, Call<?>>, Channel<?,
          ParcelableFlow<Object>>>, Function<Channel<?, Call<?>>, Channel<?, Object>>> {

    private final Converter<ResponseBody, ?> mConverter;

    private final InvocationConfiguration mInvocationConfiguration;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param converter     the body converter.
     * @param routine       the routine instance.
     * @param responseType  the response type.
     */
    private StreamBuilderAdapter(@NotNull final InvocationConfiguration configuration,
        @NotNull final Converter<ResponseBody, ?> converter,
        @NotNull final Routine<ParcelableFlow<Object>, ParcelableFlow<Object>> routine,
        @NotNull final Type responseType) {
      super(routine, responseType);
      mInvocationConfiguration = configuration;
      mConverter = converter;
    }

    @Override
    public Function<Channel<?, Call<?>>, Channel<?, Object>> apply(
        final StreamConfiguration streamConfiguration,
        final Function<Channel<?, Call<?>>, Channel<?, ParcelableFlow<Object>>> function) {
      return decorate(function).andThen(
          new BindService(streamConfiguration.toChannelConfiguration(), mConverter, getRoutine()));
    }

    @Override
    public <OUT> StreamBuilder adapt(final Call<OUT> call) {
      return JRoutineStream.<Call<?>>withStreamOf(call).withConfiguration(mInvocationConfiguration)
                                                       .map(sInvocation)
                                                       .lift(this);
    }
  }
}
