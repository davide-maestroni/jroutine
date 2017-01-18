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

package com.github.dm.jrt.reflect;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.channel.Channel;
import com.github.dm.jrt.core.config.InvocationConfiguration;
import com.github.dm.jrt.core.invocation.CallInvocation;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.routine.Routine;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;
import com.github.dm.jrt.core.util.WeakIdentityHashMap;
import com.github.dm.jrt.reflect.annotation.AsyncInput.InputMode;
import com.github.dm.jrt.reflect.annotation.AsyncOutput.OutputMode;
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilder;
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.MethodInfo;
import com.github.dm.jrt.reflect.common.Mutex;
import com.github.dm.jrt.reflect.config.ReflectionConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.callFromInvocation;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.getAnnotatedMethod;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.getSharedMutex;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.getTargetMethodInfo;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.invokeRoutine;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.withAnnotations;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p>
 * Created by davide-maestroni on 09/21/2014.
 */
class DefaultReflectionRoutineBuilder implements ReflectionRoutineBuilder {

  private static final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>> sRoutines =
      new WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>>();

  private final InvocationTarget<?> mTarget;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ReflectionConfiguration mReflectionConfiguration =
      ReflectionConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param target the invocation target.
   * @throws java.lang.IllegalArgumentException if the class of specified target represents an
   *                                            interface.
   */
  DefaultReflectionRoutineBuilder(@NotNull final InvocationTarget<?> target) {
    final Class<?> targetClass = target.getTargetClass();
    if (targetClass.isInterface()) {
      throw new IllegalArgumentException(
          "the target class must not be an interface: " + targetClass.getName());
    }

    mTarget = target;
  }

  @NotNull
  public ReflectionRoutineBuilder apply(@NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  public ReflectionRoutineBuilder apply(@NotNull final ReflectionConfiguration configuration) {
    mReflectionConfiguration =
        ConstantConditions.notNull("reflection configuration", configuration);
    return this;
  }

  @NotNull
  public InvocationConfiguration.Builder<? extends ReflectionRoutineBuilder>
  applyInvocationConfiguration() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<ReflectionRoutineBuilder>(this, config);
  }

  @NotNull
  public ReflectionConfiguration.Builder<? extends ReflectionRoutineBuilder>
  applyReflectionConfiguration() {
    final ReflectionConfiguration config = mReflectionConfiguration;
    return new ReflectionConfiguration.Builder<ReflectionRoutineBuilder>(this, config);
  }

  @NotNull
  public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
    if (!itf.isInterface()) {
      throw new IllegalArgumentException(
          "the specified class is not an interface: " + itf.getName());
    }

    final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
        new ProxyInvocationHandler());
    return itf.cast(proxy);
  }

  @NotNull
  public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {
    return itf.cast(buildProxy(itf.getRawClass()));
  }

  @NotNull
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name) {
    final Method method = getAnnotatedMethod(mTarget.getTargetClass(), name);
    if (method == null) {
      return method(name, Reflection.NO_PARAMS);
    }

    return method(method);
  }

  @NotNull
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
      @NotNull final Class<?>... parameterTypes) {
    return method(Reflection.findMethod(mTarget.getTargetClass(), name, parameterTypes));
  }

  @NotNull
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {
    return getRoutine(withAnnotations(mInvocationConfiguration, method),
        withAnnotations(mReflectionConfiguration, method), method, null, null);
  }

  @NotNull
  @SuppressWarnings("unchecked")
  private <IN, OUT> Routine<IN, OUT> getRoutine(
      @NotNull final InvocationConfiguration invocationConfiguration,
      @NotNull final ReflectionConfiguration reflectionConfiguration, @NotNull final Method method,
      @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {
    final InvocationTarget<?> target = mTarget;
    final Object targetInstance = target.getTarget();
    if (targetInstance == null) {
      throw new IllegalStateException("the target object has been destroyed");
    }

    synchronized (sRoutines) {
      final WeakIdentityHashMap<Object, HashMap<RoutineInfo, Routine<?, ?>>> routines = sRoutines;
      HashMap<RoutineInfo, Routine<?, ?>> routineMap = routines.get(targetInstance);
      if (routineMap == null) {
        routineMap = new HashMap<RoutineInfo, Routine<?, ?>>();
        routines.put(targetInstance, routineMap);
      }

      final RoutineInfo routineInfo =
          new RoutineInfo(invocationConfiguration, reflectionConfiguration, method, inputMode,
              outputMode);
      Routine<?, ?> routine = routineMap.get(routineInfo);
      if (routine == null) {
        final MethodInvocationFactory factory =
            new MethodInvocationFactory(reflectionConfiguration, target, method, inputMode,
                outputMode);
        routine = JRoutineCore.with(factory).apply(invocationConfiguration).buildRoutine();
        routineMap.put(routineInfo, routine);
      }

      return (Routine<IN, OUT>) routine;
    }
  }

  /**
   * Implementation of a simple invocation wrapping the target method.
   */
  private static class MethodCallInvocation extends CallInvocation<Object, Object> {

    private final InputMode mInputMode;

    private final Method mMethod;

    private final Mutex mMutex;

    private final OutputMode mOutputMode;

    private final InvocationTarget<?> mTarget;

    /**
     * Constructor.
     *
     * @param reflectionConfiguration the reflection configuration.
     * @param target                  the invocation target.
     * @param method                  the method to wrap.
     * @param inputMode               the input transfer mode.
     * @param outputMode              the output transfer mode.
     */
    private MethodCallInvocation(@NotNull final ReflectionConfiguration reflectionConfiguration,
        @NotNull final InvocationTarget<?> target, @NotNull final Method method,
        @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {
      final Object mutexTarget =
          (Modifier.isStatic(method.getModifiers())) ? target.getTargetClass() : target.getTarget();
      mMutex = getSharedMutex(mutexTarget, reflectionConfiguration.getSharedFieldsOrElse(null));
      mTarget = target;
      mMethod = method;
      mInputMode = inputMode;
      mOutputMode = outputMode;
    }

    @Override
    protected void onCall(@NotNull final List<?> objects,
        @NotNull final Channel<Object, ?> result) throws Exception {
      final Object target = mTarget.getTarget();
      if (target == null) {
        throw new IllegalStateException("the target object has been destroyed");
      }

      callFromInvocation(mMutex, target, mMethod, objects, result, mInputMode, mOutputMode);
    }
  }

  /**
   * Factory creating method invocations.
   */
  private static class MethodInvocationFactory extends InvocationFactory<Object, Object> {

    private final InputMode mInputMode;

    private final Method mMethod;

    private final OutputMode mOutputMode;

    private final ReflectionConfiguration mReflectionConfiguration;

    private final InvocationTarget<?> mTarget;

    /**
     * Constructor.
     *
     * @param reflectionConfiguration the reflection configuration.
     * @param target                  the invocation target.
     * @param method                  the method to wrap.
     * @param inputMode               the input transfer mode.
     * @param outputMode              the output transfer mode.
     */
    private MethodInvocationFactory(@NotNull final ReflectionConfiguration reflectionConfiguration,
        @NotNull final InvocationTarget<?> target, @NotNull final Method method,
        @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {
      super(asArgs(reflectionConfiguration, target, method, inputMode, outputMode));
      mReflectionConfiguration = reflectionConfiguration;
      mTarget = target;
      mMethod = method;
      mInputMode = inputMode;
      mOutputMode = outputMode;
    }

    @NotNull
    @Override
    public Invocation<Object, Object> newInvocation() {
      return new MethodCallInvocation(mReflectionConfiguration, mTarget, mMethod, mInputMode,
          mOutputMode);
    }
  }

  /**
   * Class used as key to identify a specific routine instance.
   */
  private static final class RoutineInfo extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param invocationConfiguration the invocation configuration.
     * @param reflectionConfiguration the reflection configuration.
     * @param method                  the method to wrap.
     * @param inputMode               the input transfer mode.
     * @param outputMode              the output transfer mode.
     */
    private RoutineInfo(@NotNull final InvocationConfiguration invocationConfiguration,
        @NotNull final ReflectionConfiguration reflectionConfiguration,
        @NotNull final Method method, @Nullable final InputMode inputMode,
        @Nullable final OutputMode outputMode) {
      super(
          asArgs(invocationConfiguration, reflectionConfiguration, method, inputMode, outputMode));
    }
  }

  /**
   * Invocation handler adapting a different interface to the target object instance.
   */
  private class ProxyInvocationHandler implements InvocationHandler {

    private final InvocationConfiguration mInvocationConfiguration;

    private final ReflectionConfiguration mReflectionConfiguration;

    /**
     * Constructor.
     */
    private ProxyInvocationHandler() {
      mInvocationConfiguration = DefaultReflectionRoutineBuilder.this.mInvocationConfiguration;
      mReflectionConfiguration = DefaultReflectionRoutineBuilder.this.mReflectionConfiguration;
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws
        Throwable {
      final MethodInfo methodInfo = getTargetMethodInfo(mTarget.getTargetClass(), method);
      final InputMode inputMode = methodInfo.inputMode;
      final OutputMode outputMode = methodInfo.outputMode;
      final Routine<Object, Object> routine =
          getRoutine(withAnnotations(mInvocationConfiguration, method),
              withAnnotations(mReflectionConfiguration, method), methodInfo.method, inputMode,
              outputMode);
      return invokeRoutine(routine, method, asArgs(args), methodInfo.invocationMode, inputMode,
          outputMode);
    }
  }
}
