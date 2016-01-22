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

package com.github.dm.jrt.invocation;

import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Utility class for creating invocation factory objects.
 * <p/>
 * Created by davide-maestroni on 02/12/2015.
 */
public class Invocations {

    /**
     * Avoid direct instantiation.
     */
    protected Invocations() {

    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class.
     *
     * @param invocationClass the invocation class.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no default constructor was found.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass) {

        return factoryOf(invocationClass, (Object[]) null);
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class by
     * passing the specified arguments to the class constructor.
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
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
            @Nullable final Object... args) {

        return new DefaultInvocationFactory<IN, OUT>(invocationClass, args);
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class token.
     *
     * @param invocationToken the invocation class token.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no default constructor was found.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken) {

        return factoryOf(invocationToken.getRawClass());
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified class token
     * by passing the specified arguments to the class constructor.
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
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends Invocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {

        return factoryOf(invocationToken.getRawClass(), args);
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified object.
     *
     * @param invocation the invocation instance.
     * @param <IN>       the input data type.
     * @param <OUT>      the output data type.
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no default constructor was found.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryOf(
            @NotNull final Invocation<IN, OUT> invocation) {

        return factoryOf((Class<? extends Invocation<IN, OUT>>) invocation.getClass());
    }

    /**
     * Builds and returns a new invocation factory creating instances of the specified object by
     * passing the specified arguments to the class constructor.
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
     * @return the invocation factory.
     * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects as
     *                                            parameters was found.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <IN, OUT> InvocationFactory<IN, OUT> factoryOf(
            @NotNull final Invocation<IN, OUT> invocation, @Nullable final Object... args) {

        return factoryOf((Class<? extends Invocation<IN, OUT>>) invocation.getClass(), args);
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final Object[] mArgs;

        private final Constructor<? extends Invocation<IN, OUT>> mConstructor;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         * @throws java.lang.IllegalArgumentException if no constructor taking the specified objects
         *                                            as parameters was found.
         */
        @SuppressWarnings("ConstantConditions")
        private DefaultInvocationFactory(
                @NotNull final Class<? extends Invocation<IN, OUT>> invocationClass,
                @Nullable final Object[] args) {

            final Object[] invocationArgs =
                    (mArgs = (args != null) ? args.clone() : Reflection.NO_ARGS);
            mConstructor = Reflection.findConstructor(invocationClass, invocationArgs);
        }

        @Override
        public int hashCode() {

            int result = Arrays.deepHashCode(mArgs);
            result = 31 * result + mConstructor.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {
                return true;
            }

            if (!(o instanceof DefaultInvocationFactory)) {
                return false;
            }

            final DefaultInvocationFactory<?, ?> that = (DefaultInvocationFactory<?, ?>) o;
            return Arrays.deepEquals(mArgs, that.mArgs) && mConstructor.equals(that.mConstructor);
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            try {
                return mConstructor.newInstance(mArgs);

            } catch (final Throwable t) {
                throw InvocationException.wrapIfNeeded(t);
            }
        }
    }
}
