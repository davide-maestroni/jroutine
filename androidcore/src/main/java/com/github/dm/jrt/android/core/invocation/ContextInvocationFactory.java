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

import android.content.Context;

import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.core.util.Reflection.asArgs;
import static com.github.dm.jrt.core.util.Reflection.cloneArgs;

/**
 * Abstract class defining a factory of Context invocations.
 * <br>
 * The inheriting class must specify the constructor arguments to be used in the {@code equals()}
 * and {@code hashCode()} implementations. Note that such methods might be employed to uniquely
 * identify the Loader backing the routine execution.
 * <p>
 * Created by davide-maestroni on 05/01/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class ContextInvocationFactory<IN, OUT> extends DeepEqualObject {

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected ContextInvocationFactory(@Nullable final Object[] args) {
        super(args);
    }

    /**
     * Builds and returns a new Context invocation factory wrapping the specified one.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified factory has not a
     *                                            static scope.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final InvocationFactory<IN, OUT> factory) {
        return new WrappingContextInvocationFactory<IN, OUT>(factory);
    }

    /**
     * Builds and returns a new Context invocation factory creating instances of the specified
     * class.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation.
     *
     * @param invocationClass the invocation class.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
     *                                            static scope or no default construct is found.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass) {
        return factoryOf(invocationClass, (Object[]) null);
    }

    /**
     * Builds and returns a new Context invocation factory creating instances of the specified
     * class by passing the specified arguments to the class constructor.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
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
     *                                            static scope or no construct constructor taking
     *                                            the specified objects as parameters is found.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
            @Nullable final Object... args) {
        if (ContextInvocation.class.isAssignableFrom(invocationClass)) {
            return new DefaultContextInvocationFactory<IN, OUT>(
                    (Class<? extends ContextInvocation<IN, OUT>>) invocationClass, args);
        }

        return factoryFrom(InvocationFactory.factoryOf(invocationClass, args));
    }

    /**
     * Builds and returns a new Context invocation factory creating instances of the specified class
     * token.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation.
     *
     * @param invocationToken the invocation class token.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if the class of the specified invocation has not a
     *                                            static scope or no default construct is found.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken) {
        return factoryOf(invocationToken.getRawClass());
    }

    /**
     * Builds and returns a new Context invocation factory creating instances of the specified class
     * token by passing the specified arguments to the class constructor.
     * <br>
     * The method accepts also classes implementing {@link ContextInvocation}.
     * <p>
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
     *                                            static scope or no construct constructor taking
     *                                            the specified objects as parameters is found.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {
        return factoryOf(invocationToken.getRawClass(), args);
    }

    /**
     * Converts the specified Context invocation factory into a factory of invocations.
     *
     * @param context the routine Context.
     * @param factory the Context invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> fromFactory(@NotNull final Context context,
            @NotNull final ContextInvocationFactory<IN, OUT> factory) {
        return new AdaptingContextInvocationFactory<IN, OUT>(context, factory);
    }

    /**
     * Creates and return a new Context invocation instance.
     * <br>
     * A proper implementation will return a new invocation instance each time it is called, unless
     * the returned object is immutable and does not cause any side effect.
     * <br>
     * Any behavior other than that may lead to unexpected results.
     *
     * @return the Context invocation instance.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    public abstract ContextInvocation<IN, OUT> newInvocation() throws Exception;

    /**
     * Implementation of an invocation factory adapting Context invocations to be used as common
     * ones.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AdaptingContextInvocationFactory<IN, OUT>
            extends InvocationFactory<IN, OUT> {

        private final Context mContext;

        private final ContextInvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param context the Context.
         * @param factory the Context invocation factory.
         */
        private AdaptingContextInvocationFactory(@NotNull final Context context,
                @NotNull final ContextInvocationFactory<IN, OUT> factory) {
            super(asArgs(ConstantConditions.notNull("routine Context", context),
                    ConstantConditions.notNull("Context invocation factory", factory)));
            mContext = context;
            mFactory = factory;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() throws Exception {
            final ContextInvocation<IN, OUT> invocation = mFactory.newInvocation();
            invocation.onContext(mContext);
            return invocation;
        }
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation factory arguments.
         * @throws java.lang.IllegalArgumentException if the class of the specified invocation has
         *                                            not a static scope.
         */
        private DefaultContextInvocationFactory(
                @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass,
                @Nullable final Object[] args) {
            super(asArgs(invocationClass, cloneArgs(args)));
            if (!Reflection.hasStaticScope(invocationClass)) {
                throw new IllegalArgumentException("the invocation class must have a static scope: "
                        + invocationClass.getName());
            }

            mFactory = InvocationFactory.factoryOf(
                    (Class<? extends Invocation<IN, OUT>>) invocationClass, args);
        }

        @NotNull
        @Override
        public ContextInvocation<IN, OUT> newInvocation() throws Exception {
            return (ContextInvocation<IN, OUT>) mFactory.newInvocation();
        }
    }

    /**
     * Implementation of a Context invocation factory wrapping common invocations.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class WrappingContextInvocationFactory<IN, OUT>
            extends ContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param factory the invocation factory.
         */
        private WrappingContextInvocationFactory(
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
        public ContextInvocation<IN, OUT> newInvocation() throws Exception {
            return new ContextInvocationWrapper<IN, OUT>(mFactory.newInvocation());
        }
    }
}
