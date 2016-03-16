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

import com.github.dm.jrt.core.invocation.ComparableInvocationFactory;
import com.github.dm.jrt.core.invocation.Invocation;
import com.github.dm.jrt.core.invocation.InvocationFactory;
import com.github.dm.jrt.core.util.ClassToken;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Abstract class defining a factory of context invocations.<br/>
 * The inheriting class must specify the constructor arguments to be used in the {@code equals()}
 * and {@code hashCode()} implementations. Note that such methods might be employed to uniquely
 * identify the loader backing the routine execution.
 * <p/>
 * Created by davide-maestroni on 05/01/2015.
 *
 * @param <IN>  the input data type.
 * @param <OUT> the output data type.
 */
public abstract class ContextInvocationFactory<IN, OUT> {

    private final Object[] mArgs;

    /**
     * Constructor.
     *
     * @param args the constructor arguments.
     */
    protected ContextInvocationFactory(@Nullable final Object[] args) {

        mArgs = (args != null) ? args.clone() : Reflection.NO_ARGS;
    }

    /**
     * Constructor.
     * <p/>
     * Forces the inheriting classes to explicitly pass the arguments.
     */
    @SuppressWarnings("unused")
    private ContextInvocationFactory() {

        this(null);
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
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass) {

        return factoryOf(invocationClass, (Object[]) null);
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
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass,
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
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> invocationToken) {

        return factoryOf(invocationToken.getRawClass());
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
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {

        return factoryOf(invocationToken.getRawClass(), args);
    }

    /**
     * Converts the specified context invocation factory into a factory of invocations.
     *
     * @param context the routine context.
     * @param factory the context invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> fromFactory(@NotNull final Context context,
            @NotNull final ContextInvocationFactory<IN, OUT> factory) {

        return new AdaptingContextInvocationFactory<IN, OUT>(context, factory);
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        final ContextInvocationFactory<?, ?> that = (ContextInvocationFactory<?, ?>) o;
        return Arrays.deepEquals(mArgs, that.mArgs);
    }

    @Override
    public int hashCode() {

        return 31 * getClass().hashCode() + Arrays.deepHashCode(mArgs);
    }

    /**
     * Creates and return a new context invocation instance.<br/>
     * A proper implementation will return a new invocation instance each time it is called, unless
     * the returned object is immutable and does not cause any side effect.<br/>
     * Any behavior other than that may lead to unexpected results.
     *
     * @return the context invocation instance.
     * @throws java.lang.Exception if an unexpected error occurs.
     */
    @NotNull
    public abstract ContextInvocation<IN, OUT> newInvocation() throws Exception;

    /**
     * Implementation of an invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class AdaptingContextInvocationFactory<IN, OUT>
            extends ComparableInvocationFactory<IN, OUT> {

        private final Context mContext;

        private final ContextInvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param context the context.
         * @param factory the context invocation factory.
         */
        @SuppressWarnings("ConstantConditions")
        private AdaptingContextInvocationFactory(@NotNull final Context context,
                @NotNull final ContextInvocationFactory<IN, OUT> factory) {

            super(asArgs(context, factory));
            if (context == null) {
                throw new NullPointerException("the routine context must not be null");
            }

            if (factory == null) {
                throw new NullPointerException("the context invocation factory must not be null");
            }

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

            super(asArgs(invocationClass, (args != null) ? args.clone() : Reflection.NO_ARGS));
            if (!Reflection.hasStaticScope(invocationClass)) {
                throw new IllegalArgumentException("the invocation class must have a static scope: "
                        + invocationClass.getName());
            }

            mFactory = InvocationFactory.factoryOf(
                    (Class<? extends Invocation<IN, OUT>>) invocationClass, args);
        }

        @NotNull
        public ContextInvocation<IN, OUT> newInvocation() throws Exception {

            return (ContextInvocation<IN, OUT>) mFactory.newInvocation();
        }
    }
}
