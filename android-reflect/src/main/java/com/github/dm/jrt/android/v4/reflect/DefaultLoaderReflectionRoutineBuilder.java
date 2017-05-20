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

package com.github.dm.jrt.android.v4.reflect;

import android.content.Context;

import com.github.dm.jrt.android.core.config.LoaderConfiguration;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocationFactory;
import com.github.dm.jrt.android.core.routine.LoaderRoutine;
import com.github.dm.jrt.android.reflect.ContextInvocationTarget;
import com.github.dm.jrt.android.reflect.builder.LoaderReflectionRoutineBuilder;
import com.github.dm.jrt.android.reflect.util.ContextInvocationReflection;
import com.github.dm.jrt.android.v4.core.JRoutineLoaderCompat;
import com.github.dm.jrt.android.v4.core.LoaderSourceCompat;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.executor.ScheduledExecutors;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.reflect.InvocationTarget;
import com.github.dm.jrt.reflect.JRoutineReflection;
import com.github.dm.jrt.reflect.annotation.AsyncInput.InputMode;
import com.github.dm.jrt.reflect.annotation.AsyncOutput.OutputMode;
import com.github.dm.jrt.reflect.common.Mutex;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;
import com.github.dm.jrt.reflect.util.InvocationReflection;
import com.github.dm.jrt.reflect.util.InvocationReflection.MethodInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.findMethod;
import static com.github.dm.jrt.reflect.util.InvocationReflection.callFromInvocation;
import static com.github.dm.jrt.reflect.util.InvocationReflection.getAnnotatedMethod;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p>
 * Created by davide-maestroni on 04/06/2015.
 */
class DefaultLoaderReflectionRoutineBuilder implements LoaderReflectionRoutineBuilder {

  private final LoaderSourceCompat mLoaderSource;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.defaultConfiguration();

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param loaderSource the Loader source.
   */
  DefaultLoaderReflectionRoutineBuilder(@NotNull final LoaderSourceCompat loaderSource) {
    mLoaderSource = ConstantConditions.notNull("Loader source", loaderSource);
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> methodOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final String name) {
    final Method targetMethod = getAnnotatedMethod(target.getTargetClass(), name);
    if (targetMethod == null) {
      return methodOf(target, name, Reflection.NO_PARAMS);
    }

    final WrapperConfiguration wrapperConfiguration =
        InvocationReflection.withAnnotations(mWrapperConfiguration, targetMethod);
    final MethodAliasInvocationFactory<IN, OUT> factory =
        new MethodAliasInvocationFactory<IN, OUT>(targetMethod, wrapperConfiguration, target, name);
    final InvocationConfiguration invocationConfiguration =
        InvocationReflection.withAnnotations(mInvocationConfiguration, targetMethod);
    final LoaderConfiguration loaderConfiguration =
        ContextInvocationReflection.withAnnotations(mLoaderConfiguration, targetMethod);
    return JRoutineLoaderCompat.routineOn(mLoaderSource)
                               .withConfiguration(invocationConfiguration)
                               .withConfiguration(loaderConfiguration)
                               .of(factory);
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> methodOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final String name, @NotNull final Class<?>... parameterTypes) {
    return methodOf(target, findMethod(target.getTargetClass(), name, parameterTypes));
  }

  @NotNull
  @Override
  public <IN, OUT> LoaderRoutine<IN, OUT> methodOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final Method method) {
    final WrapperConfiguration wrapperConfiguration =
        InvocationReflection.withAnnotations(mWrapperConfiguration, method);
    final MethodSignatureInvocationFactory<IN, OUT> factory =
        new MethodSignatureInvocationFactory<IN, OUT>(method, wrapperConfiguration, target, method);
    final InvocationConfiguration invocationConfiguration =
        InvocationReflection.withAnnotations(mInvocationConfiguration, method);
    final LoaderConfiguration loaderConfiguration =
        ContextInvocationReflection.withAnnotations(mLoaderConfiguration, method);
    return JRoutineLoaderCompat.routineOn(mLoaderSource)
                               .withConfiguration(invocationConfiguration)
                               .withConfiguration(loaderConfiguration)
                               .of(factory);
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final ClassToken<TYPE> itf) {
    return proxyOf(target, itf.getRawClass());
  }

  @NotNull
  @Override
  public <TYPE> TYPE proxyOf(@NotNull final ContextInvocationTarget<?> target,
      @NotNull final Class<TYPE> itf) {
    if (!itf.isInterface()) {
      throw new IllegalArgumentException(
          "the specified class is not an interface: " + itf.getName());
    }

    final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
        new ProxyInvocationHandler(target, this));
    return itf.cast(proxy);
  }

  @Override
  @NotNull
  public LoaderReflectionRoutineBuilder withConfiguration(
      @NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderReflectionRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public LoaderReflectionRoutineBuilder withConfiguration(
      @NotNull final LoaderConfiguration configuration) {
    mLoaderConfiguration = ConstantConditions.notNull("Loader configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends LoaderReflectionRoutineBuilder> withInvocation
      () {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<LoaderReflectionRoutineBuilder>(
        new InvocationConfiguration.Configurable<LoaderReflectionRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionRoutineBuilder withConfiguration(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultLoaderReflectionRoutineBuilder.this.withConfiguration(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public LoaderConfiguration.Builder<? extends LoaderReflectionRoutineBuilder> withLoader() {
    final LoaderConfiguration config = mLoaderConfiguration;
    return new LoaderConfiguration.Builder<LoaderReflectionRoutineBuilder>(this, config);
  }

  @NotNull
  @Override
  public WrapperConfiguration.Builder<? extends LoaderReflectionRoutineBuilder> withWrapper() {
    final WrapperConfiguration config = mWrapperConfiguration;
    return new WrapperConfiguration.Builder<LoaderReflectionRoutineBuilder>(
        new WrapperConfiguration.Configurable<LoaderReflectionRoutineBuilder>() {

          @NotNull
          @Override
          public LoaderReflectionRoutineBuilder withConfiguration(
              @NotNull final WrapperConfiguration configuration) {
            return DefaultLoaderReflectionRoutineBuilder.this.withConfiguration(configuration);
          }
        }, config);
  }

  /**
   * Alias method invocation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class MethodAliasInvocation<IN, OUT> implements ContextInvocation<IN, OUT> {

    private final String mAliasName;

    private final ContextInvocationTarget<?> mTarget;

    private final WrapperConfiguration mWrapperConfiguration;

    private Channel<IN, OUT> mChannel;

    private Object mInstance;

    private Routine<IN, OUT> mRoutine = null;

    /**
     * Constructor.
     *
     * @param wrapperConfiguration the wrapper configuration.
     * @param target               the invocation target.
     * @param name                 the alias name.
     */
    private MethodAliasInvocation(@NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {
      mWrapperConfiguration = wrapperConfiguration;
      mTarget = target;
      mAliasName = name;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mChannel.abort(reason);
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
      mInstance = target.getTarget();
      final Object targetInstance = mInstance;
      if (targetInstance == null) {
        throw new IllegalStateException("the target object has been destroyed");
      }

      mRoutine = JRoutineReflection.wrapperOn(ScheduledExecutors.syncExecutor())
                                   .withConfiguration(mWrapperConfiguration)
                                   .methodOf(target, mAliasName);
    }

    @Override
    public void onDestroy() {
      mRoutine = null;
      mInstance = null;
    }

    @Override
    public boolean onRecycle() {
      mChannel = null;
      return true;
    }

    @Override
    public void onStart() {
      mChannel = mRoutine.invoke();
    }

    @Override
    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
      mChannel.pass(input);
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) {
      result.pass(mChannel.close());
    }
  }

  /**
   * Factory of {@link MethodAliasInvocation}s.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class MethodAliasInvocationFactory<IN, OUT>
      extends ContextInvocationFactory<IN, OUT> {

    private final String mName;

    private final ContextInvocationTarget<?> mTarget;

    private final WrapperConfiguration mWrapperConfiguration;

    /**
     * Constructor.
     *
     * @param targetMethod         the target method.
     * @param wrapperConfiguration the wrapper configuration.
     * @param target               the invocation target.
     * @param name                 the alias name.
     */
    private MethodAliasInvocationFactory(@NotNull final Method targetMethod,
        @NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {
      super(asArgs(targetMethod, wrapperConfiguration, target, name));
      mWrapperConfiguration = wrapperConfiguration;
      mTarget = target;
      mName = name;
    }

    @NotNull
    @Override
    public ContextInvocation<IN, OUT> newInvocation() {
      return new MethodAliasInvocation<IN, OUT>(mWrapperConfiguration, mTarget, mName);
    }
  }

  /**
   * Generic method invocation.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class MethodSignatureInvocation<IN, OUT> implements ContextInvocation<IN, OUT> {

    private final Method mMethod;

    private final ContextInvocationTarget<?> mTarget;

    private final WrapperConfiguration mWrapperConfiguration;

    private Channel<IN, OUT> mChannel;

    private Object mInstance;

    private Routine<IN, OUT> mRoutine = null;

    /**
     * Constructor.
     *
     * @param wrapperConfiguration the wrapper configuration.
     * @param target               the invocation target.
     * @param method               the method.
     */
    private MethodSignatureInvocation(@NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
      mWrapperConfiguration = wrapperConfiguration;
      mTarget = target;
      mMethod = method;
    }

    @Override
    public void onComplete(@NotNull final Channel<OUT, ?> result) {
      result.pass(mChannel.close());
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mChannel.abort(reason);
    }

    @Override
    public void onDestroy() {
      mRoutine = null;
      mInstance = null;
    }

    @Override
    public boolean onRecycle() {
      mChannel = null;
      return true;
    }

    @Override
    public void onStart() {
      mChannel = mRoutine.invoke();
    }

    @Override
    public void onInput(final IN input, @NotNull final Channel<OUT, ?> result) {
      mChannel.pass(input);
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
      mInstance = target.getTarget();
      final Object targetInstance = mInstance;
      if (targetInstance == null) {
        throw new IllegalStateException("the target object has been destroyed");
      }

      mRoutine = JRoutineReflection.wrapperOn(ScheduledExecutors.syncExecutor())
                                   .withConfiguration(mWrapperConfiguration)
                                   .methodOf(target, mMethod);
    }
  }

  /**
   * Factory of {@link MethodSignatureInvocation}s.
   *
   * @param <IN>  the input data type.
   * @param <OUT> the output data type.
   */
  private static class MethodSignatureInvocationFactory<IN, OUT>
      extends ContextInvocationFactory<IN, OUT> {

    private final Method mMethod;

    private final ContextInvocationTarget<?> mTarget;

    private final WrapperConfiguration mWrapperConfiguration;

    /**
     * Constructor.
     *
     * @param targetMethod         the target method.
     * @param wrapperConfiguration the wrapper configuration.
     * @param target               the invocation target.
     * @param method               the method.
     */
    private MethodSignatureInvocationFactory(@NotNull final Method targetMethod,
        @NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final Method method) {
      super(asArgs(targetMethod, wrapperConfiguration, target, method));
      mWrapperConfiguration = wrapperConfiguration;
      mTarget = target;
      mMethod = method;
    }

    @NotNull
    @Override
    public ContextInvocation<IN, OUT> newInvocation() {
      return new MethodSignatureInvocation<IN, OUT>(mWrapperConfiguration, mTarget, mMethod);
    }
  }

  /**
   * Proxy method invocation.
   */
  private static class ProxyInvocation extends CallContextInvocation<Object, Object> {

    private final InputMode mInputMode;

    private final OutputMode mOutputMode;

    private final ContextInvocationTarget<?> mTarget;

    private final Method mTargetMethod;

    private final WrapperConfiguration mWrapperConfiguration;

    private Object mInstance;

    private Mutex mMutex = Mutex.NONE;

    /**
     * Constructor.
     *
     * @param targetMethod         the target method.
     * @param wrapperConfiguration the wrapper configuration.
     * @param target               the invocation target.
     * @param inputMode            the input transfer mode.
     * @param outputMode           the output transfer mode.
     */
    private ProxyInvocation(@NotNull final Method targetMethod,
        @NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final ContextInvocationTarget<?> target, @Nullable final InputMode inputMode,
        @Nullable final OutputMode outputMode) {
      mTargetMethod = targetMethod;
      mWrapperConfiguration = wrapperConfiguration;
      mTarget = target;
      mInputMode = inputMode;
      mOutputMode = outputMode;
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      super.onContext(context);
      final InvocationTarget<?> target = mTarget.getInvocationTarget(context);
      final Object mutexTarget =
          (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
              : target.getTarget();
      mMutex = InvocationReflection.getSharedMutex(mutexTarget,
          mWrapperConfiguration.getSharedFieldsOrElse(null));
      mInstance = target.getTarget();
      final Object targetInstance = mInstance;
      if (targetInstance == null) {
        throw new IllegalStateException("the target object has been destroyed");
      }
    }

    @Override
    public void onDestroy() {
      mInstance = null;
    }

    @Override
    public boolean onRecycle() {
      return true;
    }

    @Override
    protected void onCall(@NotNull final List<?> objects,
        @NotNull final Channel<Object, ?> result) throws Exception {
      callFromInvocation(mMutex, mInstance, mTargetMethod, objects, result, mInputMode,
          mOutputMode);
    }
  }

  /**
   * Factory of {@link ProxyInvocation}s.
   */
  private static class ProxyInvocationFactory extends ContextInvocationFactory<Object, Object> {

    private final InputMode mInputMode;

    private final OutputMode mOutputMode;

    private final ContextInvocationTarget<?> mTarget;

    private final Method mTargetMethod;

    private final WrapperConfiguration mWrapperConfiguration;

    /**
     * Constructor.
     *
     * @param targetMethod         the target method.
     * @param wrapperConfiguration the wrapper configuration.
     * @param target               the invocation target.
     * @param inputMode            the input transfer mode.
     * @param outputMode           the output transfer mode.
     */
    private ProxyInvocationFactory(@NotNull final Method targetMethod,
        @NotNull final WrapperConfiguration wrapperConfiguration,
        @NotNull final ContextInvocationTarget<?> target, @Nullable final InputMode inputMode,
        @Nullable final OutputMode outputMode) {
      super(asArgs(targetMethod, wrapperConfiguration, target, inputMode, outputMode));
      mTargetMethod = targetMethod;
      mWrapperConfiguration = wrapperConfiguration;
      mTarget = target;
      mInputMode = inputMode;
      mOutputMode = outputMode;
    }

    @NotNull
    @Override
    public ContextInvocation<Object, Object> newInvocation() {
      return new ProxyInvocation(mTargetMethod, mWrapperConfiguration, mTarget, mInputMode,
          mOutputMode);
    }
  }

  /**
   * Invocation handler adapting a different interface to the target object instance.
   */
  private static class ProxyInvocationHandler implements InvocationHandler {

    private final InvocationConfiguration mInvocationConfiguration;

    private final LoaderConfiguration mLoaderConfiguration;

    private final LoaderSourceCompat mLoaderSource;

    private final ContextInvocationTarget<?> mTarget;

    private final WrapperConfiguration mWrapperConfiguration;

    /**
     * Constructor.
     *
     * @param target  the invocation target.
     * @param builder the builder instance.
     */
    private ProxyInvocationHandler(@NotNull final ContextInvocationTarget<?> target,
        @NotNull final DefaultLoaderReflectionRoutineBuilder builder) {
      mTarget = ConstantConditions.notNull("invocation target", target);
      mLoaderSource = builder.mLoaderSource;
      mInvocationConfiguration = builder.mInvocationConfiguration;
      mWrapperConfiguration = builder.mWrapperConfiguration;
      mLoaderConfiguration = builder.mLoaderConfiguration;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws
        Throwable {
      final ContextInvocationTarget<?> target = mTarget;
      final MethodInfo methodInfo =
          InvocationReflection.getTargetMethodInfo(target.getTargetClass(), method);
      final Method targetMethod = methodInfo.method;
      final InputMode inputMode = methodInfo.inputMode;
      final OutputMode outputMode = methodInfo.outputMode;
      final WrapperConfiguration wrapperConfiguration =
          InvocationReflection.withAnnotations(mWrapperConfiguration, targetMethod);
      final InvocationConfiguration invocationConfiguration =
          InvocationReflection.withAnnotations(mInvocationConfiguration, method);
      final LoaderConfiguration loaderConfiguration =
          ContextInvocationReflection.withAnnotations(mLoaderConfiguration, method);
      final ProxyInvocationFactory factory =
          new ProxyInvocationFactory(targetMethod, wrapperConfiguration, target, inputMode,
              outputMode);
      final LoaderRoutine<Object, Object> routine = JRoutineLoaderCompat.routineOn(mLoaderSource)
                                                                        .withConfiguration(
                                                                            invocationConfiguration)
                                                                        .withConfiguration(
                                                                            loaderConfiguration)
                                                                        .of(factory);
      return InvocationReflection.invokeRoutine(routine, method, asArgs(args), inputMode,
          outputMode);
    }
  }
}
