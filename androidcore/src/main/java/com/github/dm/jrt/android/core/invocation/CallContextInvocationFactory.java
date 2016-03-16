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

package com.github.dm.jrt.android.core.invocation;

import com.github.dm.jrt.core.channel.ResultChannel;
import com.github.dm.jrt.core.common.RoutineException;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract class defining a factory of call context invocations.
 * <p/>
 * Created by davide-maestroni on 10/06/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class CallContextInvocationFactory<IN, OUT>
        extends ContextInvocationFactory<IN, OUT> {

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected CallContextInvocationFactory(@Nullable final Object[] args) {

        super(args);
    }

    /**
     * Builds and returns a new context invocation factory wrapping the invocation created by the
     * specified factory.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified invocation factory
     *                                            has not a static scope.
     */
    // TODO: 16/03/16 unit tests
    @NotNull
    public static <IN, OUT> CallContextInvocationFactory<IN, OUT> callFactoryFrom(
            @NotNull final InvocationFactory<IN, OUT> factory) {

        return new AdaptingContextInvocationFactory<IN, OUT>(factory);
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified
     * class by passing the specified arguments to the class constructor.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation.
     *
     * @param invocationClass the invocation class.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
     *                                            static scope.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN, OUT> CallContextInvocationFactory<IN, OUT> callFactoryOf(
            @NotNull final Class<? extends CallContextInvocation<IN, OUT>> invocationClass,
            @Nullable final Object... args) {

        return new DefaultContextInvocationFactory<IN, OUT>(invocationClass, args);
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified class
     * token.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation.
     *
     * @param invocationToken the invocation class token.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> CallContextInvocationFactory<IN, OUT> callFactoryOf(
            @NotNull final ClassToken<? extends CallContextInvocation<IN, OUT>> invocationToken) {

        return callFactoryOf(invocationToken.getRawClass());
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified class
     * token by passing the specified arguments to the class constructor.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation.
     *
     * @param invocationToken the invocation class token.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> CallContextInvocationFactory<IN, OUT> callFactoryOf(
            @NotNull final ClassToken<? extends CallContextInvocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {

        return callFactoryOf(invocationToken.getRawClass(), args);
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified
     * class.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation.
     *
     * @param invocationClass the invocation class.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> CallContextInvocationFactory<IN, OUT> callFactoryOf(
            @NotNull final Class<? extends CallContextInvocation<IN, OUT>> invocationClass) {

        return callFactoryOf(invocationClass, (Object[]) null);
    }

    @NotNull
    @Override
    public abstract CallContextInvocation<IN, OUT> newInvocation() throws Exception;

    /**
     * Context invocation wrapping a base one.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AdaptingContextInvocation<IN, OUT> extends CallContextInvocation<IN, OUT> {

        private final Invocation<IN, OUT> mInvocation;

        /**
         * Constructor.
         *
         * @param invocation the wrapped invocation.
         */
        @SuppressWarnings("ConstantConditions")
        private AdaptingContextInvocation(@NotNull final Invocation<IN, OUT> invocation) {

            if (invocation == null) {
                throw new NullPointerException("the invocation instance must not be null");
            }

            mInvocation = invocation;
        }

        @Override
        public void onAbort(@NotNull final RoutineException reason) throws Exception {

            final Invocation<IN, OUT> invocation = mInvocation;
            invocation.onAbort(reason);
            invocation.onTerminate();
        }

        @Override
        public void onDestroy() throws Exception {

            mInvocation.onDestroy();
        }

        @Override
        public void onInitialize() throws Exception {

            mInvocation.onInitialize();
        }

        @Override
        protected void onCall(@NotNull final List<? extends IN> inputs,
                @NotNull final ResultChannel<OUT> result) throws Exception {

            final Invocation<IN, OUT> invocation = mInvocation;
            for (final IN input : inputs) {
                invocation.onInput(input, result);
            }

            invocation.onResult(result);
            invocation.onTerminate();
        }
    }

    /**
     * Context invocation factory adapting base invocations.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AdaptingContextInvocationFactory<IN, OUT>
            extends CallContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param factory the invocation factory.
         * @throws java.lang.IllegalArgumentException if the class of the specified invocation
         *                                            factory has not a static scope.
         */
        private AdaptingContextInvocationFactory(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            super(asArgs(factory));
            final Class<? extends InvocationFactory> factoryClass = factory.getClass();
            if (!Reflection.hasStaticScope(factoryClass)) {
                throw new IllegalArgumentException(
                        "the invocation factory class must have a static scope: "
                                + factoryClass.getName());
            }

            mFactory = factory;
        }

        @NotNull
        @Override
        public CallContextInvocation<IN, OUT> newInvocation() throws Exception {

            return new AdaptingContextInvocation<IN, OUT>(mFactory.newInvocation());
        }
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultContextInvocationFactory<IN, OUT>
            extends CallContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         * @throws java.lang.IllegalArgumentException if the class of the specified invocation has
         *                                            not a static scope.
         */
        private DefaultContextInvocationFactory(
                @NotNull final Class<? extends CallContextInvocation<IN, OUT>> invocationClass,
                @Nullable final Object[] args) {

            super(asArgs(invocationClass, (args != null) ? args.clone() : Reflection.NO_ARGS));
            if (!Reflection.hasStaticScope(invocationClass)) {
                throw new IllegalArgumentException("the invocation class must have a static scope: "
                        + invocationClass.getName());
            }

            mFactory = InvocationFactory.factoryOf(
                    (Class<? extends Invocation<IN, OUT>>) invocationClass, args);
        }

        @NotNull
        public CallContextInvocation<IN, OUT> newInvocation() throws Exception {

            return (CallContextInvocation<IN, OUT>) mFactory.newInvocation();
        }
    }
}
