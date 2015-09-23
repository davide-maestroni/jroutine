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

import android.content.Context;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Supplier;
import com.github.dm.jrt.invocation.Invocation;
import com.github.dm.jrt.invocation.InvocationFactory;
import com.github.dm.jrt.invocation.Invocations;
import com.github.dm.jrt.util.ClassToken;
import com.github.dm.jrt.util.Reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Utility class for creating context invocation factory objects.
 * <p/>
 * Created by davide-maestroni on 05/01/2015.
 */
public class ContextInvocations {

    /**
     * Avoid direct instantiation.
     */
    protected ContextInvocations() {

    }

    /**
     * Converts the specified invocation factory into a factory of context invocations.
     *
     * @param factory the invocation factory.
     * @param <IN>    the input data type.
     * @param <OUT>   the output data type.
     * @return the context invocation factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final InvocationFactory<IN, OUT> factory) {

        return new DecoratingContextInvocationFactory<IN, OUT>(factory);
    }

    /**
     * Builds and returns a new context invocation factory based on the specified supplier instance.
     * <br/>
     * In order to prevent undesired leaks, the class of the specified supplier must have a static
     * context.
     *
     * @param supplier the supplier instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryFrom(
            @NotNull final Supplier<? extends Invocation<IN, OUT>> supplier) {

        return factoryFrom(Invocations.factoryFrom(supplier));
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
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> factoryOf(
            @NotNull final ClassToken<? extends ContextInvocation<IN, OUT>> invocationToken,
            @Nullable final Object... args) {

        return factoryOf(invocationToken.getRawClass(), args);
    }

    /**
     * Builds and returns a new context invocation factory based on the specified bi-consumer
     * instance.<br/>
     * In order to prevent undesired leaks, the class of the specified bi-consumer must have a
     * static context.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> filterFrom(
            @NotNull final BiConsumer<? super IN, ? super ResultChannel<OUT>> consumer) {

        return factoryFrom(Invocations.filterFrom(consumer));
    }

    /**
     * Builds and returns a new context invocation factory based on the specified function instance.
     * <br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * context.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> filterFrom(
            @NotNull final Function<? super IN, ? extends OUT> function) {

        return factoryFrom(Invocations.filterFrom(function));
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

    /**
     * Builds and returns a new context invocation factory based on the specified bi-consumer
     * instance.<br/>
     * In order to prevent undesired leaks, the class of the specified bi-consumer must have a
     * static context.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> functionFrom(
            @NotNull final BiConsumer<? super List<? extends IN>, ? super ResultChannel<OUT>>
                    consumer) {

        return factoryFrom(Invocations.functionFrom(consumer));
    }

    /**
     * Builds and returns a new context invocation factory based on the specified function
     * instance.<br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * context.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> ContextInvocationFactory<IN, OUT> functionFrom(
            @NotNull final Function<? super List<? extends IN>, ? extends OUT> function) {

        return factoryFrom(Invocations.functionFrom(function));
    }

    /**
     * Builds and returns a new context invocation factory based on the specified consumer instance.
     * <br/>
     * In order to prevent undesired leaks, the class of the specified consumer must have a static
     * context.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <OUT> ContextInvocationFactory<Void, OUT> procedureFrom(
            @NotNull final Consumer<? super ResultChannel<OUT>> consumer) {

        return factoryFrom(Invocations.procedureFrom(consumer));
    }

    /**
     * Builds and returns a new context invocation factory based on the specified supplier instance.
     * <br/>
     * In order to prevent undesired leaks, the class of the specified supplier must have a static
     * context.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <OUT> ContextInvocationFactory<Void, OUT> procedureFrom(
            @NotNull final Supplier<? extends OUT> supplier) {

        return factoryFrom(Invocations.procedureFrom(supplier));
    }

    /**
     * Implementation of an invocation factory.
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
         * @param factory the context invocation class.
         */
        @SuppressWarnings("ConstantConditions")
        private AdaptingContextInvocationFactory(@NotNull final Context context,
                @NotNull final ContextInvocationFactory<IN, OUT> factory) {

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
        public Invocation<IN, OUT> newInvocation() {

            final ContextInvocation<IN, OUT> invocation = mFactory.newInvocation();
            invocation.onContext(mContext);
            return invocation;
        }
    }

    /**
     * Implementation of an invocation factory decorating base invocations.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DecoratingContextInvocationFactory<IN, OUT>
            extends AbstractContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param factory the invocation factory.
         */
        @SuppressWarnings("ConstantConditions")
        private DecoratingContextInvocationFactory(
                @NotNull final InvocationFactory<IN, OUT> factory) {

            super(factory);

            if (factory == null) {

                throw new NullPointerException("the invocation factory must not be null");
            }

            mFactory = factory;
        }

        @NotNull
        public ContextInvocation<IN, OUT> newInvocation() {

            return new ContextInvocationWrapper<IN, OUT>(mFactory.newInvocation());
        }
    }

    /**
     * Default implementation of an invocation factory.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class DefaultContextInvocationFactory<IN, OUT>
            extends AbstractContextInvocationFactory<IN, OUT> {

        private final InvocationFactory<IN, OUT> mFactory;

        /**
         * Constructor.
         *
         * @param invocationClass the invocation class.
         * @param args            the invocation constructor arguments.
         */
        private DefaultContextInvocationFactory(
                @NotNull final Class<? extends ContextInvocation<IN, OUT>> invocationClass,
                @Nullable final Object[] args) {

            super(invocationClass, (args != null) ? args.clone() : Reflection.NO_ARGS);
            mFactory = Invocations.factoryOf((Class<? extends Invocation<IN, OUT>>) invocationClass,
                                             args);
        }

        @NotNull
        public ContextInvocation<IN, OUT> newInvocation() {

            return (ContextInvocation<IN, OUT>) mFactory.newInvocation();
        }
    }
}
