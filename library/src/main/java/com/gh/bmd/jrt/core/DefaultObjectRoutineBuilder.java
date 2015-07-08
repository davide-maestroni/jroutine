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
import com.gh.bmd.jrt.builder.ObjectRoutineBuilder;
import com.gh.bmd.jrt.builder.ProxyConfiguration;
import com.gh.bmd.jrt.core.RoutineBuilders.MethodInfo;
import com.gh.bmd.jrt.routine.Routine;
import com.gh.bmd.jrt.util.ClassToken;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.gh.bmd.jrt.core.RoutineBuilders.getAnnotatedMethod;
import static com.gh.bmd.jrt.core.RoutineBuilders.getTargetMethodInfo;
import static com.gh.bmd.jrt.core.RoutineBuilders.invokeRoutine;
import static com.gh.bmd.jrt.util.Reflection.NO_ARGS;

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

        final Method method = getAnnotatedMethod(name, getTargetClass());

        if (method == null) {

            throw new IllegalArgumentException(
                    "no annotated method with alias '" + name + "' has been found");
        }

        return method(method);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public InvocationConfiguration.Builder<? extends ObjectRoutineBuilder> invocations() {

        final InvocationConfiguration config = getInvocationConfiguration();
        return new InvocationConfiguration.Builder<ObjectRoutineBuilder>(mRoutineConfigurable,
                                                                         config);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public ProxyConfiguration.Builder<? extends ObjectRoutineBuilder> proxies() {

        final ProxyConfiguration config = getProxyConfiguration();
        return new ProxyConfiguration.Builder<ObjectRoutineBuilder>(mProxyConfigurable, config);
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder setConfiguration(@Nonnull final ProxyConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    @Override
    public ObjectRoutineBuilder setConfiguration(
            @Nonnull final InvocationConfiguration configuration) {

        super.setConfiguration(configuration);
        return this;
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final Class<TYPE> itf) {

        if (!itf.isInterface()) {

            throw new IllegalArgumentException(
                    "the specified class is not an interface: " + itf.getName());
        }

        final Object proxy = Proxy.newProxyInstance(itf.getClassLoader(), new Class[]{itf},
                                                    new ProxyInvocationHandler());
        return itf.cast(proxy);
    }

    @Nonnull
    public <TYPE> TYPE buildProxy(@Nonnull final ClassToken<TYPE> itf) {

        return itf.cast(buildProxy(itf.getRawClass()));
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

            final MethodInfo methodInfo = getTargetMethodInfo(method, getTargetClass());
            final InputMode inputMode = methodInfo.inputMode;
            final OutputMode outputMode = methodInfo.outputMode;
            final Routine<Object, Object> routine =
                    buildRoutine(method, methodInfo.method, inputMode, outputMode);
            return invokeRoutine(routine, method, (args != null) ? args : NO_ARGS, inputMode,
                                 outputMode);
        }

        @Nonnull
        private Routine<Object, Object> buildRoutine(@Nonnull final Method method,
                @Nonnull final Method targetMethod, @Nullable final InputMode inputMode,
                @Nullable final OutputMode outputMode) {

            String shareGroup = mProxyConfiguration.getShareGroupOr(null);
            final InvocationConfiguration configuration = mInvocationConfiguration;
            final InvocationConfiguration.Builder<InvocationConfiguration> builder =
                    configuration.builderFrom();
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
    }
}
