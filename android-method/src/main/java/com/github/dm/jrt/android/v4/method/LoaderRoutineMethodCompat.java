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

package com.github.dm.jrt.android.v4.method;

import android.content.Context;

import com.github.dm.jrt.android.channel.JRoutineAndroidChannels;
import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.android.v4.reflect.JRoutineLoaderReflectionCompat;
import com.github.dm.jrt.channel.FlowData;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.method.RoutineMethod;
import com.github.dm.jrt.method.annotation.Input;
import com.github.dm.jrt.method.annotation.Output;
import com.github.dm.jrt.reflect.config.WrapperConfigurable;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.boxingClass;
import static com.github.dm.jrt.core.util.Reflection.boxingDefault;
import static com.github.dm.jrt.core.util.Reflection.cloneArgs;
import static com.github.dm.jrt.core.util.Reflection.findBestMatchingMethod;

/**
 * This class provides an easy way to implement a routine running in dedicated Android Loaders,
 * which can be combined in complex ways with other ones.
 * <h2>How to implement a routine</h2>
 * The class behaves like a {@link RoutineMethod} with a few differences. In order to avoid
 * undesired leaks, the implementing class must be static. Moreover, each constructor must have the
 * Loader context as first argument.
 * <br>
 * Note that, for the method to be executed inside the Loader, all the input channels must be
 * closed.
 * <h2>How to access the Android Context</h2>
 * It is possible to get access to the Android Context (that is the application instance) from
 * inside the routine by calling the {@code getContext()} method.
 * <p>
 * Like, for instance:
 * <pre><code>
 * public static class MyMethod extends LoaderRoutineMethodCompat {
 *
 *   public MyMethod(final LoaderSourceCompat source) {
 *     super(source);
 *   }
 *
 *   void run(&#64;Input final Channel&lt;?, String&gt; input,
 *       &#64;Output final Channel&lt;String, ?&gt; output) {
 *     final MyApplication application = (MyApplication) getContext();
 *     // do it
 *   }
 * }
 * </code></pre>
 * <p>
 * Created by davide-maestroni on 08/20/2016.
 */
@SuppressWarnings("WeakerAccess")
public class LoaderRoutineMethodCompat extends RoutineMethod
    implements LoaderConfigurable<LoaderRoutineMethodCompat> {

  private final Object[] mArgs;

  private final Constructor<? extends LoaderRoutineMethodCompat> mConstructor;

  private final AtomicBoolean mIsFirstCall = new AtomicBoolean(true);

  private final LoaderSourceCompat mLoaderSource;

  private final ThreadLocal<Channel<?, ?>> mLocalChannel = new ThreadLocal<Channel<?, ?>>();

  private final ThreadLocal<Context> mLocalContext = new ThreadLocal<Context>();

  private final ThreadLocal<Boolean> mLocalIgnore = new ThreadLocal<Boolean>();

  private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

  private Class<?> mReturnType;

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  public LoaderRoutineMethodCompat(@NotNull final LoaderSourceCompat loaderSource) {
    this(loaderSource, (Object[]) null);
  }

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   * @param args         the constructor arguments.
   */
  public LoaderRoutineMethodCompat(@NotNull final LoaderSourceCompat loaderSource,
      @Nullable final Object... args) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
    final Class<? extends LoaderRoutineMethodCompat> type = getClass();
    if (!Reflection.hasStaticScope(type)) {
      throw new IllegalStateException(
          "the method class must have a static scope: " + type.getName());
    }

    final Object[] constructorArgs;
    final Object[] safeArgs = Reflection.asArgs(args);
    if (type.isAnonymousClass()) {
      if (safeArgs.length > 0) {
        constructorArgs = new Object[safeArgs.length + 2];
        System.arraycopy(safeArgs, 0, constructorArgs, 2, safeArgs.length);
        constructorArgs[0] = loaderSource;
        constructorArgs[1] = safeArgs;

      } else {
        constructorArgs = new Object[]{loaderSource};
      }

    } else if (safeArgs.length > 0) {
      constructorArgs = new Object[safeArgs.length + 1];
      System.arraycopy(safeArgs, 0, constructorArgs, 1, safeArgs.length);
      constructorArgs[0] = loaderSource;

    } else {
      constructorArgs = new Object[]{loaderSource};
    }

    Constructor<? extends LoaderRoutineMethodCompat> constructor = null;
    try {
      constructor = Reflection.findBestMatchingConstructor(type, constructorArgs);

    } catch (final IllegalArgumentException ignored) {
    }

    mArgs = constructorArgs;
    mConstructor = constructor;
  }

  /**
   * Builds a Loader reflection routine method by wrapping the specified static method.
   *
   * @param context the Loader context.
   * @param method  the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not static.
   */
  @NotNull
  public static ReflectionLoaderRoutineMethodCompat from(@NotNull final LoaderSourceCompat context,
      @NotNull final Method method) {
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new IllegalArgumentException("the method is not static: " + method);
    }

    return from(context, ContextInvocationTarget.classOfType(method.getDeclaringClass()), method);
  }

  /**
   * Builds a Loader reflection routine method by wrapping a method of the specified target.
   *
   * @param context the Loader context.
   * @param target  the invocation target.
   * @param method  the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not implemented by the
   *                                            target instance.
   */
  @NotNull
  public static ReflectionLoaderRoutineMethodCompat from(@NotNull final LoaderSourceCompat context,
      @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
    if (!method.getDeclaringClass().isAssignableFrom(target.getTargetClass())) {
      throw new IllegalArgumentException(
          "the method is not applicable to the specified target class: " + target.getTargetClass());
    }

    return new ReflectionLoaderRoutineMethodCompat(context, target, method);
  }

  /**
   * Builds a Loader reflection routine method by wrapping a method of the specified target.
   *
   * @param context        the Loader context.
   * @param target         the invocation target.
   * @param name           the method name.
   * @param parameterTypes the method parameter types.
   * @return the routine method instance.
   * @throws java.lang.NoSuchMethodException if no method with the specified signature is found.
   */
  @NotNull
  public static ReflectionLoaderRoutineMethodCompat from(@NotNull final LoaderSourceCompat context,
      @NotNull final ContextInvocationTarget<?> target, @NotNull final String name,
      @Nullable final Class<?>... parameterTypes) throws NoSuchMethodException {
    return from(context, target, target.getTargetClass().getMethod(name, parameterTypes));
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
    final Class<? extends LoaderRoutineMethodCompat> type = getClass();
    final Method method = findBestMatchingMethod(type, safeParams);
    final ContextInvocationFactory<FlowData<Object>, FlowData<Object>> factory;
    final Constructor<? extends LoaderRoutineMethodCompat> constructor = mConstructor;
    if (constructor != null) {
      factory = new MultiInvocationFactory(type, constructor, mArgs, method, safeParams);

    } else {
      if (!mIsFirstCall.getAndSet(false)) {
        throw new IllegalStateException(
            "cannot invoke the routine in more than once: please provide proper "
                + "constructor arguments");
      }

      setReturnType(method.getReturnType());
      factory = new SingleInvocationFactory(this, method, safeParams);
    }

    return call(factory, method, safeParams);
  }

  @NotNull
  @Override
  public LoaderRoutineMethodCompat withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    return (LoaderRoutineMethodCompat) super.withConfiguration(configuration);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public InvocationConfiguration.Builder<? extends LoaderRoutineMethodCompat> withInvocation() {
    return (InvocationConfiguration.Builder<? extends LoaderRoutineMethodCompat>) super
        .withInvocation();
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
  public LoaderRoutineMethodCompat withConfiguration(
      @NotNull final LoaderConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public Builder<? extends LoaderRoutineMethodCompat> withLoader() {
    return new Builder<LoaderRoutineMethodCompat>(this, mConfiguration);
  }

  /**
   * Returns the Android Context (that is, the application instance).
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
   * Returns the Loader configuration.
   *
   * @return the Loader configuration.
   */
  @NotNull
  protected LoaderConfiguration getLoaderConfiguration() {
    return mConfiguration;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <OUT> Channel<?, OUT> call(
      @NotNull final ContextInvocationFactory<FlowData<Object>, FlowData<Object>> factory,
      @NotNull final Method method, @NotNull final Object[] params) {
    final ArrayList<Channel<?, ?>> inputChannels = new ArrayList<Channel<?, ?>>();
    final ArrayList<Channel<?, ?>> outputChannels = new ArrayList<Channel<?, ?>>();
    final Annotation[][] annotations = method.getParameterAnnotations();
    final int length = params.length;
    for (int i = 0; i < length; ++i) {
      final Object param = params[i];
      final Class<? extends Annotation> annotationType = getAnnotationType(param, annotations[i]);
      if (annotationType == Input.class) {
        inputChannels.add((Channel<?, ?>) param);

      } else if (annotationType == Output.class) {
        outputChannels.add((Channel<?, ?>) param);
      }
    }

    final Channel<?, OUT> resultChannel = JRoutineCore.<OUT>ofData().buildChannel();
    outputChannels.add(resultChannel);
    final Channel<?, ? extends FlowData<Object>> inputChannel =
        (!inputChannels.isEmpty()) ? JRoutineAndroidChannels.mergeParcelableOutput(inputChannels)
                                                            .buildChannel()
            : JRoutineCore.<FlowData<Object>>of().buildChannel();
    final Channel<FlowData<Object>, FlowData<Object>> outputChannel = JRoutineLoaderCompat.on(mLoaderSource)
                                                                                          .with(factory)
                                                                                          .withConfiguration(
                                                                                      getConfiguration())
                                                                                          .withConfiguration(
                                                                                      getLoaderConfiguration())
                                                                                          .invoke()
                                                                                          .pass(
                                                                                      inputChannel)
                                                                                          .close();
    final Map<Integer, ? extends Channel<?, Object>> channelMap =
        JRoutineAndroidChannels.flowOutput(0, outputChannels.size(), outputChannel).buildChannelMap();
    for (final Entry<Integer, ? extends Channel<?, Object>> entry : channelMap.entrySet()) {
      ((Channel<Object, Object>) outputChannels.get(entry.getKey())).pass(entry.getValue()).close();
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
   * Implementation of a Loader routine method wrapping an object method.
   */
  public static class ReflectionLoaderRoutineMethodCompat extends LoaderRoutineMethodCompat
      implements WrapperConfigurable<ReflectionLoaderRoutineMethodCompat> {

    private final LoaderSourceCompat mContext;

    private final Method mMethod;

    private final ContextInvocationTarget<?> mTarget;

    private WrapperConfiguration mConfiguration = WrapperConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the loader context.
     * @param target  the invocation target.
     * @param method  the method instance.
     */
    private ReflectionLoaderRoutineMethodCompat(@NotNull final LoaderSourceCompat context,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
      super(context);
      mContext = context;
      mTarget = target;
      mMethod = method;
    }

    @NotNull
    @Override
    public ReflectionLoaderRoutineMethodCompat withConfiguration(
        @NotNull final WrapperConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public ReflectionLoaderRoutineMethodCompat withConfiguration(
        @NotNull final InvocationConfiguration configuration) {
      return (ReflectionLoaderRoutineMethodCompat) super.withConfiguration(configuration);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public <OUT> Channel<?, OUT> call(@Nullable final Object... params) {
      final Object[] safeParams = asArgs(params);
      final Method method = mMethod;
      if (method.getParameterTypes().length != safeParams.length) {
        throw new IllegalArgumentException(
            "wrong number of parameters: expected <" + method.getParameterTypes().length
                + "> but was <" + safeParams.length + ">");
      }

      final Routine<Object, Object> routine = JRoutineLoaderReflectionCompat.on(mContext)
                                                                            .with(mTarget)
                                                                            .withConfiguration(
                                                                                getConfiguration())
                                                                            .withConfiguration(
                                                                                getLoaderConfiguration())
                                                                            .withConfiguration(
                                                                                mConfiguration)
                                                                            .method(method);
      final Channel<Object, Object> channel = routine.invoke().sorted();
      for (final Object param : safeParams) {
        if (param instanceof Channel) {
          channel.pass((Channel<?, ?>) param);

        } else {
          channel.pass(param);
        }
      }

      return (Channel<?, OUT>) channel.close();
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public InvocationConfiguration.Builder<? extends ReflectionLoaderRoutineMethodCompat>
    withInvocation() {
      return (InvocationConfiguration.Builder<? extends ReflectionLoaderRoutineMethodCompat>)
          super.withInvocation();
    }

    @NotNull
    @Override
    public ReflectionLoaderRoutineMethodCompat withConfiguration(
        @NotNull final LoaderConfiguration configuration) {
      return (ReflectionLoaderRoutineMethodCompat) super.withConfiguration(configuration);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Builder<? extends ReflectionLoaderRoutineMethodCompat> withLoader() {
      return (Builder<? extends ReflectionLoaderRoutineMethodCompat>) super.withLoader();
    }

    @NotNull
    @Override
    public WrapperConfiguration.Builder<? extends ReflectionLoaderRoutineMethodCompat>
    withWrapper() {

      return new WrapperConfiguration.Builder<ReflectionLoaderRoutineMethodCompat>(this,
          mConfiguration);
    }
  }

  /**
   * Base invocation implementation.
   */
  private static abstract class AbstractInvocation
      implements ContextInvocation<FlowData<Object>, FlowData<Object>> {

    private final boolean mReturnResults;

    private boolean mIsAborted;

    private boolean mIsBound;

    private boolean mIsComplete;

    /**
     * Constructor.
     *
     * @param method the method instance.
     */
    private AbstractInvocation(@NotNull final Method method) {
      mReturnResults = (boxingClass(method.getReturnType()) != Void.class);
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) throws Exception {
      mIsAborted = true;
      final List<Channel<?, ?>> inputChannels = getInputChannels();
      for (final Channel<?, ?> inputChannel : inputChannels) {
        inputChannel.abort(reason);
      }

      try {
        if (!mIsComplete) {
          internalInvoke((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
        }

      } finally {
        resetIgnoreReturnValue();
        for (final Channel<?, ?> outputChannel : getOutputChannels()) {
          outputChannel.abort(reason);
        }
      }
    }

    @Override
    public void onComplete(@NotNull final Channel<FlowData<Object>, ?> result) throws Exception {
      bind(result);
      mIsComplete = true;
      if (!mIsAborted) {
        final List<Channel<?, ?>> inputChannels = getInputChannels();
        for (final Channel<?, ?> inputChannel : inputChannels) {
          inputChannel.close();
        }

        final List<Channel<?, ?>> outputChannels = getOutputChannels();
        try {
          resetIgnoreReturnValue();
          final Object methodResult =
              internalInvoke((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
          if (mReturnResults && !isIgnoreReturnValue()) {
            result.pass(new FlowData<Object>(outputChannels.size(), methodResult));
          }

        } finally {
          resetIgnoreReturnValue();
        }

        for (final Channel<?, ?> outputChannel : outputChannels) {
          outputChannel.close();
        }
      }
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onInput(final FlowData<Object> input,
        @NotNull final Channel<FlowData<Object>, ?> result) throws Exception {
      bind(result);
      @SuppressWarnings("unchecked") final Channel<Object, Object> inputChannel =
          (Channel<Object, Object>) getInputChannels().get(input.id);
      inputChannel.pass(input.data);
      try {
        resetIgnoreReturnValue();
        final Object methodResult = internalInvoke(inputChannel);
        if (mReturnResults && !isIgnoreReturnValue()) {
          result.pass(new FlowData<Object>(getOutputChannels().size(), methodResult));
        }

      } finally {
        resetIgnoreReturnValue();
      }
    }

    /**
     * Returns the list of input channels representing the input of the method.
     *
     * @return the list of input channels.
     */
    @NotNull
    protected abstract List<Channel<?, ?>> getInputChannels();

    /**
     * Returns the list of output channels representing the output of the method.
     *
     * @return the list of output channels.
     */
    @NotNull
    protected abstract List<Channel<?, ?>> getOutputChannels();

    /**
     * Invokes the method.
     *
     * @param inputChannel the ready input channel.
     * @return the method result.
     * @throws java.lang.Exception if an error occurred during the invocation.
     */
    @Nullable
    protected abstract Object invokeMethod(@Nullable Channel<?, ?> inputChannel) throws Exception;

    /**
     * Checks if the method return value must be ignored.
     *
     * @return whether the return value must be ignored.
     */
    protected abstract boolean isIgnoreReturnValue();

    /**
     * Resets the method return value ignore flag.
     */
    protected abstract void resetIgnoreReturnValue();

    private void bind(@NotNull final Channel<FlowData<Object>, ?> result) {
      if (!mIsBound) {
        mIsBound = true;
        final List<Channel<?, ?>> outputChannels = getOutputChannels();
        if (!outputChannels.isEmpty()) {
          result.pass(Channels.mergeOutput(outputChannels).buildChannel());
        }
      }
    }

    @Nullable
    private Object internalInvoke(@Nullable final Channel<?, ?> inputChannel) throws Exception {
      try {
        return invokeMethod(inputChannel);

      } catch (final InvocationTargetException e) {
        throw InvocationException.wrapIfNeeded(e.getTargetException());
      }
    }

    @Override
    public void onStart() throws Exception {
      mIsBound = false;
      mIsAborted = false;
      mIsComplete = false;
    }
  }

  /**
   * Invocation implementation supporting multiple invocation of the routine method.
   */
  private static class MultiInvocation extends AbstractInvocation {

    private final Object[] mArgs;

    private final Constructor<? extends LoaderRoutineMethodCompat> mConstructor;

    private final ArrayList<Channel<?, ?>> mInputChannels = new ArrayList<Channel<?, ?>>();

    private final Method mMethod;

    private final Object[] mOrigParams;

    private final ArrayList<Channel<?, ?>> mOutputChannels = new ArrayList<Channel<?, ?>>();

    private Context mContext;

    private LoaderRoutineMethodCompat mInstance;

    private Object[] mParams;

    /**
     * Constructor.
     *
     * @param constructor the routine method constructor.
     * @param args        the constructor arguments.
     * @param method      the method instance.
     * @param params      the method parameters.
     */
    private MultiInvocation(
        @NotNull final Constructor<? extends LoaderRoutineMethodCompat> constructor,
        @NotNull final Object[] args, @NotNull final Method method,
        @NotNull final Object[] params) {
      super(method);
      mConstructor = constructor;
      mArgs = args;
      mMethod = method;
      mOrigParams = params;
    }

    @Override
    public void onContext(@NotNull final Context context) {
      mContext = context;
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getInputChannels() {
      return mInputChannels;
    }

    @Override
    public void onStart() throws Exception {
      super.onStart();
      final LoaderRoutineMethodCompat instance = (mInstance = mConstructor.newInstance(mArgs));
      final Method method = mMethod;
      instance.setReturnType(method.getReturnType());
      mParams = replaceChannels(method, mOrigParams, mInputChannels, mOutputChannels);
    }

    @Override
    public boolean onRecycle() {
      mInputChannels.clear();
      mOutputChannels.clear();
      return true;
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getOutputChannels() {
      return mOutputChannels;
    }

    @Override
    protected Object invokeMethod(@Nullable final Channel<?, ?> inputChannel) throws
        InvocationTargetException, IllegalAccessException {
      final LoaderRoutineMethodCompat instance = mInstance;
      instance.setLocalContext(mContext);
      instance.setLocalInput(inputChannel);
      try {
        return mMethod.invoke(instance, mParams);

      } finally {
        instance.setLocalInput(null);
        instance.setLocalContext(null);
      }
    }

    @Override
    protected boolean isIgnoreReturnValue() {
      return mInstance.isIgnoreReturnValue();
    }

    @Override
    protected void resetIgnoreReturnValue() {
      mInstance.resetIgnoreReturnValue();
    }
  }

  /**
   * Invocation factory supporting multiple invocation of the routine method.
   */
  private static class MultiInvocationFactory
      extends ContextInvocationFactory<FlowData<Object>, FlowData<Object>> {

    private final Object[] mArgs;

    private final Constructor<? extends LoaderRoutineMethodCompat> mConstructor;

    private final Method mMethod;

    private final Object[] mParams;

    /**
     * Constructor.
     *
     * @param type        the routine method type.
     * @param constructor the routine method constructor.
     * @param args        the constructor arguments.
     * @param method      the method instance.
     * @param params      the method parameters.
     */
    private MultiInvocationFactory(@NotNull final Class<? extends LoaderRoutineMethodCompat> type,
        @NotNull final Constructor<? extends LoaderRoutineMethodCompat> constructor,
        @NotNull final Object[] args, @NotNull final Method method,
        @NotNull final Object[] params) {
      super(asArgs(type, args, method, cloneArgs(params)));
      mConstructor = constructor;
      mArgs = args;
      mMethod = method;
      mParams = cloneArgs(params);
    }

    @NotNull
    @Override
    public ContextInvocation<FlowData<Object>, FlowData<Object>> newInvocation() {
      return new MultiInvocation(mConstructor, mArgs, mMethod, mParams);
    }
  }

  /**
   * Invocation implementation supporting single invocation of the routine method.
   */
  private static class SingleInvocation extends AbstractInvocation {

    private final ArrayList<Channel<?, ?>> mInputChannels;

    private final LoaderRoutineMethodCompat mInstance;

    private final Method mMethod;

    private final ArrayList<Channel<?, ?>> mOutputChannels;

    private final Object[] mParams;

    private Context mContext;

    /**
     * Constructor.
     *
     * @param inputChannels  the list of input channels.
     * @param outputChannels the list of output channels.
     * @param instance       the target instance.
     * @param method         the method instance.
     * @param params         the method parameters.
     */
    private SingleInvocation(@NotNull final ArrayList<Channel<?, ?>> inputChannels,
        @NotNull final ArrayList<Channel<?, ?>> outputChannels,
        @NotNull final LoaderRoutineMethodCompat instance, @NotNull final Method method,
        @NotNull final Object[] params) {
      super(method);
      mInputChannels = inputChannels;
      mOutputChannels = outputChannels;
      mInstance = instance;
      mMethod = method;
      mParams = params;
    }

    @Override
    protected Object invokeMethod(@Nullable final Channel<?, ?> inputChannel) throws
        InvocationTargetException, IllegalAccessException {
      final LoaderRoutineMethodCompat instance = mInstance;
      instance.setLocalContext(mContext);
      instance.setLocalInput(inputChannel);
      try {
        return mMethod.invoke(instance, mParams);

      } finally {
        instance.setLocalInput(null);
        instance.setLocalContext(null);
      }
    }

    @Override
    public void onContext(@NotNull final Context context) {
      mContext = context;
    }

    @Override
    public boolean onRecycle() {
      return true;
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getInputChannels() {
      return mInputChannels;
    }

    @NotNull
    @Override
    protected List<Channel<?, ?>> getOutputChannels() {
      return mOutputChannels;
    }

    @Override
    protected boolean isIgnoreReturnValue() {
      return mInstance.isIgnoreReturnValue();
    }

    @Override
    protected void resetIgnoreReturnValue() {
      mInstance.resetIgnoreReturnValue();
    }
  }

  /**
   * Invocation factory supporting single invocation of the routine method.
   */
  private static class SingleInvocationFactory
      extends ContextInvocationFactory<FlowData<Object>, FlowData<Object>> {

    private final ArrayList<Channel<?, ?>> mInputChannels;

    private final LoaderRoutineMethodCompat mInstance;

    private final Method mMethod;

    private final ArrayList<Channel<?, ?>> mOutputChannels;

    private final Object[] mParams;

    /**
     * Constructor.
     *
     * @param instance the routine method instance.
     * @param method   the method instance.
     * @param params   the method parameters.
     */
    private SingleInvocationFactory(@NotNull final LoaderRoutineMethodCompat instance,
        @NotNull final Method method, @NotNull final Object[] params) {
      super(asArgs(instance.getClass(), method, cloneArgs(params)));
      mInstance = instance;
      mMethod = method;
      final ArrayList<Channel<?, ?>> inputChannels =
          (mInputChannels = new ArrayList<Channel<?, ?>>());
      final ArrayList<Channel<?, ?>> outputChannels =
          (mOutputChannels = new ArrayList<Channel<?, ?>>());
      mParams = replaceChannels(method, params, inputChannels, outputChannels);
    }

    @NotNull
    @Override
    public ContextInvocation<FlowData<Object>, FlowData<Object>> newInvocation() {
      return new SingleInvocation(mInputChannels, mOutputChannels, mInstance, mMethod, mParams);
    }
  }
}
