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
package com.github.dm.jrt.invocation;

import com.github.dm.jrt.channel.ResultChannel;
import com.github.dm.jrt.function.BiConsumer;
import com.github.dm.jrt.function.Consumer;
import com.github.dm.jrt.function.Function;
import com.github.dm.jrt.function.Functions;
import com.github.dm.jrt.function.Supplier;
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
     * Builds and returns a new invocation factory based on the specified supplier instance.<br/>
     * In order to prevent undesired leaks, the class of the specified supplier must have a static
     * context.
     *
     * @param supplier the supplier instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> InvocationFactory<IN, OUT> factory(
            @NotNull final Supplier<? extends Invocation<IN, OUT>> supplier) {

        return new SupplierInvocationFactory<IN, OUT>(supplier);
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
     * Builds and returns a new filter invocation based on the specified bi-consumer instance.<br/>
     * In order to prevent undesired leaks, the class of the specified bi-consumer must have a
     * static context.
     *
     * @param consumer the bi-consumer instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FilterInvocation<IN, OUT> filter(
            @NotNull final BiConsumer<IN, ResultChannel<OUT>> consumer) {

        return new ConsumerFilterInvocation<IN, OUT>(consumer);
    }

    /**
     * Builds and returns a new filter invocation based on the specified function instance.<br/>
     * In order to prevent undesired leaks, the class of the specified function must have a static
     * context.
     *
     * @param function the function instance.
     * @param <IN>     the input data type.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <IN, OUT> FilterInvocation<IN, OUT> filter(
            @NotNull final Function<IN, ? extends OUT> function) {

        return new FunctionFilterInvocation<IN, OUT>(function);
    }

    /**
     * Builds and returns a new procedure invocation based on the specified consumer instance.<br/>
     * In order to prevent undesired leaks, the class of the specified consumer must have a static
     * context.
     *
     * @param consumer the consumer instance.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <OUT> ProcedureInvocation<OUT> procedure(
            @NotNull final Consumer<ResultChannel<OUT>> consumer) {

        return new ConsumerProcedureInvocation<OUT>(consumer);
    }

    /**
     * Builds and returns a new procedure invocation based on the specified supplier instance.<br/>
     * In order to prevent undesired leaks, the class of the specified supplier must have a static
     * context.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the invocation factory.
     */
    @NotNull
    public static <OUT> ProcedureInvocation<OUT> procedure(
            @NotNull final Supplier<? extends OUT> supplier) {

        return new SupplierProcedureInvocation<OUT>(supplier);
    }

    /**
     * Filter invocation based on a bi-consumer instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class ConsumerFilterInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

        private final BiConsumer<IN, ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        private ConsumerFilterInvocation(
                @NotNull final BiConsumer<IN, ResultChannel<OUT>> consumer) {

            final Class<? extends BiConsumer> consumerClass = consumer.getClass();

            if (((consumerClass == Functions.BiConsumer.class) && !((Functions.BiConsumer) consumer)
                    .hasStaticContext()) || !Reflection.hasStaticContext(consumerClass)) {

                throw new IllegalArgumentException(
                        "the bi-consumer class must be static: " + consumerClass);
            }

            mConsumer = consumer;
        }

        @Override
        public int hashCode() {

            return mConsumer.hashCode();
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            mConsumer.accept(input, result);
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ConsumerFilterInvocation)) {

                return false;
            }

            final ConsumerFilterInvocation<?, ?> that = (ConsumerFilterInvocation<?, ?>) o;
            return mConsumer.equals(that.mConsumer);
        }
    }

    /**
     * Procedure invocation based on a consumer instance.
     *
     * @param <OUT> the output data type.
     */
    private static class ConsumerProcedureInvocation<OUT> extends ProcedureInvocation<OUT> {

        private final Consumer<ResultChannel<OUT>> mConsumer;

        /**
         * Constructor.
         *
         * @param consumer the consumer instance.
         */
        public ConsumerProcedureInvocation(final Consumer<ResultChannel<OUT>> consumer) {

            final Class<? extends Consumer> consumerClass = consumer.getClass();

            if (((consumerClass == Functions.Consumer.class)
                    && !((Functions.Consumer) consumer).hasStaticContext())
                    || !Reflection.hasStaticContext(consumerClass)) {

                throw new IllegalArgumentException(
                        "the consumer class must be static: " + consumerClass);
            }

            mConsumer = consumer;
        }

        @Override
        public int hashCode() {

            return mConsumer.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof ConsumerProcedureInvocation)) {

                return false;
            }

            final ConsumerProcedureInvocation<?> that = (ConsumerProcedureInvocation<?>) o;
            return mConsumer.equals(that.mConsumer);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            mConsumer.accept(result);
        }
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

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            try {

                return mConstructor.newInstance(mArgs);

            } catch (final Throwable t) {

                throw InvocationException.wrapIfNeeded(t);
            }
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
    }

    /**
     * Filter invocation based on a function instance.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class FunctionFilterInvocation<IN, OUT> extends FilterInvocation<IN, OUT> {

        private final Function<IN, ? extends OUT> mFunction;

        /**
         * Constructor.
         *
         * @param function the function instance.
         */
        private FunctionFilterInvocation(@NotNull final Function<IN, ? extends OUT> function) {

            final Class<? extends Function> functionClass = function.getClass();

            if (((functionClass == Functions.Function.class)
                    && !((Functions.Function) function).hasStaticContext())
                    || !Reflection.hasStaticContext(functionClass)) {

                throw new IllegalArgumentException(
                        "the function class must be static: " + functionClass);
            }

            mFunction = function;
        }

        @Override
        public int hashCode() {

            return mFunction.hashCode();
        }

        public void onInput(final IN input, @NotNull final ResultChannel<OUT> result) {

            result.pass(mFunction.apply(input));
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof FunctionFilterInvocation)) {

                return false;
            }

            final FunctionFilterInvocation<?, ?> that = (FunctionFilterInvocation<?, ?>) o;
            return mFunction.equals(that.mFunction);
        }
    }

    /**
     * Implementation of an invocation factory based on a supplier function.
     *
     * @param <IN>  the input data type.
     * @param <OUT> the output data type.
     */
    private static class SupplierInvocationFactory<IN, OUT> extends InvocationFactory<IN, OUT> {

        private final Supplier<? extends Invocation<IN, OUT>> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier function.
         */
        private SupplierInvocationFactory(
                @NotNull final Supplier<? extends Invocation<IN, OUT>> supplier) {

            final Class<? extends Supplier> supplierClass = supplier.getClass();

            if (((supplierClass == Functions.Supplier.class)
                    && !((Functions.Supplier) supplier).hasStaticContext())
                    || !Reflection.hasStaticContext(supplierClass)) {

                throw new IllegalArgumentException(
                        "the supplier class must be static: " + supplierClass);
            }

            mSupplier = supplier;
        }

        @NotNull
        @Override
        public Invocation<IN, OUT> newInvocation() {

            return mSupplier.get();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof SupplierInvocationFactory)) {

                return false;
            }

            final SupplierInvocationFactory<?, ?> that = (SupplierInvocationFactory<?, ?>) o;
            return mSupplier.equals(that.mSupplier);
        }

        @Override
        public int hashCode() {

            return mSupplier.hashCode();
        }
    }

    /**
     * Procedure invocation based on a supplier instance.
     *
     * @param <OUT> the output data type.
     */
    private static class SupplierProcedureInvocation<OUT> extends ProcedureInvocation<OUT> {

        private final Supplier<? extends OUT> mSupplier;

        /**
         * Constructor.
         *
         * @param supplier the supplier instance.
         */
        public SupplierProcedureInvocation(final Supplier<? extends OUT> supplier) {

            final Class<? extends Supplier> supplierClass = supplier.getClass();

            if (((supplierClass == Functions.Supplier.class)
                    && !((Functions.Supplier) supplier).hasStaticContext())
                    || !Reflection.hasStaticContext(supplierClass)) {

                throw new IllegalArgumentException(
                        "the supplier class must be static: " + supplierClass);
            }

            mSupplier = supplier;
        }

        @Override
        public int hashCode() {

            return mSupplier.hashCode();
        }

        @Override
        public boolean equals(final Object o) {

            if (this == o) {

                return true;
            }

            if (!(o instanceof SupplierProcedureInvocation)) {

                return false;
            }

            final SupplierProcedureInvocation<?> that = (SupplierProcedureInvocation<?>) o;
            return mSupplier.equals(that.mSupplier);
        }

        public void onResult(@NotNull final ResultChannel<OUT> result) {

            result.pass(mSupplier.get());
        }
    }
}
