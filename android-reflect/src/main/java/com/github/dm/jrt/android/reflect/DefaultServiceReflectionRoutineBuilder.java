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

package com.github.dm.jrt.android.reflect;

import android.content.Context;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.config.ServiceConfiguration;
import com.github.dm.jrt.android.core.invocation.CallContextInvocation;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.reflect.builder.AndroidReflectionRoutineBuilders;
import com.github.dm.jrt.android.reflect.builder.ServiceReflectionRoutineBuilder;
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
import com.github.dm.jrt.reflect.annotation.SharedFields;
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders;
import com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.MethodInfo;
import com.github.dm.jrt.reflect.common.Mutex;
import com.github.dm.jrt.reflect.config.WrapperConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.findMethod;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.callFromInvocation;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.getAnnotatedMethod;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.getSharedMutex;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.getTargetMethodInfo;
import static com.github.dm.jrt.reflect.builder.ReflectionRoutineBuilders.invokeRoutine;

/**
 * Class implementing a builder of routines wrapping an object methods.
 * <p>
 * Created by davide-maestroni on 03/29/2015.
 */
class DefaultServiceReflectionRoutineBuilder implements ServiceReflectionRoutineBuilder {

  private static final HashMap<String, Class<?>> sPrimitiveClassMap =
      new HashMap<String, Class<?>>(9) {{
        put(boolean.class.getName(), boolean.class);
        put(byte.class.getName(), byte.class);
        put(char.class.getName(), char.class);
        put(int.class.getName(), int.class);
        put(long.class.getName(), long.class);
        put(float.class.getName(), float.class);
        put(double.class.getName(), double.class);
        put(short.class.getName(), short.class);
        put(void.class.getName(), void.class);
      }};

  private final ServiceContext mContext;

  private final ContextInvocationTarget<?> mTarget;

  private InvocationConfiguration mInvocationConfiguration =
      InvocationConfiguration.defaultConfiguration();

  private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.defaultConfiguration();

  private WrapperConfiguration mWrapperConfiguration = WrapperConfiguration.defaultConfiguration();

  /**
   * Constructor.
   *
   * @param context the Service context.
   * @param target  the invocation target.
   */
  DefaultServiceReflectionRoutineBuilder(@NotNull final ServiceContext context,
      @NotNull final ContextInvocationTarget<?> target) {
    mContext = ConstantConditions.notNull("Service context", context);
    mTarget = ConstantConditions.notNull("invocation target", target);
  }

  @Nullable
  private static Set<String> fieldsWithShareAnnotation(
      @NotNull final WrapperConfiguration configuration, @NotNull final Method method) {
    final SharedFields sharedFieldsAnnotation = method.getAnnotation(SharedFields.class);
    if (sharedFieldsAnnotation != null) {
      final HashSet<String> set = new HashSet<String>();
      Collections.addAll(set, sharedFieldsAnnotation.value());
      return set;
    }

    return configuration.getSharedFieldsOrElse(null);
  }

  @NotNull
  private static Class<?>[] forNames(@NotNull final String[] names) throws ClassNotFoundException {
    // The forName() of primitive classes is broken...
    final int length = names.length;
    final Class<?>[] classes = new Class[length];
    final HashMap<String, Class<?>> classMap = sPrimitiveClassMap;
    for (int i = 0; i < length; ++i) {
      final String name = names[i];
      final Class<?> primitiveClass = classMap.get(name);
      if (primitiveClass != null) {
        classes[i] = primitiveClass;

      } else {
        classes[i] = Class.forName(name);
      }
    }

    return classes;
  }

  @NotNull
  private static String[] toNames(@NotNull final Class<?>[] classes) {
    final int length = classes.length;
    final String[] names = new String[length];
    for (int i = 0; i < length; ++i) {
      names[i] = classes[i].getName();
    }

    return names;
  }

  @NotNull
  @Override
  public ServiceReflectionRoutineBuilder apply(@NotNull final ServiceConfiguration configuration) {
    mServiceConfiguration = ConstantConditions.notNull("Service configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceReflectionRoutineBuilder withConfiguration(
      @NotNull final InvocationConfiguration configuration) {
    mInvocationConfiguration =
        ConstantConditions.notNull("invocation configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public ServiceReflectionRoutineBuilder apply(@NotNull final WrapperConfiguration configuration) {
    mWrapperConfiguration = ConstantConditions.notNull("wrapper configuration", configuration);
    return this;
  }

  @NotNull
  @Override
  public InvocationConfiguration.Builder<? extends ServiceReflectionRoutineBuilder> withInvocation() {
    final InvocationConfiguration config = mInvocationConfiguration;
    return new InvocationConfiguration.Builder<ServiceReflectionRoutineBuilder>(
        new InvocationConfiguration.Configurable<ServiceReflectionRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceReflectionRoutineBuilder withConfiguration(
              @NotNull final InvocationConfiguration configuration) {
            return DefaultServiceReflectionRoutineBuilder.this.withConfiguration(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public WrapperConfiguration.Builder<? extends ServiceReflectionRoutineBuilder>
  wrapperConfiguration() {
    final WrapperConfiguration config = mWrapperConfiguration;
    return new WrapperConfiguration.Builder<ServiceReflectionRoutineBuilder>(
        new WrapperConfiguration.Configurable<ServiceReflectionRoutineBuilder>() {

          @NotNull
          @Override
          public ServiceReflectionRoutineBuilder apply(
              @NotNull final WrapperConfiguration configuration) {
            return DefaultServiceReflectionRoutineBuilder.this.apply(configuration);
          }
        }, config);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final Class<TYPE> itf) {
    if (!itf.isInterface()) {
      throw new IllegalArgumentException(
          "the specified class is not an interface: " + itf.getName());
    }

    final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
        new ProxyInvocationHandler(this));
    return itf.cast(proxy);
  }

  @NotNull
  @Override
  public <TYPE> TYPE buildProxy(@NotNull final ClassToken<TYPE> itf) {
    return buildProxy(itf.getRawClass());
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name) {
    final ContextInvocationTarget<?> target = mTarget;
    final Method targetMethod = getAnnotatedMethod(target.getTargetClass(), name);
    if (targetMethod == null) {
      return method(name, Reflection.NO_PARAMS);
    }

    final Set<String> sharedFields = fieldsWithShareAnnotation(mWrapperConfiguration, targetMethod);
    final Object[] args = asArgs(sharedFields, target, name);
    final TargetInvocationFactory<Object, Object> factory =
        factoryOf(MethodAliasInvocation.class, args);
    final InvocationConfiguration invocationConfiguration =
        ReflectionRoutineBuilders.withAnnotations(mInvocationConfiguration, targetMethod);
    final ServiceConfiguration serviceConfiguration =
        AndroidReflectionRoutineBuilders.withAnnotations(mServiceConfiguration, targetMethod);
    return (Routine<IN, OUT>) JRoutineService.on(mContext)
                                             .with(factory)
                                             .withConfiguration(invocationConfiguration)
                                             .apply(serviceConfiguration)
                                             .buildRoutine();
  }

  @NotNull
  @Override
  @SuppressWarnings("unchecked")
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final String name,
      @NotNull final Class<?>... parameterTypes) {
    final ContextInvocationTarget<?> target = mTarget;
    final Method targetMethod = findMethod(target.getTargetClass(), name, parameterTypes);
    final Set<String> sharedFields = fieldsWithShareAnnotation(mWrapperConfiguration, targetMethod);
    final Object[] args = asArgs(sharedFields, target, name, toNames(parameterTypes));
    final TargetInvocationFactory<Object, Object> factory =
        factoryOf(MethodSignatureInvocation.class, args);
    final InvocationConfiguration invocationConfiguration =
        ReflectionRoutineBuilders.withAnnotations(mInvocationConfiguration, targetMethod);
    final ServiceConfiguration serviceConfiguration =
        AndroidReflectionRoutineBuilders.withAnnotations(mServiceConfiguration, targetMethod);
    return (Routine<IN, OUT>) JRoutineService.on(mContext)
                                             .with(factory)
                                             .withConfiguration(invocationConfiguration)
                                             .apply(serviceConfiguration)
                                             .buildRoutine();
  }

  @NotNull
  @Override
  public <IN, OUT> Routine<IN, OUT> method(@NotNull final Method method) {
    return method(method.getName(), method.getParameterTypes());
  }

  @NotNull
  @Override
  public ServiceConfiguration.Builder<? extends ServiceReflectionRoutineBuilder>
  serviceConfiguration() {
    final ServiceConfiguration config = mServiceConfiguration;
    return new ServiceConfiguration.Builder<ServiceReflectionRoutineBuilder>(this, config);
  }

  /**
   * Alias method invocation.
   */
  private static class MethodAliasInvocation implements ContextInvocation<Object, Object> {

    private final String mAliasName;

    private final Set<String> mSharedFields;

    private final ContextInvocationTarget<?> mTarget;

    private Channel<Object, Object> mChannel;

    @SuppressWarnings("unused")
    private Object mInstance;

    private Routine<Object, Object> mRoutine;

    /**
     * Constructor.
     *
     * @param sharedFields the set of shared field names.
     * @param target       the invocation target.
     * @param name         the alias name.
     */
    private MethodAliasInvocation(@Nullable final Set<String> sharedFields,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final String name) {
      mSharedFields = sharedFields;
      mTarget = target;
      mAliasName = name;
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mChannel.abort(reason);
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      final InvocationTarget target = mTarget.getInvocationTarget(context);
      mInstance = target.getTarget();
      mRoutine = JRoutineReflection.with(target)
                                   .withInvocation()
                                   .withExecutor(ScheduledExecutors.syncExecutor())
                                   .configured()
                                   .wrapperConfiguration()
                                   .withSharedFields(mSharedFields)
                                   .apply()
                                   .method(mAliasName);
    }

    @Override
    public void onComplete(@NotNull final Channel<Object, ?> result) {
      result.pass(mChannel.close());
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
    public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
      mChannel.pass(input);
    }

    @Override
    public void onStart() {
      mChannel = mRoutine.invoke();
    }
  }

  /**
   * Invocation based on method signature.
   */
  private static class MethodSignatureInvocation implements ContextInvocation<Object, Object> {

    private final String mMethodName;

    private final Class<?>[] mParameterTypes;

    private final Set<String> mSharedFields;

    private final ContextInvocationTarget<?> mTarget;

    private Channel<Object, Object> mChannel;

    @SuppressWarnings("unused")
    private Object mInstance;

    private Routine<Object, Object> mRoutine;

    /**
     * Constructor.
     *
     * @param sharedFields   the set of shared field names.
     * @param target         the invocation target.
     * @param name           the method name.
     * @param parameterTypes the method parameter type names.
     * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
     */
    private MethodSignatureInvocation(@Nullable final Set<String> sharedFields,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final String name,
        @NotNull final String[] parameterTypes) throws ClassNotFoundException {
      mSharedFields = sharedFields;
      mTarget = target;
      mMethodName = name;
      mParameterTypes = forNames(parameterTypes);
    }

    @Override
    public void onAbort(@NotNull final RoutineException reason) {
      mChannel.abort(reason);
    }

    @Override
    public void onComplete(@NotNull final Channel<Object, ?> result) {
      result.pass(mChannel.close());
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
    public void onInput(final Object input, @NotNull final Channel<Object, ?> result) {
      mChannel.pass(input);
    }

    @Override
    public void onStart() {
      mChannel = mRoutine.invoke();
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      final InvocationTarget target = mTarget.getInvocationTarget(context);
      mInstance = target.getTarget();
      mRoutine = JRoutineReflection.with(target)
                                   .withInvocation()
                                   .withExecutor(ScheduledExecutors.syncExecutor())
                                   .configured()
                                   .wrapperConfiguration()
                                   .withSharedFields(mSharedFields)
                                   .apply()
                                   .method(mMethodName, mParameterTypes);
    }
  }

  /**
   * Proxy method invocation.
   */
  private static class ProxyInvocation extends CallContextInvocation<Object, Object> {

    private final InputMode mInputMode;

    private final OutputMode mOutputMode;

    private final Set<String> mSharedFields;

    private final ContextInvocationTarget<?> mTarget;

    private final Method mTargetMethod;

    private Object mInstance;

    private Mutex mMutex = Mutex.NONE;

    /**
     * Constructor.
     *
     * @param sharedFields         the set of shared field names.
     * @param target               the invocation target.
     * @param targetMethodName     the target method name.
     * @param targetParameterTypes the target method parameter type names.
     * @param inputMode            the input transfer mode.
     * @param outputMode           the output transfer mode.
     * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
     * @throws java.lang.NoSuchMethodException  if the target method is not found.
     */
    private ProxyInvocation(@Nullable final Set<String> sharedFields,
        @NotNull final ContextInvocationTarget<?> target, @NotNull final String targetMethodName,
        @NotNull final String[] targetParameterTypes, @Nullable final InputMode inputMode,
        @Nullable final OutputMode outputMode) throws ClassNotFoundException,
        NoSuchMethodException {
      mSharedFields = sharedFields;
      mTarget = target;
      mTargetMethod =
          target.getTargetClass().getMethod(targetMethodName, forNames(targetParameterTypes));
      mInputMode = inputMode;
      mOutputMode = outputMode;
    }

    @Override
    public void onContext(@NotNull final Context context) throws Exception {
      super.onContext(context);
      final InvocationTarget target = mTarget.getInvocationTarget(context);
      final Object mutexTarget =
          (Modifier.isStatic(mTargetMethod.getModifiers())) ? target.getTargetClass()
              : target.getTarget();
      mMutex = getSharedMutex(mutexTarget, mSharedFields);
      mInstance = target.getTarget();
    }

    @Override
    protected void onCall(@NotNull final List<?> objects,
        @NotNull final Channel<Object, ?> result) throws Exception {
      callFromInvocation(mMutex, mInstance, mTargetMethod, objects, result, mInputMode,
          mOutputMode);
    }
  }

  /**
   * Invocation handler adapting a different interface to the target object instance.
   */
  private static class ProxyInvocationHandler implements InvocationHandler {

    private final ServiceContext mContext;

    private final InvocationConfiguration mInvocationConfiguration;

    private final ServiceConfiguration mServiceConfiguration;

    private final ContextInvocationTarget<?> mTarget;

    private final WrapperConfiguration mWrapperConfiguration;

    /**
     * Constructor.
     *
     * @param builder the builder instance.
     */
    private ProxyInvocationHandler(@NotNull final DefaultServiceReflectionRoutineBuilder builder) {
      mContext = builder.mContext;
      mTarget = builder.mTarget;
      mInvocationConfiguration = builder.mInvocationConfiguration;
      mWrapperConfiguration = builder.mWrapperConfiguration;
      mServiceConfiguration = builder.mServiceConfiguration;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws
        Throwable {
      final ContextInvocationTarget<?> target = mTarget;
      final MethodInfo methodInfo = getTargetMethodInfo(target.getTargetClass(), method);
      final Method targetMethod = methodInfo.method;
      final InputMode inputMode = methodInfo.inputMode;
      final OutputMode outputMode = methodInfo.outputMode;
      final Class<?>[] targetParameterTypes = targetMethod.getParameterTypes();
      final Set<String> sharedFields = fieldsWithShareAnnotation(mWrapperConfiguration, method);
      final Object[] factoryArgs =
          asArgs(sharedFields, target, targetMethod.getName(), toNames(targetParameterTypes),
              inputMode, outputMode);
      final TargetInvocationFactory<Object, Object> factory =
          factoryOf(ProxyInvocation.class, factoryArgs);
      final InvocationConfiguration invocationConfiguration =
          ReflectionRoutineBuilders.withAnnotations(mInvocationConfiguration, method);
      final ServiceConfiguration serviceConfiguration =
          AndroidReflectionRoutineBuilders.withAnnotations(mServiceConfiguration, method);
      final Routine<Object, Object> routine = JRoutineService.on(mContext)
                                                             .with(factory)
                                                             .withConfiguration(invocationConfiguration)
                                                             .apply(serviceConfiguration)
                                                             .buildRoutine();
      return invokeRoutine(routine, method, asArgs(args), inputMode, outputMode);
    }
  }
}
