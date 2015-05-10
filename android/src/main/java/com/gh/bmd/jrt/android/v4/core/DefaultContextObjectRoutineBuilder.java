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
package com.gh.bmd.jrt.android.v4.core;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.gh.bmd.jrt.android.annotation.CacheStrategy;
import com.gh.bmd.jrt.android.annotation.ClashResolution;
import com.gh.bmd.jrt.android.annotation.Id;
import com.gh.bmd.jrt.android.builder.ContextObjectRoutineBuilder;
import com.gh.bmd.jrt.android.builder.ContextRoutineBuilder;
import com.gh.bmd.jrt.android.builder.FactoryContext;
import com.gh.bmd.jrt.android.builder.InvocationConfiguration;
import com.gh.bmd.jrt.android.invocation.ContextInvocation;
import com.gh.bmd.jrt.android.invocation.ContextInvocationFactory;
import com.gh.bmd.jrt.android.invocation.SingleCallContextInvocation;
import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.PassMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ParameterChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.Reflection;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.builder.RoutineBuilders.getParamMode;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getReturnMode;
import static com.gh.bmd.jrt.builder.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.common.Reflection.boxingClass;
import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * Created by Davide on 4/6/2015.
 */
class DefaultContextObjectRoutineBuilder implements ContextObjectRoutineBuilder,
        InvocationConfiguration.Configurable<ContextObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<ContextObjectRoutineBuilder>,
        RoutineConfiguration.Configurable<ContextObjectRoutineBuilder> {

    private static final BoundMethodInvocationFactory<Object, Object> sBoundMethodFactory =
            new BoundMethodInvocationFactory<Object, Object>();

    private static final MethodInvocationFactory<Object, Object> sMethodFactory =
            new MethodInvocationFactory<Object, Object>();

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private static final ProxyInvocationFactory sProxyFactory = new ProxyInvocationFactory();

    private final WeakReference<Object> mContext;

    private final Class<?> mTargetClass;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private RoutineConfiguration mRoutineConfiguration = RoutineConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param activity    the context activity.
     * @param targetClass the invocation class token.
     */
    DefaultContextObjectRoutineBuilder(@Nonnull final FragmentActivity activity,
            @Nonnull final Class<?> targetClass) {

        this((Object) activity, targetClass);
    }

    /**
     * Constructor.
     *
     * @param fragment    the context fragment.
     * @param targetClass the invocation class token.
     */
    DefaultContextObjectRoutineBuilder(@Nonnull final Fragment fragment,
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
    private DefaultContextObjectRoutineBuilder(@Nonnull final Object context,
            @Nonnull final Class<?> targetClass) {

        if (context == null) {

            throw new NullPointerException("the routine context must not be null");
        }

        mContext = new WeakReference<Object>(context);
        mTargetClass = targetClass;
        final HashSet<String> bindingSet = new HashSet<String>();

        for (final Method method : targetClass.getMethods()) {

            final Bind annotation = method.getAnnotation(Bind.class);

            if (annotation != null) {

                final String name = annotation.value();

                if (bindingSet.contains(name)) {

                    throw new IllegalArgumentException(
                            "the name '" + name + "' has already been used to identify a different"
                                    + " method");
                }

                bindingSet.add(name);
            }
        }
    }

    @Nonnull
    private static InvocationConfiguration configurationWithAnnotations(
            @Nonnull final InvocationConfiguration configuration, @Nonnull final Method method) {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                configuration.builderFrom();

        final Id idAnnotation = method.getAnnotation(Id.class);

        if (idAnnotation != null) {

            builder.withId(idAnnotation.value());
        }

        final ClashResolution clashAnnotation = method.getAnnotation(ClashResolution.class);

        if (clashAnnotation != null) {

            builder.withClashResolution(clashAnnotation.value());
        }

        final CacheStrategy cacheAnnotation = method.getAnnotation(CacheStrategy.class);

        if (cacheAnnotation != null) {

            builder.withCacheStrategy(cacheAnnotation.value());
        }

        return builder.set();
    }

    @Nonnull
    private static RoutineConfiguration configurationWithTimeout(
            @Nonnull final RoutineConfiguration configuration, @Nonnull final Method method) {

        final RoutineConfiguration.Builder<RoutineConfiguration> builder =
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

    @Nullable
    private static Method getAnnotatedMethod(@Nonnull final Class<?> targetClass,
            @Nonnull final String name) {

        Method targetMethod = null;

        for (final Method method : targetClass.getMethods()) {

            final Bind annotation = method.getAnnotation(Bind.class);

            if ((annotation != null) && name.equals(annotation.value())) {

                targetMethod = method;
                break;
            }
        }

        if (targetMethod == null) {

            for (final Method method : targetClass.getDeclaredMethods()) {

                final Bind annotation = method.getAnnotation(Bind.class);

                if ((annotation != null) && name.equals(annotation.value())) {

                    targetMethod = method;
                    break;
                }
            }
        }

        return targetMethod;
    }

    @Nonnull
    private static <INPUT, OUTPUT> ContextRoutineBuilder<INPUT, OUTPUT> getBuilder(
            @Nonnull WeakReference<Object> contextReference,
            @Nonnull final ContextInvocationFactory<INPUT, OUTPUT> factory) {

        final Object context = contextReference.get();

        if (context == null) {

            throw new IllegalStateException("the routine context has been destroyed");
        }

        if (context instanceof FragmentActivity) {

            return JRoutine.onActivity((FragmentActivity) context, factory);

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
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> boundMethod(@Nonnull final String name) {

        final Class<?> targetClass = mTargetClass;
        final Method targetMethod = getAnnotatedMethod(targetClass, name);

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with name '" + name + "' has been found");
        }

        final RoutineConfiguration configuration = mRoutineConfiguration;
        warn(configuration);
        final Object[] args = configuration.getFactoryArgsOr(Reflection.NO_ARGS);
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        final Object[] invocationArgs = new Object[]{targetClass, args, shareGroup, name};
        final BoundMethodInvocationFactory<INPUT, OUTPUT> factory =
                (BoundMethodInvocationFactory<INPUT, OUTPUT>) sBoundMethodFactory;
        final RoutineConfiguration routineConfiguration =
                configurationWithTimeout(configuration, targetMethod);
        final InvocationConfiguration invocationConfiguration =
                configurationWithAnnotations(mInvocationConfiguration, targetMethod);
        return getBuilder(mContext, factory).withRoutine()
                                            .with(routineConfiguration)
                                            .withFactoryArgs(invocationArgs)
                                            .withInputOrder(OrderType.PASS_ORDER)
                                            .set()
                                            .withInvocation()
                                            .with(invocationConfiguration)
                                            .set()
                                            .buildRoutine();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        final RoutineConfiguration configuration = mRoutineConfiguration;
        warn(configuration);
        final Object[] args = configuration.getFactoryArgsOr(Reflection.NO_ARGS);
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, method);
        final Object[] invocationArgs = new Object[]{mTargetClass, args, shareGroup, method};
        final MethodInvocationFactory<INPUT, OUTPUT> factory =
                (MethodInvocationFactory<INPUT, OUTPUT>) sMethodFactory;
        final RoutineConfiguration routineConfiguration =
                configurationWithTimeout(configuration, method);
        final InvocationConfiguration invocationConfiguration =
                configurationWithAnnotations(mInvocationConfiguration, method);
        return getBuilder(mContext, factory).withRoutine()
                                            .with(routineConfiguration)
                                            .withFactoryArgs(invocationArgs)
                                            .withInputOrder(OrderType.PASS_ORDER)
                                            .set()
                                            .withInvocation()
                                            .with(invocationConfiguration)
                                            .set()
                                            .buildRoutine();
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        Method targetMethod;
        final Class<?> targetClass = mTargetClass;

        try {

            targetMethod = targetClass.getMethod(name, parameterTypes);

        } catch (final NoSuchMethodException ignored) {

            try {

                targetMethod = targetClass.getDeclaredMethod(name, parameterTypes);

            } catch (final NoSuchMethodException e) {

                throw new IllegalArgumentException(e);
            }
        }

        return method(targetMethod);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final RoutineConfiguration configuration = mRoutineConfiguration;
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
    public ProxyConfiguration.Builder<? extends ContextObjectRoutineBuilder> withProxy() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ContextObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public RoutineConfiguration.Builder<? extends ContextObjectRoutineBuilder> withRoutine() {

        final RoutineConfiguration config = mRoutineConfiguration;
        return new RoutineConfiguration.Builder<ContextObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextObjectRoutineBuilder setConfiguration(
            @Nonnull final RoutineConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mRoutineConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ContextObjectRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ContextObjectRoutineBuilder> withInvocation() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ContextObjectRoutineBuilder>(this, config);
    }

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param configuration the routine configuration.
     */
    private void warn(@Nonnull final RoutineConfiguration configuration) {

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
     * Bound method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class BoundMethodInvocation<INPUT, OUTPUT>
            extends SingleCallContextInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mBindingName;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine;

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
        public BoundMethodInvocation(@Nonnull final Class<?> targetClass,
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
                                   .boundMethod(mBindingName);
                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Factory of {@link BoundMethodInvocation}s.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class BoundMethodInvocationFactory<INPUT, OUTPUT>
            implements ContextInvocationFactory<INPUT, OUTPUT> {

        @Nonnull
        public String getInvocationType() {

            return BoundMethodInvocation.class.getName();
        }

        @Nonnull
        public ContextInvocation<INPUT, OUTPUT> newInvocation(@Nonnull final Object... args) {

            return new BoundMethodInvocation<INPUT, OUTPUT>((Class<?>) args[0], (Object[]) args[1],
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
            extends SingleCallContextInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final Method mMethod;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine;

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

        @Nonnull
        public String getInvocationType() {

            return MethodInvocation.class.getName();
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
    private static class ProxyInvocation extends SingleCallContextInvocation<Object, Object> {

        private final Object[] mArgs;

        private final boolean mIsInputCollection;

        private final boolean mIsOutputCollection;

        private final Method mMethod;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private final Class<?>[] mTargetParameterTypes;

        private Object mMutex;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param targetClass          the target object class.
         * @param args                 the factory constructor arguments.
         * @param shareGroup           the share group name.
         * @param method               the method.
         * @param targetParameterTypes the target method parameter types.
         * @param isInputCollection    whether the input is a collection.
         * @param isOutputCollection   whether the output is a collection.
         */
        public ProxyInvocation(@Nonnull final Class<?> targetClass, @Nonnull final Object[] args,
                @Nullable final String shareGroup, @Nonnull final Method method,
                @Nonnull final Class<?>[] targetParameterTypes, final boolean isInputCollection,
                final boolean isOutputCollection) {

            mTargetClass = targetClass;
            mArgs = args;
            mShareGroup = shareGroup;
            mMethod = method;
            mTargetParameterTypes = targetParameterTypes;
            mIsInputCollection = isInputCollection;
            mIsOutputCollection = isOutputCollection;
            mMutex = this;
        }

        @Nonnull
        private Method getTargetMethod(@Nonnull final Method method,
                @Nonnull final Class<?>[] targetParameterTypes) throws NoSuchMethodException {

            String name = null;
            Method targetMethod = null;
            final Class<?> targetClass = mTarget.getClass();
            final Bind annotation = method.getAnnotation(Bind.class);

            if (annotation != null) {

                name = annotation.value();
                targetMethod = getAnnotatedMethod(targetClass, name);
            }

            if (targetMethod == null) {

                if (name == null) {

                    name = method.getName();
                }

                try {

                    targetMethod = targetClass.getMethod(name, targetParameterTypes);

                } catch (final NoSuchMethodException ignored) {

                }

                if (targetMethod == null) {

                    targetMethod = targetClass.getDeclaredMethod(name, targetParameterTypes);
                }
            }

            return targetMethod;
        }

        @Override
        @SuppressWarnings("SynchronizeOnNonFinalField")
        public void onCall(@Nonnull final List<?> objects,
                @Nonnull final ResultChannel<Object> result) {

            try {

                final Method method = mMethod;
                final Class<?>[] targetParameterTypes = mTargetParameterTypes;
                final Method targetMethod = getTargetMethod(method, targetParameterTypes);
                final Class<?> returnType = targetMethod.getReturnType();
                final Pass annotation = method.getAnnotation(Pass.class);
                final Class<?> expectedType;

                if (annotation != null) {

                    expectedType = annotation.value();

                } else {

                    expectedType = method.getReturnType();
                }

                if (!returnType.isAssignableFrom(expectedType)) {

                    throw new IllegalArgumentException(
                            "the proxy method has incompatible return type: " + method);
                }

                final Object methodResult;

                synchronized (mMutex) {

                    if (mIsInputCollection) {

                        final Class<?> paramType = targetParameterTypes[0];

                        if (paramType.isArray()) {

                            final int size = objects.size();
                            final Object array =
                                    Array.newInstance(paramType.getComponentType(), size);

                            for (int i = 0; i < size; i++) {

                                Array.set(array, i, objects.get(i));
                            }

                            methodResult = targetMethod.invoke(mTarget, array);

                        } else {

                            methodResult = targetMethod.invoke(mTarget, objects);
                        }

                    } else {

                        methodResult = targetMethod.invoke(mTarget, objects.toArray());
                    }
                }

                if (!Void.class.equals(boxingClass(returnType))) {

                    if (mIsOutputCollection) {

                        if (returnType.isArray()) {

                            if (methodResult != null) {

                                final int l = Array.getLength(methodResult);

                                for (int i = 0; i < l; ++i) {

                                    result.pass(Array.get(methodResult, i));
                                }
                            }

                        } else {

                            result.pass((Iterable<?>) methodResult);
                        }

                    } else {

                        result.pass(methodResult);
                    }
                }

            } catch (final RoutineException e) {

                throw e;

            } catch (final InvocationTargetException e) {

                throw new InvocationException(e.getCause());

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
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

            return new ProxyInvocation((Class<?>) args[0], (Object[]) args[1], (String) args[2],
                                       (Method) args[3], (Class<?>[]) args[4], (Boolean) args[5],
                                       (Boolean) args[6]);
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final Object[] mArgs;

        private final WeakReference<Object> mContext;

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final RoutineConfiguration mRoutineConfiguration;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param builder the builder instance.
         */
        private ProxyInvocationHandler(@Nonnull final DefaultContextObjectRoutineBuilder builder) {

            mContext = builder.mContext;
            mTargetClass = builder.mTargetClass;
            mRoutineConfiguration = builder.mRoutineConfiguration;
            mProxyConfiguration = builder.mProxyConfiguration;
            mInvocationConfiguration = builder.mInvocationConfiguration;
            mArgs = mRoutineConfiguration.getFactoryArgsOr(Reflection.NO_ARGS);
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            boolean isParallel = false;
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            final int length = (args != null) ? args.length : 0;
            final boolean[] isAsync = new boolean[length];
            final Class<?>[] targetParameterTypes = new Class<?>[length];
            boolean isInputCollection = false;

            for (int i = 0; i < length; i++) {

                final PassMode paramMode = getParamMode(method, i);

                if (paramMode != null) {

                    isAsync[i] = true;
                    isParallel = (paramMode == PassMode.PARALLEL);
                    isInputCollection = (paramMode == PassMode.COLLECTION);

                    for (final Annotation annotation : parameterAnnotations[i]) {

                        if (annotation.annotationType() == Pass.class) {

                            targetParameterTypes[i] = ((Pass) annotation).value();
                            break;
                        }
                    }

                } else {

                    targetParameterTypes[i] = parameterTypes[i];
                }
            }

            PassMode returnMode = null;
            final Class<?> returnType = method.getReturnType();
            final Pass methodAnnotation = method.getAnnotation(Pass.class);

            if (methodAnnotation != null) {

                returnMode = getReturnMode(method);
            }

            final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, method);
            final boolean isOutputCollection = (returnMode == PassMode.COLLECTION);
            final Object[] invocationArgs =
                    new Object[]{mTargetClass, mArgs, shareGroup, method, targetParameterTypes,
                                 isInputCollection, isOutputCollection};
            final OrderType inputOrderType = (isParallel) ? OrderType.NONE : OrderType.PASS_ORDER;
            final OrderType outputOrderType =
                    (returnMode == PassMode.COLLECTION) ? OrderType.PASS_ORDER : OrderType.NONE;
            final ContextRoutineBuilder<Object, Object> routineBuilder =
                    getBuilder(mContext, sProxyFactory);
            final RoutineConfiguration routineConfiguration =
                    configurationWithTimeout(mRoutineConfiguration, method);
            final InvocationConfiguration invocationConfiguration =
                    configurationWithAnnotations(mInvocationConfiguration, method);
            final Routine<Object, Object> routine = routineBuilder.withRoutine()
                                                                  .with(routineConfiguration)
                                                                  .withFactoryArgs(invocationArgs)
                                                                  .withInputOrder(inputOrderType)
                                                                  .withOutputOrder(outputOrderType)
                                                                  .set()
                                                                  .withInvocation()
                                                                  .with(invocationConfiguration)
                                                                  .set()
                                                                  .buildRoutine();
            final ParameterChannel<Object, Object> parameterChannel =
                    (isParallel) ? routine.invokeParallel() : routine.invokeAsync();

            for (int i = 0; i < length; i++) {

                if (isAsync[i]) {

                    final Class<?> parameterType = parameterTypes[i];

                    if (OutputChannel.class.isAssignableFrom(parameterType)) {

                        parameterChannel.pass((OutputChannel<?>) args[i]);

                    } else if (args[i] == null) {

                        parameterChannel.pass((Object[]) null);

                    } else if (parameterType.isArray()) {

                        final Object array = args[i];
                        final int size = Array.getLength(array);

                        for (int j = 0; j < size; j++) {

                            parameterChannel.pass(Array.get(array, j));
                        }

                    } else if (Iterable.class.isAssignableFrom(parameterType)) {

                        for (final Object object : ((Iterable<?>) args[i])) {

                            parameterChannel.pass(object);
                        }
                    }

                } else {

                    parameterChannel.pass(args[i]);
                }
            }

            final OutputChannel<Object> outputChannel = parameterChannel.result();

            if (!Void.class.equals(boxingClass(returnType))) {

                if (methodAnnotation != null) {

                    if (OutputChannel.class.isAssignableFrom(returnType)) {

                        return outputChannel;
                    }

                    if (returnType.isAssignableFrom(List.class)) {

                        return outputChannel.readAll();
                    }

                    if (returnType.isArray()) {

                        final List<Object> results = outputChannel.readAll();
                        final int size = results.size();
                        final Object array = Array.newInstance(returnType.getComponentType(), size);

                        for (int i = 0; i < size; ++i) {

                            Array.set(array, i, results.get(i));
                        }

                        return array;
                    }
                }

                return outputChannel.readNext();
            }

            return null;
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
