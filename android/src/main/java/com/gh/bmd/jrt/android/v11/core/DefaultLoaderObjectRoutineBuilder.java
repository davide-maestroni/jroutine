/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh.bmd.jrt.android.v11.core;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import com.gh.bmd.jrt.android.annotation.CacheStrategy;
import com.gh.bmd.jrt.android.annotation.ClashResolution;
import com.gh.bmd.jrt.android.annotation.InputClashResolution;
import com.gh.bmd.jrt.android.annotation.LoaderId;
import com.gh.bmd.jrt.android.builder.FactoryContext;
import com.gh.bmd.jrt.android.builder.LoaderConfiguration;
import com.gh.bmd.jrt.android.builder.LoaderObjectRoutineBuilder;
import com.gh.bmd.jrt.android.builder.LoaderRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.ProcedureContextInvocation;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineBuilders.MethodInfo;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.Reflection;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineBuilders.callFromInvocation;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getAnnotatedMethod;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getTargetMethodInfo;
import static com.gh.bmd.jrt.builder.RoutineBuilders.invokeRoutine;
import static com.gh.bmd.jrt.common.Reflection.findConstructor;
import static com.gh.bmd.jrt.common.Reflection.findMethod;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * Created by davide-maestroni on 4/6/2015.
 */
class DefaultLoaderObjectRoutineBuilder implements LoaderObjectRoutineBuilder,
        LoaderConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<LoaderObjectRoutineBuilder>,
        InvocationConfiguration.Configurable<LoaderObjectRoutineBuilder> {

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private static final ProxyInvocationFactory sProxyFactory = new ProxyInvocationFactory();

    private final WeakReference<Object> mContext;

    private final Class<?> mTargetClass;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private LoaderConfiguration mLoaderConfiguration = LoaderConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param activity    the context activity.
     * @param targetClass the invocation class token.
     */
    DefaultLoaderObjectRoutineBuilder(@Nonnull final Activity activity,
            @Nonnull final Class<?> targetClass) {

        this((Object) activity, targetClass);
    }

    /**
     * Constructor.
     *
     * @param fragment    the context fragment.
     * @param targetClass the invocation class token.
     */
    DefaultLoaderObjectRoutineBuilder(@Nonnull final Fragment fragment,
            @Nonnull final Class<?> targetClass) {

        this((Object) fragment, targetClass);
    }

    /**
     * Constructor.
     *
     * @param context     the routine context.
     * @param targetClass the target object class.
     */
    @SuppressWarnings("ConstantConditions")
    private DefaultLoaderObjectRoutineBuilder(@Nonnull final Object context,
            @Nonnull final Class<?> targetClass) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        if (targetClass == null) {

            throw new NullPointerException("the target class must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mTargetClass = targetClass;
    }

    @Nonnull
    private static LoaderConfiguration configurationWithAnnotations(
            @Nonnull final LoaderConfiguration configuration, @Nonnull final Method method) {

        final LoaderConfiguration.Builder<LoaderConfiguration> builder =
                configuration.builderFrom();

        final LoaderId idAnnotation = method.getAnnotation(LoaderId.class);

        if (idAnnotation != null) {

            builder.withId(idAnnotation.value());
        }

        final ClashResolution clashAnnotation = method.getAnnotation(ClashResolution.class);

        if (clashAnnotation != null) {

            builder.withClashResolution(clashAnnotation.value());
        }

        final InputClashResolution inputClashAnnotation =
                method.getAnnotation(InputClashResolution.class);

        if (inputClashAnnotation != null) {

            builder.withInputClashResolution(inputClashAnnotation.value());
        }

        final CacheStrategy cacheAnnotation = method.getAnnotation(CacheStrategy.class);

        if (cacheAnnotation != null) {

            builder.withCacheStrategy(cacheAnnotation.value());
        }

        return builder.set();
    }

    @Nonnull
    private static InvocationConfiguration configurationWithTimeout(
            @Nonnull final InvocationConfiguration configuration, @Nonnull final Method method) {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                configuration.builderFrom();
        final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
        }

        final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

        if (actionAnnotation != null) {

            builder.withReadTimeoutAction(actionAnnotation.value());
        }

        return builder.set();
    }

    @Nonnull
    private static <INPUT, OUTPUT> LoaderRoutineBuilder<INPUT, OUTPUT> getBuilder(
            @Nonnull WeakReference<Object> contextReference,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory) {

        final Object context = contextReference.get();

        if (context == null) {

            throw new IllegalStateException("the routine context has been destroyed");
        }

        if (context instanceof Activity) {

            return JRoutine.onActivity((Activity) context, factory);

        } else if (context instanceof Fragment) {

            return JRoutine.onFragment((Fragment) context, factory);
        }

        throw new IllegalArgumentException("invalid context type: " + context.getClass().getName());
    }

    @Nonnull
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static Object getInstance(@Nonnull final Context context,
            @Nonnull final Class<?> targetClass, @Nonnull final Object[] args) throws
            IllegalAccessException, InvocationTargetException, InstantiationException {

        Object target = null;

        if (context instanceof FactoryContext) {

            // the context here is always the application
            synchronized (context) {

                target = ((FactoryContext) context).geInstance(targetClass, args);
            }
        }

        if (target == null) {

            target = findConstructor(targetClass, args).newInstance(args);

        } else if (!targetClass.isInstance(target)) {

            throw new InstantiationException();
        }

        return target;
    }

    @Nullable
    private static String groupWithShareAnnotation(
            @Nonnull final ProxyConfiguration proxyConfiguration, @Nonnull final Method method) {

        final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            return shareGroupAnnotation.value();
        }

        return proxyConfiguration.getShareGroupOr(null);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull final String name) {

        final Class<?> targetClass = mTargetClass;
        final Method targetMethod = getAnnotatedMethod(targetClass, name);

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with name '" + name + "' has been found");
        }

        final InvocationConfiguration configuration = mInvocationConfiguration;
        warn(configuration);
        final Object[] args = configuration.getFactoryArgsOr(Reflection.NO_ARGS);
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] invocationArgs = new Object[]{targetClass, args, shareGroup, name};
        final AliasMethodInvocationFactory<INPUT, OUTPUT> factory =
                new AliasMethodInvocationFactory<INPUT, OUTPUT>(targetMethod);
        final InvocationConfiguration invocationConfiguration =
                configurationWithTimeout(configuration, targetMethod);
        final LoaderConfiguration loaderConfiguration =
                configurationWithAnnotations(mLoaderConfiguration, targetMethod);
        return getBuilder(mContext, factory).withInvocation()
                                            .with(invocationConfiguration)
                                            .withFactoryArgs(invocationArgs)
                                            .withInputOrder(OrderType.PASS_ORDER)
                                            .set()
                                            .withLoader()
                                            .with(loaderConfiguration)
                                            .set()
                                            .buildRoutine();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        final InvocationConfiguration configuration = mInvocationConfiguration;
        warn(configuration);
        final Object[] args = configuration.getFactoryArgsOr(Reflection.NO_ARGS);
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, method);
        final Object[] invocationArgs = new Object[]{mTargetClass, args, shareGroup, method};
        final MethodInvocationFactory<INPUT, OUTPUT> factory =
                new MethodInvocationFactory<INPUT, OUTPUT>(method);
        final InvocationConfiguration invocationConfiguration =
                configurationWithTimeout(configuration, method);
        final LoaderConfiguration loaderConfiguration =
                configurationWithAnnotations(mLoaderConfiguration, method);
        return getBuilder(mContext, factory).withInvocation()
                                            .with(invocationConfiguration)
                                            .withFactoryArgs(invocationArgs)
                                            .withInputOrder(OrderType.PASS_ORDER)
                                            .set()
                                            .withLoader()
                                            .with(loaderConfiguration)
                                            .set()
                                            .buildRoutine();
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        return method(findMethod(mTargetClass, name, parameterTypes));
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final InvocationConfiguration configuration = mInvocationConfiguration;
        warn(configuration);
        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler(this));
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends LoaderObjectRoutineBuilder> withInvocation() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends LoaderObjectRoutineBuilder> withProxy() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @Nonnull final LoaderConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mLoaderConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public LoaderObjectRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    public LoaderConfiguration.Builder<? extends LoaderObjectRoutineBuilder> withLoader() {

        final LoaderConfiguration config = mLoaderConfiguration;
        return new LoaderConfiguration.Builder<LoaderObjectRoutineBuilder>(this, config);
    }

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the invocation configuration.
     */
    private void warn(@Nonnull final InvocationConfiguration configuration) {

        Logger logger = null;
        final OrderType inputOrderType = configuration.getInputOrderTypeOr(null);

        if (inputOrderType != null) {

            logger = configuration.newLogger(this);
            logger.wrn("the specified input order type will be ignored: %s", inputOrderType);
        }

        final OrderType outputOrderType = configuration.getOutputOrderTypeOr(null);

        if (outputOrderType != null) {

            if (logger == null) {

                logger = configuration.newLogger(this);
            }

            logger.wrn("the specified output order type will be ignored: %s", outputOrderType);
        }
    }

    /**
     * Alias method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AliasMethodInvocation<INPUT, OUTPUT>
            extends ProcedureContextInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mBindingName;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine = null;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param targetClass the target object class.
         * @param args        the factory constructor arguments.
         * @param shareGroup  the share group name.
         * @param name        the binding name.
         */
        @SuppressWarnings("unchecked")
        public AliasMethodInvocation(@Nonnull final Class<?> targetClass,
                @Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final String name) {

            mTargetClass = targetClass;
            mArgs = args;
            mShareGroup = shareGroup;
            mBindingName = name;
        }

        @Override
        public void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            if (mTarget == null) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(mRoutine.callSync(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final Object target = getInstance(context, mTargetClass, mArgs);
                mRoutine = JRoutine.on(target)
                                   .withProxy()
                                   .withShareGroup(mShareGroup)
                                   .set()
                                   .aliasMethod(mBindingName);
                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Factory of {@link AliasMethodInvocation}s.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AliasMethodInvocationFactory<INPUT, OUTPUT>
            implements ContextInvocationFactory<INPUT, OUTPUT> {

        private final String mType;

        /**
         * Constructor.
         *
         * @param targetMethod the target method.
         */
        private AliasMethodInvocationFactory(@Nonnull final Method targetMethod) {

            mType = targetMethod.toString();
        }

        @Nonnull
        public String getInvocationType() {

            return mType;
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

            return new AliasMethodInvocation<INPUT, OUTPUT>((Class<?>) args[0], (Object[]) args[1],
                                                            (String) args[2], (String) args[3]);
        }
    }

    /**
     * Generic method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodInvocation<INPUT, OUTPUT>
            extends ProcedureContextInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final Method mMethod;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine = null;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param targetClass the target object class.
         * @param args        the factory constructor arguments.
         * @param shareGroup  the share group name.
         * @param method      the method.
         */
        @SuppressWarnings("unchecked")
        public MethodInvocation(@Nonnull final Class<?> targetClass, @Nonnull final Object[] args,
                @Nullable final String shareGroup, @Nonnull final Method method) {

            mTargetClass = targetClass;
            mArgs = args;
            mShareGroup = shareGroup;
            mMethod = method;
        }

        @Override
        public void onCall(@Nonnull final List<? extends INPUT> inputs,
                @Nonnull final ResultChannel<OUTPUT> result) {

            if (mTarget == null) {

                throw new IllegalStateException("such error should never happen");
            }

            result.pass(mRoutine.callSync(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final Object target = getInstance(context, mTargetClass, mArgs);
                mRoutine = JRoutine.on(target)
                                   .withProxy()
                                   .withShareGroup(mShareGroup)
                                   .set()
                                   .method(mMethod);
                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Factory of {@link MethodInvocation}s.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodInvocationFactory<INPUT, OUTPUT>
            implements ContextInvocationFactory<INPUT, OUTPUT> {

        private final String mType;

        /**
         * Constructor.
         *
         * @param targetMethod the target method.
         */
        private MethodInvocationFactory(@Nonnull final Method targetMethod) {

            mType = targetMethod.toString();
        }

        @Nonnull
        public String getInvocationType() {

            return mType;
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

            return new MethodInvocation<INPUT, OUTPUT>((Class<?>) args[0], (Object[]) args[1],
                                                       (String) args[2], (Method) args[3]);
        }
    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends ProcedureContextInvocation<Object, Object> {

        private final Object[] mArgs;

        private final InputMode mInputMode;

        private final OutputMode mOutputMode;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private final Method mTargetMethod;

        private Object mMutex;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param args         the factory constructor arguments.
         * @param shareGroup   the share group name.
         * @param targetClass  the target object class.
         * @param targetMethod the target method.
         * @param inputMode    the input transfer mode.
         * @param outputMode   the output transfer mode.
         */
        public ProxyInvocation(@Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final Class<?> targetClass, @Nonnull final Method targetMethod,
                @Nullable final InputMode inputMode, @Nullable final OutputMode outputMode) {

            mArgs = args;
            mShareGroup = shareGroup;
            mTargetClass = targetClass;
            mTargetMethod = targetMethod;
            mInputMode = inputMode;
            mOutputMode = outputMode;
            mMutex = this;
        }

        @Override
        @SuppressWarnings("SynchronizeOnNonFinalField")
        public void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            callFromInvocation(mTarget, mMutex, objects, result, mTargetMethod, mInputMode,
                               mOutputMode);
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                final Object target = getInstance(context, mTargetClass, mArgs);
                final String shareGroup = mShareGroup;

                if (!ShareGroup.NONE.equals(shareGroup)) {

                    mMutex = getSharedMutex(target, shareGroup);
                }

                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Factory of {@link ProxyInvocation}s.
     */
    private static class ProxyInvocationFactory
            implements ContextInvocationFactory<Object, Object> {

        @Nonnull
        public String getInvocationType() {

            return ProxyInvocation.class.getName();
        }

        @Nonnull
        public ContextInvocation<Object, Object> newInvocation(@Nonnull final Object... args) {

            return new ProxyInvocation((Object[]) args[0], (String) args[1], (Class<?>) args[2],
                                       (Method) args[3], (InputMode) args[4], (OutputMode) args[5]);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final Object[] mArgs;

        private final WeakReference<Object> mContext;

        private final InvocationConfiguration mInvocationConfiguration;

        private final LoaderConfiguration mLoaderConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param builder the builder instance.
         */
        private ProxyInvocationHandler(@Nonnull final DefaultLoaderObjectRoutineBuilder builder) {

            mContext = builder.mContext;
            mTargetClass = builder.mTargetClass;
            mInvocationConfiguration = builder.mInvocationConfiguration;
            mProxyConfiguration = builder.mProxyConfiguration;
            mLoaderConfiguration = builder.mLoaderConfiguration;
            mArgs = mInvocationConfiguration.getFactoryArgsOr(Reflection.NO_ARGS);
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final MethodInfo methodInfo = getTargetMethodInfo(mTargetClass, method);
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, method);
            final Object[] invocationArgs =
                    new Object[]{mArgs, shareGroup, mTargetClass, targetMethod, inputMode,
                                 outputMode};
            final OrderType inputOrderType =
                    (inputMode == InputMode.ELEMENT) ? OrderType.NONE : OrderType.PASS_ORDER;
            final OrderType outputOrderType =
                    (outputMode == OutputMode.ELEMENT) ? OrderType.PASS_ORDER : OrderType.NONE;
            final LoaderRoutineBuilder<Object, Object> routineBuilder =
                    getBuilder(mContext, sProxyFactory);
            final InvocationConfiguration invocationConfiguration =
                    configurationWithTimeout(mInvocationConfiguration, method);
            final LoaderConfiguration loaderConfiguration =
                    configurationWithAnnotations(mLoaderConfiguration, method);
            final Routine<Object, Object> routine = routineBuilder.withInvocation()
                                                                  .with(invocationConfiguration)
                                                                  .withFactoryArgs(invocationArgs)
                                                                  .withInputOrder(inputOrderType)
                                                                  .withOutputOrder(outputOrderType)
                                                                  .set()
                                                                  .withLoader()
                                                                  .with(loaderConfiguration)
                                                                  .set()
                                                                  .buildRoutine();
            return invokeRoutine(routine, method, (args == null) ? Reflection.NO_ARGS : args,
                                 inputMode, outputMode);
        }
    }

    static {

        final HashMap<String, Class<?>> classMap = sPrimitiveClassMap;
        classMap.put(byte.class.getName(), byte.class);
        classMap.put(char.class.getName(), char.class);
        classMap.put(int.class.getName(), int.class);
        classMap.put(long.class.getName(), long.class);
        classMap.put(float.class.getName(), float.class);
        classMap.put(double.class.getName(), double.class);
        classMap.put(short.class.getName(), short.class);
        classMap.put(void.class.getName(), void.class);
    }
}
