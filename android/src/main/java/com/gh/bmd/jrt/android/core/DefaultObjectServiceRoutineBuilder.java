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
import android.os.Looper;

import com.gh.bmd.jrt.android.builder.FactoryContext;
import com.gh.bmd.jrt.android.builder.ObjectServiceRoutineBuilder;
import com.gh.bmd.jrt.android.invocation.ContextSingleCallInvocation;
import com.gh.bmd.jrt.android.service.RoutineService;
import com.gh.bmd.jrt.annotation.Bind;
import com.gh.bmd.jrt.annotation.Pass;
import com.gh.bmd.jrt.annotation.Pass.PassMode;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.RoutineConfiguration;
import com.gh.bmd.jrt.builder.RoutineConfiguration.Builder;
import com.gh.bmd.jrt.builder.RoutineConfiguration.OrderType;
import com.gh.bmd.jrt.builder.ShareConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.channel.ParameterChannel;
import com.gh.bmd.jrt.channel.ResultChannel;
import com.gh.bmd.jrt.common.ClassToken;
import com.gh.bmd.jrt.common.InvocationException;
import com.gh.bmd.jrt.common.Reflection;
import com.gh.bmd.jrt.common.RoutineException;
import com.gh.bmd.jrt.log.Log;
import com.gh.bmd.jrt.log.Logger;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.runner.Runner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
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
import static com.gh.bmd.jrt.builder.ShareConfiguration.withGroup;
import static com.gh.bmd.jrt.common.Reflection.boxingClass;
import static com.gh.bmd.jrt.common.Reflection.findConstructor;

/**
 * Class implementing a builder of routine objects based on methods of a concrete object instance.
 * <p/>
 * Created by davide on 3/29/15.
 */
class DefaultObjectServiceRoutineBuilder implements ObjectServiceRoutineBuilder {

    private static final HashMap<String, Class<?>> sPrimitiveClassMap =
            new HashMap<String, Class<?>>();

    private final Context mContext;

    private final Class<?> mTargetClass;

    private Object[] mArgs = Reflection.NO_ARGS;

    private Class<? extends Log> mLogClass;

    private Looper mLooper;

    private RoutineConfiguration mRoutineConfiguration;

    private Class<? extends Runner> mRunnerClass;

    private Class<? extends RoutineService> mServiceClass;

    private ShareConfiguration mShareConfiguration;

    /**
     * Constructor.
     *
     * @param context     the routine context.
     * @param targetClass the target object class.
     * @throws java.lang.NullPointerException if any of the parameter is null.
     */
    @SuppressWarnings("ConstantConditions")
    DefaultObjectServiceRoutineBuilder(@Nonnull final Context context,
            @Nonnull final Class<?> targetClass) {

        if (context == null) {

            throw new NullPointerException("the context must not be null");
        }

        mContext = context;
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
     * @param configuration the routine configuration.
     */
    private static void warn(@Nullable final Class<? extends Log> logClass,
            @Nonnull final RoutineConfiguration configuration) {

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
        final OrderType inputOrder = configuration.getInputOrderOr(null);

        if (inputOrder != null) {

            logger = Logger.newLogger(log, configuration.getLogLevelOr(Logger.getGlobalLogLevel()),
                                      DefaultObjectServiceRoutineBuilder.class);
            logger.wrn("the specified input order will be ignored: %s", inputOrder);
        }

        final OrderType outputOrder = configuration.getOutputOrderOr(null);

        if (outputOrder != null) {

            if (logger == null) {

                logger = Logger.newLogger(log,
                                          configuration.getLogLevelOr(Logger.getGlobalLogLevel()),
                                          DefaultObjectServiceRoutineBuilder.class);
            }

            logger.wrn("the specified output order will be ignored: %s", outputOrder);
        }
    }

    @Nullable
    private static String withShareAnnotation(@Nullable final ShareConfiguration configuration,
            @Nonnull final Method method) {

        final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

        if (shareGroupAnnotation != null) {

            return shareGroupAnnotation.value();
        }

        return ShareConfiguration.notNull(configuration).getGroupOr(null);
    }

    @Nonnull
    private static Builder withTimeoutAnnotation(@Nullable final RoutineConfiguration configuration,
            @Nonnull final Method method) {

        final Builder builder = RoutineConfiguration.notNull(configuration).builderFrom();
        final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

        if (timeoutAnnotation != null) {

            builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
        }

        final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

        if (actionAnnotation != null) {

            builder.onReadTimeout(actionAnnotation.value());
        }

        return builder;
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> boundMethod(@Nonnull final String name) {

        final Class<?> targetClass = mTargetClass;
        final Method targetMethod = getAnnotatedMethod(targetClass, name);

        if (targetMethod == null) {

            throw new IllegalArgumentException(
                    "no annotated method with name '" + name + "' has been found");
        }

        final RoutineConfiguration configuration = mRoutineConfiguration;

        if (configuration != null) {

            warn(mLogClass, configuration);
        }

        final BoundMethodToken<INPUT, OUTPUT> classToken = new BoundMethodToken<INPUT, OUTPUT>();
        final Object[] args = mArgs;
        return JRoutine.onService(mContext, classToken)
                       .withArgs(targetClass.getName(), args,
                                 withShareAnnotation(mShareConfiguration, targetMethod), name)
                       .configure(withTimeoutAnnotation(configuration, targetMethod).withInputOrder(
                               OrderType.PASSING_ORDER).buildConfiguration())
                       .withServiceClass(mServiceClass)
                       .withRunnerClass(mRunnerClass)
                       .withLogClass(mLogClass)
                       .dispatchingOn(mLooper)
                       .buildRoutine();
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> method(@Nonnull final Method method) {

        return method(method.getName(), method.getParameterTypes());
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

        final RoutineConfiguration configuration = mRoutineConfiguration;

        if (configuration != null) {

            warn(mLogClass, configuration);
        }

        final MethodSignatureToken<INPUT, OUTPUT> classToken =
                new MethodSignatureToken<INPUT, OUTPUT>();
        final Object[] args = mArgs;
        return JRoutine.onService(mContext, classToken)
                       .withArgs(targetClass.getName(), args,
                                 withShareAnnotation(mShareConfiguration, targetMethod), name,
                                 toNames(parameterTypes))
                       .configure(withTimeoutAnnotation(configuration, targetMethod).withInputOrder(
                               OrderType.PASSING_ORDER).buildConfiguration())
                       .withServiceClass(mServiceClass)
                       .withRunnerClass(mRunnerClass)
                       .withLogClass(mLogClass)
                       .dispatchingOn(mLooper)
                       .buildRoutine();
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getCanonicalName());
        }

        final RoutineConfiguration configuration = mRoutineConfiguration;

        if (configuration != null) {

            warn(mLogClass, configuration);
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler(this, itf));
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return buildProxy(itf.getRawClass());
    }

    @Nonnull
    public ObjectServiceRoutineBuilder configure(
            @Nullable final RoutineConfiguration configuration) {

        mRoutineConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder configure(
            @Nonnull final RoutineConfiguration.Builder builder) {

        return configure(builder.buildConfiguration());
    }

    @Nonnull
    public ObjectServiceRoutineBuilder share(@Nullable final ShareConfiguration configuration) {

        mShareConfiguration = configuration;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder share(@Nonnull final ShareConfiguration.Builder builder) {

        return share(builder.buildConfiguration());
    }

    @Nonnull
    public ObjectServiceRoutineBuilder dispatchingOn(@Nullable final Looper looper) {

        mLooper = looper;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withLogClass(@Nullable final Class<? extends Log> logClass) {

        mLogClass = logClass;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withRunnerClass(
            @Nullable final Class<? extends Runner> runnerClass) {

        mRunnerClass = runnerClass;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withServiceClass(
            @Nullable final Class<? extends RoutineService> serviceClass) {

        mServiceClass = serviceClass;
        return this;
    }

    @Nonnull
    public ObjectServiceRoutineBuilder withArgs(@Nullable final Object... args) {

        mArgs = (args == null) ? Reflection.NO_ARGS : args.clone();
        return this;
    }

    /**
     * Bound method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class BoundMethodInvocation<INPUT, OUTPUT>
            extends ContextSingleCallInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mBindingName;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine;

        /**
         * Constructor.
         *
         * @param targetClassName the target object class name.
         * @param args            the factory constructor arguments.
         * @param shareGroup      the share group name.
         * @param name            the binding name.
         */
        @SuppressWarnings("unchecked")
        public BoundMethodInvocation(@Nonnull final String targetClassName,
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

            result.pass(mRoutine.callSync(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                mRoutine = JRoutine.on(getInstance(context, mTargetClass, mArgs))
                                   .share(withGroup(mShareGroup))
                                   .boundMethod(mBindingName);

            } catch (final RoutineException e) {

                throw e;

            } catch (final Throwable t) {

                throw new InvocationException(t);
            }
        }
    }

    /**
     * Class token of a {@link BoundMethodInvocation}.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class BoundMethodToken<INPUT, OUTPUT>
            extends ClassToken<BoundMethodInvocation<INPUT, OUTPUT>> {

    }

    /**
     * Generic method invocation.
     *
     * @param <INPUT>  the input data type.
     * @param <OUTPUT> the output data type.
     */
    private static class MethodSignatureInvocation<INPUT, OUTPUT>
            extends ContextSingleCallInvocation<INPUT, OUTPUT> {

        private final Object[] mArgs;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private Routine<INPUT, OUTPUT> mRoutine;

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

            result.pass(mRoutine.callSync(inputs));
        }

        @Override
        public void onContext(@Nonnull final Context context) {

            super.onContext(context);

            try {

                mRoutine = JRoutine.on(getInstance(context, mTargetClass, mArgs))
                                   .share(withGroup(mShareGroup))
                                   .method(mMethodName, mParameterTypes);

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
    private static class ProxyInvocation extends ContextSingleCallInvocation<Object, Object> {

        private final Object[] mArgs;

        private final boolean mIsInputCollection;

        private final boolean mIsOutputCollection;

        private final String mMethodName;

        private final Class<?>[] mParameterTypes;

        private final Class<?> mProxyClass;

        private final String mShareGroup;

        private final Class<?> mTargetClass;

        private final Class<?>[] mTargetParameterTypes;

        private Object mMutex;

        private Object mTarget;

        /**
         * Constructor.
         *
         * @param proxyClassName       the proxy class name.
         * @param targetClassName      the target object class name.
         * @param args                 the factory constructor arguments.
         * @param shareGroup           the share group name.
         * @param name                 the method name.
         * @param parameterTypes       the method parameter type names.
         * @param targetParameterTypes the target method parameter type names.
         * @param isInputCollection    whether the input is a collection.
         * @param isOutputCollection   whether the output is a collection.
         * @throws java.lang.ClassNotFoundException if one of the specified classes is not found.
         */
        @SuppressWarnings("unchecked")
        public ProxyInvocation(@Nonnull final String proxyClassName,
                @Nonnull final String targetClassName, @Nonnull final Object[] args,
                @Nullable final String shareGroup, @Nonnull final String name,
                @Nonnull final String[] parameterTypes,
                @Nonnull final String[] targetParameterTypes, final boolean isInputCollection,
                final boolean isOutputCollection) throws ClassNotFoundException {

            mProxyClass = Class.forName(proxyClassName);
            mTargetClass = Class.forName(targetClassName);
            mArgs = args;
            mShareGroup = shareGroup;
            mMethodName = name;
            mParameterTypes = forNames(parameterTypes);
            mTargetParameterTypes = forNames(targetParameterTypes);
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

                final Class<?>[] parameterTypes = mParameterTypes;
                final Class<?>[] targetParameterTypes = mTargetParameterTypes;
                final Method method = mProxyClass.getMethod(mMethodName, parameterTypes);
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

                mTarget = getInstance(context, mTargetClass, mArgs);
                final String shareGroup = mShareGroup;

                if (!ShareGroup.NONE.equals(shareGroup)) {

                    mMutex = getSharedMutex(mTarget, shareGroup);
                }

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

        private final Class<? extends Log> mLogClass;

        private final Looper mLooper;

        private final Class<?> mProxyClass;

        private final RoutineConfiguration mRoutineConfiguration;

        private final Class<? extends Runner> mRunnerClass;

        private final Class<? extends RoutineService> mServiceClass;

        private final ShareConfiguration mShareConfiguration;

        private final Class<?> mTargetClass;

        /**
         * Constructor.
         *
         * @param builder    the builder instance.
         * @param proxyClass the proxy class.
         */
        private ProxyInvocationHandler(@Nonnull final DefaultObjectServiceRoutineBuilder builder,
                @Nonnull final Class<?> proxyClass) {

            mContext = builder.mContext;
            mTargetClass = builder.mTargetClass;
            mArgs = builder.mArgs;
            mServiceClass = builder.mServiceClass;
            mRoutineConfiguration = builder.mRoutineConfiguration;
            mShareConfiguration = builder.mShareConfiguration;
            mRunnerClass = builder.mRunnerClass;
            mLogClass = builder.mLogClass;
            mLooper = builder.mLooper;
            mProxyClass = proxyClass;
        }

        public Object invoke(final Object proxy, @Nonnull final Method method,
                final Object[] args) throws Throwable {

            boolean isParallel = false;
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            final int length = args.length;
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

            final boolean isOutputCollection = (returnMode == PassMode.COLLECTION);
            final Builder builder = withTimeoutAnnotation(mRoutineConfiguration, method);
            final RoutineConfiguration configuration =
                    builder.withInputOrder((isParallel) ? OrderType.NONE : OrderType.PASSING_ORDER)
                           .withOutputOrder(
                                   (returnMode == PassMode.COLLECTION) ? OrderType.PASSING_ORDER
                                           : OrderType.NONE)
                           .buildConfiguration();
            final Routine<Object, Object> routine =
                    JRoutine.onService(mContext, ClassToken.tokenOf(ProxyInvocation.class))
                            .withArgs(mProxyClass.getName(), mTargetClass.getName(), mArgs,
                                      withShareAnnotation(mShareConfiguration, method),
                                      method.getName(), toNames(parameterTypes),
                                      toNames(targetParameterTypes), isInputCollection,
                                      isOutputCollection)
                            .configure(configuration)
                            .withServiceClass(mServiceClass)
                            .withRunnerClass(mRunnerClass)
                            .withLogClass(mLogClass)
                            .dispatchingOn(mLooper)
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
