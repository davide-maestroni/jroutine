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
package com.github.dm.jrt.android.invocation;

import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.Invocations;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.dm.jrt.util.Reflection.asArgs;

/**
 * Utility class for creating function context invocation factory objects.
 * <p/>
 * Created by davide-maestroni on 10/06/2015.
 */
public class FunctionContextInvocations {

    /**
     * Avoid direct instantiation.
     */
    protected FunctionContextInvocations() {

    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified
     * class.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes has both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocationClass the invocation class.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FunctionContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends FunctionContextInvocation<IN, OUT>> invocationClass) {

        return factoryOf(invocationClass, (Object[]) null);
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified
     * class by passing the specified arguments to the class constructor.
     * <p/>
     * Note that inner and anonymous classes can be passed as well. Remember however that Java
     * creates synthetic constructors for such classes, so be sure to specify the correct arguments
     * to guarantee proper instantiation. In fact, inner classes always have the outer instance as
     * first constructor parameter, and anonymous classes has both the outer instance and all the
     * variables captured in the closure.
     *
     * @param invocationClass the invocation class.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FunctionContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final Class<? extends FunctionContextInvocation<IN, OUT>> invocationClass,
            @Nullable final Object... args) {

        return new DefaultContextInvocationFactory<IN, OUT>(invocationClass, args);
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified class
     * token.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes has both the outer instance
     * and all the variables captured in the closure.
     *
     * @param invocationToken the invocation class token.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FunctionContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends FunctionContextInvocation<IN, OUT>>
                    invocationToken) {

        return factoryOf(invocationToken.getRawClass());
    }

    /**
     * Builds and returns a new context invocation factory creating instances of the specified class
     * token by passing the specified arguments to the class constructor.
     * <p/>
     * Note that class tokens of inner and anonymous classes can be passed as well. Remember however
     * that Java creates synthetic constructors for such classes, so be sure to specify the correct
     * arguments to guarantee proper instantiation. In fact, inner classes always have the outer
     * instance as first constructor parameter, and anonymous classes has both the outer instance
     * and all the variables captured in the closure.
     *
     * @param invocationToken the invocation class token.
     * @param args            the invocation constructor arguments.
     * @param <IN>            the input data type.
     * @param <OUT>           the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FunctionContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends FunctionContextInvocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {

        return factoryOf(invocationToken.getRawClass(), args);
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultContextInvocationFactory<IN, OUT>
            extends FunctionContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         */
        private DefaultContextInvocationFactory(
                @NotNull final Class<? extends FunctionContextInvocation<IN, OUT>> invocationClass,
                @Nullable final Object[] args) {

            super(asArgs(invocationClass, (args != null) ? args.clone() : Reflection.NO_ARGS));
            mFactory = Invocations.factoryOf((Class<? extends Invocation<IN, OUT>>) invocationClass,
                                             args);
        }

        @NotNull
        public FunctionContextInvocation<IN, OUT> newInvocation() {

            return (FunctionContextInvocation<IN, OUT>) mFactory.newInvocation();
        }
    }
}
