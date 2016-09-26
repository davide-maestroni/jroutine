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

package com.github.dm.jrt.android.v11.method;

import android.content.Context;

import com.github.dm.jrt.android.channel.AndroidChannels;
import com.github.dm.jrt.android.core.config.LoaderConfigurable;
import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.config.LoaderConfiguration.Builder;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.android.v11.core.JRoutineLoader;
import com.github.dm.jrt.android.v11.core.LoaderContext;
import com.github.dm.jrt.android.v11.object.JRoutineLoaderObject;
import com.github.dm.jrt.channel.Channels;
import com.github.dm.jrt.channel.Selectable;
import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.InvocationException;
import com.github.dm.jrt.core.routine.InvocationMode;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.method.InputChannel;
import com.github.dm.jrt.method.OutputChannel;
import com.github.dm.jrt.method.RoutineMethod;
import com.github.dm.jrt.object.config.ObjectConfigurable;
import com.github.dm.jrt.object.config.ObjectConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * This class provides an easy way to implement a routine running in dedicated Android loaders,
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
 * inside the routine by calling the {@code getContext()} method. Like, for instance:
 * <pre>
 *     <code>
 *
 *         public static class MyMethod extends LoaderRoutineMethod {
 *
 *             public MyMethod(final LoaderContext context) {
 *                 super(context);
 *             }
 *
 *             void run(final InputChannel&lt;String&gt; input,
 *                     final OutputChannel&lt;String&gt; output) {
 *                 final MyApplication application = (MyApplication) getContext();
 *                 // do it
 *             }
 *         }
 *     </code>
 * </pre>
 * <p>
 * See {@link com.github.dm.jrt.android.v4.method.LoaderRoutineMethodCompat
 * LoaderRoutineMethodCompat} for support of API levels lower than
 * {@link android.os.Build.VERSION_CODES#HONEYCOMB 11}.
 * <p>
 * Created by davide-maestroni on 08/19/2016.
 */
@SuppressWarnings("WeakerAccess")
public class LoaderRoutineMethod extends RoutineMethod
    implements LoaderConfigurable<LoaderRoutineMethod> {

  private final Object[] mArgs;

  private final Constructor<? extends LoaderRoutineMethod> mConstructor;

  private final LoaderContext mContext;

  private final AtomicBoolean mIsFirstCall = new AtomicBoolean(true);

  private final ThreadLocal<InputChannel<?>> mLocalChannel = new ThreadLocal<InputChannel<?>>();

  private final ThreadLocal<Context> mLocalContext = new ThreadLocal<Context>();

  private final ThreadLocal<Boolean> mLocalIgnore = new ThreadLocal<Boolean>();

  private LoaderConfiguration mConfiguration = LoaderConfiguration.defaultConfiguration();

  private Class<?> mReturnType;

  /**
   * Constructor.
   *
   * @param context the Loader context.
   */
  public LoaderRoutineMethod(@NotNull final LoaderContext context) {
    this(context, (Object[]) null);
  }

  /**
   * Constructor.
   *
   * @param context the Loader context.
   * @param args    the constructor arguments.
   */
  public LoaderRoutineMethod(@NotNull final LoaderContext context, @Nullable final Object... args) {
    mContext = ConstantConditions.notNull("Loader context", context);
    final Class<? extends LoaderRoutineMethod> type = getClass();
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
        constructorArgs[0] = context;
        constructorArgs[1] = safeArgs;

      } else {
        constructorArgs = new Object[]{context};
      }

    } else if (safeArgs.length > 0) {
      constructorArgs = new Object[safeArgs.length + 1];
      System.arraycopy(safeArgs, 0, constructorArgs, 1, safeArgs.length);
      constructorArgs[0] = context;

    } else {
      constructorArgs = new Object[]{context};
    }

    Constructor<? extends LoaderRoutineMethod> constructor = null;
    try {
      constructor = Reflection.findBestMatchingConstructor(type, constructorArgs);

    } catch (final IllegalArgumentException ignored) {
    }

    mArgs = constructorArgs;
    mConstructor = constructor;
  }

  /**
   * Builds a Loader object routine method by wrapping the specified static method.
   *
   * @param context the Loader context.
   * @param method  the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not static.
   */
  @NotNull
  public static ObjectLoaderRoutineMethod from(@NotNull final LoaderContext context,
      @NotNull final Method method) {
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new IllegalArgumentException("the method is not static: " + method);
    }

    return from(context, ContextInvocationTarget.classOfType(method.getDeclaringClass()), method);
  }

  /**
   * Builds a Loader object routine method by wrapping a method of the specified target.
   *
   * @param context the Loader context.
   * @param target  the invocation target.
   * @param method  the method.
   * @return the routine method instance.
   * @throws java.lang.IllegalArgumentException if the specified method is not implemented by the
   *                                            target instance.
   */
  @NotNull
  public static ObjectLoaderRoutineMethod from(@NotNull final LoaderContext context,
      @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
    if (!method.getDeclaringClass().isAssignableFrom(target.getTargetClass())) {
      throw new IllegalArgumentException(
          "the method is not applicable to the specified target class: " + target.getTargetClass());
    }

    return new ObjectLoaderRoutineMethod(context, target, method);
  }

  /**
   * Builds a Loader object routine method by wrapping a method of the specified target.
   *
   * @param context        the Loader context.
   * @param target         the invocation target.
   * @param name           the method name.
   * @param parameterTypes the method parameter types.
   * @return the routine method instance.
   * @throws java.lang.NoSuchMethodException if no method with the specified signature is found.
   */
  @NotNull
  public static ObjectLoaderRoutineMethod from(@NotNull final LoaderContext context,
      @NotNull final ContextInvocationTarget<?> target, @NotNull final String name,
      @Nullable final Class<?>... parameterTypes) throws NoSuchMethodException {
    return from(context, target, target.getTargetClass().getMethod(name, parameterTypes));
  }

  @NotNull
  @Override
  public LoaderRoutineMethod apply(@NotNull final InvocationConfiguration configuration) {
    return (LoaderRoutineMethod) super.apply(configuration);
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public InvocationConfiguration.Builder<? extends LoaderRoutineMethod>
  applyInvocationConfiguration() {
    return (InvocationConfiguration.Builder<? extends LoaderRoutineMethod>) super
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
  public <OUT> OutputChannel<OUT> call(@Nullable final Object... params) {
    final Object[] safeParams = asArgs(params);
    final Class<? extends LoaderRoutineMethod> type = getClass();
    final Method method = findBestMatchingMethod(type, safeParams);
    final ContextInvocationFactory<Selectable<Object>, Selectable<Object>> factory;
    final Constructor<? extends LoaderRoutineMethod> constructor = mConstructor;
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

    return call(factory, InvocationMode.ASYNC, safeParams);
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
  public <OUT> OutputChannel<OUT> callParallel(@Nullable final Object... params) {
    final Constructor<? extends LoaderRoutineMethod> constructor = mConstructor;
    if (constructor == null) {
      throw new IllegalStateException(
          "cannot invoke the routine in parallel mode: please provide proper "
              + "constructor arguments");
    }

    final Object[] safeParams = asArgs(params);
    final Class<? extends LoaderRoutineMethod> type = getClass();
    final Method method = findBestMatchingMethod(type, safeParams);
    return call(new MultiInvocationFactory(type, constructor, mArgs, method, safeParams),
        InvocationMode.PARALLEL, safeParams);
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
  protected <IN> InputChannel<IN> switchInput() {
    return (InputChannel<IN>) mLocalChannel.get();
  }

  @NotNull
  @Override
  public LoaderRoutineMethod apply(@NotNull final LoaderConfiguration configuration) {
    mConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public Builder<? extends LoaderRoutineMethod> applyLoaderConfiguration() {
    return new Builder<LoaderRoutineMethod>(this, mConfiguration);
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
  private <OUT> OutputChannel<OUT> call(
      @NotNull final ContextInvocationFactory<Selectable<Object>, Selectable<Object>> factory,
      @NotNull final InvocationMode mode, @NotNull final Object[] params) {
    final ArrayList<InputChannel<?>> inputChannels = new ArrayList<InputChannel<?>>();
    final ArrayList<OutputChannel<?>> outputChannels = new ArrayList<OutputChannel<?>>();
    for (final Object param : params) {
      if (param instanceof InputChannel) {
        inputChannels.add((InputChannel<?>) param);

      } else if (param instanceof OutputChannel) {
        outputChannels.add((OutputChannel<?>) param);
      }
    }

    final OutputChannel<OUT> resultChannel = outputChannel();
    outputChannels.add(resultChannel);
    final Channel<?, ? extends Selectable<Object>> inputChannel =
        (!inputChannels.isEmpty()) ? AndroidChannels.mergeParcelable(inputChannels).buildChannels()
            : JRoutineCore.io().<Selectable<Object>>of();
    final Channel<Selectable<Object>, Selectable<Object>> outputChannel = mode.invoke(JRoutineLoader
        .on(mContext)
        .with(factory)
        .apply(getConfiguration())
        .apply(getLoaderConfiguration())).pass(inputChannel).close();
    final Map<Integer, Channel<?, Object>> channelMap =
        AndroidChannels.selectOutput(0, outputChannels.size(), outputChannel).buildChannels();
    for (final Entry<Integer, Channel<?, Object>> entry : channelMap.entrySet()) {
      entry.getValue().bind((OutputChannel<Object>) outputChannels.get(entry.getKey())).close();
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

  private void setLocalInput(@Nullable final InputChannel<?> inputChannel) {
    mLocalChannel.set(inputChannel);
  }

  private void setReturnType(@NotNull final Class<?> returnType) {
    mReturnType = returnType;
  }

  /**
   * Implementation of a Loader routine method wrapping an object method.
   */
  public static class ObjectLoaderRoutineMethod extends LoaderRoutineMethod
      implements ObjectConfigurable<ObjectLoaderRoutineMethod> {

    private final LoaderContext mContext;

    private final Method mMethod;

    private final ContextInvocationTarget<?> mTarget;

    private ObjectConfiguration mConfiguration = ObjectConfiguration.defaultConfiguration();

    /**
     * Constructor.
     *
     * @param context the loader context.
     * @param target  the invocation target.
     * @param method  the method instance.
     */
    private ObjectLoaderRoutineMethod(@NotNull final LoaderContext context,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
      super(context);
      mContext = context;
      mTarget = target;
      mMethod = method;
    }

    @NotNull
    @Override
    public ObjectLoaderRoutineMethod apply(@NotNull final ObjectConfiguration configuration) {
      mConfiguration = ConstantConditions.notNull("object configuration", configuration);
      return this;
    }

    @NotNull
    @Override
    public ObjectLoaderRoutineMethod apply(@NotNull final InvocationConfiguration configuration) {
      return (ObjectLoaderRoutineMethod) super.apply(configuration);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public InvocationConfiguration.Builder<? extends ObjectLoaderRoutineMethod>
    applyInvocationConfiguration() {
      return (InvocationConfiguration.Builder<? extends ObjectLoaderRoutineMethod>) super
          .applyInvocationConfiguration();
    }

    @NotNull
    @Override
    public <OUT> OutputChannel<OUT> call(@Nullable final Object... params) {
      return call(InvocationMode.ASYNC, params);
    }

    @NotNull
    @Override
    public <OUT> OutputChannel<OUT> callParallel(@Nullable final Object... params) {
      return call(InvocationMode.PARALLEL, params);
    }

    @NotNull
    @Override
    public ObjectLoaderRoutineMethod apply(@NotNull final LoaderConfiguration configuration) {
      return (ObjectLoaderRoutineMethod) super.apply(configuration);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Builder<? extends ObjectLoaderRoutineMethod> applyLoaderConfiguration() {
      return (Builder<? extends ObjectLoaderRoutineMethod>) super.applyLoaderConfiguration();
    }

    @NotNull
    @Override
    public ObjectConfiguration.Builder<? extends ObjectLoaderRoutineMethod>
    applyObjectConfiguration() {
      return new ObjectConfiguration.Builder<ObjectLoaderRoutineMethod>(this, mConfiguration);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <OUT> OutputChannel<OUT> call(@NotNull final InvocationMode mode,
        @Nullable final Object[] params) {
      final Object[] safeParams = asArgs(params);
      final Method method = mMethod;
      if (method.getParameterTypes().length != safeParams.length) {
        throw new IllegalArgumentException("wrong number of parameters: expected <" +
            method.getParameterTypes().length + "> but was <" + safeParams.length + ">");
      }

      final Routine<Object, Object> routine = JRoutineLoaderObject.on(mContext)
                                                                  .with(mTarget)
                                                                  .apply(getConfiguration())
                                                                  .apply(getLoaderConfiguration())
                                                                  .apply(mConfiguration)
                                                                  .method(method);
      final Channel<Object, Object> channel = mode.invoke(routine).sorted();
      for (final Object param : safeParams) {
        if (param instanceof InputChannel) {
          channel.pass((InputChannel<?>) param);

        } else {
          channel.pass(param);
        }
      }

      return (OutputChannel<OUT>) toOutput(channel.close());
    }
  }

  /**
   * Base invocation implementation.
   */
  private static abstract class AbstractInvocation
      implements ContextInvocation<Selectable<Object>, Selectable<Object>> {

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
      final List<InputChannel<?>> inputChannels = getInputChannels();
      for (final InputChannel<?> inputChannel : inputChannels) {
        inputChannel.abort(reason);
      }

      try {
        if (!mIsComplete) {
          internalInvoke((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
        }

      } finally {
        resetIgnoreReturnValue();
        for (final OutputChannel<?> outputChannel : getOutputChannels()) {
          outputChannel.abort(reason);
        }
      }
    }

    @Override
    public void onComplete(@NotNull final Channel<Selectable<Object>, ?> result) throws Exception {
      bind(result);
      mIsComplete = true;
      if (!mIsAborted) {
        final List<InputChannel<?>> inputChannels = getInputChannels();
        for (final InputChannel<?> inputChannel : inputChannels) {
          inputChannel.close();
        }

        final List<OutputChannel<?>> outputChannels = getOutputChannels();
        try {
          resetIgnoreReturnValue();
          final Object methodResult =
              internalInvoke((!inputChannels.isEmpty()) ? inputChannels.get(0) : null);
          if (mReturnResults && !isIgnoreReturnValue()) {
            result.pass(new Selectable<Object>(methodResult, outputChannels.size()));
          }

        } finally {
          resetIgnoreReturnValue();
        }

        for (final OutputChannel<?> outputChannel : outputChannels) {
          outputChannel.close();
        }
      }
    }

    @Override
    public void onInput(final Selectable<Object> input,
        @NotNull final Channel<Selectable<Object>, ?> result) throws Exception {
      bind(result);
      @SuppressWarnings("unchecked") final InputChannel<Object> inputChannel =
          (InputChannel<Object>) getInputChannels().get(input.index);
      inputChannel.pass(input.data);
      try {
        resetIgnoreReturnValue();
        final Object methodResult = internalInvoke(inputChannel);
        if (mReturnResults && !isIgnoreReturnValue()) {
          result.pass(new Selectable<Object>(methodResult, getOutputChannels().size()));
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
    protected abstract List<InputChannel<?>> getInputChannels();

    /**
     * Returns the list of output channels representing the output of the method.
     *
     * @return the list of output channels.
     */
    @NotNull
    protected abstract List<OutputChannel<?>> getOutputChannels();

    /**
     * Invokes the method.
     *
     * @param inputChannel the ready input channel.
     * @return the method result.
     * @throws java.lang.Exception if an error occurred during the invocation.
     */
    @Nullable
    protected abstract Object invokeMethod(@Nullable InputChannel<?> inputChannel) throws Exception;

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

    private void bind(@NotNull final Channel<Selectable<Object>, ?> result) {
      if (!mIsBound) {
        mIsBound = true;
        final List<OutputChannel<?>> outputChannels = getOutputChannels();
        if (!outputChannels.isEmpty()) {
          result.pass(Channels.merge(outputChannels).buildChannels());
        }
      }
    }

    @Nullable
    private Object internalInvoke(@Nullable final InputChannel<?> inputChannel) throws Exception {
      try {
        return invokeMethod(inputChannel);

      } catch (final InvocationTargetException e) {
        throw InvocationException.wrapIfNeeded(e.getTargetException());
      }
    }

    @Override
    public void onRestart() throws Exception {
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

    private final Constructor<? extends LoaderRoutineMethod> mConstructor;

    private final ArrayList<InputChannel<?>> mInputChannels = new ArrayList<InputChannel<?>>();

    private final Method mMethod;

    private final Object[] mOrigParams;

    private final ArrayList<OutputChannel<?>> mOutputChannels = new ArrayList<OutputChannel<?>>();

    private Context mContext;

    private LoaderRoutineMethod mInstance;

    private Object[] mParams;

    /**
     * Constructor.
     *
     * @param constructor the routine method constructor.
     * @param args        the constructor arguments.
     * @param method      the method instance.
     * @param params      the method parameters.
     */
    private MultiInvocation(@NotNull final Constructor<? extends LoaderRoutineMethod> constructor,
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
    protected List<InputChannel<?>> getInputChannels() {
      return mInputChannels;
    }

    @Override
    public void onRestart() throws Exception {
      super.onRestart();
      final LoaderRoutineMethod instance = (mInstance = mConstructor.newInstance(mArgs));
      instance.setReturnType(mMethod.getReturnType());
      mParams = replaceChannels(mOrigParams, mInputChannels, mOutputChannels);
    }

    @Override
    public void onRecycle(final boolean isReused) throws Exception {
      mInputChannels.clear();
      mOutputChannels.clear();
    }

    @NotNull
    @Override
    protected List<OutputChannel<?>> getOutputChannels() {
      return mOutputChannels;
    }

    @Override
    protected Object invokeMethod(@Nullable final InputChannel<?> inputChannel) throws
        InvocationTargetException, IllegalAccessException {
      final LoaderRoutineMethod instance = mInstance;
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
      extends ContextInvocationFactory<Selectable<Object>, Selectable<Object>> {

    private final Object[] mArgs;

    private final Constructor<? extends LoaderRoutineMethod> mConstructor;

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
    private MultiInvocationFactory(@NotNull final Class<? extends LoaderRoutineMethod> type,
        @NotNull final Constructor<? extends LoaderRoutineMethod> constructor,
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
    public ContextInvocation<Selectable<Object>, Selectable<Object>> newInvocation() {
      return new MultiInvocation(mConstructor, mArgs, mMethod, mParams);
    }
  }

  /**
   * Invocation implementation supporting single invocation of the routine method.
   */
  private static class SingleInvocation extends AbstractInvocation {

    private final ArrayList<InputChannel<?>> mInputChannels;

    private final LoaderRoutineMethod mInstance;

    private final Method mMethod;

    private final ArrayList<OutputChannel<?>> mOutputChannels;

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
    private SingleInvocation(@NotNull final ArrayList<InputChannel<?>> inputChannels,
        @NotNull final ArrayList<OutputChannel<?>> outputChannels,
        @NotNull final LoaderRoutineMethod instance, @NotNull final Method method,
        @NotNull final Object[] params) {
      super(method);
      mInputChannels = inputChannels;
      mOutputChannels = outputChannels;
      mInstance = instance;
      mMethod = method;
      mParams = params;
    }

    @Override
    protected Object invokeMethod(@Nullable final InputChannel<?> inputChannel) throws
        InvocationTargetException, IllegalAccessException {
      final LoaderRoutineMethod instance = mInstance;
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
    public void onRecycle(final boolean isReused) {
    }

    @NotNull
    @Override
    protected List<InputChannel<?>> getInputChannels() {
      return mInputChannels;
    }

    @NotNull
    @Override
    protected List<OutputChannel<?>> getOutputChannels() {
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
      extends ContextInvocationFactory<Selectable<Object>, Selectable<Object>> {

    private final ArrayList<InputChannel<?>> mInputChannels;

    private final LoaderRoutineMethod mInstance;

    private final Method mMethod;

    private final ArrayList<OutputChannel<?>> mOutputChannels;

    private final Object[] mParams;

    /**
     * Constructor.
     *
     * @param instance the routine method instance.
     * @param method   the method instance.
     * @param params   the method parameters.
     */
    private SingleInvocationFactory(@NotNull final LoaderRoutineMethod instance,
        @NotNull final Method method, @NotNull final Object[] params) {
      super(asArgs(instance.getClass(), method, cloneArgs(params)));
      mInstance = instance;
      mMethod = method;
      final ArrayList<InputChannel<?>> inputChannels =
          (mInputChannels = new ArrayList<InputChannel<?>>());
      final ArrayList<OutputChannel<?>> outputChannels =
          (mOutputChannels = new ArrayList<OutputChannel<?>>());
      mParams = replaceChannels(params, inputChannels, outputChannels);
    }

    @NotNull
    @Override
    public ContextInvocation<Selectable<Object>, Selectable<Object>> newInvocation() {
      return new SingleInvocation(mInputChannels, mOutputChannels, mInstance, mMethod, mParams);
    }
  }
}
