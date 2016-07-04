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

package com.github.dm.jrt.function;

import com.github.dm.jrt.core.util.ConstantConditions;
import com.github.dm.jrt.core.util.DeepEqualObject;
import com.github.dm.jrt.core.util.Reflection;

import org.jetbrains.annotations.NotNull;

import static com.github.dm.jrt.core.util.Reflection.asArgs;

/**
 * Class decorating a supplier instance.
 * <p>
 * Created by davide-maestroni on 10/11/2015.
 *
 * @param <OUT> the output data type.
 */
public class SupplierDecorator<OUT> extends DeepEqualObject implements Supplier<OUT>, Decorator {

    private final FunctionDecorator<?, OUT> mFunction;

    private final Supplier<?> mSupplier;

    /**
     * Constructor.
     *
     * @param supplier the initial wrapped supplier.
     */
    private SupplierDecorator(@NotNull final Supplier<?> supplier) {
        this(ConstantConditions.notNull("supplier instance", supplier),
                FunctionDecorator.<OUT>identity());
    }

    /**
     * Constructor.
     *
     * @param supplier the initial wrapped supplier.
     * @param function the concatenated function chain.
     */
    private SupplierDecorator(@NotNull final Supplier<?> supplier,
            @NotNull final FunctionDecorator<?, OUT> function) {
        super(asArgs(supplier, function));
        mSupplier = supplier;
        mFunction = function;
    }

    /**
     * Returns a supplier decorator always returning the same result.
     * <br>
     * The returned object will support concatenation and comparison.
     *
     * @param result the result.
     * @param <OUT>  the output data type.
     * @return the supplier decorator.
     */
    @NotNull
    public static <OUT> SupplierDecorator<OUT> constant(final OUT result) {
        return new SupplierDecorator<OUT>(new ConstantSupplier<OUT>(result));
    }

    /**
     * Decorates the specified supplier instance so to provide additional features.
     * <br>
     * The returned object will support concatenation and comparison.
     * <p>
     * Note that the passed object is expected to have a functional behavior, that is, it must not
     * retain a mutable internal state.
     * <br>
     * Note also that any external object used inside the function must be synchronized in order to
     * avoid concurrency issues.
     *
     * @param supplier the supplier instance.
     * @param <OUT>    the output data type.
     * @return the decorated supplier.
     */
    @NotNull
    public static <OUT> SupplierDecorator<OUT> decorate(@NotNull final Supplier<OUT> supplier) {
        if (supplier instanceof SupplierDecorator) {
            return (SupplierDecorator<OUT>) supplier;
        }

        return new SupplierDecorator<OUT>(supplier);
    }

    /**
     * Returns a composed supplier decorator that first gets this supplier result, and then applies
     * the after function to it.
     *
     * @param after   the function to apply after this function is applied.
     * @param <AFTER> the type of output of the after function.
     * @return the composed function.
     */
    @NotNull
    public <AFTER> SupplierDecorator<AFTER> andThen(
            @NotNull final Function<? super OUT, ? extends AFTER> after) {
        return new SupplierDecorator<AFTER>(mSupplier, mFunction.andThen(after));
    }

    @SuppressWarnings("unchecked")
    public OUT get() throws Exception {
        return ((Function<Object, OUT>) mFunction).apply(mSupplier.get());
    }

    public boolean hasStaticScope() {
        return Reflection.hasStaticScope(mSupplier) && mFunction.hasStaticScope();
    }

    /**
     * Supplier implementation returning always the same object.
     *
     * @param <OUT> the output data type.
     */
    private static class ConstantSupplier<OUT> extends DeepEqualObject implements Supplier<OUT> {

        private final OUT mResult;

        /**
         * Constructor.
         *
         * @param result the object to return.
         */
        private ConstantSupplier(final OUT result) {
            super(asArgs(result));
            mResult = result;
        }

        public OUT get() {
            return mResult;
        }
    }
}
