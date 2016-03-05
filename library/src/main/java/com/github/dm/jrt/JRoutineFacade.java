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

package com.github.dm.jrt;

import com.github.dm.jrt.core.JRoutineCore;
import com.github.dm.jrt.core.builder.IOChannelBuilder;
import com.github.dm.jrt.core.builder.RoutineBuilder;
import com.github.dm.jrt.core.channel.Channels;
import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.OutputConsumerBuilder;
import com.github.dm.jrt.function.Predicate;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.CommandInvocation;
import com.github.dm.jrt.invocation.FilterInvocation;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.object.builder.ObjectRoutineBuilder;
import com.github.dm.jrt.object.core.InvocationTarget;
import com.github.dm.jrt.object.core.JRoutineObject;
import com.github.dm.jrt.proxy.builder.ProxyRoutineBuilder;
import com.github.dm.jrt.proxy.core.JRoutineProxy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.function.Functions.consumerCommand;
import static com.github.dm.jrt.function.Functions.consumerFactory;
import static com.github.dm.jrt.function.Functions.consumerFilter;
import static com.github.dm.jrt.function.Functions.functionFactory;
import static com.github.dm.jrt.function.Functions.functionFilter;
import static com.github.dm.jrt.function.Functions.predicateFilter;
import static com.github.dm.jrt.function.Functions.supplierCommand;
import static com.github.dm.jrt.function.Functions.supplierFactory;
import static com.github.dm.jrt.invocation.InvocationFactories.factoryOf;
import static com.github.dm.jrt.object.core.InvocationTarget.classOfType;
import static com.github.dm.jrt.object.core.InvocationTarget.instance;

/**
 * Class acting as a façade of all the JRoutine library features.
 * <p/>
 * Created by davide-maestroni on 02/29/2016.
 */
public class JRoutineFacade extends Channels {

    /**
     * Avoid direct instantiation.
     */
    protected JRoutineFacade() {

    }

    /**
     * Returns an I/O channel builder.
     *
     * @return the channel builder instance.
     */
    @NotNull
    public static IOChannelBuilder io() {

        return JRoutineCore.io();
    }

    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

        return JRoutineCore.on(consumerFilter(consumer));
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
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass) {

        return on(factoryOf(invocationClass));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the specified
     * class by passing the specified arguments to the class constructor.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes have both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocationClass the invocation class.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
            @Nullable final Object... args) {

        return on(factoryOf(invocationClass, args));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the specified
     * class token.
     *
     * @param invocationToken the invocation class token.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no default constructor was found.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken) {

        return on(factoryOf(invocationToken));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the specified
     * class token by passing the specified arguments to the class constructor.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes have both the outer instance
     * and all the variables captured in the closure.
     *
     * @param invocationToken the invocation class token.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {

        return on(factoryOf(invocationToken, args));
    }

    @NotNull
    public static <OUT> RoutineBuilder<Void, OUT> on(
            @NotNull final CommandInvocation<OUT> invocation) {

        return on(((InvocationFactory<Void, OUT>) invocation));
    }

    @NotNull
    public static <OUT> RoutineBuilder<Void, OUT> on(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return JRoutineCore.on(consumerCommand(consumer));
    }

    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final FilterInvocation<IN, OUT> invocation) {

        return on(((InvocationFactory<IN, OUT>) invocation));
    }

    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final Function<? super IN, ? extends OUT> function) {

        return JRoutineCore.on(functionFilter(function));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the specified
     * object.
     *
     * @param invocation the invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no default constructor was found.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final Invocation<IN, OUT> invocation) {

        return on(factoryOf(invocation));
    }

    /**
     * Returns a routine builder based on an invocation factory creating instances of the specified
     * object by passing the specified arguments to the class constructor.
     * <p/>
     * Note that inner and anonymous objects can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes have both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocation the invocation instance.
     * @param args       the invocation constructor arguments.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final Invocation<IN, OUT> invocation, @Nullable final Object... args) {

        return on(factoryOf(invocation, args));
    }

    /**
     * Returns a routine builder based on the specified invocation factory.<br/>
     * In order to prevent undesired leaks, the class of the specified factory should have a static
     * scope.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the routine builder instance.
     */
    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> on(
            @NotNull final InvocationFactory<IN, OUT> factory) {

        return JRoutineCore.on(factory);
    }

    /**
     * Returns a routine builder wrapping the specified target object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.<br/>
     * Note also that the invocation input data will be cached, and the results will be produced
     * only after the invocation channel is closed, so be sure to avoid streaming inputs in
     * order to prevent starvation or out of memory errors.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the specified object class represents an
     *                                            interface.
     */
    @NotNull
    public static ObjectRoutineBuilder on(@NotNull final InvocationTarget<?> target) {

        return JRoutineObject.on(target);
    }

    @NotNull
    public static WrapRoutineBuilder on(@NotNull final Object object) {

        final InvocationTarget<?> target;
        if (object instanceof Class) {
            target = classOfType((Class<?>) object);

        } else {
            target = instance(object);
        }

        return new DefaultWrapRoutineBuilder(target);
    }

    @NotNull
    public static <IN> RoutineBuilder<IN, IN> on(@NotNull final Predicate<? super IN> predicate) {

        return JRoutineCore.on(predicateFilter(predicate));
    }

    @NotNull
    public static <OUT> RoutineBuilder<Void, OUT> on(
            @NotNull final Supplier<? extends OUT> supplier) {

        return JRoutineCore.on(supplierCommand(supplier));
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation completion.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public static OutputConsumerBuilder<Object> onComplete(@NotNull final Consumer<Void> consumer) {

        return Functions.onComplete(consumer);
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation errors.
     *
     * @param consumer the consumer function.
     * @return the builder instance.
     */
    @NotNull
    public static OutputConsumerBuilder<Object> onError(
            @NotNull final Consumer<RoutineException> consumer) {

        return Functions.onError(consumer);
    }

    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> onFactory(
            @NotNull final Supplier<? extends Invocation<? super IN, ? extends OUT>> supplier) {

        return JRoutineCore.on(supplierFactory(supplier));
    }

    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> onFunction(
            @NotNull final BiConsumer<? super List<IN>, ? super ResultChannel<OUT>> consumer) {

        return JRoutineCore.on(consumerFactory(consumer));
    }

    @NotNull
    public static <IN, OUT> RoutineBuilder<IN, OUT> onFunction(
            @NotNull final Function<? super List<IN>, ? extends OUT> function) {

        return JRoutineCore.on(functionFactory(function));
    }

    /**
     * Returns an output consumer builder employing the specified consumer function to handle the
     * invocation outputs.
     *
     * @param consumer the consumer function.
     * @param <OUT>    the output data type.
     * @return the builder instance.
     */
    @NotNull
    public static <OUT> OutputConsumerBuilder<OUT> onOutput(@NotNull final Consumer<OUT> consumer) {

        return Functions.onOutput(consumer);
    }

    /**
     * Returns a routine builder wrapping the specified target object.<br/>
     * Note that it is responsibility of the caller to retain a strong reference to the target
     * instance to prevent it from being garbage collected.<br/>
     * Note also that the invocation input data will be cached, and the results will be produced
     * only after the invocation channel is closed, so be sure to avoid streaming inputs in order to
     * prevent starvation or out of memory errors.
     *
     * @param target the invocation target.
     * @return the routine builder instance.
     * @throws java.lang.IllegalArgumentException if the specified object class represents an
     *                                            interface.
     */
    @NotNull
    public static ProxyRoutineBuilder onProxied(@NotNull final InvocationTarget<?> target) {

        return JRoutineProxy.on(target);
    }
}
