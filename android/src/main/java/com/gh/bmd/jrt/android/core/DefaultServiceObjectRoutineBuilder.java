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
package com.gh.bmd.jrt.android.core;

import android.content.Context;

import com.gh.bmd.jrt.android.builder.FactoryContext;
import com.gh.bmd.jrt.android.builder.ServiceConfiguration;
import com.gh.bmd.jrt.android.builder.ServiceObjectRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.FunctionContextInvocation;
import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.Priority;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.channel.RoutineException;
import com.gh.bmd.jrt.core.RoutineBuilders.MethodInfo;
import com.gh.bmd.jrt.invocation.InvocationException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.Reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.core.RoutineBuilders.callFromInvocation;
import static com.gh.bmd.jrt.core.RoutineBuilders.getAnnotatedMethod;
import static com.gh.bmd.jrt.core.RoutineBuilders.getSharedMutex;
import static com.gh.bmd.jrt.core.RoutineBuilders.getTargetMethodInfo;
import static com.gh.bmd.jrt.core.RoutineBuilders.invokeRoutine;
import static com.gh.bmd.jrt.util.Reflection.findConstructor;
import static com.gh.bmd.jrt.util.Reflection.findMethod;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * Created by davide-maestroni on 3/29/15.
 */
class DefaultServiceObjectRoutineBuilder implements ServiceObjectRoutineBuilder,
        InvocationConfiguration.Configurable<ServiceObjectRoutineBuilder>,
        ProxyConfiguration.Configurable<ServiceObjectRoutineBuilder>,
        ServiceConfiguration.Configurable<ServiceObjectRoutineBuilder> {

    private static final ClassToken<ProxyInvocation> PROXY_TOKEN =
            ClassToken.tokenOf(ProxyInvocation.class);

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private final Context mContext;

    private final Class<?> mTargetClass;

    private InvocationConfiguration mInvocationConfiguration =
            InvocationConfiguration.DEFAULT_CONFIGURATION;

    private ProxyConfiguration mProxyConfiguration = ProxyConfiguration.DEFAULT_CONFIGURATION;

    private ServiceConfiguration mServiceConfiguration = ServiceConfiguration.DEFAULT_CONFIGURATION;

    /**
     * Constructor.
     *
     * @param context     the routine context.
     * @param targetClass the target object class.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultServiceObjectRoutineBuilder(@Nonnull final Context context,
            @Nonnull final Class<?> targetClass) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        if (targetClass == null) {

            throw new NullPointerException("the target class must not be null");
        }

        mContext = context;
        mTargetClass = targetClass;
    }

    @Nonnull
    private static InvocationConfiguration configurationWithAnnotations(
            @Nonnull final InvocationConfiguration configuration, @Nonnull final Method method) {

        final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                InvocationConfiguration.builderFrom(configuration);
        final Priority priorityAnnotation = method.getAnnotation(Priority.class);

        if (priorityAnnotation != null) {

            builder.withPriority(priorityAnnotation.value());
        }

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
    private static Class<?>[] forNames(@Nonnull final String[] names) throws
            ClassNotFoundException {

        final int length = names.length;
        final Class<?>[] classes = new Class[length];
        final HashMap<String, Class<?>> classMap = sPrimitiveClassMap;

        for (int i = 0; i < length; i++) {

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

    @Nonnull
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static Object getInstance(@Nonnull final Context context,
            @Nonnull final Class<?> targetClass, @Nonnull final Object[] args) throws
            IllegalAccessException, InvocationTargetException, InstantiationException {

        Object target = null;

        if (context instanceof FactoryContext) {

            // the context here is always the service
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
    private static String groupWithShareAnnotation(@Nonnull final ProxyConfiguration configuration,
            @Nonnull final Method method) {

        final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            return shareGroupAnnotation.value();
        }

        return configuration.getShareGroupOr(null);
    }

    @Nonnull
    private static String[] toNames(@Nonnull final Class<?>[] classes) {

        final int length = classes.length;
        final String[] names = new String[length];

        for (int i = 0; i < length; i++) {

            names[i] = classes[i].getName();
        }

        return names;
    }

    /**
     * Logs any warning related to ignored options in the specified configuration.
     *
     * @param logClass      the log class.
     * @param configuration the invocation configuration.
     */
    private static void warn(@Nullable final Class<? extends Log> logClass,
            @Nonnull final InvocationConfiguration configuration) {

        Log log = null;

        if (logClass != null) {

            final Constructor<? extends Log> constructor = findConstructor(logClass);

            try {

                log = constructor.newInstance();

            } catch (final Throwable t) {

                throw new IllegalArgumentException(t);
            }
        }

        if (log == null) {

            log = configuration.getLogOr(Logger.getGlobalLog());
        }

        Logger logger = null;
        final OrderType inputOrderType = configuration.getInputOrderTypeOr(null);

        if (inputOrderType != null) {

            logger = Logger.newLogger(log, configuration.getLogLevelOr(Logger.getGlobalLogLevel()),
                                      DefaultServiceObjectRoutineBuilder.class);
            logger.wrn("the specified input order type will be ignored: %s", inputOrderType);
        }

        final OrderType outputOrderType = configuration.getOutputOrderTypeOr(null);

        if (outputOrderType != null) {

            if (logger == null) {

                logger = Logger.newLogger(log,
                                          configuration.getLogLevelOr(Logger.getGlobalLogLevel()),
                                          DefaultServiceObjectRoutineBuilder.class);
            }

            logger.wrn("the specified output order type will be ignored: %s", outputOrderType);
        }
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull final String name) {

        final Class<?> targetClass = mTargetClass;
        final Method targetMethod = getAnnotatedMethod(targetClass, name);

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
        final ServiceConfiguration serviceConfiguration = mServiceConfiguration;
        final Object[] args = invocationConfiguration.getFactoryArgsOr(Reflection.NO_ARGS);
        warn(serviceConfiguration.getLogClassOr(null), invocationConfiguration);
        final AliasMethodToken<INPUT, OUTPUT> classToken = new AliasMethodToken<INPUT, OUTPUT>();
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        return JRoutine.onService(mContext, classToken)
                       .withInvocation()
                       .with(configurationWithAnnotations(invocationConfiguration, targetMethod))
                       .withFactoryArgs(targetClass.getName(), args, shareGroup, name)
                       .withInputOrder(OrderType.PASS_ORDER)
                       .set()
                       .withService()
                       .with(serviceConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(method.getName(), method.getParameterTypes());
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final String name,
            @Nonnull final Class<?>... parameterTypes) {

        final Class<?> targetClass = mTargetClass;
        final Method targetMethod = findMethod(targetClass, name, parameterTypes);
        final InvocationConfiguration invocationConfiguration = mInvocationConfiguration;
        final ServiceConfiguration serviceConfiguration = mServiceConfiguration;
        final Object[] args = invocationConfiguration.getFactoryArgsOr(Reflection.NO_ARGS);
        warn(serviceConfiguration.getLogClassOr(null), invocationConfiguration);
        final MethodSignatureToken<INPUT, OUTPUT> classToken =
                new MethodSignatureToken<INPUT, OUTPUT>();
        final String shareGroup = groupWithShareAnnotation(mProxyConfiguration, targetMethod);
        return JRoutine.onService(mContext, classToken)
                       .withInvocation()
                       .with(configurationWithAnnotations(invocationConfiguration, targetMethod))
                       .withFactoryArgs(targetClass.getName(), args, shareGroup, name,
                                        toNames(parameterTypes))
                       .withInputOrder(OrderType.PASS_ORDER)
                       .set()
                       .withService()
                       .with(serviceConfiguration)
                       .set()
                       .buildRoutine();
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        warn(mServiceConfiguration.getLogClassOr(null), mInvocationConfiguration);
        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler(this));
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @Nonnull final ServiceConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mServiceConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @Nonnull final ProxyConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mProxyConfiguration = configuration;
        return this;
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    public ServiceObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        if (configuration == null) {

            throw new NullPointerException("the configuration must not be null");
        }

        mInvocationConfiguration = configuration;
        return this;
    }

    @Nonnull
    public InvocationConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withInvocation() {

        final InvocationConfiguration config = mInvocationConfiguration;
        return new InvocationConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public ProxyConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withProxy() {

        final ProxyConfiguration config = mProxyConfiguration;
        return new ProxyConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    @Nonnull
    public ServiceConfiguration.Builder<? extends ServiceObjectRoutineBuilder> withService() {

        final ServiceConfiguration config = mServiceConfiguration;
        return new ServiceConfiguration.Builder<ServiceObjectRoutineBuilder>(this, config);
    }

    /**
     * Alias method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AliasMethodInvocation<INPUT, OUTPUT>
            extends FunctionContextInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mBindingName;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param targetClassName the target object class name.
         * @param args            the factory constructor arguments.
         * @param shareGroup      the share group name.
         * @param name            the binding name.
         */
        @SuppressWarnings("unchecked")
        public AliasMethodInvocation(@Nonnull final String targetClassName,
                @Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final String name) throws ClassNotFoundException {

            mTargetClass = Class.forName(targetClassName);
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
     * Class token of a {@link AliasMethodInvocation}.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class AliasMethodToken<INPUT, OUTPUT>
            extends ClassToken<AliasMethodInvocation<INPUT, OUTPUT>> {

    }

    /**
     * Generic method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodSignatureInvocation<INPUT, OUTPUT>
            extends FunctionContextInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param targetClassName the target object class name.
         * @param args            the factory constructor arguments.
         * @param shareGroup      the share group name.
         * @param name            the method name.
         * @param parameterTypes  the method parameter type names.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         */
        @SuppressWarnings("unchecked")
        public MethodSignatureInvocation(@Nonnull final String targetClassName,
                @Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final String name, @Nonnull final String[] parameterTypes) throws
                ClassNotFoundException {

            mTargetClass = Class.forName(targetClassName);
            mArgs = args;
            mShareGroup = shareGroup;
            mMethodName = name;
            mParameterTypes = forNames(parameterTypes);
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
                                   .method(mMethodName, mParameterTypes);
                mTarget = target;

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Class token of a {@link MethodSignatureInvocation}.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodSignatureToken<INPUT, OUTPUT>
            extends ClassToken<MethodSignatureInvocation<INPUT, OUTPUT>> {

    }

    /**
     * Proxy method invocation.
     */
    private static class ProxyInvocation extends FunctionContextInvocation<Object, Object> {

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
         * @param args                 the factory constructor arguments.
         * @param shareGroup           the share group name.
         * @param targetClassName      the target object class name.
         * @param targetMethodName     the target method name.
         * @param targetParameterTypes the target method parameter type names.
         * @param inputMode            the input transfer mode.
         * @param outputMode           the output transfer mode.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         * @throws java.lang.NoSuchMethodException  if the target method is not found.
         */
        @SuppressWarnings("unchecked")
        public ProxyInvocation(@Nonnull final Object[] args, @Nullable final String shareGroup,
                @Nonnull final String targetClassName, @Nonnull final String targetMethodName,
                @Nonnull final String[] targetParameterTypes, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) throws ClassNotFoundException,
                NoSuchMethodException {

            final Class<?> targetClass = Class.forName(targetClassName);
            mArgs = args;
            mShareGroup = shareGroup;
            mTargetClass = targetClass;
            mTargetMethod = targetClass.getMethod(targetMethodName, forNames(targetParameterTypes));
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
     * Invocation handler adapting a different interface to the target object instance.
     */
    private static class ProxyInvocationHandler implements InvocationHandler {

        private final Object[] mArgs;

        private final Context mContext;

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        private final ServiceConfiguration mServiceConfiguration;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param builder the builder instance.
         */
        private ProxyInvocationHandler(@Nonnull final DefaultServiceObjectRoutineBuilder builder) {

            mContext = builder.mContext;
            mTargetClass = builder.mTargetClass;
            mInvocationConfiguration = builder.mInvocationConfiguration;
            mProxyConfiguration = builder.mProxyConfiguration;
            mServiceConfiguration = builder.mServiceConfiguration;
            mArgs = mInvocationConfiguration.getFactoryArgsOr(Reflection.NO_ARGS);
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final Class<?> targetClass = mTargetClass;
            final MethodInfo methodInfo = getTargetMethodInfo(targetClass, method);
            final Method targetMethod = methodInfo.method;
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Class<?>[] targetParameterTypes = targetMethod.getParameterTypes();
            final Object[] factoryArgs =
                    new Object[]{mArgs, groupWithShareAnnotation(mProxyConfiguration, method),
                                 targetClass.getName(), targetMethod.getName(),
                                 toNames(targetParameterTypes), inputMode, outputMode};
            final OrderType inputOrder =
                    (inputMode == InputMode.ELEMENT) ? OrderType.NONE : OrderType.PASS_ORDER;
            final OrderType outputOrder =
                    (outputMode == OutputMode.ELEMENT) ? OrderType.PASS_ORDER : OrderType.NONE;
            final Routine<Object, Object> routine = JRoutine.onService(mContext, PROXY_TOKEN)
                                                            .withInvocation()
                                                            .with(configurationWithAnnotations(
                                                                    mInvocationConfiguration,
                                                                    method))
                                                            .withFactoryArgs(factoryArgs)
                                                            .withInputOrder(inputOrder)
                                                            .withOutputOrder(outputOrder)
                                                            .set()
                                                            .withService()
                                                            .with(mServiceConfiguration)
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
