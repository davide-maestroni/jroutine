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
package com.gh.bmd.jrt.core;

import com.gh.bmd.jrt.annotation.Input.InputMode;
import com.gh.bmd.jrt.annotation.Output.OutputMode;
import com.gh.bmd.jrt.annotation.Priority;
import com.gh.bmd.jrt.annotation.ShareGroup;
import com.gh.bmd.jrt.annotation.Timeout;
import com.gh.bmd.jrt.annotation.TimeoutAction;
import com.gh.bmd.jrt.builder.InvocationConfiguration;
import com.gh.bmd.jrt.builder.InvocationConfiguration.OrderType;
import com.gh.bmd.jrt.builder.InvocationConfiguration.TimeoutActionType;
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.channel.OutputChannel;
import com.gh.bmd.jrt.core.JRoutineBuilders.MethodInfo;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;
import com.gh.bmd.jrt.util.TimeDuration;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.core.JRoutineBuilders.getAnnotatedMethod;
import static com.gh.bmd.jrt.core.JRoutineBuilders.getTargetMethodInfo;
import static com.gh.bmd.jrt.core.JRoutineBuilders.invokeRoutine;
import static com.gh.bmd.jrt.util.Reflection.NO_ARGS;
import static com.gh.bmd.jrt.util.Reflection.boxingClass;
import static com.gh.bmd.jrt.util.TimeDuration.fromUnit;

/**
 * Class implementing a builder of routines wrapping an object instance.
 * <p/>
 * Created by davide-maestroni on 9/21/14.
 */
class DefaultObjectRoutineBuilder extends DefaultClassRoutineBuilder
        implements ObjectRoutineBuilder {

    private final ProxyConfiguration.Configurable<ObjectRoutineBuilder> mProxyConfigurable =
            new ProxyConfiguration.Configurable<ObjectRoutineBuilder>() {

                @Nonnull
                public ObjectRoutineBuilder setConfiguration(
                        @Nonnull final ProxyConfiguration configuration) {

                    return DefaultObjectRoutineBuilder.this.setConfiguration(configuration);
                }
            };

    private final InvocationConfiguration.Configurable<ObjectRoutineBuilder> mRoutineConfigurable =
            new InvocationConfiguration.Configurable<ObjectRoutineBuilder>() {

                @Nonnull
                public ObjectRoutineBuilder setConfiguration(
                        @Nonnull final InvocationConfiguration configuration) {

                    return DefaultObjectRoutineBuilder.this.setConfiguration(configuration);
                }
            };

    /**
     * Constructor.
     *
     * @param target the target object instance.
     */
    DefaultObjectRoutineBuilder(@Nonnull final Object target) {

        super(target);
    }

    @Nonnull
    public <INPUT, OUTPUT> Routine<INPUT, OUTPUT> aliasMethod(@Nonnull final String name) {

        final Method method = getAnnotatedMethod(getTargetClass(), name);

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        return method(method);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public InvocationConfiguration.Builder<? extends ObjectRoutineBuilder> withInvocation() {

        final InvocationConfiguration config = getInvocationConfiguration();
        return new InvocationConfiguration.Builder<ObjectRoutineBuilder>(mRoutineConfigurable,
                                                                         config);
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder setConfiguration(@Nonnull final ProxyConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public ProxyConfiguration.Builder<? extends ObjectRoutineBuilder> withProxy() {

        final ProxyConfiguration config = getProxyConfiguration();
        return new ProxyConfiguration.Builder<ObjectRoutineBuilder>(mProxyConfigurable, config);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final InvocationHandler handler;

        if (itf.isAssignableFrom(getTargetClass())) {

            handler = new InterfaceInvocationHandler();

        } else {

            handler = new ProxyInvocationHandler();
        }

        final Object proxy =
                Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf}, handler);
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return itf.cast(buildProxy(itf.getRawClass()));
    }

    /**
     * Invocation handler wrapping the target object instance.
     */
    private class InterfaceInvocationHandler implements InvocationHandler {

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        /**
         * Constructor.
         */
        private InterfaceInvocationHandler() {

            mInvocationConfiguration = getInvocationConfiguration();
            mProxyConfiguration = getProxyConfiguration();
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final InvocationConfiguration invocationConfiguration;
            final Priority priorityAnnotation = method.getAnnotation(Priority.class);

            if (priorityAnnotation != null) {

                final int priority = priorityAnnotation.value();
                invocationConfiguration =
                        mInvocationConfiguration.builderFrom().withPriority(priority).set();

            } else {

                invocationConfiguration = mInvocationConfiguration;
            }

            final OutputChannel<Object> outputChannel =
                    method(invocationConfiguration, mProxyConfiguration, method).callAsync(args);
            final Class<?> returnType = method.getReturnType();

            if (!Void.class.equals(boxingClass(returnType))) {

                TimeDuration outputTimeout = null;
                TimeoutActionType outputAction = null;
                final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

                if (timeoutAnnotation != null) {

                    outputTimeout = fromUnit(timeoutAnnotation.value(), timeoutAnnotation.unit());
                }

                final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

                if (actionAnnotation != null) {

                    outputAction = actionAnnotation.value();
                }

                if (outputTimeout != null) {

                    outputChannel.afterMax(outputTimeout);
                }

                if (outputAction == TimeoutActionType.DEADLOCK) {

                    outputChannel.eventuallyDeadlock();

                } else if (outputAction == TimeoutActionType.EXIT) {

                    outputChannel.eventuallyExit();

                } else if (outputAction == TimeoutActionType.ABORT) {

                    outputChannel.eventuallyAbort();
                }

                return outputChannel.next();
            }

            return null;
        }
    }

    /**
     * Invocation handler adapting a different interface to the target object instance.
     */
    private class ProxyInvocationHandler implements InvocationHandler {

        private final InvocationConfiguration mInvocationConfiguration;

        private final ProxyConfiguration mProxyConfiguration;

        /**
         * Constructor.
         */
        private ProxyInvocationHandler() {

            mInvocationConfiguration = getInvocationConfiguration();
            mProxyConfiguration = getProxyConfiguration();
        }

        @Nonnull
        private Routine<Object, Object> buildRoutine(@Nonnull final Method method,
                @Nonnull final Method targetMethod, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            String shareGroup = mProxyConfiguration.getShareGroupOr(null);
            final InvocationConfiguration configuration = mInvocationConfiguration;
            final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                    configuration.builderFrom();
            warn(configuration);
            builder.withInputOrder(
                    (inputMode == InputMode.ELEMENT) ? OrderType.NONE : OrderType.PASS_ORDER)
                   .withInputMaxSize(Integer.MAX_VALUE)
                   .withInputTimeout(TimeDuration.ZERO)
                   .withOutputOrder((outputMode == OutputMode.ELEMENT) ? OrderType.PASS_ORDER
                                            : OrderType.NONE)
                   .withOutputMaxSize(Integer.MAX_VALUE)
                   .withOutputTimeout(TimeDuration.ZERO);
            final Priority priorityAnnotation = method.getAnnotation(Priority.class);

            if (priorityAnnotation != null) {

                builder.withPriority(priorityAnnotation.value());
            }

            final ShareGroup shareGroupAnnotation = method.getAnnotation(ShareGroup.class);

            if (shareGroupAnnotation != null) {

                shareGroup = shareGroupAnnotation.value();
            }

            final Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);

            if (timeoutAnnotation != null) {

                builder.withReadTimeout(timeoutAnnotation.value(), timeoutAnnotation.unit());
            }

            final TimeoutAction actionAnnotation = method.getAnnotation(TimeoutAction.class);

            if (actionAnnotation != null) {

                builder.withReadTimeoutAction(actionAnnotation.value());
            }

            return getRoutine(builder.set(), shareGroup, targetMethod, inputMode, outputMode);
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws
                Throwable {

            final WeakReference<?> targetReference = getTargetReference();

            if (targetReference == null) {

                throw new IllegalStateException("the target reference must not be null");
            }

            final Object target = targetReference.get();

            if (target == null) {

                throw new IllegalStateException("the target object has been destroyed");
            }

            final MethodInfo methodInfo = getTargetMethodInfo(getTargetClass(), method);
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Routine<Object, Object> routine =
                    buildRoutine(method, methodInfo.method, inputMode, outputMode);
            return invokeRoutine(routine, method, (args != null) ? args : NO_ARGS, inputMode,
                                 outputMode);
        }
    }
}
