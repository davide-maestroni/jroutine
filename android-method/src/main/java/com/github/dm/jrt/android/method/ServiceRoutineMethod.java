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

package com.github.dm.jrt.android.method;

import android.content.Context;

import com.github.dm.jrt.android.channel.AndroidChannels;
import com.github.dm.jrt.android.channel.ParcelableFlow;
import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.config.ServiceConfigurable;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.config.ServiceConfiguration.Builder;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.object.JRoutineServiceObject;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.ChannelBuilder;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.method.RoutineMethod;
import com.github.dm.jrt.method.annotation.Input;
import com.github.dm.jrt.method.annotation.Output;
import com.github.dm.jrt.object.config.ObjectConfigurable;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.boxingClass;
import static com.github.dm.jrt.core.util.Reflection.boxingDefault;
import static com.github.dm.jrt.core.util.Reflection.cloneArgs;
import static com.github.dm.jrt.core.util.Reflection.findBestMatchingMethod;

/**
 * This class provides an easy way to implement a routine running in a dedicated Android Service,
 * which can be combined in complex ways with other ones.
 * <h2>How to implement a routine</h2>
 * The class behaves like a {@link RoutineMethod} with a few differences. In order to run in a
 * Service, the implementing class must be static. Moreover, each constructor must have the Service
 * context as first argument and all the other arguments must be among the ones supported by the
 * {@link android.os.Parcel#writeValue(Object)} method.
 * <br>
 * In case a remote Service is employed (that is, a Service running in a different process), the
 * same restriction applies to the method parameters (other than input and output channels) and
 * to the input and output data.
 * <h2>How to access the Android Context</h2>
 * It is possible to get access to the Android Context (that is the Service instance) from inside
 * the routine by calling the {@code getContext()} method.
 * <p>
 * Like, for instance:
 * <pre><code>
 * public static class MyMethod extends ServiceRoutineMethod {
 *
 *   public MyMethod(final ServiceContext context) {
 *     super(context);
 *   }
 *
 *   void run(&#64;Input final Channel&lt;?, String&gt; input,
 *       &#64;Output final Channel&lt;String, ?&gt; output) {
 *     final MyService service = (MyService) getContext();
 *     // do it
 *   }
 * }
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 08/18/2016.
 */
@SuppressWarnings("WeakerAccess")
public class ServiceRoutineMethod extends RoutineMethod
    implements ServiceConfigurable<ServiceRoutineMethod> {

  private final Object[] mArgs;

  private final ServiceContext mContext;

  private final ThreadLocal<Channel<?, ?>> mLocalChannel = new ThreadLocal<Channel<?, ?>>();

  private final ThreadLocal<Context> mLocalContext = new ThreadLocal<Context>();

  private final ThreadLocal<Boolean> mLocalIgnore = new ThreadLocal<Boolean>();

  private ServiceConfiguration mConfiguration = ServiceConfiguration.defaultConfiguration();

  private Class<?> mReturnType;

  /**
   * Constructor.
   *
   * @param context the Service context.
   */
  public ServiceRoutineMethod(@NotNull final ServiceContext context) {
    this(context, (Object[]) null);
  }

  /**
   * Constructor.
   *
   * @param context the Service context.
   * @param args    the constructor arguments.
   */
  public ServiceRoutineMethod(@NotNull final ServiceContext context,
      @Nullable final Object... args) {
    mContext = ConstantConditions.notNull("Service context", context);
    final Class<? extends RoutineMethod> type = getClass();
    if (!Reflection.hasStaticScope(type)) {
      throw new IllegalStateException(
          "the method class must have a static scope: " + type.getName());
    }

    final Object[] additionalArgs;
    final Object[] safeArgs = Reflection.asArgs(args);
    if (type.isAnonymousClass()) {
      if (safeArgs.length > 0) {
        additionalArgs = new Object[safeArgs.length + 1];
        System.arraycopy(safeArgs, 0, additionalArgs, 1, safeArgs.length);
        additionalArgs[0] = safeArgs;

      } else {
        additionalArgs = safeArgs;
      }

    } else {
      additionalArgs = cloneArgs(safeArgs);
    }

    final Object[] constructorArgs = new Object[additionalArgs.length + 1];
    System.arraycopy(additionalArgs, 0, constructorArgs, 1, additionalArgs.length);
    constructorArgs[0] = context;
    Reflection.findBestMatchingConstructor(type, constructorArgs);
    mArgs = additionalArgs;
  }

  /**
   * Builds a Service object routine method by wrapping the specified static method.
   *
   * @param context the Service context.
   * @param method  the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not static.
   */
  @NotNull
  public static ObjectServiceRoutineMethod from(@NotNull final ServiceContext context,
      @NotNull final Method method) {
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new IllegalArgumentException("the method is not static: " + method);
    }

    return from(context, ContextInvocationTarget.classOfType(method.getDeclaringClass()), method);
  }

  /**
   * Builds a Service object routine method by wrapping a method of the specified target.
   *
   * @param context the Service context.
   * @param target  the invocation target.
   * @param method  the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not implemented by the
   *                                            target instance.
   */
  @NotNull
  public static ObjectServiceRoutineMethod from(@NotNull final ServiceContext context,
      @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
    if (!method.getDeclaringClass().isAssignableFrom(target.getTargetClass())) {
      throw new IllegalArgumentException(
          "the method is not applicable to the specified target class: " + target.getTargetClass());
    }

    return new ObjectServiceRoutineMethod(context, target, method);
  }

  /**
   * Builds a Service object routine method by wrapping a method of the specified target.
   *
   * @param context        the Service context.
   * @param target         the invocation target.
   * @param name           the method name.
   * @param parameterTypes the method parameter types.
   * @return the routine method instance.
   * @throws java.lang.NoSuchMethodException if no method with the specified signature is found.
   */
  @NotNull
  public static ObjectServiceRoutineMethod from(@NotNull final ServiceContext context,
      @NotNull final ContextInvocationTarget<?> target, @NotNull final String name,
      @Nullable final Class<?>... parameterTypes) throws NoSuchMethodException {
    return from(context, target, target.getTargetClass().getMethod(name, parameterTypes));
  }

  @NotNull
  @Override
  public ServiceRoutineMethod apply(@NotNull final InvocationConfiguration configuration) {
    return (ServiceRoutineMethod) super.apply(configuration);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public InvocationConfiguration.Builder<? extends ServiceRoutineMethod>
  applyInvocationConfiguration() {
    return (InvocationConfiguration.Builder<? extends ServiceRoutineMethod>) super
        .applyInvocationConfiguration();
  }

  /**
   * Calls the routine.
   * <br>
   * The output channel will produced the data returned by the method. In case the method does not
   * return any output, the channel will be anyway notified of invocation abortion and completion.
   * <p>
   * Note that the specific method will be selected based on the specified parameters. If no
   * matching method is found, the call will fail with an exception.
   *
   * @param params the parameters.
   * @param <OUT>  the output data type.
   * @return the output channel instance.
   */
  @NotNull
  @Override
  public <OUT> Channel<?, OUT> call(@Nullable final Object... params) {
    final Object[] safeParams = asArgs(params);
    return call(findBestMatchingMethod(getClass(), safeParams), InvocationMode.ASYNC, safeParams);
  }

  /**
   * Calls the routine in parallel mode.
   * <br>
   * The output channel will produced the data returned by the method. In case the method does not
   * return any output, the channel will be anyway notified of invocation abortion and completion.
   * <p>
   * Note that the specific method will be selected based on the specified parameters. If no
   * matching method is found, the call will fail with an exception.
   *
   * @param params the parameters.
   * @param <OUT>  the output data type.
   * @return the output channel instance.
   * @see com.github.dm.jrt.core.routine.Routine Routine
   */
  @NotNull
  @Override
  public <OUT> Channel<?, OUT> callParallel(@Nullable final Object... params) {
    final Object[] safeParams = asArgs(params);
    return call(findBestMatchingMethod(getClass(), safeParams), InvocationMode.PARALLEL,
        safeParams);
  }

  /**
   * Tells the routine to ignore the method return value, that is, it will not be passed to the
   * output channel.
   *
   * @param <OUT> the output data type.
   * @return the return value.
   */
  @SuppressWarnings("unchecked")
  protected <OUT> OUT ignoreReturnValue() {
    mLocalIgnore.set(true);
    return (OUT) boxingDefault(mReturnType);
  }

  /**
   * Returns the input channel which is ready to produce data. If the method takes no input channel
   * as parameter, null will be returned.
   * <p>
   * Note this method will return null if called outside the routine method invocation or from a
   * different thread.
   *
   * @param <IN> the input data type.
   * @return the input channel producing data or null.
   */
  @Override
  @SuppressWarnings("unchecked")
  protected <IN> Channel<?, IN> switchInput() {
    return (Channel<?, IN>) mLocalChannel.get();
  }

  @NotNull
  @Override
  public ServiceRoutineMethod apply(@NotNull final ServiceConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public Builder<? extends ServiceRoutineMethod> applyServiceConfiguration() {
    return new Builder<ServiceRoutineMethod>(this, mConfiguration);
  }

  /**
   * Returns the Android Context (that is, the Service instance).
   * <p>
   * Note this method will return null if called outside the routine method invocation or from a
   * different thread.
   *
   * @return the Context.
   */
  protected Context getContext() {
    return mLocalContext.get();
  }

  /**
   * Returns the Service configuration.
   *
   * @return the Service configuration.
   */
  @NotNull
  protected ServiceConfiguration getServiceConfiguration() {
    return mConfiguration;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <OUT> Channel<?, OUT> call(@NotNull final Method method,
      @NotNull final InvocationMode mode, @NotNull final Object[] params) {
    final ArrayList<Channel<?, ?>> inputChannels = new ArrayList<Channel<?, ?>>();
    final ArrayList<Channel<?, ?>> outputChannels = new ArrayList<Channel<?, ?>>();
    final Annotation[][] annotations = method.getParameterAnnotations();
    for (int i = 0; i < params.length; ++i) {
      final Object param = params[i];
      final Class<? extends Annotation> annotationType = getAnnotationType(param, annotations[i]);
      if (annotationType == Input.class) {
        params[i] = InputChannelPlaceHolder.class;
        inputChannels.add((Channel<?, ?>) param);

      } else if (annotationType == Output.class) {
        params[i] = OutputChannelPlaceHolder.class;
        outputChannels.add((Channel<?, ?>) param);
      }
    }

    final Channel<OUT, OUT> resultChannel = JRoutineCore.<OUT>ofInputs().buildChannel();
    outputChannels.add(resultChannel);
    final Channel<?, ? extends ParcelableFlow<Object>> inputChannel =
        (!inputChannels.isEmpty()) ? AndroidChannels.mergeParcelable(inputChannels).buildChannel()
            : JRoutineCore.<ParcelableFlow<Object>>of().buildChannel();
    final Channel<ParcelableFlow<Object>, ParcelableFlow<Object>> outputChannel =
        mode.invoke(JRoutineService.on(mContext)
                                   .with(factoryOf(ServiceInvocation.class, getClass(), mArgs,
                                       params))
                                   .apply(getConfiguration())
                                   .apply(getServiceConfiguration())).pass(inputChannel).close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        AndroidChannels.flowOutput(0, outputChannels.size(), outputChannel).buildChannelMap();
    for (final Entry<Integer, ? extends Channel<?, Object>> entry : channelMap.entrySet()) {
      entry.getValue().bind((Channel<Object, Object>) outputChannels.get(entry.getKey())).close();
    }

    return resultChannel;
  }

  private boolean isIgnoreReturnValue() {
    return (mLocalIgnore.get() != null);
  }

  private void resetIgnoreReturnValue() {
    mLocalIgnore.set(null);
  }

  private void setLocalContext(@Nullable final Context context) {
    mLocalContext.set(context);
  }

  private void setLocalInput(@Nullable final Channel<?, ?> inputChannel) {
    mLocalChannel.set(inputChannel);
  }

  private void setReturnType(@NotNull final Class<?> returnType) {
    mReturnType = returnType;
  }

  /**
   * Implementation of a Service routine method wrapping an object method.
   */
  public static class ObjectServiceRoutineMethod extends ServiceRoutineMethod
      implements ObjectConfigurable<ObjectServiceRoutineMethod> {

    private final ServiceContext mContext;

    private final Method mMethod;

    private final ContextInvocationTarget<?> mTarget;

    private ObjectConfiguration mConfiguration = ObjectConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the Service context.
     * @param target  the invocation target.
     * @param method  the method instance.
     */
    private ObjectServiceRoutineMethod(@NotNull final ServiceContext context,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
      super(context, target, method);
      mContext = context;
      mTarget = target;
      mMethod = method;
    }

    @NotNull
    @Override
    public ObjectServiceRoutineMethod apply(@NotNull final InvocationConfiguration configuration) {
      return (ObjectServiceRoutineMethod) super.apply(configuration);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public InvocationConfiguration.Builder<? extends ObjectServiceRoutineMethod>
    applyInvocationConfiguration() {
      return (InvocationConfiguration.Builder<? extends ObjectServiceRoutineMethod>) super
          .applyInvocationConfiguration();
    }

    @NotNull
    @Override
    public <OUT> Channel<?, OUT> call(@Nullable final Object... params) {
      return call(InvocationMode.ASYNC, params);
    }

    @NotNull
    @Override
    public <OUT> Channel<?, OUT> callParallel(@Nullable final Object... params) {
      return call(InvocationMode.PARALLEL, params);
    }

    @NotNull
    @Override
    public ObjectServiceRoutineMethod apply(@NotNull final ServiceConfiguration configuration) {
      return (ObjectServiceRoutineMethod) super.apply(configuration);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Builder<? extends ObjectServiceRoutineMethod> applyServiceConfiguration() {
      return (Builder<? extends ObjectServiceRoutineMethod>) super.applyServiceConfiguration();
    }

    @NotNull
    @Override
    public ObjectServiceRoutineMethod apply(@NotNull final ObjectConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("object configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public ObjectConfiguration.Builder<? extends ObjectServiceRoutineMethod>
    applyObjectConfiguration() {
      return new ObjectConfiguration.Builder<ObjectServiceRoutineMethod>(this, mConfiguration);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <OUT> Channel<?, OUT> call(@NotNull final InvocationMode mode,
        @Nullable final Object[] params) {
      final Object[] safeParams = asArgs(params);
      final Method method = mMethod;
      if (method.getParameterTypes().length != safeParams.length) {
        throw new IllegalArgumentException("wrong number of parameters: expected <" +
            method.getParameterTypes().length + "> but was <" + safeParams.length + ">");
      }

      final Routine<Object, Object> routine = JRoutineServiceObject.on(mContext)
                                                                   .with(mTarget)
                                                                   .apply(getConfiguration())
                                                                   .apply(getServiceConfiguration())
                                                                   .apply(mConfiguration)
                                                                   .method(method);
      final Channel<Object, Object> channel = mode.invoke(routine).sorted();
      for (final Object param : safeParams) {
        if (param instanceof Channel) {
          channel.pass((Channel<?, ?>) param);

        } else {
          channel.pass(param);
        }
      }

      return (Channel<?, OUT>) channel.close();
    }
  }

  /**
   * Input channel placeholder class used to make the method parameters parcelable.
   */
  private static class InputChannelPlaceHolder {}

  /**
   * Output channel placeholder class used to make the method parameters parcelable.
   */
  private static class OutputChannelPlaceHolder {}

  /**
   * Context invocation implementation.
   */
  private static class ServiceInvocation
      implements ContextInvocation<ParcelableFlow<Object>, ParcelableFlow<Object>> {

    private final Object[] mArgs;

    private final ArrayList<Channel<?, ?>> mInputChannels = new ArrayList<Channel<?, ?>>();

    private final Method mMethod;

    private final Object[] mOrigParams;

    private final ArrayList<Channel<?, ?>> mOutputChannels = new ArrayList<Channel<?, ?>>();

    private final boolean mReturnResults;

    private final Class<? extends ServiceRoutineMethod> mType;

    private Constructor<? extends ServiceRoutineMethod> mConstructor;

    private Object[] mConstructorArgs;

    private Context mContext;

    private ServiceRoutineMethod mInstance;

    private boolean mIsAborted;

    private boolean mIsBound;

    private boolean mIsComplete;

    private Object[] mParams;

    /**
     * Constructor.
     *
     * @param type   the Service routine method type.
     * @param args   the constructor arguments.
     * @param params the method parameters.
     */
    private ServiceInvocation(@NotNull final Class<? extends ServiceRoutineMethod> type,
        @NotNull final Object[] args, @NotNull final Object[] params) {
      final ChannelBuilder<?, ?> channelBuilder = JRoutineCore.ofInputs();
      for (int i = 0; i < params.length; ++i) {
        final Object param = params[i];
        if (param == InputChannelPlaceHolder.class) {
          params[i] = channelBuilder.buildChannel();

        } else if (param == OutputChannelPlaceHolder.class) {
          params[i] = channelBuilder.buildChannel();
        }
      }

      mType = type;
      mArgs = args;
      mMethod = findBestMatchingMethod(type, params);
      mOrigParams = params;
      mReturnResults = (boxingClass(mMethod.getReturnType()) != Void.class);
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mIsAborted = true;
      final List<Channel<?, ?>> inputChannels = mInputChannels;
      for (final Channel<?, ?> inputChannel : inputChannels) {
        inputChannel.abort(reason);
      }

      final ServiceRoutineMethod instance = mInstance;
      instance.setLocalInput((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
      try {
        if (!mIsComplete) {
          invokeMethod();
        }

      } finally {
        instance.resetIgnoreReturnValue();
        instance.setLocalInput(null);
        for (final Channel<?, ?> outputChannel : mOutputChannels) {
          outputChannel.abort(reason);
        }
      }
    }

    @Override
    public void onComplete(@NotNull final Channel<ParcelableFlow<Object>, ?> result) throws
        Exception {
      bind(result);
      mIsComplete = true;
      if (!mIsAborted) {
        final List<Channel<?, ?>> inputChannels = mInputChannels;
        for (final Channel<?, ?> inputChannel : inputChannels) {
          inputChannel.close();
        }

        final ServiceRoutineMethod instance = mInstance;
        instance.setLocalInput((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
        instance.resetIgnoreReturnValue();
        final List<Channel<?, ?>> outputChannels = mOutputChannels;
        try {
          final Object methodResult = invokeMethod();
          if (mReturnResults && !instance.isIgnoreReturnValue()) {
            result.pass(new ParcelableFlow<Object>(outputChannels.size(), methodResult));
          }

        } finally {
          instance.resetIgnoreReturnValue();
          instance.setLocalInput(null);
        }

        for (final Channel<?, ?> outputChannel : outputChannels) {
          outputChannel.close();
        }
      }
    }

    @Override
    public void onInput(final ParcelableFlow<Object> input,
        @NotNull final Channel<ParcelableFlow<Object>, ?> result) throws Exception {
      bind(result);
      @SuppressWarnings("unchecked") final Channel<Object, Object> inputChannel =
          (Channel<Object, Object>) mInputChannels.get(input.id);
      inputChannel.pass(input.data);
      final ServiceRoutineMethod instance = mInstance;
      instance.setLocalInput(inputChannel);
      try {
        final Object methodResult = invokeMethod();
        if (mReturnResults && !instance.isIgnoreReturnValue()) {
          result.pass(new ParcelableFlow<Object>(mOutputChannels.size(), methodResult));
        }

      } finally {
        instance.resetIgnoreReturnValue();
        instance.setLocalInput(null);
      }
    }

    @Override
    public void onRecycle(final boolean isReused) {
      mInputChannels.clear();
      mOutputChannels.clear();
    }

    @Override
    public void onRestart() throws Exception {
      mIsBound = false;
      mIsAborted = false;
      mIsComplete = false;
      final ServiceRoutineMethod instance =
          (mInstance = mConstructor.newInstance(mConstructorArgs));
      instance.setReturnType(mMethod.getReturnType());
      mParams = replaceChannels(mMethod, mOrigParams, mInputChannels, mOutputChannels);
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      mContext = context;
      final Object[] additionalArgs = mArgs;
      final Object[] constructorArgs = (mConstructorArgs = new Object[additionalArgs.length + 1]);
      System.arraycopy(additionalArgs, 0, constructorArgs, 1, additionalArgs.length);
      constructorArgs[0] = ServiceContext.serviceFrom(context);
      mConstructor = Reflection.findBestMatchingConstructor(mType, constructorArgs);
    }

    private void bind(@NotNull final Channel<ParcelableFlow<Object>, ?> result) {
      if (!mIsBound) {
        mIsBound = true;
        final List<Channel<?, ?>> outputChannels = mOutputChannels;
        if (!outputChannels.isEmpty()) {
          result.pass(AndroidChannels.mergeParcelable(outputChannels).buildChannel());
        }
      }
    }

    @Nullable
    private Object invokeMethod() throws Exception {
      final ServiceRoutineMethod instance = mInstance;
      instance.setLocalContext(mContext);
      try {
        return mMethod.invoke(instance, mParams);

      } catch (final InvocationTargetException e) {
        throw InvocationException.wrapIfNeeded(e.getTargetException());

      } finally {
        instance.setLocalContext(null);
      }
    }
  }
}
