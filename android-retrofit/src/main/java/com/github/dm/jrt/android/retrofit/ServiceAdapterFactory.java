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

import com.github.dm.jrt.android.channel.ParcelableFlowData;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceSource;
import com.github.dm.jrt.android.core.config.ServiceConfigurable;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.reflect.util.ContextInvocationReflection;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.ChannelConfiguration;
import com.github.dm.jrt.core.config.InvocationConfigurable;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.TemplateInvocation;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.function.util.Function;
import com.github.dm.jrt.function.util.Supplier;
import com.github.dm.jrt.reflect.util.InvocationReflection;
import com.github.dm.jrt.stream.JRoutineStream;
import com.github.dm.jrt.stream.routine.StreamRoutine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

import static com.github.dm.jrt.android.core.invocation.InvocationFactoryReference.factoryOf;
import static com.github.dm.jrt.function.util.SupplierDecorator.wrapSupplier;

/**
 * Implementation of a Call adapter factory supporting {@code Routine} and {@code StreamRoutine}
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
   * @param serviceSource the Service source.
   * @return the builder instance.
   */
  @NotNull
  public static Builder factoryOn(@NotNull final ServiceSource serviceSource) {
    return new Builder(serviceSource);
  }

  @Override
  public CallAdapter<?> get(final Type returnType, final Annotation[] annotations,
      final Retrofit retrofit) {
    Type rawType = null;
    Type responseType = Object.class;
    if (returnType instanceof ParameterizedType) {
      final ParameterizedType parameterizedType = (ParameterizedType) returnType;
      rawType = parameterizedType.getRawType();
      if ((Routine.class == rawType) || (StreamRoutine.class == rawType)) {
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
          ContextInvocationReflection.withAnnotations(mServiceConfiguration, annotations);
      if ((Routine.class == rawType) || (StreamRoutine.class == rawType)) {
        return new StreamRoutineAdapter(invocationConfiguration,
            retrofit.responseBodyConverter(responseType, annotations),
            buildRoutine(invocationConfiguration, serviceConfiguration), responseType);
      }
    }

    return null;
  }

  @NotNull
  private Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> buildRoutine(
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final ServiceConfiguration serviceConfiguration) {
    return JRoutineService.routineOn(mServiceSource)
                          .withConfiguration(invocationConfiguration)
                          .withConfiguration(serviceConfiguration)
                          .of(factoryOf(ServiceCallInvocation.class));
  }

  /**
   * Builder of Service routine adapter factory instances.
   * <p>
   * The options set through the builder configuration will be applied to all the routine handling
   * the Retrofit calls, unless they are overwritten by specific annotations.
   *
   * @see InvocationReflection#withAnnotations(InvocationConfiguration, Annotation...)
   * @see ContextInvocationReflection#withAnnotations(ServiceConfiguration, Annotation...)
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
     * @param serviceSource the Service source.
     */
    private Builder(@NotNull final ServiceSource serviceSource) {
      mServiceSource = ConstantConditions.notNull("Service source", serviceSource);
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
   * Lift function used to create the stream channel instances.
   */
  private static class LiftService
      implements Function<Channel<Call<?>, ParcelableFlowData<Object>>, Channel<Call<?>, Object>> {

    private final ChannelConfiguration mConfiguration;

    private final Converter<ResponseBody, ?> mConverter;

    private final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> mRoutine;

    /**
     * Constructor.
     *
     * @param configuration the channel configuration.
     * @param converter     the body converter.
     * @param routine       the routine instance.
     */
    private LiftService(@NotNull final ChannelConfiguration configuration,
        @NotNull final Converter<ResponseBody, ?> converter,
        @NotNull final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine) {
      mConfiguration = configuration;
      mConverter = converter;
      mRoutine = routine;
    }

    @Override
    public Channel<Call<?>, Object> apply(
        final Channel<Call<?>, ParcelableFlowData<Object>> channel) {
      final Channel<Object, Object> outputChannel =
          JRoutineCore.channel().withConfiguration(mConfiguration).ofType();
      mRoutine.invoke()
              .consume(new ConverterChannelConsumer(mConverter, outputChannel))
              .pass(channel)
              .close();
      return JRoutineCore.flatten(channel, JRoutineCore.readOnly(outputChannel));
    }
  }

  /**
   * Stream routine adapter implementation.
   */
  private static class StreamRoutineAdapter implements CallAdapter<StreamRoutine>,
      Function<Supplier<? extends Channel<Call<?>, ParcelableFlowData<Object>>>,
          Supplier<Channel<Call<?>, Object>>> {

    private final ChannelConfiguration mConfiguration;

    private final Converter<ResponseBody, ?> mConverter;

    private final Type mResponseType;

    private final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> mRoutine;

    /**
     * Constructor.
     *
     * @param configuration the invocation configuration.
     * @param converter     the body converter.
     * @param routine       the routine instance.
     * @param responseType  the response type.
     */
    private StreamRoutineAdapter(@NotNull final InvocationConfiguration configuration,
        @NotNull final Converter<ResponseBody, ?> converter,
        @NotNull final Routine<ParcelableFlowData<Object>, ParcelableFlowData<Object>> routine,
        @NotNull final Type responseType) {
      mRoutine = routine;
      mResponseType = responseType;
      mConfiguration = configuration.outputConfigurationBuilder().configuration();
      mConverter = converter;
    }

    @Override
    public Type responseType() {
      return mResponseType;
    }

    @Override
    public <OUT> StreamRoutine adapt(final Call<OUT> call) {
      return JRoutineStream.streamOfSingleton(new InputCallInvocation(call))
                           .map(JRoutineCore.routine().of(sInvocation))
                           .lift(this);
    }

    @Override
    public Supplier<Channel<Call<?>, Object>> apply(
        final Supplier<? extends Channel<Call<?>, ParcelableFlowData<Object>>> supplier) {
      return wrapSupplier(supplier).andThen(new LiftService(mConfiguration, mConverter, mRoutine));
    }
  }
}
