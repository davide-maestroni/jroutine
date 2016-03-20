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

package com.github.dm.jrt.android;

import com.github.dm.jrt.android.core.JRoutineService;
import com.github.dm.jrt.android.core.ServiceContext;
import com.github.dm.jrt.android.core.builder.ServiceRoutineBuilder;
import com.github.dm.jrt.android.core.invocation.ContextInvocation;
import com.github.dm.jrt.android.core.invocation.TargetInvocationFactory;
import com.github.dm.jrt.android.object.ContextInvocationTarget;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.util.ClassToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.android.core.invocation.TargetInvocationFactory.factoryOf;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.classOfType;
import static com.github.dm.jrt.android.object.ContextInvocationTarget.instanceOf;

/**
 * Created by davide-maestroni on 03/06/2016.
 */
public class ServiceContextBuilder {

    private final ServiceContext mContext;

    /**
     * Constructor.
     *
     * @param context the service context.
     */
    @SuppressWarnings("ConstantConditions")
    ServiceContextBuilder(@NotNull final ServiceContext context) {

        if (context == null) {
            throw new NullPointerException("the context must not be null");
        }

        mContext = context;
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the
     * specified class.
     *
     * @param invocationClass the invocation class.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no default constructor was found.
     */
    @NotNull
    public <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass) {

        return on(factoryOf(invocationClass));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the
     * specified class by passing the specified arguments to the class constructor.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes have both the outer
     * instance and all the variables captured in the closure.
     *
     * @param invocationClass the invocation class.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects
     *                                            as parameters was found.
     */
    @NotNull
    public <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass,
            @Nullable final Object... args) {

        return on(factoryOf(invocationClass, args));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the
     * specified class token.
     *
     * @param invocationToken the invocation class token.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no default constructor was found.
     */
    @NotNull
    public <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> invocationToken) {

        return on(factoryOf(invocationToken));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the
     * specified class token by passing the specified arguments to the class constructor.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember
     * however that Java creates synthetic constructors for such classes, so be sure to specify
     * the correct arguments to guarantee proper instantiation. In fact, inner classes always
     * have the outer instance as first constructor parameter, and anonymous classes have both
     * the outer instance and all the variables captured in the closure.
     *
     * @param invocationToken the invocation class token.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects
     *                                            as parameters was found.
     */
    @NotNull
    public <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {

        return on(factoryOf(invocationToken, args));
    }

    /**
     * Returns a builder of routines running in a service based on the builder context, wrapping
     * the specified target object.<br/>
     * In order to customize the object creation, the caller must employ an implementation of a
     * {@link com.github.dm.jrt.android.object.builder.FactoryContext FactoryContext} as the
     * invocation service.
     * <p/>
     * Note that the built routine results will be dispatched into the configured looper, thus,
     * waiting for the outputs on the very same looper thread, immediately after its invocation,
     * will result in a deadlock. By default output results are dispatched in the main looper.<br/>
     * Note also that the invocation input data will be cached, and the results will be produced
     * only after the invocation channel is closed, so be sure to avoid streaming inputs in
     * order to prevent starvation or out of memory errors.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     */
    @NotNull
    public ServiceTargetRoutineBuilder on(@NotNull final ContextInvocationTarget<?> target) {

        return new DefaultServiceTargetRoutineBuilder(mContext, target);
    }

    /**
     * Returns a builder of routines running in a service based on the builder context.<br/>
     * In order to customize the invocation creation, the caller must override the method
     * {@link com.github.dm.jrt.android.core.service.InvocationService#getInvocationFactory(
     *Class, Object...) getInvocationFactory(Class, Object...)} of the routine service.
     * <p/>
     * Note that the built routine results will be dispatched into the configured looper, thus,
     * waiting for the outputs on the very same looper thread, immediately after its invocation,
     * will result in a deadlock. By default output results are dispatched in the main looper.
     *
     * @param target the invocation target.
     * @param <IN>   the input data type.
     * @param <OUT>  the output data type.
     * @return the routine builder instance.
     */
    @NotNull
    public <IN, OUT> ServiceRoutineBuilder<IN, OUT> on(
            @NotNull final TargetInvocationFactory<IN, OUT> target) {

        return JRoutineService.with(mContext).on(target);
    }

    @NotNull
    public ServiceTargetRoutineBuilder onClass(@NotNull final Class<?> targetClass) {

        return on(classOfType(targetClass));
    }

    @NotNull
    public ServiceTargetRoutineBuilder onInstance(@NotNull final Class<?> targetClass) {

        return on(instanceOf(targetClass));
    }

    @NotNull
    public ServiceTargetRoutineBuilder onInstance(@NotNull final Class<?> targetClass,
            @Nullable final Object... factoryArgs) {

        return on(instanceOf(targetClass, factoryArgs));
    }
}
